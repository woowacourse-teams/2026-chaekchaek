# AndroidX graphics-path 1.0.1 source at commit
# 8a05a22af450d589ef911d772a001a49dcb05b71. Built locally so Play receives symbols.
cmake_minimum_required(VERSION 3.22.1)
project("androidx.graphics.path")

set(VERSION_SCRIPT "${CMAKE_CURRENT_SOURCE_DIR}/libandroidx.graphics.path.map")

add_library(
    androidx.graphics.path
    SHARED
    Conic.cpp
    PathIterator.cpp
    pathway.cpp
)

target_link_options(
    androidx.graphics.path
    PRIVATE
    "-Wl,--version-script=${VERSION_SCRIPT},-z,max-page-size=16384"
)
