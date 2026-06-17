package com.orange.playerlibrary.component;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
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

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.orange.playerlibrary.OrangeVideoController;
import com.orange.playerlibrary.PlayerConstants;
import com.orange.playerlibrary.R;
import com.orange.playerlibrary.interfaces.ControlWrapper;
import com.orange.playerlibrary.interfaces.IControlComponent;

import java.io.File;

/**
 * 视频播放准备视图组件
 * 用于显示视频加载前的缩略图、开始播放按钮、加载进度以及网络警告等状态
 * 
 * Requirements: 3.1
 */
public class PrepareView extends FrameLayout implements IControlComponent {
    
    private static final String TAG = "PrepareView";
    private static boolean sDebug = false;
    
    // 控制器包装类
    private ControlWrapper mControlWrapper;
    
    // UI 组件
    private ImageView mThumb;
    private ImageView mStartPlay;
    private FrameLayout mNetWarning;
    private ProgressBar mLoadingProgress;
    
    // Glide 请求选项
    private RequestOptions mGlideOptions;
    
    // 是否允许移动网络播放
    private static boolean sPlayOnMobileNetwork = false;

    public PrepareView(@NonNull Context context) {
        super(context);
        initView(context);
    }

    public PrepareView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public PrepareView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    /**
     * 初始化布局和控件
     */
    private void initView(Context context) {
        LayoutInflater.from(context).inflate(R.layout.orange_layout_prepare_view, this, true);
        
        // 初始化控件引用
        mThumb = findViewById(R.id.thumb);
        mStartPlay = findViewById(R.id.start_play);
        mNetWarning = findViewById(R.id.net_warning_layout);
        mLoadingProgress = findViewById(R.id.loading);
        
        // 设置网络警告按钮点击事件
        View statusBtn = findViewById(R.id.status_btn);
        if (statusBtn != null) {
            statusBtn.setOnClickListener(v -> {
                if (mNetWarning != null) {
                    mNetWarning.setVisibility(GONE);
                }
                sPlayOnMobileNetwork = true;
                if (mControlWrapper != null) {
                    mControlWrapper.start();
                }
            });
        }
        
        // 初始化 Glide 选项
        mGlideOptions = new RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.ALL);
    }
    
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        // 附加到窗口时，确保初始状态为 VISIBLE（准备视图应该默认显示）
        setVisibility(VISIBLE);
    }


    /**
     * 设置点击播放按钮时触发播放
     */
    public void setClickStart() {
        // 设置整个视图的点击事件，与参考实现一致
        setOnClickListener(v -> {
            // 只有在可见状态下才响应点击，避免横竖屏切换后误触发
            if (getVisibility() != VISIBLE) {
                return;
            }
            
            if (mControlWrapper != null) {
                mControlWrapper.start();
            }
        });
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
        switch (playState) {
            case PlayerConstants.STATE_ERROR:
            case PlayerConstants.STATE_PLAYING:
            case PlayerConstants.STATE_PAUSED:
            case PlayerConstants.STATE_PLAYBACK_COMPLETED:
            case PlayerConstants.STATE_BUFFERING:
            case PlayerConstants.STATE_BUFFERED:
            case PlayerConstants.STATE_PREPARED:
                // 隐藏准备视图（包括准备中状态）
                hideAllViews();
                break;
            case PlayerConstants.STATE_PREPARING:
                showPreparingView();
                break;
                
            case PlayerConstants.STATE_IDLE:
                // 显示准备视图
                showPrepareView();
                break;
                
            case PlayerConstants.STATE_STARTSNIFFING:
                // 嗅探开始 - 隐藏准备视图
                hideAllViews();
                break;
                
            case PlayerConstants.STATE_ENDSNIFFING:
                // 嗅探结束 - 保持隐藏，等待其他状态决定是否显示
                break;
                
            case PlayerConstants.STATE_M3U8_AD_REMOVAL:
                // M3U8去广告开始 - 隐藏准备视图
                hideAllViews();
                break;
                
            case PlayerConstants.STATE_M3U8_AD_REMOVAL_END:
                // M3U8去广告结束 - 保持隐藏，等待其他状态决定是否显示
                break;
                
            case 8: // 移动网络警告状态
                showNetWarning();
                break;
        }
    }
    
    /**
     * 隐藏所有视图
     */
    private void hideAllViews() {
        setVisibility(GONE);
        setClickable(false);
        if (mStartPlay != null) {
            mStartPlay.setVisibility(GONE);
        }
        if (mNetWarning != null) {
            mNetWarning.setVisibility(GONE);
        }
        if (mThumb != null) {
            mThumb.setVisibility(GONE);
        }
        if (mLoadingProgress != null) {
            mLoadingProgress.setVisibility(GONE);
        }
    }
    
    /**
     * 显示准备视图
     */
    private void showPrepareView() {
        if (mLoadingProgress != null) {
            mLoadingProgress.setVisibility(GONE);
        }
        if (mNetWarning != null) {
            mNetWarning.setVisibility(GONE);
        }
        if (mStartPlay != null) {
            mStartPlay.setVisibility(VISIBLE);
        }
        if (mThumb != null) {
            mThumb.setVisibility(VISIBLE);
        }
        setVisibility(VISIBLE);
        setClickable(true);
        bringToFront();
    }

    private void showPreparingView() {
        if (mLoadingProgress != null) {
            mLoadingProgress.setVisibility(GONE);
        }
        if (mNetWarning != null) {
            mNetWarning.setVisibility(GONE);
        }
        if (mStartPlay != null) {
            mStartPlay.setVisibility(GONE);
        }
        if (mThumb != null) {
            mThumb.setVisibility(GONE);
        }
        setVisibility(VISIBLE);
        setClickable(false);
        bringToFront();
    }
    
    /**
     * 显示网络警告
     */
    private void showNetWarning() {
        setVisibility(VISIBLE);
        if (mNetWarning != null) {
            mNetWarning.setVisibility(VISIBLE);
            mNetWarning.bringToFront();
        }
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

    // ===== 缩略图设置方法 =====

    /**
     * 设置缩略图 - 支持多种类型
     * @param source 缩略图源（Bitmap、资源ID、URL字符串、File对象）
     */
    public void setThumbnail(Object source) {
        if (mThumb == null || source == null) return;
        
        if (source instanceof Bitmap) {
            mThumb.setImageBitmap((Bitmap) source);
        } else if (source instanceof Integer) {
            mThumb.setImageResource((Integer) source);
        } else if (source instanceof String) {
            setThumbnailFromUrl((String) source);
        } else if (source instanceof File) {
            loadImageWithGlide(source, mThumb);
        } else {
            debug("不支持的缩略图源类型: " + source.getClass().getSimpleName());
        }
    }

    /**
     * 从URL设置缩略图
     */
    public void setThumbnailFromUrl(String url) {
        if (mThumb == null || url == null) return;
        
        if (url.startsWith("http://") || url.startsWith("https://")) {
            loadImageWithGlide(url, mThumb);
        } else if (url.startsWith("file://") || url.startsWith("/")) {
            String path = url.startsWith("file://") ? url.substring(7) : url;
            File file = new File(path);
            if (file.exists()) {
                loadImageWithGlide(file, mThumb);
            }
        } else {
            try {
                int resId = Integer.parseInt(url);
                mThumb.setImageResource(resId);
            } catch (NumberFormatException e) {
                loadResourceThumbnail(url);
            }
        }
    }

    /**
     * 使用 Glide 加载图片
     */
    private void loadImageWithGlide(Object source, ImageView imageView) {
        if (source == null || imageView == null) return;
        
        try {
            Glide.with(getContext())
                    .load(source)
                    .apply(mGlideOptions)
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                Target<Drawable> target, boolean isFirstResource) {
                            debug("Glide加载失败: " + (e != null ? e.getMessage() : "未知错误"));
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model,
                                Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            return false;
                        }
                    })
                    .into(imageView);
        } catch (Exception e) {
            debug("Glide加载异常: " + e.getMessage());
        }
    }

    /**
     * 通过资源名称加载缩略图
     */
    private void loadResourceThumbnail(String resourceName) {
        try {
            Context context = mThumb.getContext();
            int resId = context.getResources().getIdentifier(
                    resourceName, "drawable", context.getPackageName());
            if (resId != 0) {
                mThumb.setImageResource(resId);
            }
        } catch (Exception e) {
            debug("加载资源缩略图失败: " + e.getMessage());
        }
    }

    // ===== Glide 配置方法 =====

    public void setGlidePlaceholder(int placeholderResId) {
        mGlideOptions = mGlideOptions.placeholder(placeholderResId);
    }

    public void setGlideErrorImage(int errorResId) {
        mGlideOptions = mGlideOptions.error(errorResId);
    }

    public void setGlideDiskCacheStrategy(DiskCacheStrategy strategy) {
        mGlideOptions = mGlideOptions.diskCacheStrategy(strategy);
    }

    // ===== 获取组件 =====

    public ProgressBar getLoadingProgress() {
        return mLoadingProgress;
    }

    public ImageView getThumb() {
        return mThumb;
    }

    public ImageView getStartPlay() {
        return mStartPlay;
    }

    // ===== 静态方法 =====

    public static void setPlayOnMobileNetwork(boolean allow) {
        sPlayOnMobileNetwork = allow;
    }

    public static boolean isPlayOnMobileNetwork() {
        return sPlayOnMobileNetwork;
    }

    // ===== 调试相关 =====

    public void setDebug(boolean debug) {
        sDebug = debug;
    }

    private void debug(Object message) {
        if (sDebug && OrangeVideoController.isdebug()) {
        }
    }
}
