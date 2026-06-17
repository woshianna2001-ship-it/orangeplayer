package com.orange.player.tv.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.FragmentActivity;

import com.orange.player.tv.R;

/**
 * TV 主界面 Activity
 * 使用 Leanback Fragment 展示视频列表
 */
public class TvMainActivity extends FragmentActivity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tv_main);
        
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main_browse_fragment, new TvMainFragment())
                    .commitNow();
        }
    }
    
    /**
     * 启动播放器
     */
    public void playVideo(String videoUrl, String videoTitle) {
        Intent intent = new Intent(this, TvPlayerActivity.class);
        intent.putExtra("video_url", videoUrl);
        intent.putExtra("video_title", videoTitle);
        startActivity(intent);
    }
}
