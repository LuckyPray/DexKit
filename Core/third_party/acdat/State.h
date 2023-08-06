#pragma once

#include <set>
#include <map>
#include <cstdint>
#include <vector>

namespace acdat {


class State {
private:
    State *failure = nullptr;
    std::set<int, std::greater<>> emits;
    std::map<uint8_t, State *> success;
    int index = 0;
protected:
    const int depth;
public:
    State() : depth(0) {}

    explicit State(int depth) : depth(depth) {}

    [[nodiscard]] int GetDepth() const {
        return depth;
    }

    void AddEmit(int keyword) {
        if (keyword == -1) return;
        this->emits.insert(keyword);
    }

    int GetLargestValueId() {
        if (emits.empty()) return -1;
        return *emits.begin();
    }

    void AddEmit(const std::set<int, std::greater<>> &emitSet) {
        for (auto &i: emitSet) {
            AddEmit(i);
        }
    }

    std::set<int, std::greater<>> Emit() {
        return this->emits;
    }

    bool IsAcceptable() {
        return this->depth > 0 && !this->emits.empty();
    }

    State *GetFailure() {
        return this->failure;
    }

    void SetFailure(State *failState, std::vector<int> &fail) {
        this->failure = failState;
        fail[index] = failState->index;
    }

private:
    State *Next(uint8_t ch, bool ignoreRootState) {
        auto it = this->success.find(ch);
        State *nextState = it != this->success.end() ? (*it).second : nullptr;
        if (!ignoreRootState && nextState == nullptr && this->depth == 0) {
            nextState = this;
        }
        return nextState;
    }

public:
    State *Next(uint8_t ch) {
        return Next(ch, false);
    }

    State *NextStateIgnoreRootState(uint8_t ch) {
        return Next(ch, true);
    }

    State *AddState(uint8_t ch) {
        State *nextState = NextStateIgnoreRootState(ch);
        if (nextState == nullptr) {
            nextState = new State(this->depth + 1);
            this->success[ch] = nextState;
        }
        return nextState;
    }

    std::vector<State *> GetStates() {
        std::vector<State *> states;
        states.reserve(this->success.size());
        for (auto &i: this->success) {
            states.push_back(i.second);
        }
        return states;
    }

    std::vector<uint8_t> GetTransitions() {
        std::vector<uint8_t> transitions;
        transitions.reserve(this->success.size());
        for (auto &i: this->success) {
            transitions.push_back(i.first);
        }
        return transitions;
    }

    std::map<uint8_t, State *> GetSuccess() {
        return this->success;
    }

    [[nodiscard]] int GetIndex() const {
        return this->index;
    }

    void SetIndex(int idx) {
        this->index = idx;
    }
};

}
