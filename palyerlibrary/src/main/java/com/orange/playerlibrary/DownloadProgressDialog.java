package com.orange.playerlibrary;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * 下载进度对话框
 * 显示圆形进度和百分比
 */
public class DownloadProgressDialog {
    
    private Dialog mDialog;
    private ProgressBar mProgressBar;
    private TextView mProgressText;
    private TextView mTitleText;
    private TextView mHintText;
    private Handler mHandler;
    private int mCurrentProgress = 0;
    private boolean mIsSimulating = false;
    private Runnable mSimulateRunnable;
    
    public DownloadProgressDialog(Context context) {
        mHandler = new Handler(Looper.getMainLooper());
        createDialog(context);
    }
    
    private void createDialog(Context context) {
        mDialog = new Dialog(context);
        mDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        mDialog.setCancelable(false);
        mDialog.setCanceledOnTouchOutside(false);
        
        // 使用自定义布局
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_download_progress, null);
        mTitleText = view.findViewById(R.id.tv_title);
        mProgressBar = view.findViewById(R.id.progress_bar);
        mProgressText = view.findViewById(R.id.tv_progress);
        mHintText = view.findViewById(R.id.tv_hint);
        
        mDialog.setContentView(view);
        
        // 设置对话框样式
        Window window = mDialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams params = window.getAttributes();
            params.gravity = Gravity.CENTER;
            params.dimAmount = 0.6f;
            window.setAttributes(params);
        }
    }
    
    /**
     * 显示对话框并开始模拟进度（用于 ML Kit 等不提供真实进度的场景）
     */
    public void show() {
        show("正在下载翻译模型", true);
    }

    public void show(String title) {
        show(title, true);
    }
    
    /**
     * 显示对话框
     * @param title 标题
     * @param simulateProgress 是否模拟进度（如果有真实进度回调，设为 false）
     */
    public void show(String title, boolean simulateProgress) {
        if (mDialog != null && !mDialog.isShowing()) {
            if (mTitleText != null) mTitleText.setText(title);
            if (mHintText != null) mHintText.setText("首次使用需要下载");
            mCurrentProgress = 0;
            updateProgress(0);
            mDialog.show();
            if (simulateProgress) {
                startSimulateProgress();
            }
        }
    }
    
    /**
     * 显示对话框，使用真实进度（不模拟）
     */
    public void showWithRealProgress(String title) {
        show(title, false);
    }
    
    /**
     * 设置真实进度（会自动停止模拟进度）
     */
    public void setProgress(int progress) {
        // 收到真实进度时，停止模拟
        if (mIsSimulating) {
            mIsSimulating = false;
            if (mSimulateRunnable != null) {
                mHandler.removeCallbacks(mSimulateRunnable);
            }
        }
        mCurrentProgress = progress;
        updateProgress(progress);
    }
    
    /**
     * 设置进度和状态文字
     */
    public void setProgress(int progress, String status) {
        setProgress(progress);
        if (mHintText != null && status != null) {
            mHandler.post(() -> mHintText.setText(status));
        }
    }
    
    private void updateProgress(int progress) {
        mHandler.post(() -> {
            if (mProgressBar != null) {
                mProgressBar.setProgress(progress);
            }
            if (mProgressText != null) {
                mProgressText.setText(progress + "%");
            }
        });
    }
    
    /**
     * 开始模拟进度（因为 ML Kit 不提供真实进度）
     */
    private void startSimulateProgress() {
        mIsSimulating = true;
        mCurrentProgress = 0;
        
        mSimulateRunnable = new Runnable() {
            @Override
            public void run() {
                if (!mIsSimulating) return;
                
                // 模拟进度：快速到 30%，然后慢慢到 90%
                if (mCurrentProgress < 30) {
                    mCurrentProgress += 3;
                } else if (mCurrentProgress < 60) {
                    mCurrentProgress += 2;
                } else if (mCurrentProgress < 90) {
                    mCurrentProgress += 1;
                }
                
                updateProgress(Math.min(mCurrentProgress, 90));
                
                if (mIsSimulating && mCurrentProgress < 90) {
                    mHandler.postDelayed(this, 100);
                }
            }
        };
        
        mHandler.postDelayed(mSimulateRunnable, 100);
    }
    
    /**
     * 完成下载，显示 100% 并关闭
     */
    public void complete() {
        mIsSimulating = false;
        if (mSimulateRunnable != null) {
            mHandler.removeCallbacks(mSimulateRunnable);
        }
        
        mHandler.post(() -> {
            updateProgress(100);
            if (mHintText != null) mHintText.setText("下载完成");
            mHandler.postDelayed(this::dismiss, 500);
        });
    }
    
    /**
     * 下载失败
     */
    public void fail(String error) {
        mIsSimulating = false;
        if (mSimulateRunnable != null) {
            mHandler.removeCallbacks(mSimulateRunnable);
        }
        dismiss();
    }
    
    /**
     * 关闭对话框
     */
    public void dismiss() {
        mIsSimulating = false;
        if (mSimulateRunnable != null) {
            mHandler.removeCallbacks(mSimulateRunnable);
        }
        if (mDialog != null && mDialog.isShowing()) {
            try {
                mDialog.dismiss();
            } catch (Exception e) {
                // ignore
            }
        }
    }
    
    public boolean isShowing() {
        return mDialog != null && mDialog.isShowing();
    }
}
