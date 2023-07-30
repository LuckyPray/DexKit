cmake_minimum_required(VERSION 3.4.1)
project(dex_kit)

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

add_library(${PROJECT_NAME}_static
        STATIC
        ${SLICER_SOURCES}
        ${DEXKIT_SRC_FILES}
)
target_include_directories(${PROJECT_NAME}_static
        PUBLIC
        dexkit/include
        dexkit/utils
        dexkit/schema
        third_party/slicer/export
        third_party/thread_pool
        third_party/acdat
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