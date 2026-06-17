package com.orange.playerlibrary.download;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.orange.downloader.VideoDownloadManager;
import com.orange.downloader.model.VideoTaskItem;
import com.orange.downloader.model.VideoTaskState;
import com.orange.playerlibrary.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

/**
 * 下载任务列表适配器
 */
public class DownloadListAdapter extends RecyclerView.Adapter<DownloadListAdapter.ViewHolder> {
    
    private static final String TAG = "DownloadListAdapter";
    private static final Object sLock = new Object();  // 同步锁
    
    private Context mContext;
    private List<VideoTaskItem> mItems = new ArrayList<>();
    private OnItemClickListener mItemClickListener;
    private final Map<String, Bitmap> mThumbnailCache = new ConcurrentHashMap<>();
    
    public interface OnItemClickListener {
        void onItemClick(VideoTaskItem item);
        void onDeleteClick(VideoTaskItem item);
    }
    
    public DownloadListAdapter(Context context) {
        mContext = context;
    }
    
    public void setItems(List<VideoTaskItem> items) {
        synchronized (sLock) {
            mItems.clear();
            if (items != null) {
                mItems.addAll(items);
            }
        }
        notifyDataSetChanged();
    }
    
    public void updateItem(VideoTaskItem item) {
        synchronized (sLock) {
            for (int i = 0; i < mItems.size(); i++) {
                if (mItems.get(i).getUrl().equals(item.getUrl())) {
                    mItems.set(i, item);
                    notifyItemChanged(i);
                    return;
                }
            }
            // 新任务，添加到列表
            mItems.add(0, item);
            notifyItemInserted(0);
        }
    }
    
    public void removeItem(VideoTaskItem item) {
        synchronized (sLock) {
            for (int i = 0; i < mItems.size(); i++) {
                if (mItems.get(i).getUrl().equals(item.getUrl())) {
                    mItems.remove(i);
                    notifyItemRemoved(i);
                    notifyItemRangeChanged(i, mItems.size());
                    return;
                }
            }
        }
    }
    
    public void setOnItemClickListener(OnItemClickListener listener) {
        mItemClickListener = listener;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.orange_item_download_task, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // 防止并发修改导致崩溃
        VideoTaskItem item;
        synchronized (sLock) {
            if (position < 0 || position >= mItems.size()) {
                return;
            }
            item = mItems.get(position);
        }
        
        // 标题
        holder.tvTitle.setText(item.getTitle() != null ? item.getTitle() : "未知视频");
        
        // 缩略图
        loadThumbnail(holder, item);
        
        // 状态
        setStateText(holder.tvStatus, item);
        
        // 进度
        setProgressInfo(holder, item);
        
        // 操作按钮
        setActionButton(holder.btnAction, item);
        
        // 点击事件
        holder.itemView.setOnClickListener(v -> {
            handleActionClick(item);
        });
        
        holder.btnAction.setOnClickListener(v -> {
            handleActionClick(item);
        });
        
        holder.btnDelete.setOnClickListener(v -> {
            if (mItemClickListener != null) {
                mItemClickListener.onDeleteClick(item);
            }
        });
    }
    
    private void handleActionClick(VideoTaskItem item) {
        if (item.isCompleted() || item.getPercent() >= 99.9f) {
            if (mItemClickListener != null) {
                mItemClickListener.onItemClick(item);
            }
            return;
        }
        if (item.isInitialTask()) {
            // 初始状态 → 开始下载
            VideoDownloadManager.getInstance().startDownload(item);
        } else if (item.isRunningTask()) {
            // 下载中 → 暂停
            VideoDownloadManager.getInstance().pauseDownloadTask(item.getUrl());
        } else if (item.isInterruptTask()) {
            // 中断 → 恢复下载
            VideoDownloadManager.getInstance().resumeDownload(item.getUrl());
        }
    }
    
