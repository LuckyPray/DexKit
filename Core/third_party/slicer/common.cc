/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "slicer/common.h"

#include <stdio.h>
#include <stdlib.h>

#include <set>
#include <thread>
#include <utility>

#include "matcher_thread_cache_registry.h"

namespace slicer {

static void log_(const std::string& msg) {
  printf("%s", msg.c_str());
  fflush(stdout);
}

static logger_type log = log_;

void set_logger(logger_type new_logger) {
  log = new_logger;
}

// Helper for the default SLICER_CHECK() policy
void _checkFailed(const char* expr, int line, const char* file) {
  char buf[512];
  std::snprintf(buf, sizeof(buf),
                "\nSLICER_CHECK failed [%s] at %s:%d\n\n",
                expr, file, line);
  log(buf);
  std::abort();
}
void _checkFailedOp(const void* lhs, const void* rhs, const char* op, const char* suffix,
                    int line, const char* file) {
  char buf[512];
  std::snprintf(buf, sizeof(buf),
                "\nSLICER_CHECK_%s failed [%p %s %p] at %s:%d",
                suffix, lhs, op, rhs, file, line);
  log(buf);
  std::abort();
}
void _checkFailedOp(uint32_t lhs, uint32_t rhs, const char* op, const char* suffix,
                    int line, const char* file) {
  char buf[512];
  std::snprintf(buf, sizeof(buf),
                "\nSLICER_CHECK_%s failed [%u %s %u] at %s:%d",
                suffix, lhs, op, rhs, file, line);
  log(buf);
  std::abort();
}

namespace {

std::set<std::pair<int, const char*>>& get_weak_failures() {
  // Avoid direct non-trivial thread_local destruction on Windows DLL TLS
  // teardown. Native-owned worker-thread instances are released through the
  // shared registry when the pool shuts down; externally-owned threads keep the
  // small set alive until process exit.
  thread_local std::set<std::pair<int, const char*>>* weak_failures = nullptr;
  if (weak_failures == nullptr) {
    weak_failures = new std::set<std::pair<int, const char*>>();
    dexkit::RegisterMatcherThreadLocalCache(
        std::this_thread::get_id(),
        weak_failures,
        [](void* ptr) {
          delete reinterpret_cast<std::set<std::pair<int, const char*>>*>(ptr);
        });
  }
  return *weak_failures;
}

}  // namespace

// Helper for the default SLICER_WEAK_CHECK() policy
//
// TODO: implement a modal switch (abort/continue)
//
void _weakCheckFailed(const char* expr, int line, const char* file) {
  auto failure_id = std::make_pair(line, file);
  auto& weak_failures = get_weak_failures();
  if (weak_failures.find(failure_id) == weak_failures.end()) {
    printf("\nSLICER_WEAK_CHECK failed [%s] at %s:%d\n\n", expr, file, line);
    weak_failures.insert(failure_id);
  }
}

// Prints a formatted message and aborts
void _fatal(const char* format, ...) {
  va_list args;
  va_start(args, format);
  vprintf(format, args);
  va_end(args);
  abort();
}

} // namespace slicer
