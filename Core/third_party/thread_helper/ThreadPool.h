/** Credits to Jakob Progsch (@progschj)
 https://github.com/progschj/thread_pool
 Copyright (c) 2012 Jakob Progsch, Václav Zeman
 This software is provided 'as-is', without any express or implied
 warranty. In no event will the authors be held liable for any damages
 arising from the use of this software.
 Permission is granted to anyone to use this software for any purpose,
 including commercial applications, and to alter it and redistribute it
 freely, subject to the following restrictions:
 1. The origin of this software must not be misrepresented; you must not
 claim that you wrote the original software. If you use this software
 in a product, an acknowledgment in the product documentation would be
 appreciated but is not required.
 2. Altered source versions must be plainly marked as such, and must not be
 misrepresented as being the original software.
 3. This notice may not be removed or altered from any source
 distribution.
 Modified by teble at LuckyPray, 2022.
*/

#pragma once

#include <vector>
#include <queue>
#include <memory>
#include <thread>
#include <mutex>
#include <condition_variable>
#include <future>
#include <functional>
#include <stdexcept>

#include "ThreadVariable.h"

class ThreadPool {
public:
    explicit ThreadPool(size_t);

    template<class F, class... Args>
    auto enqueue(F &&f, Args &&... args)
    -> std::future<typename std::invoke_result<F, Args...>::type>;

    void skip_unexec_tasks() {
        std::unique_lock lock(this->queue_mutex);
        _skip_unexec_tasks = true;
    }

    ~ThreadPool();

private:
    // need to keep track of threads so we can join them
    std::vector<std::thread> workers;
    // the task queue
    std::queue<std::function<void()> > tasks;
    std::mutex init_lock;
    std::mutex wait_lock;
    std::condition_variable init_condition;
    std::atomic<int> ready_tasks = 0;
    // synchronization
    std::mutex queue_mutex;
    std::condition_variable condition;
    bool stop;
    bool _skip_unexec_tasks = false;
    std::vector<std::thread::id> _thread_ids;
};

// the constructor just launches some amount of workers
inline ThreadPool::ThreadPool(size_t threads)
        : stop(false) {
    for (size_t i = 0; i < threads; ++i)
        workers.emplace_back(
                [this, threads] {
                    {
                        std::unique_lock lock(this->init_lock);
                        this->_thread_ids.push_back(std::this_thread::get_id());
                    }
                    ThreadVariable::InitThreadVariableMap();
                    this->ready_tasks++;
                    if (this->ready_tasks == threads) {
                        this->init_condition.notify_all();
                    }
                    {
                        std::unique_lock lock(init_lock);
                        this->init_condition.wait(lock,
                                                [this, threads] { return this->ready_tasks == threads; });
                    }
                    for (;;) {
                        std::function<void()> task;

                        {
                            std::unique_lock lock(this->queue_mutex);
                            this->condition.wait(lock,
                                                 [this] { return this->stop || !this->tasks.empty(); });
                            if (this->stop && this->tasks.empty())
                                return;
                            task = std::move(this->tasks.front());
                            this->tasks.pop();
                        }

                        task();
                    }
                }
        );
}

// add new work item to the pool
template<class F, class... Args>
auto ThreadPool::enqueue(F &&f, Args &&... args)
-> std::future<typename std::invoke_result<F, Args...>::type> {
    using return_type = typename std::invoke_result<F, Args...>::type;

    auto task = std::make_shared<std::packaged_task<return_type()> >(
            [f = std::forward<F>(f), args = std::make_tuple(std::forward<Args>(args)...), this]() mutable {
                if (this->_skip_unexec_tasks) {
                    if constexpr (std::is_same_v<return_type, void>) return;
                    return return_type();
                }
                return std::apply(f, args);
            }
    );

    std::future<return_type> res = task->get_future();
    {
        std::unique_lock lock(queue_mutex);

        // don't allow enqueueing after stopping the pool
        if (stop)
            abort();
//            throw std::runtime_error("enqueue on stopped thread_pool");

        tasks.emplace([task]() {
            (*task)();
        });
    }
    condition.notify_one();
    return res;
}

// the destructor joins all threads
inline ThreadPool::~ThreadPool() {
    {
        std::unique_lock lock(queue_mutex);
        stop = true;
    }
    condition.notify_all();
    for (std::thread &worker: workers)
        worker.join();
    ThreadVariable::ClearThreadVariables(_thread_ids);
}
