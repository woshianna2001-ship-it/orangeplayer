package com.orange.downloader.merge;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import com.orange.downloader.common.DownloadConstants;
import com.orange.downloader.utils.LogUtils;

public final class MergeFeatureToggle {

    public static final String META_ENABLE_FFMPEG_MERGE = "orange.downloader.ffmpeg.merge.enabled";
    public static final String META_ENABLE_JAVA_FALLBACK = "orange.downloader.ffmpeg.merge.java_fallback.enabled";

    private static final String PREF_NAME = "orange_downloader_feature_flags";
    private static final String PREF_ENABLE_FFMPEG_MERGE = "enable_ffmpeg_merge";
    private static final String PREF_ENABLE_JAVA_FALLBACK = "enable_java_fallback";

    private static volatile boolean sInitialized = false;
    private static volatile boolean sEnableFfmpegMerge = true;
    private static volatile boolean sEnableJavaFallback = true;

    private MergeFeatureToggle() {
    }

    public static synchronized void initialize(Context context) {
        if (context == null || sInitialized) {
            return;
        }
        Context appContext = context.getApplicationContext();
        boolean defaultFfmpeg = readMetaBoolean(appContext, META_ENABLE_FFMPEG_MERGE, true);
        boolean defaultFallback = readMetaBoolean(appContext, META_ENABLE_JAVA_FALLBACK, true);

        SharedPreferences sp = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sEnableFfmpegMerge = sp.getBoolean(PREF_ENABLE_FFMPEG_MERGE, defaultFfmpeg);
        sEnableJavaFallback = sp.getBoolean(PREF_ENABLE_JAVA_FALLBACK, defaultFallback);
        sInitialized = true;

        LogUtils.i(DownloadConstants.TAG, "[MERGE_FLAG] init ffmpeg=" + sEnableFfmpegMerge + ", fallback=" + sEnableJavaFallback);
    }

    public static boolean isFfmpegMergeEnabled() {
        return sEnableFfmpegMerge;
    }

    public static boolean isJavaFallbackEnabled() {
        return sEnableJavaFallback;
    }

    public static synchronized void setFfmpegMergeEnabled(Context context, boolean enabled) {
        if (context == null) {
            return;
        }
        initialize(context);
        sEnableFfmpegMerge = enabled;
        persist(context, PREF_ENABLE_FFMPEG_MERGE, enabled);
        LogUtils.i(DownloadConstants.TAG, "[MERGE_FLAG] ffmpeg enabled=" + enabled);
    }

    public static synchronized void setJavaFallbackEnabled(Context context, boolean enabled) {
        if (context == null) {
            return;
        }
        initialize(context);
        sEnableJavaFallback = enabled;
        persist(context, PREF_ENABLE_JAVA_FALLBACK, enabled);
        LogUtils.i(DownloadConstants.TAG, "[MERGE_FLAG] java fallback enabled=" + enabled);
    }

    private static void persist(Context context, String key, boolean value) {
        Context appContext = context.getApplicationContext();
        appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(key, value)
                .apply();
    }

    private static boolean readMetaBoolean(Context context, String key, boolean defaultValue) {
        try {
            ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(
                    context.getPackageName(),
                    PackageManager.GET_META_DATA
            );
            Bundle metaData = appInfo.metaData;
            if (metaData == null || !metaData.containsKey(key)) {
                return defaultValue;
            }
            return metaData.getBoolean(key, defaultValue);
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
