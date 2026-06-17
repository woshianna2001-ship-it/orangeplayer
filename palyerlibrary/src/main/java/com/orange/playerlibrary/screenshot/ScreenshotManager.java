package com.orange.playerlibrary.screenshot;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.PixelCopy;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 视频截图管理器
 * 提供截图、保存到相册、分享等功能
 */
public class ScreenshotManager {
    
    private static final String TAG = "ScreenshotManager";
    private static final String SCREENSHOT_FOLDER = "OrangePlayer";
    
    private Context mContext;
    private View mVideoView;
    
    public ScreenshotManager(Context context, View videoView) {
        this.mContext = context.getApplicationContext();
        this.mVideoView = videoView;
    }
    
    /**
     * 截图回调接口
     */
    public interface ScreenshotCallback {
        void onSuccess(Bitmap bitmap, String message);
        void onError(String message);
    }
    
    /**
     * 保存回调接口
     */
    public interface SaveCallback {
        void onSuccess(String filePath);
        void onError(String message);
    }

    /**
     * 获取截图
     * @param callback 截图回调
     */
    public void takeScreenshot(ScreenshotCallback callback) {
        takeScreenshot(false, callback);
    }
    
    /**
     * 获取截图
     * @param highQuality 是否高清
     * @param callback 截图回调
     */
    public void takeScreenshot(boolean highQuality, ScreenshotCallback callback) {
        if (mVideoView == null) {
            if (callback != null) {
                callback.onError("播放器未初始化");
            }
            return;
        }
        
        try {
            // 尝试从 TextureView 获取截图
            TextureView textureView = findTextureView(mVideoView);
            if (textureView != null) {
                Bitmap bitmap = textureView.getBitmap();
                if (bitmap != null) {
                    if (callback != null) {
                        callback.onSuccess(bitmap, "截图成功");
                    }
                    return;
                }
            }
            
            // 尝试从 SurfaceView 获取截图 (Android N+)
            SurfaceView surfaceView = findSurfaceView(mVideoView);
            if (surfaceView != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                takeScreenshotFromSurfaceView(surfaceView, callback);
                return;
            }
            
            // 回退：截取整个 View
            Bitmap bitmap = captureView(mVideoView);
            if (bitmap != null) {
                if (callback != null) {
                    callback.onSuccess(bitmap, "截图成功");
                }
            } else {
                if (callback != null) {
                    callback.onError("截图失败：无法获取画面");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "takeScreenshot error", e);
            if (callback != null) {
                callback.onError("截图失败：" + e.getMessage());
            }
        }
    }
    
    /**
     * 从 SurfaceView 获取截图 (Android N+)
     */
    private void takeScreenshotFromSurfaceView(SurfaceView surfaceView, ScreenshotCallback callback) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Bitmap bitmap = Bitmap.createBitmap(
                surfaceView.getWidth(), 
                surfaceView.getHeight(), 
                Bitmap.Config.ARGB_8888);
            
            try {
                PixelCopy.request(surfaceView, bitmap, copyResult -> {
                    if (copyResult == PixelCopy.SUCCESS) {
                        if (callback != null) {
                            callback.onSuccess(bitmap, "截图成功");
                        }
                    } else {
                        if (callback != null) {
                            callback.onError("截图失败：PixelCopy 错误 " + copyResult);
                        }
                    }
                }, new Handler(Looper.getMainLooper()));
            } catch (Exception e) {
                Log.e(TAG, "PixelCopy error", e);
                if (callback != null) {
                    callback.onError("截图失败：" + e.getMessage());
                }
            }
        }
    }
    
    /**
     * 递归查找 TextureView
     */
    private TextureView findTextureView(View view) {
        if (view instanceof TextureView) {
            return (TextureView) view;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                TextureView result = findTextureView(group.getChildAt(i));
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }
    
    /**
     * 递归查找 SurfaceView
     */
    private SurfaceView findSurfaceView(View view) {
        if (view instanceof SurfaceView) {
            return (SurfaceView) view;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                SurfaceView result = findSurfaceView(group.getChildAt(i));
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }
    
    /**
     * 截取 View
     */
    private Bitmap captureView(View view) {
        try {
            Bitmap bitmap = Bitmap.createBitmap(
                view.getWidth(), 
                view.getHeight(), 
                Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            view.draw(canvas);
            return bitmap;
        } catch (Exception e) {
            Log.e(TAG, "captureView error", e);
            return null;
        }
    }

    
    /**
     * 截图并保存到相册
     * @param callback 保存回调
     */
    public void takeAndSave(SaveCallback callback) {
        takeAndSave(false, callback);
    }
    
    /**
     * 截图并保存到相册
     * @param highQuality 是否高清
     * @param callback 保存回调
     */
    public void takeAndSave(boolean highQuality, SaveCallback callback) {
        takeScreenshot(highQuality, new ScreenshotCallback() {
            @Override
            public void onSuccess(Bitmap bitmap, String message) {
                saveToGallery(bitmap, callback);
            }
            
            @Override
            public void onError(String message) {
                if (callback != null) {
                    callback.onError(message);
                }
            }
        });
    }
    
    /**
     * 保存 Bitmap 到相册
     * @param bitmap 要保存的图片
     * @param callback 保存回调
     */
    public void saveToGallery(Bitmap bitmap, SaveCallback callback) {
        if (bitmap == null) {
            if (callback != null) {
                callback.onError("图片为空");
            }
            return;
        }
        
        new Thread(() -> {
            try {
                String fileName = generateFileName();
                String filePath;
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Android 10+ 使用 MediaStore
                    filePath = saveWithMediaStore(bitmap, fileName);
                } else {
                    // Android 9 及以下使用传统方式
                    filePath = saveToExternalStorage(bitmap, fileName);
                }
                
                if (filePath != null && callback != null) {
                    Handler mainHandler = new Handler(mContext.getMainLooper());
                    mainHandler.post(() -> callback.onSuccess(filePath));
                }
            } catch (Exception e) {
                Log.e(TAG, "saveToGallery error", e);
                if (callback != null) {
                    Handler mainHandler = new Handler(mContext.getMainLooper());
                    mainHandler.post(() -> callback.onError("保存失败：" + e.getMessage()));
                }
            }
        }).start();
    }
    
    /**
     * Android 10+ 使用 MediaStore 保存
     */
    private String saveWithMediaStore(Bitmap bitmap, String fileName) throws Exception {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/" + SCREENSHOT_FOLDER);
        }
        
        Uri uri = mContext.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (uri != null) {
            try (OutputStream out = mContext.getContentResolver().openOutputStream(uri)) {
                if (out != null) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                    return uri.toString();
                }
            }
        }
        throw new Exception("无法创建文件");
    }
    
    /**
     * Android 9 及以下使用传统方式保存
     */
    @SuppressWarnings("deprecation")
    private String saveToExternalStorage(Bitmap bitmap, String fileName) throws Exception {
        File dir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), SCREENSHOT_FOLDER);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        File file = new File(dir, fileName);
        try (FileOutputStream out = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        }
        
        // 通知相册更新
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(Uri.fromFile(file));
        mContext.sendBroadcast(mediaScanIntent);
        
        return file.getAbsolutePath();
    }
    
    /**
     * 生成文件名
     */
    private String generateFileName() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        return "screenshot_" + sdf.format(new Date()) + ".png";
    }
    
    /**
     * 分享截图
     * @param bitmap 要分享的图片
     */
    public void shareScreenshot(Bitmap bitmap) {
        if (bitmap == null) {
            return;
        }
        
        // 先保存到缓存目录
        try {
            File cacheDir = new File(mContext.getCacheDir(), "screenshots");
            if (!cacheDir.exists()) {
                cacheDir.mkdirs();
            }
            
            File file = new File(cacheDir, "share_" + System.currentTimeMillis() + ".png");
            try (FileOutputStream out = new FileOutputStream(file)) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            }
            
            // 使用 FileProvider 获取 Uri
            Uri uri = androidx.core.content.FileProvider.getUriForFile(
                    mContext,
                    mContext.getPackageName() + ".fileprovider",
                    file);
            
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/png");
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            mContext.startActivity(Intent.createChooser(shareIntent, "分享截图").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Exception e) {
            Log.e(TAG, "shareScreenshot error", e);
        }
    }
}
