cmake_minimum_required(VERSION 3.4.1)
project(dexkit)
option(DEXKIT_ENABLE_INTERNAL_METRICS "Compile internal metrics into core" OFF)
option(DEXKIT_ENABLE_INTERNAL_METRICS_API "Enable internal metrics JNI bridge" OFF)

if (DEXKIT_ENABLE_INTERNAL_METRICS_API)
    set(DEXKIT_ENABLE_INTERNAL_METRICS ON)
endif ()

find_package(cxx REQUIRED CONFIG)
link_libraries(cxx::cxx)

add_subdirectory(../Core Core)

add_library(${PROJECT_NAME} SHARED
        ../dexkit/src/main/cpp/native-bridge.cpp)

if (DEXKIT_ENABLE_INTERNAL_METRICS_API)
    target_sources(${PROJECT_NAME} PRIVATE ../dexkit/src/main/cpp/native-bridge-metrics.cpp)
endif ()

find_library(log-lib log)

target_link_libraries(${PROJECT_NAME}
        dexkit_static
        z
        ${log-lib})
