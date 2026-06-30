cmake_minimum_required(VERSION 3.10)
project(qjs C)
enable_language(CXX)
add_compile_definitions(CONFIG_VERSION="2024-01-12")
include_directories(${CMAKE_CURRENT_SOURCE_DIR})
include_directories(${CMAKE_CURRENT_SOURCE_DIR}/..)
add_executable(qjs 
    quickjs.c
    quickjs-libc.c
    cutils.c
    dtoa.c
    libregexp.c
    libunicode.c
    list.h
    repl.c
    qjs.c
    /Users/efremov.andrey65/wb/collection-1.5.0/native-test/IntSetBuiltins.cpp
)
set_target_properties(qjs PROPERTIES RUNTIME_OUTPUT_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR})
