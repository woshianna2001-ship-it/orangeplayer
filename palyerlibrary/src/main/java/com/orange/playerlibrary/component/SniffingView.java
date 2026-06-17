package com.orange.playerlibrary.component;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.orange.playerlibrary.OrangeVideoController;
import com.orange.playerlibrary.R;
import com.orange.playerlibrary.VideoSniffing;
import com.orange.playerlibrary.interfaces.ControlWrapper;
import com.orange.playerlibrary.interfaces.IControlComponent;
import com.orange.playerlibrary.sniffing.SniffingResultManager;

import java.util.ArrayList;
import java.util.List;

/**
 * 视频嗅探组件
 * 显示嗅探进度和嗅探结果，用户可以选择嗅探到的视频进行播放
 * 
 * 功能特性:
 * - 显示嗅探进度（进度条 + 状态文本）
 * - 显示嗅探结果列表（RecyclerView）
 * - 支持选择视频播放
 * - 支持删除单个结果
 * - 支持清空所有结果
 * - 结果持久化存储
 * - 符合现有组件主题风格
 */
public class SniffingView extends FrameLayout implements IControlComponent {
    
    private static final String TAG = "SniffingView";
    
    // 控制器包装类
    private ControlWrapper mControlWrapper;
    
    // UI 组件
    private View mRootView;
    private TextView mTvStatus;
    private ProgressBar mProgressBar;
    private RecyclerView mRecyclerView;
    private TextView mTvEmpty;
    private ImageView mBtnClose;
    private ImageView mBtnClear;
    
    // 数据
    private SniffingResultAdapter mAdapter;
    private List<VideoSniffing.VideoInfo> mVideoList = new ArrayList<>();
    // 移除持久化管理器，改为使用私有变量（app 重启失效）
    
    // 回调
    private OnVideoSelectedListener mOnVideoSelectedListener;

    public SniffingView(@NonNull Context context) {
        super(context);
        initView(context);
    }

    public SniffingView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public SniffingView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    /**
     * 初始化布局和控件
     */
    private void initView(Context context) {
        setVisibility(GONE);
        setClickable(true);
        
        mRootView = LayoutInflater.from(context).inflate(R.layout.orange_layout_sniffing_view, this, true);
        
        // 初始化控件引用
        mTvStatus = findViewById(R.id.tv_sniffing_status);
        mProgressBar = findViewById(R.id.progress_sniffing);
        mRecyclerView = findViewById(R.id.rv_sniffing_results);
        mTvEmpty = findViewById(R.id.tv_empty);
        mBtnClose = findViewById(R.id.btn_close);
        mBtnClear = findViewById(R.id.btn_clear);
        
        // 移除持久化管理器初始化
        
        // 设置 RecyclerView
        mRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        mAdapter = new SniffingResultAdapter(mVideoList);
        mRecyclerView.setAdapter(mAdapter);
        
        // 设置适配器回调
        mAdapter.setOnItemClickListener(new SniffingResultAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(VideoSniffing.VideoInfo videoInfo) {
                // 选择视频播放
                if (mOnVideoSelectedListener != null) {
                    mOnVideoSelectedListener.onVideoSelected(videoInfo);
                }
                hide();
            }

            @Override
            public void onDeleteClick(VideoSniffing.VideoInfo videoInfo, int position) {
                // 删除单个结果（仅从内存中删除）
                mVideoList.remove(position);
                mAdapter.notifyItemRemoved(position);
                updateEmptyView();
            }
        });
        
        // 关闭按钮
        mBtnClose.setOnClickListener(v -> hide());
        
        // 清空按钮
        mBtnClear.setOnClickListener(v -> clearAllResults());
        
