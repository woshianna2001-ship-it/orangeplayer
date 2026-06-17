package com.orange.playerlibrary.speech;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioPlaybackCaptureConfiguration;
import android.media.AudioRecord;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

/**
 * 音频捕获服务
 * 使用 AudioPlaybackCapture API 捕获应用播放的音频
 * 需要 Android 10 (API 29) 以上
 */
@RequiresApi(api = Build.VERSION_CODES.Q)
public class AudioCaptureService extends Service {
    
    private static final String TAG = "AudioCaptureService";
    private static final String CHANNEL_ID = "audio_capture_channel";
    private static final int NOTIFICATION_ID = 1001;
    private static final int SAMPLE_RATE = 16000;
    private static final int BUFFER_SIZE = 4096;
    
    private final IBinder mBinder = new LocalBinder();
    private MediaProjection mMediaProjection;
    private AudioRecord mAudioRecord;
    private Thread mCaptureThread;
    private volatile boolean mIsCapturing = false;
    private Handler mMainHandler;
    
    private VoskSpeechEngine mVoskEngine;
    private AudioCaptureCallback mCallback;
    
    public interface AudioCaptureCallback {
        void onPartialResult(String text);
        void onFinalResult(String text);
        void onError(String error);
        void onStateChanged(boolean isCapturing);
    }
    
