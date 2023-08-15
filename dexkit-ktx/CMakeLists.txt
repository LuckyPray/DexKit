cmake_minimum_required(VERSION 3.4.1)
project(dexkit)

find_package(cxx REQUIRED CONFIG)
link_libraries(cxx::cxx)

add_subdirectory(../Core Core)

add_library(${PROJECT_NAME} SHARED
        ../dexkit-lib/src/main/cpp/native-bridge.cpp)

include_directories(
        ../Core/dexkit/include
        ../Core/dexkit/utils
        ../Core/third_party/slicer/export
        ../Core/third_party/thread_helper
        ../Core/third_party/aho_corasick_trie
        ../Core/third_party/parallel_hashmap
        ../Core/third_party/flatbuffers/include
)

find_library(log-lib log)

target_link_libraries(${PROJECT_NAME}
        dexkit_static
        z
        ${log-lib})
