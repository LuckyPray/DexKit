cmake_minimum_required(VERSION 3.10)
project(dexkit)

set(CMAKE_CXX_STANDARD 17)

add_subdirectory(../../../../Core Core)

set(JAVA_AWT_LIBRARY NotNeeded)
set(JAVA_JVM_LIBRARY NotNeeded)
set(JAVA_AWT_INCLUDE_PATH NotNeeded)
find_package(JNI REQUIRED)
include_directories(${JAVA_INCLUDE_PATH})
include_directories(${JAVA_INCLUDE_PATH2})

include_directories(include)

message(STATUS "Disable dexkit log")
if (DISABLE_DEXKIT_LOG)
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -DNDEXKIT_LOG")
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -DNDEXKIT_LOG")
endif ()

add_library(${PROJECT_NAME} SHARED
        native-bridge.cpp)

target_link_libraries(${PROJECT_NAME}
        dex_kit_static)