    public class LocalBinder extends Binder {
        public AudioCaptureService getService() {
            return AudioCaptureService.this;
        }
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        mMainHandler = new Handler(Looper.getMainLooper());
        createNotificationChannel();
        Log.d(TAG, "Service created");
    }
    
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, createNotification());
        return START_NOT_STICKY;
    }
    
    @Override
    public void onDestroy() {
        stopCapture();
        super.onDestroy();
        Log.d(TAG, "Service destroyed");
    }
    
    /**
     * 设置回调
     */
    public void setCallback(AudioCaptureCallback callback) {
        mCallback = callback;
    }
    
    /**
     * 初始化 Vosk 引擎（异步加载模型，避免卡顿）
     */
    public void initVoskAsync(String language, InitCallback callback) {
        // 在后台线程加载模型
        new Thread(() -> {
            try {
                mVoskEngine = new VoskSpeechEngine();
                boolean success = mVoskEngine.init(this, language);
                
                if (success) {
                    mVoskEngine.setCallback(new SpeechEngine.SpeechCallback() {
                        @Override
                        public void onPartialResult(String text) {
                            if (mCallback != null) {
                                mMainHandler.post(() -> mCallback.onPartialResult(text));
                            }
                        }
                        
                        @Override
                        public void onFinalResult(String text) {
                            if (mCallback != null) {
                                mMainHandler.post(() -> mCallback.onFinalResult(text));
                            }
                        }
                        
                        @Override
                        public void onError(int errorCode, String errorMessage) {
                            if (mCallback != null) {
                                mMainHandler.post(() -> mCallback.onError(errorMessage));
                            }
                        }
                        
                        @Override
                        public void onReady() {
                            Log.d(TAG, "Vosk ready");
                        }
                        
                        @Override
                        public void onSpeechStart() {}
                        
                        @Override
                        public void onSpeechEnd() {}
                    });
                }
                
                // 回调到主线程
                final boolean finalSuccess = success;
                mMainHandler.post(() -> {
                    if (callback != null) {
                        callback.onInitComplete(finalSuccess);
                    }
                });
                
            } catch (Exception e) {
                Log.e(TAG, "Failed to init Vosk", e);
                mMainHandler.post(() -> {
                    if (callback != null) {
                        callback.onInitComplete(false);
                    }
                });
            }
        }, "VoskInitThread").start();
    }
    
    /**
     * 初始化回调接口
     */
    public interface InitCallback {
        void onInitComplete(boolean success);
    }
    
    /**
     * 初始化 Vosk 引擎（同步方式，保留兼容性）
     * @deprecated 使用 initVoskAsync 避免主线程卡顿
     */
    @Deprecated
    public boolean initVosk(String language) {
        mVoskEngine = new VoskSpeechEngine();
        boolean success = mVoskEngine.init(this, language);
        
        if (success) {
            mVoskEngine.setCallback(new SpeechEngine.SpeechCallback() {
                @Override
                public void onPartialResult(String text) {
                    if (mCallback != null) {
                        mMainHandler.post(() -> mCallback.onPartialResult(text));
                    }
                }
                
                @Override
                public void onFinalResult(String text) {
                    if (mCallback != null) {
                        mMainHandler.post(() -> mCallback.onFinalResult(text));
                    }
                }
                
                @Override
                public void onError(int errorCode, String errorMessage) {
                    if (mCallback != null) {
                        mMainHandler.post(() -> mCallback.onError(errorMessage));
                    }
                }
                
                @Override
                public void onReady() {
                    Log.d(TAG, "Vosk ready");
                }
                
                @Override
                public void onSpeechStart() {}
                
                @Override
                public void onSpeechEnd() {}
            });
        }
        
        return success;
    }
    
    /**
     * 设置 MediaProjection 并开始捕获
     */
    public void startCapture(MediaProjection mediaProjection) {
        if (mIsCapturing) {
            Log.w(TAG, "Already capturing");
            return;
        }
        
        mMediaProjection = mediaProjection;
        
        try {
            // 配置音频捕获
            AudioPlaybackCaptureConfiguration config = new AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
                    .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                    .addMatchingUsage(AudioAttributes.USAGE_GAME)
                    .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
                    .build();
            
            // 创建 AudioRecord
            AudioFormat audioFormat = new AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                    .build();
            
            int minBufferSize = AudioRecord.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);
            
            mAudioRecord = new AudioRecord.Builder()
                    .setAudioPlaybackCaptureConfig(config)
                    .setAudioFormat(audioFormat)
                    .setBufferSizeInBytes(Math.max(minBufferSize, BUFFER_SIZE))
                    .build();
            
            if (mAudioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord initialization failed");
                if (mCallback != null) {
                    mMainHandler.post(() -> mCallback.onError("音频捕获初始化失败"));
                }
                return;
            }
            
            // 开始录制
            mAudioRecord.startRecording();
            mIsCapturing = true;
            
            if (mVoskEngine != null) {
                mVoskEngine.startListening();
            }
            
            // 启动捕获线程
            mCaptureThread = new Thread(this::captureLoop, "AudioCaptureThread");
            mCaptureThread.start();
            
            if (mCallback != null) {
                mMainHandler.post(() -> mCallback.onStateChanged(true));
            }
            
            Log.d(TAG, "Capture started");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to start capture", e);
            if (mCallback != null) {
                mMainHandler.post(() -> mCallback.onError("启动音频捕获失败: " + e.getMessage()));
            }
        }
    }
    
    /**
     * 停止捕获
     */
    public void stopCapture() {
        Log.d(TAG, "stopCapture called");
        
        // 先设置标志，让捕获线程退出
        mIsCapturing = false;
        
        // 等待捕获线程结束
        if (mCaptureThread != null) {
            try {
                mCaptureThread.join(2000); // 等待最多 2 秒
            } catch (InterruptedException e) {
                Log.e(TAG, "Interrupted while waiting for capture thread", e);
            }
            mCaptureThread = null;
        }
        
        // 停止 Vosk（在捕获线程结束后）
        if (mVoskEngine != null) {
            mVoskEngine.stopListening();
        }
        
        // 停止 AudioRecord
        if (mAudioRecord != null) {
            try {
                if (mAudioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                    mAudioRecord.stop();
                }
                mAudioRecord.release();
            } catch (Exception e) {
                Log.e(TAG, "Error stopping AudioRecord", e);
            }
            mAudioRecord = null;
        }
        
        // 停止 MediaProjection
        if (mMediaProjection != null) {
            try {
                mMediaProjection.stop();
            } catch (Exception e) {
                Log.e(TAG, "Error stopping MediaProjection", e);
            }
            mMediaProjection = null;
        }
        
        if (mCallback != null) {
            mMainHandler.post(() -> mCallback.onStateChanged(false));
        }
        
        Log.d(TAG, "Capture stopped");
    }
    
    /**
     * 释放资源
     */
    public void release() {
        stopCapture();
        
        if (mVoskEngine != null) {
            mVoskEngine.release();
            mVoskEngine = null;
        }
    }
    
    /**
     * 是否正在捕获
     */
    public boolean isCapturing() {
        return mIsCapturing;
    }
    
    /**
     * 音频捕获循环
     */
    private void captureLoop() {
        byte[] buffer = new byte[BUFFER_SIZE];
        
        while (mIsCapturing && mAudioRecord != null) {
            int bytesRead = mAudioRecord.read(buffer, 0, buffer.length);
            
            if (bytesRead > 0 && mVoskEngine != null) {
                mVoskEngine.processAudioData(buffer, bytesRead);
            } else if (bytesRead < 0) {
                Log.e(TAG, "AudioRecord read error: " + bytesRead);
                break;
            }
        }
        
        Log.d(TAG, "Capture loop ended");
    }
    
    /**
     * 创建通知渠道
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "语音识别服务",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("用于捕获视频音频进行语音识别");
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
    
    /**
     * 创建前台服务通知
     */
    private Notification createNotification() {
        Intent notificationIntent = new Intent();
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE);
        
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("语音识别")
                .setContentText("正在识别视频中的语音...")
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }
    
    /**
     * 检查是否支持音频捕获
     */
    public static boolean isSupported() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q;
    }
}
