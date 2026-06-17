package com.orange.player;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.orange.playerlibrary.OrangevideoView;
import com.orange.playerlibrary.PlayerSettingsManager;
import com.shuyu.gsyvideoplayer.utils.GSYVideoType;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * 播放器状态管理集成测试
 * 
 * 测试完整的播放流程、配置改变场景和视频比例功能
 * 
 * Requirements: 所有需求 (1.1-8.4)
 */
@RunWith(AndroidJUnit4.class)
public class PlayerStateManagementIntegrationTest {

    private Context context;
    private PlayerSettingsManager settingsManager;
    private ActivityScenario<MainActivity> scenario;
    
    // 测试视频 URL
    private static final String TEST_VIDEO_URL = "http://vfx.mtime.cn/Video/2019/03/21/mp4/190321153853126488.mp4";
    
    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        settingsManager = PlayerSettingsManager.getInstance(context);
        
        // 清除之前的设置
        settingsManager.clearAllSettings();
        
        // 启动 MainActivity
        scenario = ActivityScenario.launch(MainActivity.class);
    }
    
    @After
    public void tearDown() {
        if (scenario != null) {
            scenario.close();
        }
        
        // 清理设置
        if (settingsManager != null) {
            settingsManager.clearAllSettings();
        }
    }
    
    /**
     * 测试 1: 完整播放流程
     * 
     * 验证从初始化到播放完成的完整流程
     * Requirements: 1.1, 1.2, 2.1, 3.1, 5.1
     */
    @Test
    public void testCompletePlaybackFlow() throws InterruptedException {
        scenario.onActivity(activity -> {
            OrangevideoView videoView = activity.findViewById(R.id.video_player);
            assertNotNull("VideoView should not be null", videoView);
            
            // 设置视频 URL
            videoView.setUp(TEST_VIDEO_URL, true, "测试视频");
            
            // 开始播放
            videoView.startPlayLogic();
            
            // 等待视频准备完成
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            
            // 验证播放状态
            assertTrue("Video should be playing or preparing", 
                videoView.isPlaying() || videoView.getCurrentState() == OrangevideoView.CURRENT_STATE_PREPAREING);
        });
    }
    
    /**
     * 测试 2: 视频比例记忆功能
     * 
     * 验证视频比例设置能够被保存和恢复
     * Requirements: 1.1, 1.2, 1.3, 1.4
     */
    @Test
    public void testVideoScaleMemory() throws InterruptedException {
        // 测试所有视频比例类型
        String[] scaleTypes = {"默认", "16:9", "4:3", "全屏裁剪", "全屏拉伸"};
        
        for (String scaleType : scaleTypes) {
            // 保存视频比例设置
            settingsManager.setVideoScale(scaleType);
            
            // 验证设置已保存
            assertEquals("Video scale should be saved", scaleType, settingsManager.getVideoScale());
            
            // 重新启动 Activity 模拟重新播放
            scenario.recreate();
            
            scenario.onActivity(activity -> {
                OrangevideoView videoView = activity.findViewById(R.id.video_player);
                assertNotNull("VideoView should not be null", videoView);
                
                // 设置视频并准备
                videoView.setUp(TEST_VIDEO_URL, true, "测试视频");
                
                // 等待准备完成
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                
                // 验证视频比例已应用
                int expectedType = getVideoTypeForScale(scaleType);
                assertEquals("Video scale should be applied", expectedType, videoView.getVideoType());
            });
        }
    }
    
    /**
     * 测试 3: 屏幕旋转状态保持
     * 
     * 验证屏幕旋转时播放状态和位置保持
     * Requirements: 2.1, 2.2, 2.3, 2.4, 5.1, 5.2, 5.3, 5.4
     */
    @Test
    public void testScreenRotationStatePreservation() throws InterruptedException {
        scenario.onActivity(activity -> {
            OrangevideoView videoView = activity.findViewById(R.id.video_player);
            assertNotNull("VideoView should not be null", videoView);
            
            // 设置视频并开始播放
            videoView.setUp(TEST_VIDEO_URL, true, "测试视频");
            videoView.startPlayLogic();
            
            // 等待播放开始
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            
            // 保存当前播放位置和状态
            long positionBefore = videoView.getCurrentPositionWhenPlaying();
            boolean wasPlaying = videoView.isPlaying();
            
            // 模拟屏幕旋转
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            
            // 等待旋转完成
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            
            // 验证播放位置保持（允许1秒误差）
            long positionAfter = videoView.getCurrentPositionWhenPlaying();
            assertTrue("Playback position should be preserved within 1 second", 
                Math.abs(positionAfter - positionBefore) < 1000);
            
            // 验证播放状态保持
            assertEquals("Playback state should be preserved", wasPlaying, videoView.isPlaying());
            
            // 旋转回竖屏
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            
            // 等待旋转完成
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            
            // 再次验证状态保持
            long positionFinal = videoView.getCurrentPositionWhenPlaying();
            assertTrue("Playback position should still be preserved", 
                Math.abs(positionFinal - positionBefore) < 2000);
        });
    }
    
    /**
     * 测试 4: 暂停状态下全屏切换
     * 
     * 验证暂停状态下切换全屏不出现黑屏
     * Requirements: 2.1, 2.2, 2.5, 7.1, 7.2, 7.3, 7.4, 7.5
     */
    @Test
    public void testFullscreenToggleWhilePaused() throws InterruptedException {
        scenario.onActivity(activity -> {
            OrangevideoView videoView = activity.findViewById(R.id.video_player);
            assertNotNull("VideoView should not be null", videoView);
            
            // 设置视频并开始播放
            videoView.setUp(TEST_VIDEO_URL, true, "测试视频");
            videoView.startPlayLogic();
            
            // 等待播放开始
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            
            // 暂停播放
            videoView.onVideoPause();
            
            // 等待暂停生效
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            
            // 验证已暂停
            assertFalse("Video should be paused", videoView.isPlaying());
            
            // 保存当前播放位置
            long positionBefore = videoView.getCurrentPositionWhenPlaying();
            
            // 切换到全屏
            videoView.startWindowFullscreen(activity, true, true);
            
            // 等待全屏切换完成
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            
            // 验证仍然暂停
            assertFalse("Video should still be paused after fullscreen", videoView.isPlaying());
            
            // 验证播放位置保持
            long positionAfter = videoView.getCurrentPositionWhenPlaying();
            assertTrue("Playback position should be preserved", 
                Math.abs(positionAfter - positionBefore) < 1000);
            
            // 验证视频画面不是黑屏（通过检查播放器状态）
            assertTrue("Video surface should be valid", 
                videoView.getCurrentState() != OrangevideoView.CURRENT_STATE_ERROR);
            
            // 退出全屏
            videoView.backFromWindowFull(activity);
            
            // 等待退出全屏完成
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            
            // 再次验证状态保持
            assertFalse("Video should still be paused after exiting fullscreen", videoView.isPlaying());
            long positionFinal = videoView.getCurrentPositionWhenPlaying();
            assertTrue("Playback position should still be preserved", 
                Math.abs(positionFinal - positionBefore) < 1000);
        });
    }
    
    /**
     * 测试 5: 进度条更新连续性
     * 
     * 验证进度条在各种场景下持续更新
     * Requirements: 3.2, 3.3, 3.4, 3.5, 6.1, 6.2, 6.3, 6.4
     */
    @Test
    public void testProgressUpdateContinuity() throws InterruptedException {
        scenario.onActivity(activity -> {
            OrangevideoView videoView = activity.findViewById(R.id.video_player);
            assertNotNull("VideoView should not be null", videoView);
            
            // 设置视频并开始播放
            videoView.setUp(TEST_VIDEO_URL, true, "测试视频");
            videoView.startPlayLogic();
            
            // 等待播放开始
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            
            // 记录初始位置
            long position1 = videoView.getCurrentPositionWhenPlaying();
            
            // 等待1秒
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            
            // 验证进度已更新
            long position2 = videoView.getCurrentPositionWhenPlaying();
            assertTrue("Progress should update during playback", position2 > position1);
            
            // 切换全屏
            videoView.startWindowFullscreen(activity, true, true);
            
            // 等待全屏切换完成
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            
            // 记录全屏后位置
            long position3 = videoView.getCurrentPositionWhenPlaying();
            
            // 等待1秒
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            
            // 验证全屏后进度继续更新
            long position4 = videoView.getCurrentPositionWhenPlaying();
            assertTrue("Progress should continue updating in fullscreen", position4 > position3);
        });
    }
    
    /**
     * 测试 6: 组件状态同步
     * 
     * 验证所有组件状态在配置改变后保持同步
     * Requirements: 4.1, 4.2, 4.3, 4.4, 4.5
     */
    @Test
    public void testComponentStateSync() throws InterruptedException {
        scenario.onActivity(activity -> {
            OrangevideoView videoView = activity.findViewById(R.id.video_player);
            assertNotNull("VideoView should not be null", videoView);
            
            // 设置视频并开始播放
            videoView.setUp(TEST_VIDEO_URL, true, "测试视频");
            videoView.startPlayLogic();
            
            // 等待播放开始
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            
            // 保存当前状态
            long positionBefore = videoView.getCurrentPositionWhenPlaying();
            boolean wasPlaying = videoView.isPlaying();
            
            // 触发配置改变（模拟屏幕旋转）
            Configuration newConfig = new Configuration(activity.getResources().getConfiguration());
            newConfig.orientation = Configuration.ORIENTATION_LANDSCAPE;
            activity.onConfigurationChanged(newConfig);
            
            // 等待配置改变处理完成
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            
            // 验证播放状态同步
            assertEquals("Playback state should be synced", wasPlaying, videoView.isPlaying());
            
            // 验证播放位置同步
            long positionAfter = videoView.getCurrentPositionWhenPlaying();
            assertTrue("Playback position should be synced", 
                Math.abs(positionAfter - positionBefore) < 1000);
        });
    }
    
    /**
     * 测试 7: 错误恢复机制
     * 
     * 验证播放器能够从错误状态恢复
     * Requirements: 8.1, 8.2, 8.3, 8.4
     */
    @Test
    public void testErrorRecovery() throws InterruptedException {
        scenario.onActivity(activity -> {
            OrangevideoView videoView = activity.findViewById(R.id.video_player);
            assertNotNull("VideoView should not be null", videoView);
            
            // 设置一个无效的视频 URL
            videoView.setUp("http://invalid.url/video.mp4", true, "测试视频");
            videoView.startPlayLogic();
            
            // 等待错误发生
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            
            // 验证进入错误状态
            assertTrue("Should be in error state", 
                videoView.getCurrentState() == OrangevideoView.CURRENT_STATE_ERROR ||
                videoView.getCurrentState() == OrangevideoView.CURRENT_STATE_AUTO_COMPLETE);
            
            // 设置有效的视频 URL 并重试
            videoView.setUp(TEST_VIDEO_URL, true, "测试视频");
            videoView.startPlayLogic();
            
            // 等待恢复
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            
            // 验证已恢复正常
            assertTrue("Should recover from error", 
                videoView.isPlaying() || videoView.getCurrentState() == OrangevideoView.CURRENT_STATE_PREPAREING);
        });
    }
    
    /**
     * 辅助方法：根据比例名称获取 GSYVideoType
     */
    private int getVideoTypeForScale(String scaleType) {
        switch (scaleType) {
            case "16:9":
                return GSYVideoType.SCREEN_TYPE_16_9;
            case "4:3":
                return GSYVideoType.SCREEN_TYPE_4_3;
            case "全屏裁剪":
                return GSYVideoType.SCREEN_TYPE_FULL;
            case "全屏拉伸":
                return GSYVideoType.SCREEN_MATCH_FULL;
            case "默认":
            default:
                return GSYVideoType.SCREEN_TYPE_DEFAULT;
        }
    }
}
