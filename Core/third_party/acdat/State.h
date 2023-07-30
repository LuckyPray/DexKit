#pragma once

#include <set>
#include <map>
#include <vector>

namespace acdat {


class State {
private:
    State *failure = nullptr;
    std::set<int, std::greater<>> emits;
    std::map<unsigned char, State *> success;
    int index = 0;
protected:
    const int depth;
public:
    State() : depth(0) {}

    explicit State(int depth) : depth(depth) {}

    [[nodiscard]] int getDepth() const {
        return depth;
    }

    void addEmit(int keyword) {
        if (keyword == -1) return;
        this->emits.insert(keyword);
    }

    int getLargestValueId() {
        if (emits.empty()) return -1;
        return *emits.begin();
    }

    void addEmit(const std::set<int, std::greater<>> &emitSet) {
        for (auto &i: emitSet) {
            addEmit(i);
        }
    }

    std::set<int, std::greater<>> emit() {
        return this->emits;
    }

    bool isAcceptable() {
        return this->depth > 0 && !this->emits.empty();
    }

    State *getFailure() {
        return this->failure;
    }

    void setFailure(State *failState, std::vector<int> &fail) {
        this->failure = failState;
        fail[index] = failState->index;
    }

private:
    State *next(unsigned char c, bool ignoreRootState) {
        auto it = this->success.find(c);
        State *nextState = it != this->success.end() ? (*it).second : nullptr;
        if (!ignoreRootState && nextState == nullptr && this->depth == 0) {
            nextState = this;
        }
        return nextState;
    }

public:
    State *next(unsigned char c) {
        return next(c, false);
    }

    State *nextStateIgnoreRootState(unsigned char c) {
        return next(c, true);
    }

    State *addState(unsigned char c) {
        State *nextState = nextStateIgnoreRootState(c);
        if (nextState == nullptr) {
            nextState = new State(this->depth + 1);
            this->success[c] = nextState;
        }
        return nextState;
    }

    std::vector<State *> getStates() {
        std::vector<State *> states;
        states.reserve(this->success.size());
        for (auto &i: this->success) {
            states.push_back(i.second);
        }
        return states;
    }

    std::vector<unsigned char> getTransitions() {
        std::vector<unsigned char> transitions;
        transitions.reserve(this->success.size());
        for (auto &i: this->success) {
            transitions.push_back(i.first);
        }
        return transitions;
    }

    std::map<unsigned char, State *> getSuccess() {
        return this->success;
    }

    [[nodiscard]] int getIndex() const {
        return this->index;
    }

    void setIndex(int idx) {
        this->index = idx;
    }
};

}
