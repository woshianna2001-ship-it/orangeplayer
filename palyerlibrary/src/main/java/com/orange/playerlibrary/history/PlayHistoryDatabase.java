package com.orange.playerlibrary.history;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * 播放历史数据库
 */
public class PlayHistoryDatabase extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "play_history.db";
    private static final int DATABASE_VERSION = 2;

    public static final String TABLE_NAME = "play_history";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_VIDEO_URL = "video_url";
    public static final String COLUMN_VIDEO_TITLE = "video_title";
    public static final String COLUMN_THUMBNAIL_URL = "thumbnail_url";
    public static final String COLUMN_THUMBNAIL_BASE64 = "thumbnail_base64";
    public static final String COLUMN_DURATION = "duration";
    public static final String COLUMN_POSITION = "position";
    public static final String COLUMN_LAST_PLAY_TIME = "last_play_time";
    public static final String COLUMN_PLAY_COUNT = "play_count";

    private static final String SQL_CREATE_TABLE =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_VIDEO_URL + " TEXT UNIQUE NOT NULL, " +
                    COLUMN_VIDEO_TITLE + " TEXT, " +
                    COLUMN_THUMBNAIL_URL + " TEXT, " +
                    COLUMN_THUMBNAIL_BASE64 + " TEXT, " +
                    COLUMN_DURATION + " INTEGER DEFAULT 0, " +
                    COLUMN_POSITION + " INTEGER DEFAULT 0, " +
                    COLUMN_LAST_PLAY_TIME + " INTEGER DEFAULT 0, " +
                    COLUMN_PLAY_COUNT + " INTEGER DEFAULT 1" +
                    ")";

    private static final String SQL_CREATE_INDEX =
            "CREATE INDEX idx_last_play_time ON " + TABLE_NAME + " (" + COLUMN_LAST_PLAY_TIME + " DESC)";

    public PlayHistoryDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_TABLE);
        db.execSQL(SQL_CREATE_INDEX);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // 简单处理：删除旧表重建
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }
}
