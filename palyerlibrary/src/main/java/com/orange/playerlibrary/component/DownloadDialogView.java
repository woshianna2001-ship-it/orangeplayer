package com.orange.playerlibrary.component;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.orange.playerlibrary.R;
import com.orange.playerlibrary.download.DownloadDatabase;
import com.orange.playerlibrary.download.DownloadListener;
import com.orange.playerlibrary.download.DownloadManager;
import com.orange.playerlibrary.download.DownloadTask;

import java.util.ArrayList;
import java.util.List;

/**
 * 下载管理对话框
 * 显示下载任务列表和管理功能
 */
public class DownloadDialogView {
    
    private Context mContext;
    private Dialog mDialog;
    private RecyclerView mRecyclerView;
    private DownloadAdapter mAdapter;
    private View mEmptyView;
    
    private DownloadManager mDownloadManager;
    private DownloadDatabase mDatabase;
    
    public DownloadDialogView(Context context) {
        mContext = context;
        mDownloadManager = DownloadManager.getInstance(context);
        mDatabase = DownloadDatabase.getInstance(context);
        initDialog();
    }
    
    private void initDialog() {
        mDialog = new Dialog(mContext, android.R.style.Theme_Translucent_NoTitleBar);
        View view = LayoutInflater.from(mContext).inflate(R.layout.orange_dialog_download_list, null);
        
        mRecyclerView = view.findViewById(R.id.recycler_view);
        mEmptyView = view.findViewById(R.id.empty_view);
        ImageView closeButton = view.findViewById(R.id.btn_close);
        ImageView clearButton = view.findViewById(R.id.btn_clear);
        
        mRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));
        mAdapter = new DownloadAdapter();
        mRecyclerView.setAdapter(mAdapter);
        
        if (closeButton != null) {
            closeButton.setOnClickListener(v -> dismiss());
        }
        
        if (clearButton != null) {
            clearButton.setOnClickListener(v -> clearCompletedTasks());
        }
        
        mDialog.setContentView(view);
        
        // 添加下载监听器
        mDownloadManager.addListener(mDownloadListener);
    }
    
    /**
     * 显示对话框
     */
    public void show() {
        if (mDialog != null) {
            loadTasks();
            mDialog.show();
        }
    }
    
    /**
     * 关闭对话框
     */
    public void dismiss() {
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
        }
    }
    
    /**
     * 加载任务列表
     */
    private void loadTasks() {
        List<DownloadTask> tasks = mDatabase.queryAllTasks();
        mAdapter.setTasks(tasks);
        
        if (tasks.isEmpty()) {
            mEmptyView.setVisibility(View.VISIBLE);
            mRecyclerView.setVisibility(View.GONE);
        } else {
            mEmptyView.setVisibility(View.GONE);
            mRecyclerView.setVisibility(View.VISIBLE);
        }
    }
    
    /**
     * 清空已完成的任务
     */
    private void clearCompletedTasks() {
        mDatabase.clearCompletedTasks();
        loadTasks();
    }
    
    /**
     * 下载监听器
     */
    private DownloadListener mDownloadListener = new DownloadListener() {
        @Override
        public void onDownloadStart(DownloadTask task) {
            loadTasks();
        }
        
        @Override
        public void onDownloadProgress(DownloadTask task, int progress, long speed) {
            mAdapter.updateTask(task);
        }
        
        @Override
        public void onDownloadPaused(DownloadTask task) {
            mAdapter.updateTask(task);
        }
        
        @Override
        public void onDownloadCompleted(DownloadTask task) {
            loadTasks();
        }
        
        @Override
        public void onDownloadFailed(DownloadTask task, String errorMessage) {
            loadTasks();
        }
        
        @Override
        public void onDownloadCancelled(DownloadTask task) {
            loadTasks();
        }
    };
    
    /**
     * 下载任务适配器
     */
    private class DownloadAdapter extends RecyclerView.Adapter<DownloadViewHolder> {
        
        private List<DownloadTask> mTasks = new ArrayList<>();
        
        public void setTasks(List<DownloadTask> tasks) {
            mTasks = tasks;
            notifyDataSetChanged();
        }
        
        public void updateTask(DownloadTask task) {
            for (int i = 0; i < mTasks.size(); i++) {
                if (mTasks.get(i).getTaskId().equals(task.getTaskId())) {
                    mTasks.set(i, task);
                    notifyItemChanged(i);
                    break;
                }
            }
        }
        
        @NonNull
        @Override
        public DownloadViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.orange_item_download_task, parent, false);
            return new DownloadViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull DownloadViewHolder holder, int position) {
            DownloadTask task = mTasks.get(position);
            holder.bind(task);
        }
        
        @Override
        public int getItemCount() {
            return mTasks.size();
        }
    }
    
    /**
     * 下载任务 ViewHolder
     */
    private class DownloadViewHolder extends RecyclerView.ViewHolder {
        
        TextView titleText;
        TextView statusText;
        TextView progressText;
        ProgressBar progressBar;
        ImageView actionButton;
        ImageView deleteButton;
        
        public DownloadViewHolder(@NonNull View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.tv_title);
            statusText = itemView.findViewById(R.id.tv_status);
            progressText = itemView.findViewById(R.id.tv_progress);
            progressBar = itemView.findViewById(R.id.progress_bar);
            actionButton = itemView.findViewById(R.id.btn_action);
            deleteButton = itemView.findViewById(R.id.btn_delete);
        }
        
        public void bind(DownloadTask task) {
            titleText.setText(task.getTitle());
            statusText.setText(task.getStateDescription());
            
            int state = task.getState();
            
            // 设置进度
            if (state == DownloadTask.STATE_DOWNLOADING || 
                state == DownloadTask.STATE_PAUSED) {
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setProgress(task.getProgress());
                progressText.setVisibility(View.VISIBLE);
                progressText.setText(String.format("%s / %s (%d%%)",
                        task.getFormattedDownloadedSize(),
                        task.getFormattedTotalSize(),
                        task.getProgress()));
            } else if (state == DownloadTask.STATE_COMPLETED) {
                progressBar.setVisibility(View.GONE);
                progressText.setVisibility(View.VISIBLE);
                progressText.setText(task.getFormattedTotalSize());
            } else {
                progressBar.setVisibility(View.GONE);
                progressText.setVisibility(View.GONE);
            }
            
            // 设置操作按钮
            if (state == DownloadTask.STATE_DOWNLOADING) {
                actionButton.setImageResource(android.R.drawable.ic_media_pause);
                actionButton.setVisibility(View.VISIBLE);
                actionButton.setOnClickListener(v -> {
                    mDownloadManager.pauseDownload(task.getTaskId());
                });
            } else if (state == DownloadTask.STATE_PAUSED || 
                       state == DownloadTask.STATE_FAILED) {
                actionButton.setImageResource(android.R.drawable.ic_media_play);
                actionButton.setVisibility(View.VISIBLE);
                actionButton.setOnClickListener(v -> {
                    mDownloadManager.resumeDownload(task.getTaskId());
                });
            } else {
                actionButton.setVisibility(View.GONE);
            }
            
            // 设置删除按钮
            deleteButton.setOnClickListener(v -> {
                mDownloadManager.cancelDownload(task.getTaskId());
            });
        }
    }
    
    /**
     * 释放资源
     */
    public void release() {
        if (mDownloadManager != null) {
            mDownloadManager.removeListener(mDownloadListener);
        }
    }
}
