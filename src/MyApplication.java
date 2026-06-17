package com.uaoanlao.m3u8down;
import android.content.Context;
import android.app.Application;

import com.orange.downloader.common.DownloadConstants;
import com.orange.downloader.VideoDownloadConfig;
import com.orange.downloader.VideoDownloadManager;
import com.orange.downloader.utils.VideoStorageUtils;

import java.io.File;

public class MyApplication extends Application {
    
    private static Context mApplicationContext;

    @Override
    public void onCreate() {
        super.onCreate();
        mApplicationContext = getApplicationContext();
        com.kongzue.dbv3.DB.init(this,"m3u8", true);     //初始化数据库
        File file = VideoStorageUtils.getVideoCacheDir(this);
        if (!file.exists()) {
            file.mkdir();
        }
        VideoDownloadConfig config = new VideoDownloadManager.Build(this)
                .setCacheRoot(file.getAbsolutePath())
                .setTimeOut(DownloadConstants.READ_TIMEOUT, DownloadConstants.CONN_TIMEOUT)
                .setConcurrentCount(DownloadConstants.CONCURRENT)
                .setIgnoreCertErrors(true)  //忽略所有证书错误
                .setShouldM3U8Merged(true)  //设置下载完成是否自动合并成mp4
                .buildConfig();
        VideoDownloadManager.getInstance().initConfig(config);
    }
    
    public static Context getContext() {
        return mApplicationContext;
    }
}



//QQ 1823565614 七桐
//QQ 1823565614 七桐
//QQ交流群598138277
/*
github地址 https://github.com/JeffMony/VideoDownloader.git
更多方法自行去github查看
移植到项目请注意看清单文件的代码
*/
