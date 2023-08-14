#pragma once

#include <map>
#include <shared_mutex>

class ThreadVariable {
public:
    template<typename T, typename Arg>
    static void SetThreadVariable(uint32_t key, Arg &&arg) {
        auto shared_ptr = std::make_shared<T>(std::forward<Arg>(arg));
        std::unique_lock<std::shared_mutex> lock(_lock);
        _thread_variables[std::this_thread::get_id()][key] = shared_ptr;
    }

    template<typename T>
    static std::shared_ptr<T> GetThreadVariable(uint32_t key) {
        auto thread_id = std::this_thread::get_id();
        std::shared_lock<std::shared_mutex> lock(_lock);
        if (_thread_variables.find(thread_id) != _thread_variables.end()) {
            auto &map = _thread_variables[thread_id];
            if (map.find(key) != map.end()) {
                auto &shared_ptr = map[key];
                return *reinterpret_cast<std::shared_ptr<T> *>(&shared_ptr);
            }
        }
        return nullptr;
    }

    static void ClearThreadVariables() {
        auto thread_id = std::this_thread::get_id();
        std::unique_lock<std::shared_mutex> lock(_lock);
        if (_thread_variables.find(thread_id) != _thread_variables.end()) {
            _thread_variables.erase(thread_id);
        }
    }

private:
    inline static std::shared_mutex _lock;
    inline static std::map<std::thread::id, std::map<uint32_t, std::shared_ptr<void>>> _thread_variables;
};