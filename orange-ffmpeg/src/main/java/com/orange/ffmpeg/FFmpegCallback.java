package com.orange.ffmpeg;

public interface FFmpegCallback {
    void onComplete(int code, String message);
}
