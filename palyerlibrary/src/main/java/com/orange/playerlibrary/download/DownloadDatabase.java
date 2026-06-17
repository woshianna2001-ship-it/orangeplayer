package com.orange.playerlibrary.download;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * 下载记录数据库
 * 使用 SQLite 存储下载任务信息
 */
public class DownloadDatabase extends SQLiteOpenHelper {
    
    private static final String TAG = "DownloadDatabase";
    private static final String DATABASE_NAME = "orange_download.db";
    private static final int DATABASE_VERSION = 1;
    
    // 表名
    private static final String TABLE_DOWNLOADS = "downloads";
    
    // 列名
    private static final String COLUMN_TASK_ID = "task_id";
    private static final String COLUMN_URL = "url";
    private static final String COLUMN_TITLE = "title";
    private static final String COLUMN_SAVE_PATH = "save_path";
    private static final String COLUMN_FILE_NAME = "file_name";
    private static final String COLUMN_TOTAL_SIZE = "total_size";
    private static final String COLUMN_DOWNLOADED_SIZE = "downloaded_size";
    private static final String COLUMN_STATE = "state";
    private static final String COLUMN_PROGRESS = "progress";
    private static final String COLUMN_CREATE_TIME = "create_time";
    private static final String COLUMN_UPDATE_TIME = "update_time";
    private static final String COLUMN_ERROR_MESSAGE = "error_message";
    private static final String COLUMN_IS_M3U8 = "is_m3u8";
    
    private static volatile DownloadDatabase sInstance;
    
    private DownloadDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    
    public static DownloadDatabase getInstance(Context context) {
        if (sInstance == null) {
            synchronized (DownloadDatabase.class) {
                if (sInstance == null) {
                    sInstance = new DownloadDatabase(context.getApplicationContext());
                }
            }
        }
        return sInstance;
    }
    
    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_DOWNLOADS + " (" +
                COLUMN_TASK_ID + " TEXT PRIMARY KEY, " +
                COLUMN_URL + " TEXT NOT NULL, " +
                COLUMN_TITLE + " TEXT, " +
                COLUMN_SAVE_PATH + " TEXT, " +
                COLUMN_FILE_NAME + " TEXT, " +
                COLUMN_TOTAL_SIZE + " INTEGER DEFAULT 0, " +
                COLUMN_DOWNLOADED_SIZE + " INTEGER DEFAULT 0, " +
                COLUMN_STATE + " INTEGER DEFAULT 0, " +
                COLUMN_PROGRESS + " INTEGER DEFAULT 0, " +
                COLUMN_CREATE_TIME + " INTEGER, " +
                COLUMN_UPDATE_TIME + " INTEGER, " +
                COLUMN_ERROR_MESSAGE + " TEXT, " +
                COLUMN_IS_M3U8 + " INTEGER DEFAULT 0" +
                ")";
        db.execSQL(createTable);
        