    private void setStateText(TextView tvStatus, VideoTaskItem item) {
        switch (item.getTaskState()) {
            case VideoTaskState.PENDING:
                tvStatus.setText("等待中");
                tvStatus.setTextColor(0xFFAAAAAA);
                break;
            case VideoTaskState.PREPARE:
                if (item.getPercent() >= 99.9f) {
                    tvStatus.setText("合并中");
                    tvStatus.setTextColor(0xFFFF9800);
                } else {
                    tvStatus.setText("等待中");
                    tvStatus.setTextColor(0xFFAAAAAA);
                }
                break;
            case VideoTaskState.START:
            case VideoTaskState.DOWNLOADING:
                tvStatus.setText("下载中");
                tvStatus.setTextColor(0xFF0082EC);
                break;
            case VideoTaskState.PAUSE:
                tvStatus.setText("已暂停");
                tvStatus.setTextColor(0xFFFF9800);
                break;
            case VideoTaskState.SUCCESS:
                tvStatus.setText("已完成");
                tvStatus.setTextColor(0xFF4CAF50);
                break;
            case VideoTaskState.ERROR:
                tvStatus.setText("下载失败");
                tvStatus.setTextColor(0xFFF44336);
                break;
            default:
                tvStatus.setText("未下载");
                tvStatus.setTextColor(0xFFAAAAAA);
                break;
        }
    }
    
    private void setProgressInfo(ViewHolder holder, VideoTaskItem item) {
        int progress = (int) item.getPercent();
        holder.progressBar.setProgress(progress);
        if (item.getTaskState() == VideoTaskState.PREPARE && item.getPercent() >= 99.9f) {
            holder.tvProgress.setText("分片已完成，正在合并 MP4...");
            return;
        }
        
        String sizeInfo = formatSize(item.getDownloadSize()) + " / " + formatSize(item.getTotalSize());
        String percentInfo = String.format("%.1f%%", item.getPercent());
        holder.tvProgress.setText(sizeInfo + " (" + percentInfo + ")");
        
        // 下载中显示速度
        if (item.getTaskState() == VideoTaskState.DOWNLOADING && item.getSpeed() > 0) {
            holder.tvProgress.setText(sizeInfo + " (" + percentInfo + ") - " + formatSpeed(item.getSpeed()));
        }
    }
    
    private void setActionButton(ImageView btnAction, VideoTaskItem item) {
        switch (item.getTaskState()) {
            case VideoTaskState.PENDING:
            case VideoTaskState.PREPARE:
            case VideoTaskState.START:
            case VideoTaskState.DOWNLOADING:
                btnAction.setImageResource(android.R.drawable.ic_media_pause);
                break;
            case VideoTaskState.PAUSE:
            case VideoTaskState.ERROR:
                btnAction.setImageResource(android.R.drawable.ic_media_play);
                break;
            case VideoTaskState.SUCCESS:
                // 下载完成显示播放图标
                btnAction.setImageResource(android.R.drawable.ic_media_play);
                btnAction.setColorFilter(0xFF4CAF50);  // 绿色
                break;
            default:
                btnAction.setImageResource(android.R.drawable.ic_media_play);
                break;
        }
    }
    
