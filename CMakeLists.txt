cmake_minimum_required(VERSION 3.4.1)
project(dex_kit)

set(CMAKE_CXX_STANDARD 20)

#set(ANDROID_ABI "arm64-v8a")
#set(ANDROID_ABI "armeabi-v7a")

set(DB_SOURCES
        slicer/bytecode_encoder.cc
        slicer/code_ir.cc
        slicer/common.cc
        slicer/control_flow_graph.cc
        slicer/debuginfo_encoder.cc
        slicer/dex_bytecode.cc
        slicer/dex_format.cc
        slicer/dex_ir.cc
        slicer/dex_ir_builder.cc
        slicer/dex_utf8.cc
        slicer/instrumentation.cc
        slicer/reader.cc
        slicer/tryblocks_encoder.cc
        slicer/writer.cc
        )

set(CFLAGS -flto -fvisibility=default)
set(CMAKE_CXX_FLAGS  "${CMAKE_CXX_FLAGS} -Wall -O3 -fPIC -Wshift-count-overflow")
set(CMAKE_C_FLAGS  "${CMAKE_C_FLAGS} -Wall -O3 -fPIC -Wshift-count-overflow")

add_library(${PROJECT_NAME}_static STATIC ${DB_SOURCES})
target_include_directories(${PROJECT_NAME}_static PUBLIC include)
target_link_libraries(${PROJECT_NAME}_static PUBLIC z)

set(EXECUTABLE ON)
if (DEFINED EXECUTABLE)
    link_libraries(z ${PROJECT_NAME}_static)
    add_executable(${PROJECT_NAME}
            main.cpp)
endif ()