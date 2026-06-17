package com.orange.player;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.orange.downloader.merge.MergeFeatureToggle;
import com.orange.downloader.merge.TsMergeService;
import com.orange.downloader.model.VideoTaskItem;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class TsMergeServiceE2ETest {

    private Context context;
    private File baseDir;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        MergeFeatureToggle.initialize(context);
        baseDir = new File(context.getCacheDir(), "ts-merge-e2e-" + UUID.randomUUID());
        if (!baseDir.exists()) {
            baseDir.mkdirs();
        }
    }

    @After
    public void tearDown() {
        MergeFeatureToggle.setFfmpegMergeEnabled(context, true);
        MergeFeatureToggle.setJavaFallbackEnabled(context, true);
        deleteRecursively(baseDir);
    }

    @Test
    public void plainM3u8_shouldMergeSuccessfully() throws Exception {
        MergeFeatureToggle.setFfmpegMergeEnabled(context, false);
        MergeFeatureToggle.setJavaFallbackEnabled(context, true);

        File caseDir = createCaseDir("plain");
        String hash = "plain_hash";
        writeBytes(new File(caseDir, "seg1.ts"), "plain-seg-1".getBytes(StandardCharsets.UTF_8));
        writeBytes(new File(caseDir, "seg2.ts"), "plain-seg-2".getBytes(StandardCharsets.UTF_8));
        writeText(new File(caseDir, hash + "_local.m3u8"),
                "#EXTM3U\n" +
                "#EXTINF:5,\n" +
                "seg1.ts\n" +
                "#EXTINF:5,\n" +
                "seg2.ts\n");

        VideoTaskItem taskItem = createTaskItem(caseDir, hash, "plain-case");
        VideoTaskItem merged = runMerge(taskItem);

        assertNotNull(merged);
        assertEquals(TsMergeService.MERGE_OK, merged.getErrorCode());
        assertTrue(new File(merged.getFilePath()).exists());
        assertTrue(new File(merged.getFilePath()).length() > 0);
    }

    @Test
    public void aes128TaggedM3u8_shouldMergeSuccessfully() throws Exception {
        MergeFeatureToggle.setFfmpegMergeEnabled(context, false);
        MergeFeatureToggle.setJavaFallbackEnabled(context, true);

        File caseDir = createCaseDir("aes128");
        String hash = "aes_hash";
        writeBytes(new File(caseDir, "enc.key"), "0123456789abcdef".getBytes(StandardCharsets.UTF_8));
        writeBytes(new File(caseDir, "enc1.ts"), "enc-seg-1".getBytes(StandardCharsets.UTF_8));
        writeBytes(new File(caseDir, "enc2.ts"), "enc-seg-2".getBytes(StandardCharsets.UTF_8));
        writeText(new File(caseDir, hash + "_local.m3u8"),
                "#EXTM3U\n" +
                "#EXT-X-VERSION:3\n" +
                "#EXT-X-KEY:METHOD=AES-128,URI=\"enc.key\",IV=0x00000000000000000000000000000001\n" +
                "#EXTINF:4,\n" +
                "enc1.ts\n" +
                "#EXTINF:4,\n" +
                "enc2.ts\n");

        VideoTaskItem taskItem = createTaskItem(caseDir, hash, "aes-case");
        VideoTaskItem merged = runMerge(taskItem);

        assertNotNull(merged);
        assertEquals(TsMergeService.MERGE_OK, merged.getErrorCode());
        assertTrue(new File(merged.getFilePath()).exists());
        assertTrue(new File(merged.getFilePath()).length() > 0);
    }

    @Test
    public void invalidM3u8_shouldReturnErrorCode() throws Exception {
        MergeFeatureToggle.setFfmpegMergeEnabled(context, false);
        MergeFeatureToggle.setJavaFallbackEnabled(context, true);

        File caseDir = createCaseDir("invalid");
        String hash = "invalid_hash";
        writeText(new File(caseDir, hash + "_local.m3u8"),
                "#EXTM3U\n" +
                "#EXTINF:5,\n" +
                "missing-seg.ts\n");

        VideoTaskItem taskItem = createTaskItem(caseDir, hash, "invalid-case");
        VideoTaskItem merged = runMerge(taskItem);

        assertNotNull(merged);
        assertEquals(TsMergeService.MERGE_ERROR_OUTPUT_EMPTY, merged.getErrorCode());
    }

    private File createCaseDir(String name) {
        File dir = new File(baseDir, name);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    private VideoTaskItem createTaskItem(File saveDir, String fileHash, String title) {
        VideoTaskItem taskItem = new VideoTaskItem("https://example.com/" + title + ".m3u8");
        taskItem.setSaveDir(saveDir.getAbsolutePath());
        taskItem.setFileHash(fileHash);
        taskItem.setTitle(title);
        return taskItem;
    }

    private VideoTaskItem runMerge(VideoTaskItem taskItem) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        VideoTaskItem[] result = new VideoTaskItem[1];
        TsMergeService service = new TsMergeService();
        service.merge(taskItem, item -> {
            result[0] = item;
            latch.countDown();
        });
        assertTrue("merge callback timeout", latch.await(10, TimeUnit.SECONDS));
        return result[0];
    }

    private void writeText(File file, String content) throws Exception {
        writeBytes(file, content.getBytes(StandardCharsets.UTF_8));
    }

    private void writeBytes(File file, byte[] bytes) throws Exception {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(bytes);
            fos.flush();
        }
    }

    private void deleteRecursively(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        file.delete();
    }
}