    private String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }
    
    private String formatSpeed(float speed) {
        if (speed < 1024) {
            return String.format("%.0f B/s", speed);
        } else if (speed < 1024 * 1024) {
            return String.format("%.1f KB/s", speed / 1024.0);
        } else {
            return String.format("%.1f MB/s", speed / (1024.0 * 1024.0));
        }
    }
    
    @Override
    public int getItemCount() {
        synchronized (sLock) {
            return mItems.size();
        }
    }
    
    // 线程池用于提取视频帧
    private ExecutorService mExecutor = Executors.newFixedThreadPool(2);
    
    /**
     * 加载缩略图
     * 优先级：封面URL > 本地视频帧 > 视频URL(Glide) > 默认图标
     * 对于m3u8类型，从本地ts分片或合并后的mp4提取帧
     */
    private void loadThumbnail(ViewHolder holder, VideoTaskItem item) {
        String coverUrl = item.getCoverUrl();
        String filePath = item.getFilePath();
        String saveDir = item.getSaveDir();
        int videoType = item.getVideoType();
        String key = buildThumbnailKey(item);
        if (key != null) {
            Bitmap cached = mThumbnailCache.get(key);
            if (cached != null) {
                holder.ivThumbnail.setImageBitmap(cached);
                holder.ivThumbnail.setTag(key);
                return;
            }
            Object tag = holder.ivThumbnail.getTag();
            if (tag != null && tag.equals(key)) {
                return;
            }
            holder.ivThumbnail.setTag(key);
        }
        
        // 1. 优先使用封面 URL
        if (coverUrl != null && !coverUrl.isEmpty()) {
            Glide.with(mContext)
                .load(coverUrl)
                .placeholder(R.drawable.ic_download)
                .error(R.drawable.ic_download)
                .centerCrop()
                .into(holder.ivThumbnail);
            return;
        }
        
        // 2. 已完成的本地视频，提取第一帧
        if (item.isCompleted() && filePath != null && new File(filePath).exists()) {
            loadLocalVideoFrame(holder, filePath, key);
            return;
        }
        
        // 3. m3u8类型，尝试从本地ts分片提取帧
        if (videoType == com.orange.downloader.model.Video.Type.HLS_TYPE && saveDir != null) {
            File tsFile = findFirstTsFile(saveDir);
            if (tsFile != null && tsFile.exists()) {
                loadLocalVideoFrame(holder, tsFile.getAbsolutePath(), key);
                return;
            }
        }
        
        // 4. 默认图标
        holder.ivThumbnail.setImageResource(R.drawable.ic_download);
    }
    
    /**
     * 从本地视频文件提取第一帧
     */
    private void loadLocalVideoFrame(ViewHolder holder, String filePath, String key) {
        // 检查线程池是否已终止
        if (mExecutor == null || mExecutor.isShutdown()) {
            return;
        }
        if (key != null) {
            Bitmap cached = mThumbnailCache.get(key);
            if (cached != null) {
                holder.ivThumbnail.setImageBitmap(cached);
                return;
            }
        }
        try {
            mExecutor.execute(() -> {
                try {
                    MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                    retriever.setDataSource(filePath);
                    Bitmap bitmap = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
                    retriever.release();
                    
                    if (bitmap != null && holder.getAdapterPosition() != RecyclerView.NO_POSITION) {
                        Bitmap finalBitmap = bitmap;
                        holder.ivThumbnail.post(() -> {
                            if (holder.getAdapterPosition() != RecyclerView.NO_POSITION) {
                                if (key != null) {
                                    Object tag = holder.ivThumbnail.getTag();
                                    if (tag != null && !tag.equals(key)) {
                                        return;
                                    }
                                    mThumbnailCache.put(key, finalBitmap);
                                }
                                holder.ivThumbnail.setImageBitmap(finalBitmap);
                            }
                        });
                    }
                } catch (Exception e) {
                    android.util.Log.e(TAG, "Failed to extract frame from: " + filePath + ", " + e.getMessage());
                }
            });
        } catch (RejectedExecutionException e) {
            // 线程池已终止，忽略
        }
    }
    
    /**
     * 查找目录下第一个ts文件
     */
    private File findFirstTsFile(String saveDir) {
        File dir = new File(saveDir);
        if (!dir.exists() || !dir.isDirectory()) return null;
        
        File[] files = dir.listFiles((d, name) -> 
            name.endsWith(".ts") || name.endsWith(".TS") || name.endsWith(".mp4") || name.endsWith(".MP4"));
        
        if (files != null && files.length > 0) {
            return files[0];
        }
        return null;
    }

    private String buildThumbnailKey(VideoTaskItem item) {
        String filePath = item.getFilePath();
        if (filePath != null && !filePath.isEmpty()) {
            return "file:" + filePath;
        }
        String saveDir = item.getSaveDir();
        if (saveDir != null && !saveDir.isEmpty()) {
            return "dir:" + saveDir;
        }
        String coverUrl = item.getCoverUrl();
        if (coverUrl != null && !coverUrl.isEmpty()) {
            return "cover:" + coverUrl;
        }
        String url = item.getUrl();
        if (url != null && !url.isEmpty()) {
            return "url:" + url;
        }
        return null;
    }
    
    /**
     * 清理资源
     */
    public void cleanup() {
        if (mExecutor != null && !mExecutor.isShutdown()) {
            mExecutor.shutdown();
        }
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivThumbnail;
        TextView tvTitle;
        TextView tvStatus;
        ProgressBar progressBar;
        TextView tvProgress;
        ImageView btnAction;
        ImageView btnDelete;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivThumbnail = itemView.findViewById(R.id.iv_thumbnail);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvStatus = itemView.findViewById(R.id.tv_status);
            progressBar = itemView.findViewById(R.id.progress_bar);
            tvProgress = itemView.findViewById(R.id.tv_progress);
            btnAction = itemView.findViewById(R.id.btn_action);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }
}
