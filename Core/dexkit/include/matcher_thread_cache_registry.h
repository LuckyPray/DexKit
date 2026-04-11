#pragma once

#include <cstdint>
#include <thread>
#include <vector>

#define POINT_CASE(X) reinterpret_cast<std::uintptr_t>(X)

namespace dexkit {

void RegisterMatcherThreadLocalCache(
        std::thread::id thread_id,
        void *cache,
        void (*deleter)(void *)
);

void ReleaseMatcherThreadLocalCaches(const std::vector<std::thread::id> &thread_ids);

} // namespace dexkit
