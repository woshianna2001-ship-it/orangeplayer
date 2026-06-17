package com.orange.playerlibrary.history;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;

import com.orange.playerlibrary.PlaybackProgressManager;
import com.orange.playerlibrary.PlayerSettingsManager;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 播放历史管理器
 * 单例模式，提供播放历史的增删改查功能
 */
public class PlayHistoryManager {

    private static final String TAG = "PlayHistoryManager";
    private static final int MAX_HISTORY_COUNT = 100; // 最大历史记录数
    private static final long MIN_DURATION_TO_SAVE = 60000; // 最小保存时长（1分钟）
    private static final long AUTO_SAVE_INTERVAL = 15000; // 自动保存间隔（15秒）

    private static volatile PlayHistoryManager sInstance;
    private PlayHistoryDatabase mDbHelper;
    private Context mContext;
    
    // 自动保存相关
    private Handler mAutoSaveHandler;
    private Runnable mAutoSaveRunnable;
    private String mCurrentUrl;
    private String mCurrentTitle;
    private ProgressProvider mProgressProvider;

    private PlayHistoryManager(Context context) {
        mContext = context.getApplicationContext();
        mDbHelper = new PlayHistoryDatabase(mContext);
        mAutoSaveHandler = new Handler(Looper.getMainLooper());
    }

    public static PlayHistoryManager getInstance(Context context) {
        if (sInstance == null) {
            synchronized (PlayHistoryManager.class) {
                if (sInstance == null) {
                    sInstance = new PlayHistoryManager(context);
                }
            }
        }
        return sInstance;
    }
    
    /**
     * 进度提供者接口
     */
    public interface ProgressProvider {
        long getCurrentPosition();
        long getDuration();
        View getVideoView(); // 用于截取缩略图
    }
    
    /**
     * 开始自动保存
     */
    public void startAutoSave(String url, String title, ProgressProvider provider) {
        stopAutoSave();
        
        mCurrentUrl = url;
        mCurrentTitle = title;
        mProgressProvider = provider;
        
        // 立即保存一次（创建记录）
        if (mProgressProvider != null && mCurrentUrl != null) {
            long position = mProgressProvider.getCurrentPosition();
            long duration = mProgressProvider.getDuration();
            if (duration > 0) {
                // 截取缩略图
                String thumbnailBase64 = null;
                View videoView = mProgressProvider.getVideoView();
                if (videoView != null) {
                    thumbnailBase64 = captureVideoThumbnail(videoView);
                }
                saveProgressWithThumbnail(mCurrentUrl, mCurrentTitle, duration, position, thumbnailBase64);
                Log.d(TAG, "Initial save: url=" + url + ", position=" + position + ", duration=" + duration + ", hasThumbnail=" + (thumbnailBase64 != null));
            }
        }
        
        mAutoSaveRunnable = new Runnable() {
            @Override
            public void run() {
                // 如果全局记忆播放开关关闭，则不保存进度到数据库
                if (!PlayerSettingsManager.getInstance(mContext).isMemoryPlayEnabled()) {
                    mAutoSaveHandler.postDelayed(this, AUTO_SAVE_INTERVAL);
                    return;
                }
                
                if (mProgressProvider != null && mCurrentUrl != null) {
                    long position = mProgressProvider.getCurrentPosition();
                    long duration = mProgressProvider.getDuration();
                    
                    // 过滤条件：进度大于1分钟且距离结尾大于1分钟才触发记忆写入
                    boolean shouldSave = true;
                    if (position < 60000) {
                        shouldSave = false;
                    } else if (duration > 0 && (duration - position) < 60000) {
                        shouldSave = false;
                    }
                    
                    if (shouldSave && position > 0 && duration > 0) {
                        // 定期保存时也更新缩略图
                        String thumbnailBase64 = null;
                        View videoView = mProgressProvider.getVideoView();
                        if (videoView != null) {
                            thumbnailBase64 = captureVideoThumbnail(videoView);
                        }
                        saveProgressWithThumbnail(mCurrentUrl, mCurrentTitle, duration, position, thumbnailBase64);
                        
                        // 同时同步更新到 PlaybackProgressManager，以保证两边逻辑统一
                        PlaybackProgressManager.getInstance(mContext).saveProgress(mCurrentUrl, position, duration);
                        
                        Log.d(TAG, "Auto save (real-time to db): position=" + position);
                    } else {
                        Log.d(TAG, "Auto save skipped due to filter conditions: position=" + position + ", duration=" + duration);
                    }
                }
                mAutoSaveHandler.postDelayed(this, AUTO_SAVE_INTERVAL);
            }
        };
        mAutoSaveHandler.postDelayed(mAutoSaveRunnable, AUTO_SAVE_INTERVAL);
        Log.d(TAG, "Started auto save for: " + url);
    }
    
