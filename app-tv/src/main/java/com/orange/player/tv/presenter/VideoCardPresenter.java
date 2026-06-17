package com.orange.player.tv.presenter;

import android.view.ViewGroup;

import androidx.leanback.widget.ImageCardView;
import androidx.leanback.widget.Presenter;

import com.bumptech.glide.Glide;
import com.orange.player.tv.R;
import com.orange.player.tv.model.Video;

/**
 * 视频卡片 Presenter
 * 用于在 Leanback 界面中展示视频卡片
 */
public class VideoCardPresenter extends Presenter {
    
    private static final int CARD_WIDTH = 313;
    private static final int CARD_HEIGHT = 176;
    
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        ImageCardView cardView = new ImageCardView(parent.getContext());
        cardView.setFocusable(true);
        cardView.setFocusableInTouchMode(true);
        return new ViewHolder(cardView);
    }
    
    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Object item) {
        Video video = (Video) item;
        ImageCardView cardView = (ImageCardView) viewHolder.view;
        
        // 设置标题
        cardView.setTitleText(video.getTitle());
        
        // 设置时长
        if (video.getDuration() != null && !video.getDuration().isEmpty()) {
            cardView.setContentText(video.getDuration());
        }
        
        // 设置卡片尺寸
        cardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT);
        
        // 加载缩略图
        if (video.getThumbnailUrl() != null && !video.getThumbnailUrl().isEmpty()) {
            Glide.with(cardView.getContext())
                    .load(video.getThumbnailUrl())
                    .placeholder(R.drawable.ic_video_placeholder)
                    .error(R.drawable.ic_video_placeholder)
                    .into(cardView.getMainImageView());
        } else {
            // 使用默认占位图
            cardView.getMainImageView().setImageResource(R.drawable.ic_video_placeholder);
        }
    }
    
    @Override
    public void onUnbindViewHolder(ViewHolder viewHolder) {
        ImageCardView cardView = (ImageCardView) viewHolder.view;
        cardView.setBadgeImage(null);
        cardView.setMainImage(null);
    }
}
