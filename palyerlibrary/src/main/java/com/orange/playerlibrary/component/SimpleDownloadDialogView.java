package com.orange.playerlibrary.component;

import android.content.Context;

import com.orange.playerlibrary.download.DownloadListDialog;
import com.orange.playerlibrary.download.SimpleDownloadManager;

/**
 * 简单下载管理对话框
 * 
 * 使用 DownloadListDialog 显示下载任务列表
 * 支持暂停/恢复/删除任务
 */
public class SimpleDownloadDialogView {
    
    private Context mContext;
    private SimpleDownloadManager mDownloadManager;
    private DownloadListDialog mDialog;
    private DownloadListDialog.OnPlayLocalListener mPlayLocalListener;
    
    public SimpleDownloadDialogView(Context context) {
        mContext = context;
        mDownloadManager = SimpleDownloadManager.getInstance(context);
    }
    
    /**
     * 显示下载列表对话框
     */
    public void show() {
        try {
            if (mDialog == null) {
                mDialog = new DownloadListDialog(mContext);
            }
            if (mPlayLocalListener != null) {
                mDialog.setOnPlayLocalListener(mPlayLocalListener);
            }
            mDialog.show();
        } catch (Exception e) {
            // VideoDownloader 未初始化或不可用
            android.widget.Toast.makeText(mContext, 
                "下载功能暂不可用\n请确保 VideoDownloader 已初始化", 
                android.widget.Toast.LENGTH_LONG).show();
        }
    }
    
    /**
     * 隐藏对话框
     */
    public void dismiss() {
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
        }
    }
    
    /**
     * 刷新下载列表
     */
    public void refresh() {
        // DownloadListDialog 在 show() 时会自动刷新
        if (mDialog != null && mDialog.isShowing()) {
            // 对话框已显示，无需操作
        }
    }
    
    /**
     * 清理资源
     */
    public void cleanup() {
        if (mDownloadManager != null) {
            mDownloadManager.cleanup();
        }
        if (mDialog != null) {
            mDialog.dismiss();
            mDialog = null;
        }
    }

    public void setOnPlayLocalListener(DownloadListDialog.OnPlayLocalListener listener) {
        mPlayLocalListener = listener;
        if (mDialog != null) {
            mDialog.setOnPlayLocalListener(listener);
        }
    }
}