    /**
     * 截取视频缩略图并转为 Base64
     */
    private String captureVideoThumbnail(View videoView) {
        try {
            // 查找 TextureView
            TextureView textureView = findTextureView(videoView);
            if (textureView != null) {
                Bitmap bitmap = textureView.getBitmap();
                if (bitmap != null) {
                    // 缩放到小尺寸节省存储空间
                    Bitmap scaled = Bitmap.createScaledBitmap(bitmap, 160, 90, true);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    scaled.compress(Bitmap.CompressFormat.JPEG, 70, baos);
                    byte[] bytes = baos.toByteArray();
                    String base64 = Base64.encodeToString(bytes, Base64.NO_WRAP);
                    scaled.recycle();
                    if (bitmap != scaled) {
                        bitmap.recycle();
                    }
                    return base64;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "captureVideoThumbnail error", e);
        }
        return null;
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
     * 停止自动保存
     */
    public void stopAutoSave() {
        if (mAutoSaveRunnable != null) {
            mAutoSaveHandler.removeCallbacks(mAutoSaveRunnable);
            mAutoSaveRunnable = null;
        }
        // 停止时保存一次，同样要判断开关和条件
        if (mProgressProvider != null && mCurrentUrl != null && PlayerSettingsManager.getInstance(mContext).isMemoryPlayEnabled()) {
            long position = mProgressProvider.getCurrentPosition();
            long duration = mProgressProvider.getDuration();
            
            boolean shouldSave = true;
            if (position < 60000) {
                shouldSave = false;
            } else if (duration > 0 && (duration - position) < 60000) {
                shouldSave = false;
            }
            
            if (shouldSave && position > 0 && duration > 0) {
                saveProgress(mCurrentUrl, mCurrentTitle, duration, position);
                PlaybackProgressManager.getInstance(mContext).saveProgress(mCurrentUrl, position, duration);
            }
        }
        mProgressProvider = null;
        Log.d(TAG, "Stopped auto save");
    }

    /**
     * 保存播放进度
     */
    public void saveProgress(String url, String title, long duration, long position) {
        saveProgressWithThumbnail(url, title, duration, position, null);
    }
    
    /**
     * 保存播放进度（带缩略图）
     */
    public void saveProgressWithThumbnail(String url, String title, long duration, long position, String thumbnailBase64) {
        if (url == null || url.isEmpty()) {
            return;
        }
        
        // 短视频不保存历史
        if (duration > 0 && duration < MIN_DURATION_TO_SAVE) {
            Log.d(TAG, "Video too short, skip saving: " + duration + "ms");
            return;
        }

        SQLiteDatabase db = null;
        try {
            db = mDbHelper.getWritableDatabase();
            
            ContentValues values = new ContentValues();
            values.put(PlayHistoryDatabase.COLUMN_VIDEO_URL, url);
            values.put(PlayHistoryDatabase.COLUMN_VIDEO_TITLE, title != null ? title : "");
            values.put(PlayHistoryDatabase.COLUMN_DURATION, duration);
            values.put(PlayHistoryDatabase.COLUMN_POSITION, position);
            values.put(PlayHistoryDatabase.COLUMN_LAST_PLAY_TIME, System.currentTimeMillis());
            if (thumbnailBase64 != null) {
                values.put(PlayHistoryDatabase.COLUMN_THUMBNAIL_BASE64, thumbnailBase64);
            }

            // 检查是否已存在
            PlayHistory existing = getHistoryByUrl(url);
            if (existing != null) {
                // 更新现有记录（不增加播放次数，只更新进度和缩略图）
                db.update(PlayHistoryDatabase.TABLE_NAME, values,
                        PlayHistoryDatabase.COLUMN_VIDEO_URL + " = ?",
                        new String[]{url});
                Log.d(TAG, "Updated history: " + url + ", position=" + position);
            } else {
                // 插入新记录
                values.put(PlayHistoryDatabase.COLUMN_PLAY_COUNT, 1);
                db.insert(PlayHistoryDatabase.TABLE_NAME, null, values);
                Log.d(TAG, "Inserted history: " + url + ", position=" + position);
                
                // 清理超出限制的旧记录
                cleanupOldRecords(db);
            }
        } catch (Exception e) {
            Log.e(TAG, "saveProgress error", e);
        }
    }

    /**
     * 仅更新播放位置（不增加播放次数）
     */
    public void updatePosition(String url, long position) {
        if (url == null || url.isEmpty()) return;

        SQLiteDatabase db = null;
        try {
            db = mDbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(PlayHistoryDatabase.COLUMN_POSITION, position);
            values.put(PlayHistoryDatabase.COLUMN_LAST_PLAY_TIME, System.currentTimeMillis());
            
            db.update(PlayHistoryDatabase.TABLE_NAME, values,
                    PlayHistoryDatabase.COLUMN_VIDEO_URL + " = ?",
                    new String[]{url});
        } catch (Exception e) {
            Log.e(TAG, "updatePosition error", e);
        }
    }

    /**
     * 获取播放进度
     */
    public long getProgress(String url) {
        PlayHistory history = getHistoryByUrl(url);
        return history != null ? history.getPosition() : 0;
    }

    /**
     * 根据 URL 获取历史记录
     */
    public PlayHistory getHistoryByUrl(String url) {
        if (url == null || url.isEmpty()) return null;

        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = mDbHelper.getReadableDatabase();
            cursor = db.query(PlayHistoryDatabase.TABLE_NAME,
                    null,
                    PlayHistoryDatabase.COLUMN_VIDEO_URL + " = ?",
                    new String[]{url},
                    null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                return cursorToHistory(cursor);
            }
        } catch (Exception e) {
            Log.e(TAG, "getHistoryByUrl error", e);
        } finally {
            if (cursor != null) cursor.close();
        }
        return null;
    }

    /**
     * 获取播放历史列表
     */
    public List<PlayHistory> getHistoryList(int limit) {
        List<PlayHistory> list = new ArrayList<>();
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = mDbHelper.getReadableDatabase();
            cursor = db.query(PlayHistoryDatabase.TABLE_NAME,
                    null, null, null, null, null,
                    PlayHistoryDatabase.COLUMN_LAST_PLAY_TIME + " DESC",
                    limit > 0 ? String.valueOf(limit) : null);

            if (cursor != null) {
                while (cursor.moveToNext()) {
                    list.add(cursorToHistory(cursor));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "getHistoryList error", e);
        } finally {
            if (cursor != null) cursor.close();
        }
        return list;
    }


    /**
     * 删除历史记录
     */
    public void deleteHistory(String url) {
        if (url == null || url.isEmpty()) return;

        SQLiteDatabase db = null;
        try {
            db = mDbHelper.getWritableDatabase();
            db.delete(PlayHistoryDatabase.TABLE_NAME,
                    PlayHistoryDatabase.COLUMN_VIDEO_URL + " = ?",
                    new String[]{url});
            Log.d(TAG, "Deleted history: " + url);
        } catch (Exception e) {
            Log.e(TAG, "deleteHistory error", e);
        }
    }

    /**
     * 清空所有历史
     */
    public void clearAll() {
        SQLiteDatabase db = null;
        try {
            db = mDbHelper.getWritableDatabase();
            db.delete(PlayHistoryDatabase.TABLE_NAME, null, null);
            Log.d(TAG, "Cleared all history");
        } catch (Exception e) {
            Log.e(TAG, "clearAll error", e);
        }
    }

    /**
     * 获取历史记录数量
     */
    public int getHistoryCount() {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = mDbHelper.getReadableDatabase();
            cursor = db.rawQuery("SELECT COUNT(*) FROM " + PlayHistoryDatabase.TABLE_NAME, null);
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
        } catch (Exception e) {
            Log.e(TAG, "getHistoryCount error", e);
        } finally {
            if (cursor != null) cursor.close();
        }
        return 0;
    }