        // 移除加载持久化结果的调用
    }

    @Override
    public void attach(@NonNull ControlWrapper controlWrapper) {
        mControlWrapper = controlWrapper;
    }

    @Override
    public View getView() {
        return this;
    }

    @Override
    public void onVisibilityChanged(boolean isVisible, Animation anim) {
        // 空实现
    }

    @Override
    public void onPlayStateChanged(int playState) {
        // 空实现
    }

    @Override
    public void onPlayerStateChanged(int playerState) {
        // 空实现
    }

    @Override
    public void setProgress(int duration, int position) {
        // 空实现
    }

    @Override
    public void onLockStateChanged(boolean isLocked) {
        // 空实现
    }

    // ===== 公共方法 =====

    /**
     * 显示嗅探视图
     */
    public void show() {
        setVisibility(VISIBLE);
        bringToFront();
    }

    /**
     * 隐藏嗅探视图
     */
    public void hide() {
        setVisibility(GONE);
    }

    /**
     * 是否正在显示
     */
    public boolean isShowing() {
        return getVisibility() == VISIBLE;
    }

    /**
     * 开始嗅探
     */
    public void startSniffing() {
        mTvStatus.setText("正在嗅探视频...");
        mProgressBar.setVisibility(VISIBLE);
        mBtnClear.setVisibility(GONE);
    }

    /**
     * 嗅探完成
     */
    public void finishSniffing(int count) {
        mProgressBar.setVisibility(GONE);
        mBtnClear.setVisibility(VISIBLE);
        
        if (count > 0) {
            mTvStatus.setText("嗅探完成，共发现 " + count + " 个视频");
        } else {
            mTvStatus.setText("嗅探完成，未发现视频");
        }
    }

    /**
     * 添加嗅探结果（仅存储在内存中）
     */
    public void addSniffingResult(VideoSniffing.VideoInfo videoInfo) {
        // 去重检查
        for (VideoSniffing.VideoInfo info : mVideoList) {
            if (info.url.equals(videoInfo.url)) {
                return;
            }
        }
        
        mVideoList.add(videoInfo);
        mAdapter.notifyItemInserted(mVideoList.size() - 1);
        
        updateEmptyView();
    }

    /**
     * 设置嗅探结果列表（仅存储在内存中）
     */
    public void setSniffingResults(List<VideoSniffing.VideoInfo> results) {
        mVideoList.clear();
        if (results != null) {
            mVideoList.addAll(results);
        }
        mAdapter.notifyDataSetChanged();
        
        updateEmptyView();
    }

    /**
     * 清空所有结果（仅清空内存）
     */
    public void clearAllResults() {
        mVideoList.clear();
        mAdapter.notifyDataSetChanged();
        updateEmptyView();
        mTvStatus.setText("已清空所有嗅探结果");
    }

    /**
     * 获取嗅探结果数量
     */
    public int getResultCount() {
        return mVideoList.size();
    }

    /**
     * 更新空视图显示
     */
    private void updateEmptyView() {
        if (mVideoList.isEmpty()) {
            mTvEmpty.setVisibility(VISIBLE);
            mRecyclerView.setVisibility(GONE);
        } else {
            mTvEmpty.setVisibility(GONE);
            mRecyclerView.setVisibility(VISIBLE);
        }
    }

    /**
     * 设置视频选择监听器
     */
    public void setOnVideoSelectedListener(OnVideoSelectedListener listener) {
        mOnVideoSelectedListener = listener;
    }

    /**
     * 视频选择监听器接口
     */
    public interface OnVideoSelectedListener {
        void onVideoSelected(VideoSniffing.VideoInfo videoInfo);
    }

    // ===== 适配器 =====

    private static class SniffingResultAdapter extends RecyclerView.Adapter<SniffingResultAdapter.ViewHolder> {

        private final List<VideoSniffing.VideoInfo> mList;
        private OnItemClickListener mListener;

        interface OnItemClickListener {
            void onItemClick(VideoSniffing.VideoInfo videoInfo);
            void onDeleteClick(VideoSniffing.VideoInfo videoInfo, int position);
        }

        SniffingResultAdapter(List<VideoSniffing.VideoInfo> list) {
            mList = list;
        }

        void setOnItemClickListener(OnItemClickListener listener) {
            mListener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.orange_item_sniffing_result, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            VideoSniffing.VideoInfo videoInfo = mList.get(position);
            holder.bind(videoInfo, mListener, position);
        }

        @Override
        public int getItemCount() {
            return mList.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvUrl, tvType;
            ImageView btnDelete;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tv_video_title);
                tvUrl = itemView.findViewById(R.id.tv_video_url);
                tvType = itemView.findViewById(R.id.tv_video_type);
                btnDelete = itemView.findViewById(R.id.btn_delete);
            }

            void bind(VideoSniffing.VideoInfo videoInfo, OnItemClickListener listener, int position) {
                // 标题
                String title = videoInfo.title;
                if (title == null || title.isEmpty() || title.equals("null")) {
                    title = "视频 " + (position + 1);
                }
                tvTitle.setText(title);

                // URL（截断显示）
                String url = videoInfo.url;
                if (url.length() > 60) {
                    url = url.substring(0, 57) + "...";
                }
                tvUrl.setText(url);

                // 类型
                String type = videoInfo.contentType;
                if (type != null && !type.isEmpty()) {
                    tvType.setText(type);
                    tvType.setVisibility(View.VISIBLE);
                } else {
                    tvType.setVisibility(View.GONE);
                }

                // 点击播放
                itemView.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onItemClick(videoInfo);
                    }
                });

                // 删除按钮
                btnDelete.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onDeleteClick(videoInfo, position);
                    }
                });
            }
        }
    }
}
