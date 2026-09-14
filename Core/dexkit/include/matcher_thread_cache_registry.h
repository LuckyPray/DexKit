#pragma once

#include <cstdint>
#include <thread>

#define POINT_CASE(X) reinterpret_cast<std::uintptr_t>(X)

namespace dexkit {

void RegisterMatcherThreadLocalCache(
        std::thread::id thread_id,
        void *cache,
        void (*deleter)(void *)
);

// Call on the owning worker after its final task, before the thread exits.
void ReleaseCurrentThreadLocalCaches();

} // namespace dexkit
