package com.orange.player;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.orange.playerlibrary.OrangevideoView;
import com.orange.playerlibrary.PlayerSettingsManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * 播放器状态管理集成测试（轻量稳定版）
 * 目标：恢复 androidTest 编译与基础运行链路。
 */
@RunWith(AndroidJUnit4.class)
public class PlayerStateManagementIntegrationTest {

    private static final String PREF_NAME = "orange_player_settings";

    private Context context;
    private PlayerSettingsManager settingsManager;
    private ActivityScenario<MainActivity> scenario;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        settingsManager = PlayerSettingsManager.getInstance(context);
        clearSettings();
        scenario = ActivityScenario.launch(MainActivity.class);
    }

    @After
    public void tearDown() {
        if (scenario != null) {
            scenario.close();
        }
        clearSettings();
    }

    @Test
    public void testMainActivityAndVideoViewBinding() {
        scenario.onActivity(activity -> {
            OrangevideoView videoView = activity.findViewById(R.id.video_view);
            assertNotNull("VideoView should be bound from activity_main", videoView);
        });
    }

    @Test
    public void testVideoScaleSettingPersistence() {
        String[] scales = new String[]{"默认", "16:9", "4:3", "全屏裁剪", "全屏拉伸"};
        for (String scale : scales) {
            settingsManager.setVideoScale(scale);
            assertEquals("Video scale should persist", scale, settingsManager.getVideoScale());
        }
    }

    @Test
    public void testPortraitFullscreenToggleState() {
        scenario.onActivity(activity -> {
            OrangevideoView videoView = activity.findViewById(R.id.video_view);
            assertNotNull(videoView);

            videoView.startPortraitFullScreen();
            assertTrue("Should enter portrait fullscreen", videoView.isPortraitFullScreen());

            videoView.stopPortraitFullScreen();
            assertFalse("Should exit portrait fullscreen", videoView.isPortraitFullScreen());
        });
    }

    private void clearSettings() {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sp.edit().clear().commit();
    }
}
