package com.orange.downloader.listener;

import com.orange.downloader.m3u8.M3U8;
import com.orange.downloader.model.VideoTaskItem;

public interface IVideoInfoParseListener {

    void onM3U8FileParseSuccess(VideoTaskItem info, M3U8 m3u8);

    void onM3U8FileParseFailed(VideoTaskItem info, Throwable error);
}