        // 创建索引
        db.execSQL("CREATE INDEX idx_state ON " + TABLE_DOWNLOADS + "(" + COLUMN_STATE + ")");
        db.execSQL("CREATE INDEX idx_create_time ON " + TABLE_DOWNLOADS + "(" + COLUMN_CREATE_TIME + ")");
    }
    
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // 简单的升级策略：删除旧表，创建新表
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_DOWNLOADS);
        onCreate(db);
    }
    
    /**
     * 插入下载任务
     */
    public long insertTask(DownloadTask task) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = taskToContentValues(task);
        long result = db.insert(TABLE_DOWNLOADS, null, values);
        return result;
    }
    
    /**
     * 更新下载任务
     */
    public int updateTask(DownloadTask task) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = taskToContentValues(task);
        return db.update(TABLE_DOWNLOADS, values, 
                COLUMN_TASK_ID + " = ?", 
                new String[]{task.getTaskId()});
    }
    
    /**
     * 删除下载任务
     */
    public int deleteTask(String taskId) {
        SQLiteDatabase db = getWritableDatabase();
        return db.delete(TABLE_DOWNLOADS, 
                COLUMN_TASK_ID + " = ?", 
                new String[]{taskId});
    }
    
    /**
     * 根据任务ID查询
     */
    public DownloadTask queryTask(String taskId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_DOWNLOADS, null, 
                COLUMN_TASK_ID + " = ?", 
                new String[]{taskId}, 
                null, null, null);
        
        DownloadTask task = null;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                task = cursorToTask(cursor);
            }
            cursor.close();
        }
        return task;
    }
    
    /**
     * 查询所有任务
     */
    public List<DownloadTask> queryAllTasks() {
        return queryTasks(null, null, COLUMN_CREATE_TIME + " DESC");
    }
    
    /**
     * 根据状态查询任务
     */
    public List<DownloadTask> queryTasksByState(int state) {
        return queryTasks(COLUMN_STATE + " = ?", 
                new String[]{String.valueOf(state)}, 
                COLUMN_CREATE_TIME + " DESC");
    }
    
    /**
     * 查询正在下载的任务
     */
    public List<DownloadTask> queryDownloadingTasks() {
        return queryTasks(COLUMN_STATE + " IN (?, ?)", 
                new String[]{
                    String.valueOf(DownloadTask.STATE_WAITING),
                    String.valueOf(DownloadTask.STATE_DOWNLOADING)
                }, 
                COLUMN_CREATE_TIME + " DESC");
    }
    
    /**
     * 查询已完成的任务
     */
    public List<DownloadTask> queryCompletedTasks() {
        return queryTasksByState(DownloadTask.STATE_COMPLETED);
    }
    
    /**
     * 通用查询方法
     */
    private List<DownloadTask> queryTasks(String selection, String[] selectionArgs, String orderBy) {
        SQLiteDatabase db = getReadableDatabase();
        List<DownloadTask> tasks = new ArrayList<>();
        
        Cursor cursor = db.query(TABLE_DOWNLOADS, null, 
                selection, selectionArgs, 
                null, null, orderBy);
        
        if (cursor != null) {
            while (cursor.moveToNext()) {
                tasks.add(cursorToTask(cursor));
            }
            cursor.close();
        }
        
        return tasks;
    }
    
    /**
     * 清空所有任务
     */
    public void clearAllTasks() {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_DOWNLOADS, null, null);
    }
    
    /**
     * 清空已完成的任务
     */
    public void clearCompletedTasks() {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_DOWNLOADS, 
                COLUMN_STATE + " = ?", 
                new String[]{String.valueOf(DownloadTask.STATE_COMPLETED)});
    }
    
    /**
     * 清空失败的任务
     */
    public void clearFailedTasks() {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_DOWNLOADS, 
                COLUMN_STATE + " = ?", 
                new String[]{String.valueOf(DownloadTask.STATE_FAILED)});
    }
    
    /**
     * 获取任务总数
     */
    public int getTaskCount() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_DOWNLOADS, null);
        int count = 0;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }
            cursor.close();
        }
        return count;
    }
    
    /**
     * 获取指定状态的任务数量
     */
    public int getTaskCountByState(int state) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT COUNT(*) FROM " + TABLE_DOWNLOADS + " WHERE " + COLUMN_STATE + " = ?", 
                new String[]{String.valueOf(state)});
        int count = 0;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }
            cursor.close();
        }
        return count;
    }
    
    /**
     * 将 DownloadTask 转换为 ContentValues
     */
    private ContentValues taskToContentValues(DownloadTask task) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_TASK_ID, task.getTaskId());
        values.put(COLUMN_URL, task.getUrl());
        values.put(COLUMN_TITLE, task.getTitle());
        values.put(COLUMN_SAVE_PATH, task.getSavePath());
        values.put(COLUMN_FILE_NAME, task.getFileName());
        values.put(COLUMN_TOTAL_SIZE, task.getTotalSize());
        values.put(COLUMN_DOWNLOADED_SIZE, task.getDownloadedSize());
        values.put(COLUMN_STATE, task.getState());
        values.put(COLUMN_PROGRESS, task.getProgress());
        values.put(COLUMN_CREATE_TIME, task.getCreateTime());
        values.put(COLUMN_UPDATE_TIME, task.getUpdateTime());
        values.put(COLUMN_ERROR_MESSAGE, task.getErrorMessage());
        values.put(COLUMN_IS_M3U8, task.isM3u8() ? 1 : 0);
        return values;
    }
    
    /**
     * 将 Cursor 转换为 DownloadTask
     */
    private DownloadTask cursorToTask(Cursor cursor) {
        DownloadTask task = new DownloadTask();
        task.setTaskId(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TASK_ID)));
        task.setUrl(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_URL)));
        task.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE)));
        task.setSavePath(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SAVE_PATH)));
        task.setFileName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FILE_NAME)));
        task.setTotalSize(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TOTAL_SIZE)));
        task.setDownloadedSize(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_DOWNLOADED_SIZE)));
        task.setState(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_STATE)));
        task.setProgress(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PROGRESS)));
        task.setCreateTime(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CREATE_TIME)));
        task.setUpdateTime(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_UPDATE_TIME)));
        task.setErrorMessage(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ERROR_MESSAGE)));
        task.setM3u8(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_M3U8)) == 1);
        return task;
    }
}
