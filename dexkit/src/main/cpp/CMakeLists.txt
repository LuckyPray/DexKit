cmake_minimum_required(VERSION 3.4.1...3.27)
project(dexkit)

set(CMAKE_CXX_STANDARD 20)

add_subdirectory(../../../../Core Core)

set(JAVA_AWT_LIBRARY NotNeeded)
set(JAVA_JVM_LIBRARY NotNeeded)
set(JAVA_AWT_INCLUDE_PATH NotNeeded)
find_package(JNI REQUIRED)
include_directories(${JAVA_INCLUDE_PATH})
include_directories(${JAVA_INCLUDE_PATH2})

message(STATUS "Disable dexkit log")
if (DISABLE_DEXKIT_LOG)
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -DNDEXKIT_LOG")
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -DNDEXKIT_LOG")
endif ()

add_library(${PROJECT_NAME} SHARED
        native-bridge.cpp)

if (WIN32)
    if (CMAKE_CXX_COMPILER_ID STREQUAL "GNU")
        target_link_options(${PROJECT_NAME} PRIVATE
                -static-libstdc++
                -static-libgcc
        )
    endif()
elseif (UNIX AND NOT APPLE)
    if(USE_MUSL_LIBC)
        if(DEFINED ENV{MUSL_PATH})
            set(MUSL_PATH $ENV{MUSL_PATH})
        else()
            set(MUSL_PATH "" CACHE PATH "Path to PIC musl install")
        endif()

        if(NOT EXISTS "${MUSL_PATH}/include/stdlib.h" OR NOT EXISTS "${MUSL_PATH}/lib/libc.a")
            message(FATAL_ERROR "MUSL_PATH=${MUSL_PATH} is illegal. Please check whether you have made install musl-pic.")
        endif()

        set_target_properties(${PROJECT_NAME} PROPERTIES
                SKIP_BUILD_RPATH TRUE
        )
        target_include_directories(${PROJECT_NAME}
                PRIVATE
                ${MUSL_PATH}/include
        )

        target_link_directories(${PROJECT_NAME}
                PRIVATE
                ${MUSL_PATH}/lib
        )

        target_compile_options(${PROJECT_NAME}
                PRIVATE
                -ffunction-sections
                -fdata-sections
                -flto
        )

        target_link_options(${PROJECT_NAME}
                PRIVATE
                -nodefaultlibs
                -Wl,--whole-archive -lc -Wl,--no-whole-archive
                -Wl,--gc-sections
                -Wl,--exclude-libs,ALL
                -Wl,--strip-all
                -flto
        )
    endif()
elseif (APPLE)
    set(CMAKE_OSX_DEPLOYMENT_TARGET "11.0" CACHE STRING "Minimum macOS deployment version" FORCE)
endif()

target_link_libraries(${PROJECT_NAME}
        dexkit_static
)