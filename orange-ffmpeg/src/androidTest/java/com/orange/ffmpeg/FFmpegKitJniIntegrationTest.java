package com.orange.ffmpeg;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class FFmpegKitJniIntegrationTest {

    @Test
    public void initAndVersion_shouldUseNativePath() {
        int init = FFmpegKit.init();
        assertEquals(FFmpegKit.RESULT_OK, init);
        assertFalse("native lib should be loaded in integration test", FFmpegKit.isStubMode());

        String version = FFmpegKit.getVersion();
        assertTrue(version != null && version.startsWith("jni-stub-"));
    }

    @Test
    public void executeVersion_shouldReturnOk() {
        int init = FFmpegKit.init();
        assertEquals(FFmpegKit.RESULT_OK, init);

        int execute = FFmpegKit.execute(new String[]{"-version"});
        assertEquals(FFmpegKit.RESULT_OK, execute);
    }

    @Test
    public void executeLocalTsMergeCommand_shouldGenerateOutput() throws Exception {
        int init = FFmpegKit.init();
        assertEquals(FFmpegKit.RESULT_OK, init);

        File caseDir = new File(InstrumentationRegistry.getInstrumentation().getTargetContext().getCacheDir(),
                "ffmpeg-jni-merge-" + UUID.randomUUID());
        if (!caseDir.exists()) {
            caseDir.mkdirs();
        }

        File ts1 = new File(caseDir, "1.ts");
        File ts2 = new File(caseDir, "2.ts");
        File m3u8 = new File(caseDir, "local.m3u8");
        File out = new File(caseDir, "output.mp4");

        write(ts1, "seg-1");
        write(ts2, "seg-2");
        write(m3u8, "#EXTM3U\n#EXTINF:4,\n1.ts\n#EXTINF:4,\n2.ts\n");

        String[] command = new String[]{
                "-allowed_extensions", "ALL",
                "-protocol_whitelist", "file,http,https,tcp,tls,crypto,data",
                "-i", m3u8.getAbsolutePath(),
                "-c", "copy",
                out.getAbsolutePath()
        };

        int execute = FFmpegKit.execute(command);
        assertEquals(FFmpegKit.RESULT_OK, execute);
        assertTrue(out.exists());
        assertTrue(out.length() > 0);

        deleteRecursively(caseDir);
    }

    private void write(File file, String content) throws Exception {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(content.getBytes(StandardCharsets.UTF_8));
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
