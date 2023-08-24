cmake_minimum_required(VERSION 3.4.1)
project(dexkit)

find_program(CCACHE ccache)

if (CCACHE)
    set(CMAKE_CXX_COMPILER_LAUNCHER ${CCACHE})
    set(CMAKE_C_COMPILER_LAUNCHER ${CCACHE})
endif ()

set(CMAKE_CXX_STANDARD 20)

file(GLOB_RECURSE SLICER_SRC_FILES third_party/slicer/*.cc)
file(GLOB_RECURSE DEXKIT_SRC_FILES dexkit/*.cpp)

set(SLICER_SOURCES
        ${SLICER_SRC_FILES}
)

set(COMPILE_FLAGS "-Wall -O3 -fPIC -Wno-empty-body -Wno-unused-private-field \
    -Wno-unused-value -Wno-unused-variable -Wno-unused-function")
set(CMAKE_C_FLAGS "${CMAKE_CXX_FLAGS} ${COMPILE_FLAGS}")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} ${COMPILE_FLAGS}")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -pthread")

add_library(${PROJECT_NAME}_static
        STATIC
        ${SLICER_SOURCES}
        ${DEXKIT_SRC_FILES}
)
target_include_directories(${PROJECT_NAME}_static
        PUBLIC
        dexkit/include
        third_party/slicer/export
        third_party/thread_helper
        third_party/aho_corasick_trie
        third_party/parallel_hashmap
        third_party/flatbuffers/include
)
target_link_libraries(${PROJECT_NAME}_static
        PUBLIC
        z
)

option(EXECUTABLE_TEST "execute test cpp" OFF)
if (EXECUTABLE_TEST)
    link_libraries(z ${PROJECT_NAME}_static)
    add_executable(${PROJECT_NAME}
            main.cpp
            )
endif ()