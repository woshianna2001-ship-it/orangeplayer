package com.orange.playerlibrary.download;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

/**
 * 下载通知管理器
 * 负责显示和更新下载进度通知
 */
public class DownloadNotification {
    
    private static final String TAG = "DownloadNotification";
    private static final String CHANNEL_ID = "orange_download_channel";
    private static final String CHANNEL_NAME = "视频下载";
    private static final int NOTIFICATION_ID_BASE = 10000;
    
    private Context mContext;
    private NotificationManager mNotificationManager;
    private NotificationCompat.Builder mBuilder;
    
    public DownloadNotification(Context context) {
        mContext = context.getApplicationContext();
        mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel();
    }
    
    /**
     * 创建通知渠道（Android 8.0+）
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("视频下载进度通知");
            channel.setShowBadge(false);
            channel.enableLights(false);
            channel.enableVibration(false);
            mNotificationManager.createNotificationChannel(channel);
        }
    }
    
    /**
     * 显示下载开始通知
     */
    public void showDownloadStart(DownloadTask task) {
        int notificationId = getNotificationId(task.getTaskId());
        
        mBuilder = new NotificationCompat.Builder(mContext, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle(task.getTitle())
                .setContentText("准备下载...")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setAutoCancel(false);
        
        mNotificationManager.notify(notificationId, mBuilder.build());
    }
    
    /**
     * 更新下载进度
     */
    public void updateDownloadProgress(DownloadTask task) {
        int notificationId = getNotificationId(task.getTaskId());
        
        if (mBuilder == null) {
            mBuilder = new NotificationCompat.Builder(mContext, CHANNEL_ID);
        }
        
        String contentText = String.format("已下载 %s / %s (%d%%) - %s",
                task.getFormattedDownloadedSize(),
                task.getFormattedTotalSize(),
                task.getProgress(),
                task.getFormattedSpeed());
        
        mBuilder.setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle(task.getTitle())
                .setContentText(contentText)
                .setProgress(100, task.getProgress(), false)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setAutoCancel(false);
        
        mNotificationManager.notify(notificationId, mBuilder.build());
    }
    
    /**
     * 显示下载完成通知
     */
    public void showDownloadCompleted(DownloadTask task) {
        int notificationId = getNotificationId(task.getTaskId());
        
        mBuilder = new NotificationCompat.Builder(mContext, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle(task.getTitle())
                .setContentText("下载完成 - " + task.getFormattedTotalSize())
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(false)
                .setAutoCancel(true);
        
        mNotificationManager.notify(notificationId, mBuilder.build());
    }
    
    /**
     * 显示下载失败通知
     */
    public void showDownloadFailed(DownloadTask task) {
        int notificationId = getNotificationId(task.getTaskId());
        
        String errorText = "下载失败";
        if (task.getErrorMessage() != null && !task.getErrorMessage().isEmpty()) {
            errorText += ": " + task.getErrorMessage();
        }
        
        mBuilder = new NotificationCompat.Builder(mContext, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setContentTitle(task.getTitle())
                .setContentText(errorText)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(false)
                .setAutoCancel(true);
        
        mNotificationManager.notify(notificationId, mBuilder.build());
    }
    
    /**
     * 显示下载暂停通知
     */
    public void showDownloadPaused(DownloadTask task) {
        int notificationId = getNotificationId(task.getTaskId());
        
        String contentText = String.format("已暂停 - %d%% (%s / %s)",
                task.getProgress(),
                task.getFormattedDownloadedSize(),
                task.getFormattedTotalSize());
        
        mBuilder = new NotificationCompat.Builder(mContext, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_media_pause)
                .setContentTitle(task.getTitle())
                .setContentText(contentText)
                .setProgress(100, task.getProgress(), false)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(false)
                .setAutoCancel(true);
        
        mNotificationManager.notify(notificationId, mBuilder.build());
    }
    
    /**
     * 取消通知
     */
    public void cancelNotification(String taskId) {
        int notificationId = getNotificationId(taskId);
        mNotificationManager.cancel(notificationId);
    }
    
    /**
     * 取消所有通知
     */
    public void cancelAllNotifications() {
        mNotificationManager.cancelAll();
    }
    
    /**
     * 根据任务ID生成通知ID
     */
    private int getNotificationId(String taskId) {
        return NOTIFICATION_ID_BASE + Math.abs(taskId.hashCode() % 10000);
    }
    
    /**
     * 创建前台服务通知
     */
    public Notification createForegroundNotification() {
        return new NotificationCompat.Builder(mContext, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle("视频下载服务")
                .setContentText("正在后台下载视频...")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setAutoCancel(false)
                .build();
    }
}
