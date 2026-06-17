package com.orange.downloader.merge;

import android.text.TextUtils;

import com.orange.downloader.common.DownloadConstants;
import com.orange.downloader.model.Video;
import com.orange.downloader.model.VideoTaskItem;
import com.orange.downloader.model.VideoTaskState;
import com.orange.downloader.utils.LogUtils;
import com.orange.downloader.utils.VideoDownloadUtils;

import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TsMergeService {

    public static final int MERGE_OK = 0;
    public static final int MERGE_ERROR_INVALID_PARAM = 7001;
    public static final int MERGE_ERROR_SOURCE_NOT_FOUND = 7002;
    public static final int MERGE_ERROR_NO_TS_FILES = 7003;
    public static final int MERGE_ERROR_FFMPEG_AND_FALLBACK_DISABLED = 7004;
    public static final int MERGE_ERROR_OUTPUT_EMPTY = 7005;
    public static final int MERGE_ERROR_ENCRYPTED_FALLBACK_UNSUPPORTED = 7006;
    public static final int MERGE_ERROR_EXCEPTION = 7099;
    private static final int[] EDGE_SKIP_RETRY_COUNTS = new int[]{1, 3, 5};
    private static final int MIN_SEGMENTS_AFTER_SKIP = 3;

    public interface MergeCallback {
        void onResult(VideoTaskItem taskItem);
    }

    public void merge(VideoTaskItem taskItem, MergeCallback callback) {
        LogUtils.i(DownloadConstants.TAG, "[MERGE] start, url=" + (taskItem != null ? taskItem.getUrl() : "null"));
        if (taskItem == null || TextUtils.isEmpty(taskItem.getSaveDir())) {
            markMergeFailed(taskItem, MERGE_ERROR_INVALID_PARAM, "taskItem/saveDir invalid", null);
            notifyResult(callback, taskItem);
            return;
        }

        new Thread(() -> {
            try {
                doMergeInternal(taskItem);
                taskItem.setErrorCode(MERGE_OK);
                taskItem.setTaskState(VideoTaskState.SUCCESS);
                LogUtils.i(DownloadConstants.TAG, "[MERGE] success, output=" + taskItem.getFilePath());
            } catch (MergeException e) {
                markMergeFailed(taskItem, e.errorCode, e.getMessage(), e);
            } catch (Exception e) {
                markMergeFailed(taskItem, MERGE_ERROR_EXCEPTION, e.getMessage(), e);
            }
            notifyResult(callback, taskItem);
        }, "orange-ts-merge").start();
    }

    private void doMergeInternal(VideoTaskItem taskItem) throws Exception {
        String saveDir = taskItem.getSaveDir();
        File dir = new File(saveDir);
        if (!dir.exists() || !dir.isDirectory()) {
            throw new MergeException(MERGE_ERROR_SOURCE_NOT_FOUND, "saveDir not found: " + saveDir);
        }

        if (TextUtils.isEmpty(taskItem.getFileHash())) {
            taskItem.setFileHash(VideoDownloadUtils.computeMD5(taskItem.getUrl()));
        }
        String fileHash = taskItem.getFileHash();

        File localM3U8 = resolveLocalM3U8File(taskItem, dir, fileHash);
        if (!localM3U8.exists()) {
            throw new MergeException(MERGE_ERROR_SOURCE_NOT_FOUND, "local m3u8 not found");
        }

        String outputFileName = buildOutputFileName(taskItem, fileHash);
        File outputFile = new File(dir, outputFileName);

        boolean ffmpegEnabled = MergeFeatureToggle.isFfmpegMergeEnabled();
        boolean fallbackEnabled = MergeFeatureToggle.isJavaFallbackEnabled();
        boolean encryptedM3u8 = isEncryptedM3U8(localM3U8);
        LogUtils.i(DownloadConstants.TAG, "[MERGE] flags ffmpeg=" + ffmpegEnabled + ", fallback=" + fallbackEnabled + ", encrypted=" + encryptedM3u8);

        boolean mergedByFfmpeg = false;
        if (ffmpegEnabled) {
            mergedByFfmpeg = tryMergeByFfmpeg(taskItem, localM3U8, outputFile);
        }

        boolean mergedByJavaFallback = false;
        if (!mergedByFfmpeg) {
            if (!fallbackEnabled) {
                throw new MergeException(MERGE_ERROR_FFMPEG_AND_FALLBACK_DISABLED, "ffmpeg merge failed and java fallback disabled");
            }
            if (encryptedM3u8) {
                throw new MergeException(MERGE_ERROR_ENCRYPTED_FALLBACK_UNSUPPORTED, "encrypted m3u8 cannot use java concat fallback");
            }
            List<File> tsFiles = parseM3U8ForTsFiles(localM3U8);
            if (tsFiles.isEmpty()) {
                throw new MergeException(MERGE_ERROR_NO_TS_FILES, "no ts files found");
            }
            outputFileName = buildTsOutputFileName(taskItem, fileHash);
            outputFile = new File(dir, outputFileName);
            mergeTsByJava(tsFiles, outputFile);
            mergedByJavaFallback = true;
        }

        if (!outputFile.exists() || outputFile.length() <= 0) {
            throw new MergeException(MERGE_ERROR_OUTPUT_EMPTY, "merged output missing or empty");
        }

        taskItem.setFileName(outputFileName);
        taskItem.setFilePath(outputFile.getAbsolutePath());
        taskItem.setMimeType(mergedByJavaFallback ? "video/mp2t" : "video/mp4");
        taskItem.setVideoType(Video.Type.HLS_TYPE);
        cleanupIntermediateArtifactsIfNeeded(dir, localM3U8, fileHash, outputFile, mergedByJavaFallback);
    }

    private File resolveLocalM3U8File(VideoTaskItem taskItem, File dir, String fileHash) {
        List<File> candidates = new ArrayList<>();
        if (taskItem != null && !TextUtils.isEmpty(taskItem.getFilePath())) {
            candidates.add(new File(taskItem.getFilePath()));
        }
        candidates.add(new File(dir, fileHash + "_" + VideoDownloadUtils.LOCAL_M3U8));
        candidates.add(new File(dir, fileHash + "_" + VideoDownloadUtils.LOCAL_M3U8_WITH_KEY));
        candidates.add(new File(dir, fileHash + "_local.m3u8"));
        candidates.add(new File(dir, fileHash + "_local_key_url.m3u8"));

        for (File candidate : candidates) {
            if (candidate.exists() && candidate.isFile()) {
                if (!candidate.equals(candidates.get(0))) {
                    LogUtils.i(DownloadConstants.TAG, "[MERGE] resolved local m3u8 from fallback path=" + candidate.getAbsolutePath());
                }
                return candidate;
            }
        }

        File[] localM3u8Files = dir.listFiles((currentDir, name) ->
                name != null && (name.endsWith("_" + VideoDownloadUtils.LOCAL_M3U8)
                        || name.endsWith("_" + VideoDownloadUtils.LOCAL_M3U8_WITH_KEY)));
        if (localM3u8Files != null && localM3u8Files.length > 0) {
            LogUtils.w(DownloadConstants.TAG, "[MERGE] local m3u8 hash mismatch, fallback use=" + localM3u8Files[0].getAbsolutePath());
            return localM3u8Files[0];
        }

        return new File(dir, fileHash + "_" + VideoDownloadUtils.LOCAL_M3U8);
    }

    private void notifyResult(MergeCallback callback, VideoTaskItem taskItem) {
        if (callback != null) {
            callback.onResult(taskItem);
        }
    }

    private void markMergeFailed(VideoTaskItem taskItem, int errorCode, String message, Throwable throwable) {
        if (taskItem != null) {
            taskItem.setErrorCode(errorCode);
            taskItem.setTaskState(VideoTaskState.ERROR);
            taskItem.setIsCompleted(false);
        }
        if (throwable == null) {
            LogUtils.e(DownloadConstants.TAG, "[MERGE] failed, code=" + errorCode + ", msg=" + message);
        } else {
            LogUtils.e(DownloadConstants.TAG, "[MERGE] failed, code=" + errorCode + ", msg=" + message + ", throwable=" + throwable.getClass().getSimpleName());
        }
    }

    private String buildOutputFileName(VideoTaskItem taskItem, String fileHash) {
        String title = taskItem.getTitle();
        if (!TextUtils.isEmpty(title)) {
            String safeName = title.replaceAll("[\\\\/:*?\"<>|]", "_");
            return safeName + ".mp4";
        }
        return fileHash + ".mp4";
    }

    private String buildTsOutputFileName(VideoTaskItem taskItem, String fileHash) {
        String title = taskItem.getTitle();
        if (!TextUtils.isEmpty(title)) {
            String safeName = title.replaceAll("[\\\\/:*?\"<>|]", "_");
            return safeName + ".ts";
        }
        return fileHash + ".ts";
    }

    private boolean isEncryptedM3U8(File m3u8File) {
        try (BufferedReader reader = new BufferedReader(new FileReader(m3u8File))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("#EXT-X-KEY") && !line.contains("METHOD=NONE")) {
                    return true;
                }
            }
        } catch (Exception e) {
            LogUtils.w(DownloadConstants.TAG, "[MERGE] check encrypted m3u8 failed: " + e.getMessage());
        }
        return false;
    }

    private boolean tryMergeByFfmpeg(VideoTaskItem taskItem, File localM3U8, File outputFile) {
        try {
            Class<?> ffmpegKitClass = Class.forName("com.orange.ffmpeg.FFmpegKit");
            int initRet = (int) ffmpegKitClass.getMethod("init").invoke(null);
            if (initRet != 0) {
                LogUtils.w(DownloadConstants.TAG, "[MERGE] FFmpeg init failed, ret=" + initRet);
                return false;
            }
            boolean stubMode = false;
            try {
                stubMode = (boolean) ffmpegKitClass.getMethod("isStubMode").invoke(null);
            } catch (Throwable ignore) {
            }
            if (stubMode) {
                LogUtils.w(DownloadConstants.TAG, "[MERGE] FFmpeg is running in stub mode, native merge unavailable");
            }
            
            // 从 taskItem 读取存储的 headers
            Map<String, String> headers = parseHeadersFromJson(taskItem.getRequestHeaders());

            boolean merged = executeFfmpegMerge(ffmpegKitClass, headers, localM3U8, outputFile);
            if (merged) {
                return true;
            }

            List<File> retryPlaylists = buildRetryPlaylists(localM3U8);
            try {
                for (File retryPlaylist : retryPlaylists) {
                    LogUtils.w(DownloadConstants.TAG, "[MERGE] retry FFmpeg merge with filtered playlist=" + retryPlaylist.getAbsolutePath());
                    if (executeFfmpegMerge(ffmpegKitClass, headers, retryPlaylist, outputFile)) {
                        LogUtils.i(DownloadConstants.TAG, "[MERGE] retry merge success, playlist=" + retryPlaylist.getName());
                        return true;
                    }
                }
            } finally {
                cleanupTempFiles(retryPlaylists);
            }
            return false;
        } catch (Throwable e) {
            LogUtils.w(DownloadConstants.TAG, "[MERGE] FFmpeg merge exception: " + e.getMessage());
            return false;
        }
    }

    private boolean executeFfmpegMerge(Class<?> ffmpegKitClass, Map<String, String> headers, File inputM3u8, File outputFile) throws Exception {
        String[] command = buildFfmpegCommand(headers, inputM3u8, outputFile);
        LogUtils.i(DownloadConstants.TAG, "[MERGE] FFmpeg command with headers: " + (headers != null && !headers.isEmpty()) + ", input=" + inputM3u8.getAbsolutePath());
        if (outputFile.exists()) {
            outputFile.delete();
        }
        int executeRet = (int) ffmpegKitClass.getMethod("execute", String[].class).invoke(null, (Object) command);
        boolean outputReady = outputFile.exists() && outputFile.length() > 0;
        boolean merged = outputReady;
        if (!merged) {
            LogUtils.w(DownloadConstants.TAG, "[MERGE] FFmpeg execute failed, ret=" + executeRet + ", outputExists=" + outputFile.exists() + ", outputSize=" + (outputFile.exists() ? outputFile.length() : 0));
        } else if (executeRet != 0) {
            LogUtils.w(DownloadConstants.TAG, "[MERGE] FFmpeg returned non-zero but output is ready, ret=" + executeRet + ", outputSize=" + outputFile.length());
        }
        return merged;
    }

    private void cleanupIntermediateArtifactsIfNeeded(File dir, File localM3U8, String fileHash, File outputFile, boolean mergedByJavaFallback) {
        if (mergedByJavaFallback) {
            return;
        }
        if (outputFile == null || !outputFile.exists() || outputFile.length() <= 0) {
            return;
        }
        if (!outputFile.getName().toLowerCase().endsWith(".mp4")) {
            return;
        }
        try {
            List<File> tsFiles = parseM3U8ForTsFiles(localM3U8);
            for (File tsFile : tsFiles) {
                if (tsFile != null && tsFile.exists() && tsFile.isFile()) {
                    tsFile.delete();
                }
            }
        } catch (Exception e) {
            LogUtils.w(DownloadConstants.TAG, "[MERGE] cleanup ts failed: " + e.getMessage());
        }
        deleteIfExists(localM3U8);
        deleteIfExists(new File(dir, fileHash + "_" + VideoDownloadUtils.LOCAL_M3U8));
        deleteIfExists(new File(dir, fileHash + "_" + VideoDownloadUtils.LOCAL_M3U8_WITH_KEY));
        deleteIfExists(new File(dir, VideoDownloadUtils.REMOTE_M3U8));
        File[] keyFiles = dir.listFiles((d, name) -> name != null && name.toLowerCase().endsWith(".key"));
        if (keyFiles != null) {
            for (File keyFile : keyFiles) {
                deleteIfExists(keyFile);
            }
        }
    }

    private void deleteIfExists(File file) {
        if (file != null && file.exists()) {
            file.delete();
        }
    }

    private String[] buildFfmpegCommand(Map<String, String> headers, File inputM3u8, File outputFile) {
        List<String> cmdList = new ArrayList<>();
        cmdList.add("-allowed_extensions");
        cmdList.add("ALL");
        cmdList.add("-protocol_whitelist");
        cmdList.add("file,http,https,tcp,tls,crypto,data,udp,rtp,rtmp,rtsp");
        cmdList.add("-fflags");
        cmdList.add("+discardcorrupt");
        cmdList.add("-err_detect");
        cmdList.add("ignore_err");

        String headersStr = buildHeadersString(headers);
        if (!TextUtils.isEmpty(headersStr)) {
            cmdList.add("-headers");
            cmdList.add(headersStr);
        }

        String userAgent = extractHeader(headers, "User-Agent", "user-agent");
        String referer = extractHeader(headers, "Referer", "referer");
        String cookies = extractHeader(headers, "Cookie", "cookie");

        if (!TextUtils.isEmpty(userAgent)) {
            cmdList.add("-user_agent");
            cmdList.add(userAgent);
        }
        if (!TextUtils.isEmpty(referer)) {
            cmdList.add("-referer");
            cmdList.add(referer);
        }
        if (!TextUtils.isEmpty(cookies)) {
            cmdList.add("-cookies");
            cmdList.add(cookies);
        }

        cmdList.add("-i");
        cmdList.add(inputM3u8.getAbsolutePath());
        cmdList.add("-c");
        cmdList.add("copy");
        cmdList.add(outputFile.getAbsolutePath());
        return cmdList.toArray(new String[0]);
    }

    private List<File> buildRetryPlaylists(File localM3U8) {
        List<File> retryPlaylists = new ArrayList<>();
        try {
            PlaylistModel playlistModel = parsePlaylist(localM3U8);
            if (playlistModel.segments.size() < MIN_SEGMENTS_AFTER_SKIP) {
                return retryPlaylists;
            }
            List<Integer> invalidIndexes = collectInvalidSegmentIndexes(playlistModel);
            LogUtils.i(DownloadConstants.TAG, "[MERGE] playlist segments=" + playlistModel.segments.size() + ", invalidSegments=" + invalidIndexes.size());
            if (!invalidIndexes.isEmpty()) {
                File invalidFiltered = writeFilteredPlaylist(localM3U8, playlistModel, new LinkedHashSet<>(invalidIndexes), "skip-invalid");
                if (invalidFiltered != null) {
                    retryPlaylists.add(invalidFiltered);
                }
            }
            if (isEncryptedM3U8(localM3U8)) {
                for (int skipCount : EDGE_SKIP_RETRY_COUNTS) {
                    addEdgeTrimRetry(retryPlaylists, localM3U8, playlistModel, invalidIndexes, skipCount, 0);
                    addEdgeTrimRetry(retryPlaylists, localM3U8, playlistModel, invalidIndexes, 0, skipCount);
                    addEdgeTrimRetry(retryPlaylists, localM3U8, playlistModel, invalidIndexes, skipCount, skipCount);
                }
            }
        } catch (Exception e) {
            LogUtils.w(DownloadConstants.TAG, "[MERGE] build retry playlists failed: " + e.getMessage());
        }
        return retryPlaylists;
    }

    private void addEdgeTrimRetry(List<File> retryPlaylists, File localM3U8, PlaylistModel playlistModel, List<Integer> invalidIndexes, int skipHead, int skipTail) throws Exception {
        Set<Integer> skipIndexes = new LinkedHashSet<>(invalidIndexes);
        for (int i = 0; i < skipHead && i < playlistModel.segments.size(); i++) {
            skipIndexes.add(i);
        }
        for (int i = 0; i < skipTail && i < playlistModel.segments.size(); i++) {
            skipIndexes.add(playlistModel.segments.size() - 1 - i);
        }
        if (playlistModel.segments.size() - skipIndexes.size() < MIN_SEGMENTS_AFTER_SKIP) {
            return;
        }
        if (skipIndexes.isEmpty()) {
            return;
        }
        File retryFile = writeFilteredPlaylist(localM3U8, playlistModel, skipIndexes, "skip-h" + skipHead + "-t" + skipTail);
        if (retryFile != null) {
            retryPlaylists.add(retryFile);
        }
    }

    private PlaylistModel parsePlaylist(File m3u8File) throws Exception {
        PlaylistModel model = new PlaylistModel();
        String currentKeyLine = null;
        String currentMapLine = null;
        List<String> pendingLines = new ArrayList<>();
        boolean beforeFirstSegment = true;
        try (BufferedReader reader = new BufferedReader(new FileReader(m3u8File))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (TextUtils.isEmpty(trimmed)) {
                    continue;
                }
                if (!trimmed.startsWith("#")) {
                    PlaylistSegment segment = new PlaylistSegment();
                    segment.index = model.segments.size();
                    segment.uriLine = trimmed;
                    segment.keyLine = currentKeyLine;
                    segment.mapLine = currentMapLine;
                    segment.metadataLines.addAll(pendingLines);
                    segment.file = resolveSegmentFile(m3u8File, trimmed);
                    model.segments.add(segment);
                    pendingLines.clear();
                    beforeFirstSegment = false;
                    continue;
                }
                if (trimmed.startsWith("#EXT-X-ENDLIST")) {
                    model.footerLines.add(trimmed);
                    continue;
                }
                if (trimmed.startsWith("#EXT-X-KEY")) {
                    currentKeyLine = trimmed;
                    continue;
                }
                if (trimmed.startsWith("#EXT-X-MAP")) {
                    currentMapLine = trimmed;
                    continue;
                }
                if (beforeFirstSegment && isHeaderLine(trimmed)) {
                    model.headerLines.add(trimmed);
                } else {
                    pendingLines.add(trimmed);
                }
            }
        }
        return model;
    }

    private boolean isHeaderLine(String line) {
        return line.startsWith("#EXTM3U")
                || line.startsWith("#EXT-X-VERSION")
                || line.startsWith("#EXT-X-MEDIA-SEQUENCE")
                || line.startsWith("#EXT-X-TARGETDURATION")
                || line.startsWith("#EXT-X-PLAYLIST-TYPE")
                || line.startsWith("#EXT-X-INDEPENDENT-SEGMENTS")
                || line.startsWith("#EXT-X-START")
                || line.startsWith("#EXT-X-ALLOW-CACHE");
    }

    private File resolveSegmentFile(File playlistFile, String uriLine) {
        File segment = new File(uriLine);
        if (!segment.isAbsolute()) {
            segment = new File(playlistFile.getParentFile(), uriLine);
        }
        return segment;
    }

    private List<Integer> collectInvalidSegmentIndexes(PlaylistModel playlistModel) {
        List<Integer> invalidIndexes = new ArrayList<>();
        for (PlaylistSegment segment : playlistModel.segments) {
            if (segment.file == null || !segment.file.exists() || segment.file.length() <= 0) {
                invalidIndexes.add(segment.index);
                LogUtils.w(DownloadConstants.TAG, "[MERGE] invalid ts segment index=" + segment.index + ", uri=" + segment.uriLine + ", exists=" + (segment.file != null && segment.file.exists()) + ", size=" + (segment.file != null && segment.file.exists() ? segment.file.length() : 0));
            }
        }
        return invalidIndexes;
    }

    private File writeFilteredPlaylist(File originalM3u8, PlaylistModel playlistModel, Set<Integer> skipIndexes, String suffix) throws Exception {
        if (skipIndexes.isEmpty()) {
            return null;
        }
        File retryFile = new File(originalM3u8.getParentFile(), originalM3u8.getName().replace(".m3u8", "_" + suffix + ".m3u8"));
        String lastKeyLine = null;
        String lastMapLine = null;
        int keptSegments = 0;
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(retryFile, false))) {
            for (String headerLine : playlistModel.headerLines) {
                writer.write(headerLine);
                writer.newLine();
            }
            for (PlaylistSegment segment : playlistModel.segments) {
                if (skipIndexes.contains(segment.index)) {
                    LogUtils.w(DownloadConstants.TAG, "[MERGE] skip ts segment index=" + segment.index + ", uri=" + segment.uriLine + ", playlist=" + retryFile.getName());
                    continue;
                }
                if (!TextUtils.isEmpty(segment.keyLine) && !TextUtils.equals(segment.keyLine, lastKeyLine)) {
                    writer.write(segment.keyLine);
                    writer.newLine();
                    lastKeyLine = segment.keyLine;
                }
                if (!TextUtils.isEmpty(segment.mapLine) && !TextUtils.equals(segment.mapLine, lastMapLine)) {
                    writer.write(segment.mapLine);
                    writer.newLine();
                    lastMapLine = segment.mapLine;
                }
                for (String metadataLine : segment.metadataLines) {
                    writer.write(metadataLine);
                    writer.newLine();
                }
                writer.write(segment.uriLine);
                writer.newLine();
                keptSegments++;
            }
            for (String footerLine : playlistModel.footerLines) {
                writer.write(footerLine);
                writer.newLine();
            }
        }
        if (keptSegments < MIN_SEGMENTS_AFTER_SKIP) {
            retryFile.delete();
            return null;
        }
        LogUtils.i(DownloadConstants.TAG, "[MERGE] generated retry playlist=" + retryFile.getAbsolutePath() + ", keptSegments=" + keptSegments + ", skipped=" + skipIndexes.size());
        return retryFile;
    }

    private void cleanupTempFiles(List<File> files) {
        for (File file : files) {
            if (file != null && file.exists()) {
                file.delete();
            }
        }
    }
    
    /**
     * 从 JSON 字符串解析 headers
     */
    private Map<String, String> parseHeadersFromJson(String json) {
        if (TextUtils.isEmpty(json)) {
            return null;
        }
        try {
            Map<String, String> headers = new HashMap<>();
            JSONObject obj = new JSONObject(json);
            for (java.util.Iterator<String> it = obj.keys(); it.hasNext(); ) {
                String key = it.next();
                headers.put(key, obj.getString(key));
            }
            return headers;
        } catch (Exception e) {
            LogUtils.w(DownloadConstants.TAG, "[MERGE] parseHeadersFromJson failed: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 构建 HTTP headers 字符串，格式: "Key1: Value1\r\nKey2: Value2\r\n"
     */
    private String buildHeadersString(Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            String key = entry.getKey();
            // 跳过内部使用的 header
            if ("Range".equalsIgnoreCase(key)) {
                continue;
            }
            sb.append(key).append(": ").append(entry.getValue()).append("\r\n");
        }
        return sb.length() > 0 ? sb.toString() : null;
    }
    
    /**
     * 从 headers 中提取指定 key 的值（支持多种大小写形式）
     */
    private String extractHeader(Map<String, String> headers, String... keys) {
        if (headers == null) {
            return null;
        }
        for (String key : keys) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(key)) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }

    private List<File> parseM3U8ForTsFiles(File m3u8File) throws Exception {
        List<File> tsFiles = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(m3u8File))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.startsWith("#") && line.endsWith(".ts")) {
                    File ts = new File(line);
                    if (!ts.isAbsolute()) {
                        ts = new File(m3u8File.getParentFile(), line);
                    }
                    tsFiles.add(ts);
                }
            }
        }
        return tsFiles;
    }

    private void mergeTsByJava(List<File> tsFiles, File outputFile) throws Exception {
        long totalSize = 0;
        byte[] buffer = new byte[8192];
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            for (File tsFile : tsFiles) {
                if (!tsFile.exists()) {
                    continue;
                }
                try (FileInputStream fis = new FileInputStream(tsFile)) {
                    int len;
                    while ((len = fis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                        totalSize += len;
                    }
                }
            }
            fos.flush();
        }
        LogUtils.i(DownloadConstants.TAG, "[MERGE] java merge done, size=" + totalSize);
    }

    private void cleanupTs(List<File> tsFiles) {
        for (File tsFile : tsFiles) {
            if (tsFile.exists()) {
                tsFile.delete();
            }
        }
    }

    private static final class MergeException extends Exception {
        final int errorCode;

        MergeException(int errorCode, String message) {
            super(message);
            this.errorCode = errorCode;
        }
    }

    private static final class PlaylistModel {
        final List<String> headerLines = new ArrayList<>();
        final List<String> footerLines = new ArrayList<>();
        final List<PlaylistSegment> segments = new ArrayList<>();
    }

    private static final class PlaylistSegment {
        int index;
        String keyLine;
        String mapLine;
        String uriLine;
        File file;
        final List<String> metadataLines = new ArrayList<>();
    }
}