    /**
     * 清理超出限制的旧记录
     */
    private void cleanupOldRecords(SQLiteDatabase db) {
        try {
            // 删除超出 MAX_HISTORY_COUNT 的旧记录
            String sql = "DELETE FROM " + PlayHistoryDatabase.TABLE_NAME +
                    " WHERE " + PlayHistoryDatabase.COLUMN_ID + " NOT IN (" +
                    "SELECT " + PlayHistoryDatabase.COLUMN_ID + " FROM " + PlayHistoryDatabase.TABLE_NAME +
                    " ORDER BY " + PlayHistoryDatabase.COLUMN_LAST_PLAY_TIME + " DESC" +
                    " LIMIT " + MAX_HISTORY_COUNT + ")";
            db.execSQL(sql);
        } catch (Exception e) {
            Log.e(TAG, "cleanupOldRecords error", e);
        }
    }

    /**
     * Cursor 转 PlayHistory 对象
     */
    private PlayHistory cursorToHistory(Cursor cursor) {
        PlayHistory history = new PlayHistory();
        history.setId(cursor.getLong(cursor.getColumnIndexOrThrow(PlayHistoryDatabase.COLUMN_ID)));
        history.setVideoUrl(cursor.getString(cursor.getColumnIndexOrThrow(PlayHistoryDatabase.COLUMN_VIDEO_URL)));
        history.setVideoTitle(cursor.getString(cursor.getColumnIndexOrThrow(PlayHistoryDatabase.COLUMN_VIDEO_TITLE)));
        history.setThumbnailUrl(cursor.getString(cursor.getColumnIndexOrThrow(PlayHistoryDatabase.COLUMN_THUMBNAIL_URL)));
        // 读取缩略图 Base64
        int thumbIndex = cursor.getColumnIndex(PlayHistoryDatabase.COLUMN_THUMBNAIL_BASE64);
        if (thumbIndex >= 0) {
            history.setThumbnailBase64(cursor.getString(thumbIndex));
        }
        history.setDuration(cursor.getLong(cursor.getColumnIndexOrThrow(PlayHistoryDatabase.COLUMN_DURATION)));
        history.setPosition(cursor.getLong(cursor.getColumnIndexOrThrow(PlayHistoryDatabase.COLUMN_POSITION)));
        history.setLastPlayTime(cursor.getLong(cursor.getColumnIndexOrThrow(PlayHistoryDatabase.COLUMN_LAST_PLAY_TIME)));
        history.setPlayCount(cursor.getInt(cursor.getColumnIndexOrThrow(PlayHistoryDatabase.COLUMN_PLAY_COUNT)));
        return history;
    }
}
