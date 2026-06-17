cmake_minimum_required(VERSION 3.22.1)
project(orangeffmpegkit C)

set(FFMPEG_THIRD_PARTY_DIR ${CMAKE_CURRENT_SOURCE_DIR}/third_party/ffmpeg/${ANDROID_ABI})
set(FFMPEG_INCLUDE_DIR ${FFMPEG_THIRD_PARTY_DIR}/include)
set(FFMPEG_LIB_DIR ${FFMPEG_THIRD_PARTY_DIR}/lib)

set(REQUIRED_FFMPEG_ARCHIVES
        libavcodec.a
        libavformat.a
        libavutil.a
        libswresample.a
        libswscale.a)

foreach(archive_file ${REQUIRED_FFMPEG_ARCHIVES})
    if(NOT EXISTS ${FFMPEG_LIB_DIR}/${archive_file})
        message(FATAL_ERROR "[orange-ffmpeg] missing ${FFMPEG_LIB_DIR}/${archive_file}. Please run scripts/build_ffmpeg.sh and copy static outputs into orange-ffmpeg/src/main/cpp/third_party/ffmpeg/${ANDROID_ABI}/lib")
    endif()
endforeach()

if(NOT EXISTS ${FFMPEG_INCLUDE_DIR}/libavformat/avformat.h)
    message(FATAL_ERROR "[orange-ffmpeg] missing FFmpeg headers in ${FFMPEG_INCLUDE_DIR}. Please copy include/ from custom FFmpeg build output.")
endif()

function(import_static_archive target_name file_name)
    add_library(${target_name} STATIC IMPORTED)
    set_target_properties(${target_name} PROPERTIES IMPORTED_LOCATION ${FFMPEG_LIB_DIR}/${file_name})
endfunction()

import_static_archive(ffmpeg_avcodec libavcodec.a)
import_static_archive(ffmpeg_avformat libavformat.a)
import_static_archive(ffmpeg_avutil libavutil.a)
import_static_archive(ffmpeg_swresample libswresample.a)
import_static_archive(ffmpeg_swscale libswscale.a)

set(OPENSSL_LINK_LIBS)
set(HAS_SSL OFF)
set(HAS_CRYPTO OFF)

if(EXISTS ${FFMPEG_LIB_DIR}/libssl.a)
    set(HAS_SSL ON)
endif()
if(EXISTS ${FFMPEG_LIB_DIR}/libcrypto.a)
    set(HAS_CRYPTO ON)
endif()

if(HAS_SSL AND HAS_CRYPTO)
    import_static_archive(ffmpeg_ssl libssl.a)
    import_static_archive(ffmpeg_crypto libcrypto.a)
    list(APPEND OPENSSL_LINK_LIBS ffmpeg_ssl ffmpeg_crypto)
elseif(HAS_SSL OR HAS_CRYPTO)
    message(FATAL_ERROR "[orange-ffmpeg] found only one of libssl.a/libcrypto.a in ${FFMPEG_LIB_DIR}; both are required together.")
endif()

add_library(
        orangeffmpegkit
        SHARED
        orange_ffmpegkit_jni.c)

find_library(log-lib log)
find_library(android-lib android)
find_library(z-lib z)
find_library(m-lib m)
find_library(dl-lib dl)

if(NOT z-lib)
    set(z-lib z)
endif()
if(NOT m-lib)
    set(m-lib m)
endif()
if(NOT dl-lib)
    set(dl-lib dl)
endif()

target_include_directories(
        orangeffmpegkit
        PRIVATE
        ${FFMPEG_INCLUDE_DIR})

target_link_libraries(
        orangeffmpegkit
        -Wl,--start-group
        ffmpeg_avformat
        ffmpeg_avcodec
        ffmpeg_swresample
        ffmpeg_swscale
        ffmpeg_avutil
        ${OPENSSL_LINK_LIBS}
        -Wl,--end-group
        ${log-lib}
        ${android-lib}
        ${z-lib}
        ${m-lib}
        ${dl-lib})
