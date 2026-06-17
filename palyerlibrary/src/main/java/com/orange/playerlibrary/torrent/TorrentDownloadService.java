package com.orange.playerlibrary.torrent;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

/**
 * 磁力下载前台服务
 * 用于保护 libtorrent 下载进程不被系统杀死
 */
public class TorrentDownloadService extends Service {
    
    private static final String CHANNEL_ID = "orange_torrent_download";
    private static final int NOTIFICATION_ID = 1002;
    
    public static void start(Context context) {
        if (context == null) {
            return;
        }
        Intent intent = new Intent(context, TorrentDownloadService.class);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent);
            } else {
                context.startService(intent);
            }
        } catch (Exception e) {
            android.util.Log.e("TorrentDownloadService", "Failed to start service", e);
        }
    }
    
    public static void stop(Context context) {
        if (context == null) {
            return;
        }
        Intent intent = new Intent(context, TorrentDownloadService.class);
        context.stopService(intent);
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, createNotification());
        return START_STICKY;
    }
    
    @Override
    public void onDestroy() {
        stopForeground(true);
        super.onDestroy();
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    private Notification createNotification() {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, 
                "磁力下载", 
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("显示磁力链接下载进度");
            channel.setShowBadge(false);
            manager.createNotificationChannel(channel);
        }
        
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle("磁力下载进行中")
                .setContentText("正在缓冲视频数据...")
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }
}
