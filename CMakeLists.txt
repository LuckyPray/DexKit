cmake_minimum_required(VERSION 3.4.1)
project(dex_kit)

find_program(CCACHE ccache)

if (CCACHE)
    set(CMAKE_CXX_COMPILER_LAUNCHER ${CCACHE})
    set(CMAKE_C_COMPILER_LAUNCHER ${CCACHE})
endif ()

set(CMAKE_CXX_STANDARD 17)

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
        dex_kit.cc)

set(CFLAGS -flto -fvisibility=default)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wall -O3 -fPIC -Wdeprecated-declarations")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wall -O3 -fPIC -Wdeprecated-declarations")

add_library(${PROJECT_NAME}_static STATIC ${DB_SOURCES})
target_include_directories(${PROJECT_NAME}_static PUBLIC include)
target_link_libraries(${PROJECT_NAME}_static PUBLIC z)

#link_libraries(z ${PROJECT_NAME}_static)
#add_executable(${PROJECT_NAME}
#        main.cpp
#        )