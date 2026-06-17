package com.orange.player.tv.ui;

import android.os.Bundle;

import androidx.leanback.app.BrowseSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;

import com.orange.player.tv.model.Video;
import com.orange.player.tv.presenter.VideoCardPresenter;

import java.util.ArrayList;
import java.util.List;

/**
 * TV 主界面 Fragment
 * 使用 Leanback BrowseSupportFragment 展示视频列表
 */
public class TvMainFragment extends BrowseSupportFragment {
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        
        setupUI();
        loadData();
    }
    
    private void setupUI() {
        // 设置标题
        setTitle("OrangePlayer TV");
        
        // 设置头部颜色
        setBrandColor(getResources().getColor(android.R.color.holo_orange_dark));
        
        // 设置搜索图标颜色
        setSearchAffordanceColor(getResources().getColor(android.R.color.white));
    }
    
    private void loadData() {
        ArrayObjectAdapter rowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        
        // 示例视频列表
        List<Video> demoVideos = createDemoVideos();
        
        // 添加"示例视频"分类
        HeaderItem header1 = new HeaderItem(0, "示例视频");
        ArrayObjectAdapter listRowAdapter1 = new ArrayObjectAdapter(new VideoCardPresenter());
        for (Video video : demoVideos) {
            listRowAdapter1.add(video);
        }
        rowsAdapter.add(new ListRow(header1, listRowAdapter1));
        
        // 添加"测试视频"分类
        HeaderItem header2 = new HeaderItem(1, "测试视频");
        ArrayObjectAdapter listRowAdapter2 = new ArrayObjectAdapter(new VideoCardPresenter());
        List<Video> testVideos = createTestVideos();
        for (Video video : testVideos) {
            listRowAdapter2.add(video);
        }
        rowsAdapter.add(new ListRow(header2, listRowAdapter2));
        
        setAdapter(rowsAdapter);
        
        // 设置点击监听
        setOnItemViewClickedListener((itemViewHolder, item, rowViewHolder, row) -> {
            if (item instanceof Video) {
                playVideo((Video) item);
            }
        });
    }
    
    /**
     * 创建示例视频列表
     */
    private List<Video> createDemoVideos() {
        List<Video> videos = new ArrayList<>();
        
        // 使用稳定的测试视频
        videos.add(new Video(
                "阿里云测试视频",
                "http://player.alicdn.com/video/aliyunmedia.mp4",
                "",
                "00:30"
        ));
        
        videos.add(new Video(
                "Sintel 预告片",
                "https://media.w3.org/2010/05/sintel/trailer.mp4",
                "",
                "00:52"
        ));
        
        videos.add(new Video(
                "测试视频 3",
                "http://vfx.mtime.cn/Video/2019/03/21/mp4/190321153853126488.mp4",
                "",
                "00:15"
        ));
        
        videos.add(new Video(
                "测试视频 4",
                "http://vfx.mtime.cn/Video/2019/03/19/mp4/190319222227698228.mp4",
                "",
                "00:15"
        ));
        
        return videos;
    }
    
    /**
     * 创建测试视频列表
     */
    private List<Video> createTestVideos() {
        List<Video> videos = new ArrayList<>();
        
        // 添加更多测试视频
        videos.add(new Video(
                "测试视频 5",
                "http://vfx.mtime.cn/Video/2019/03/18/mp4/190318231014076505.mp4",
                "",
                "00:15"
        ));
        
        videos.add(new Video(
                "测试视频 6",
                "http://vfx.mtime.cn/Video/2019/03/14/mp4/190314223540373995.mp4",
                "",
                "00:15"
        ));
        
        videos.add(new Video(
                "测试视频 7",
                "http://vfx.mtime.cn/Video/2019/03/13/mp4/190313094901111138.mp4",
                "",
                "00:15"
        ));
        
        videos.add(new Video(
                "测试视频 8",
                "http://vfx.mtime.cn/Video/2019/03/12/mp4/190312083533415853.mp4",
                "",
                "00:15"
        ));
        
        return videos;
    }
    
    /**
     * 播放视频
     */
    private void playVideo(Video video) {
        if (getActivity() instanceof TvMainActivity) {
            ((TvMainActivity) getActivity()).playVideo(video.getUrl(), video.getTitle());
        }
    }
}
