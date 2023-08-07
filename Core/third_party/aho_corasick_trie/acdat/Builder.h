#pragma once

#include <queue>
#include "AhoCorasickDoubleArrayTrie.h"

namespace acdat {

template<typename V>
class Builder {
private:
    State *rootState = new State();
    std::vector<bool> used;
    int allocSize = 0;
    int progress = 0;
    int nextCheckPos = 0;
    int keySize = 0;

    AhoCorasickDoubleArrayTrie <V> *mACTrie = nullptr;

    static int Fetch(State *parent, std::vector<std::pair<int, State *>> &siblings) {
        if (parent->IsAcceptable()) {
            auto *fakeNode = new State(-(parent->GetDepth() + 1));
            fakeNode->AddEmit(parent->GetLargestValueId());
            siblings.emplace_back(0, fakeNode);
        }
        for (auto &item: parent->GetSuccess()) {
            siblings.emplace_back(item.first + 1, item.second);
        }
        return (int) siblings.size();
    }

    void AddKeyword(std::string_view keyword, int index) {
        State *currentState = this->rootState;
        for (char i : keyword) {
            i = i >= 'A' && i <= 'Z' ? (i + 32) : i;
            currentState = currentState->AddState(i);
        }
        currentState->AddEmit(index);
        mACTrie->l[index] = keyword.size();
    }

    void AddAllKeyword(const std::vector<std::string_view> &keywords) {
        int i = 0;
        for (auto keyword: keywords) {
            AddKeyword(keyword, i++);
        }
    }

    void ConstructFailureStates() {
        mACTrie->fail.resize(mACTrie->size + 1);
        mACTrie->output.resize(mACTrie->size + 1);
        std::queue<State *> queue;

        // 第一步，将深度为1的节点的failure设为根节点
        for (State *depthOneState: this->rootState->GetStates()) {
            depthOneState->SetFailure(this->rootState, mACTrie->fail);
            queue.push(depthOneState);
            ConstructOutput(depthOneState);
        }

        // 第二步，为深度 > 1 的节点建立failure表，这是一个bfs
        while (!queue.empty()) {
            State *currentState = queue.front();
            queue.pop();

            for (auto &c: currentState->GetTransitions()) {
                State *targetState = currentState->Next(c);
                queue.push(targetState);

                State *traceFailureState = currentState->GetFailure();
                while (traceFailureState->Next(c) == nullptr) {
                    traceFailureState = traceFailureState->GetFailure();
                }
                State *newFailureState = traceFailureState->Next(c);
                targetState->SetFailure(newFailureState, mACTrie->fail);
                targetState->AddEmit(newFailureState->Emit());
                ConstructOutput(targetState);
            }
        }
    }

    void ConstructOutput(State *targetState) {
        std::set<int, std::greater<>> emit = targetState->Emit();
        if (emit.empty()) {
            return;
        }
        std::vector<int> vec;
        vec.reserve(emit.size());
        for (auto &item: emit) {
            vec.push_back(item);
        }
        mACTrie->output[targetState->GetIndex()] = vec;
    }

    void BuildDoubleArrayTrie(int siz) {
        this->progress = 0;
        this->keySize = siz;
        Resize(512 * 32);

        mACTrie->base[0] = 1;
        nextCheckPos = 0;

        State *rootNode = this->rootState;

        std::vector<std::pair<int, State *>> siblings;
        Fetch(rootNode, siblings);
        if (!siblings.empty()) {
            Insert(siblings);
        }
    }

    int Resize(int newSize) {
        mACTrie->base.resize(newSize);
        mACTrie->check.resize(newSize);
        used.resize(newSize);

        return allocSize = newSize;
    }

    void Insert(std::vector<std::pair<int, State *>> &firstSiblings) {
        std::queue<std::pair<int, std::vector<std::pair<int, State *>>>> siblingQueue;
        siblingQueue.emplace(-1, firstSiblings);
        while (!siblingQueue.empty()) {
            Insert(siblingQueue);
        }
    }

    void Insert(std::queue<std::pair<int, std::vector<std::pair<int, State *>>>> &siblingQueue) {
        std::pair<int, std::vector<std::pair<int, State *>>> tCurrent = siblingQueue.front();
        siblingQueue.pop();
        std::vector<std::pair<int, State *>> siblings = tCurrent.second;

        int begin = 0;
        int pos = std::max(siblings[0].first + 1, nextCheckPos) - 1;
        int nonzero_num = 0;
        int first = 0;

        if (allocSize <= pos) {
            Resize(pos + 1);
        }

        outer:
        while (true) {
            pos++;

            if (allocSize <= pos) {
                Resize(pos + 1);
            }

            if (mACTrie->check[pos] != 0) {
                nonzero_num++;
                continue;
            } else if (first == 0) {
                nextCheckPos = pos;
                first = 1;
            }

            // 当前位置离第一个兄弟节点的距离
            begin = pos - siblings[0].first;
            if (allocSize <= (begin + siblings[siblings.size() - 1].first)) {
                // progress can be zero // 防止progress产生除零错误
                double ll = std::max(1.05, 1.5 * keySize / (progress + 1));
                Resize((int) (allocSize * ll));
            }

            if (used[begin]) {
                continue;
            }

            for (int i = 1; i < siblings.size(); ++i) {
                if (mACTrie->check[begin + siblings[i].first] != 0) {
                    goto outer;
                }
            }

            break;
        }

        // -- Simple heuristics --
        // if the percentage of non-empty contents in check between the
        // index
        // 'next_check_pos' and 'check' is greater than some constant value
        // (e.g. 0.9),
        // new 'next_check_pos' index is written by 'check'.
        if (1.0 * nonzero_num / (pos - nextCheckPos + 1) >= 0.95) {
            nextCheckPos = pos;
        }
        used[begin] = true;

        mACTrie->size = std::max(mACTrie->size, begin + siblings[siblings.size() - 1].first + 1);

        for (auto &sibling: siblings) {
            mACTrie->check[begin + sibling.first] = begin;
        }

        for (auto &sibling: siblings) {
            std::vector<std::pair<int, State *>> newSiblings;
            newSiblings.reserve(rootState->GetSuccess().size());
            if (Fetch(sibling.second, newSiblings) == 0) {
                mACTrie->base[begin + sibling.first] = (-sibling.second->GetLargestValueId() - 1);
                progress++;
            } else {
                auto pair = std::pair<int, std::vector<std::pair<int, State *>>>(begin + sibling.first, newSiblings);
                siblingQueue.push(pair);
            }
            sibling.second->SetIndex(begin + sibling.first);
        }

        // Insert siblings
        int parentBaseIndex = tCurrent.first;
        if (parentBaseIndex != -1) {
            mACTrie->base[parentBaseIndex] = begin;
        }
    }

    /**
     * free the unnecessary memory
     */
    void LosWeight() {
        mACTrie->base.resize(mACTrie->size + 512);
        mACTrie->check.resize(mACTrie->size + 512);
    }

public:
    void Build(std::vector<std::pair<V, bool>> &keywords, AhoCorasickDoubleArrayTrie <V> *acdat) {
        this->mACTrie = acdat;
        std::map<std::string, int> index_map;
        std::vector<std::string_view> views;
        int index = 0;
        for (auto &[key, keyIgnore]: keywords) {
            std::string str(key);
            std::transform(str.begin(), str.end(), str.begin(), ::tolower);
            if (!index_map.contains(str)) {
                index_map[str] = index++;
                views.emplace_back(key);
                std::vector<std::pair<V, bool>> vec;
                vec.emplace_back(key, keyIgnore);
                mACTrie->v.emplace_back(vec);
            } else {
                int i = index_map[str];
                mACTrie->v[i].emplace_back(key, keyIgnore);
            }
        }
        mACTrie->l.resize(views.size());
        AddAllKeyword(views);
        BuildDoubleArrayTrie(views.size());
        ConstructFailureStates();
        rootState = nullptr;
        LosWeight();
    }

    void Build(std::vector<V> &keywords, AhoCorasickDoubleArrayTrie <V> *acdat) {
        std::vector<std::pair<V, bool>> vec;
        for (auto &key: keywords) {
            vec.emplace_back(key, false);
        }
        Build(vec, acdat);
    }
};

}