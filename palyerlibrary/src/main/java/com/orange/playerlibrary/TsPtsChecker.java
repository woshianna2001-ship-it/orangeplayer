package com.orange.playerlibrary;

import android.content.Context;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * TS 片段 PTS 检测器
 * 
 * 功能：
 * 1. 下载 TS 片段的前几个包（不需要完整下载）
 * 2. 支持 AES-128 加密的 TS 解密
 * 3. 解析 TS 包头，提取 PTS 时间戳
 * 4. 检测 DISCONTINUITY 前后的 PTS 是否连续
 * 5. 如果 PTS 跳变超过阈值，建议切换到 ExoPlayer
 */
public class TsPtsChecker {
    
    private static final String TAG = "TsPtsChecker";
    
    // TS 包大小
    private static final int TS_PACKET_SIZE = 188;
    
    // 下载前 N 个 TS 包用于 PTS 检测（188 * 50 = 9.4KB）
    private static final int MAX_PACKETS_TO_CHECK = 50;
    
    // PTS 跳变阈值（秒）：如果 PTS 跳变超过这个值，认为不连续
    // 注意：广告插入会导致几百到几千秒的跳变，但广告会被移除，所以这些跳变是正常的
    // 只有正片内部的 PTS 跳变才是问题，通常跳变不会超过 10 秒
    // 设置为 10 秒，只检测正片内部的小跳变
    private static final double PTS_JUMP_THRESHOLD = 10.0;
    
    // 连接超时
    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 5000;
    
    private static Context sContext;
    
    /**
     * 初始化（必须在使用前调用）
     */
    public static void init(Context context) {
        sContext = context.getApplicationContext();
    }
    
    /**
     * PTS 检测结果
     */
    public static class PtsCheckResult {
        public boolean success;           // 是否成功检测
        public double beforePts;          // DISCONTINUITY 前的 PTS（秒）
        public double afterPts;           // DISCONTINUITY 后的 PTS（秒）
        public double ptsDiff;            // PTS 差异（秒）
        public boolean hasPtsJump;        // 是否有 PTS 跳变
        public boolean shouldUseExoPlayer; // 是否建议使用 ExoPlayer
        public String message;            // 检测消息
        
        @Override
        public String toString() {
            return String.format("PtsCheckResult{success=%s, beforePts=%.2fs, afterPts=%.2fs, " +
                    "ptsDiff=%.2fs, hasPtsJump=%s, shouldUseExoPlayer=%s, message='%s'}",
                    success, beforePts, afterPts, ptsDiff, hasPtsJump, shouldUseExoPlayer, message);
        }
    }
    
    /**
     * 加密信息
     */
    public static class EncryptionInfo {
        public String method;      // 加密方法（AES-128, NONE）
        public String keyUri;      // 密钥 URI
        public String iv;          // IV（初始化向量）
        public byte[] keyData;     // 密钥数据
        
        public boolean isEncrypted() {
            return "AES-128".equalsIgnoreCase(method) && keyUri != null;
        }
    }
    
    /**
     * 检测第一个片段的 PTS 是否从 0 开始
     * 
     * @param tsUrl 第一个 TS URL
     * @param encryption 加密信息
     * @return PTS 检测结果（beforePts 字段存储第一个 PTS 值）
     */
    public static PtsCheckResult checkFirstSegmentPts(String tsUrl, EncryptionInfo encryption) {
        PtsCheckResult result = new PtsCheckResult();
        result.success = false;
        
        try {
            Log.d(TAG, "Checking first segment PTS: " + tsUrl);
            
            Double firstPts = extractFirstPts(tsUrl, encryption);
            if (firstPts == null) {
                result.message = "Failed to extract PTS from first segment";
                Log.w(TAG, result.message);
                return result;
            }
            
            result.beforePts = firstPts;
            result.afterPts = 0;
            result.ptsDiff = firstPts;
            result.hasPtsJump = firstPts > 1.0; // 如果第一个 PTS 大于 1 秒，认为有偏移
            result.shouldUseExoPlayer = result.hasPtsJump;
            result.success = true;
            
            if (result.hasPtsJump) {
                result.message = String.format("First segment PTS is %.2fs (expected ~0s), PTS offset detected", firstPts);
            } else {
                result.message = String.format("First segment PTS is %.2fs, no offset", firstPts);
            }
            
            Log.d(TAG, result.message);
            
        } catch (Exception e) {
            result.message = "First segment PTS check failed: " + e.getMessage();
            Log.e(TAG, result.message, e);
        }
        
        return result;
    }
    
    /**
     * 检测 DISCONTINUITY 前后的 PTS 是否连续
     * 
     * @param tsUrlBefore DISCONTINUITY 前的最后一个 TS URL
     * @param tsUrlAfter DISCONTINUITY 后的第一个 TS URL
     * @param encryptionBefore DISCONTINUITY 前的加密信息
     * @param encryptionAfter DISCONTINUITY 后的加密信息
     * @return PTS 检测结果
     */
    public static PtsCheckResult checkPtsContinuity(String tsUrlBefore, String tsUrlAfter, 
                                                     EncryptionInfo encryptionBefore, EncryptionInfo encryptionAfter) {
        PtsCheckResult result = new PtsCheckResult();
        result.success = false;
        
        try {
            Log.d(TAG, "Checking PTS continuity:");
            Log.d(TAG, "  Before: " + tsUrlBefore + " (encrypted=" + (encryptionBefore != null && encryptionBefore.isEncrypted()) + ")");
            Log.d(TAG, "  After: " + tsUrlAfter + " (encrypted=" + (encryptionAfter != null && encryptionAfter.isEncrypted()) + ")");
            
            // 1. 获取 DISCONTINUITY 前的 PTS（取最后一个 PTS）
            Double ptsBefore = extractLastPts(tsUrlBefore, encryptionBefore);
            if (ptsBefore == null) {
                result.message = "Failed to extract PTS from segment before DISCONTINUITY";
                Log.w(TAG, result.message);
                return result;
            }
            result.beforePts = ptsBefore;
            
            // 2. 获取 DISCONTINUITY 后的 PTS（取第一个 PTS）
            Double ptsAfter = extractFirstPts(tsUrlAfter, encryptionAfter);
            if (ptsAfter == null) {
                result.message = "Failed to extract PTS from segment after DISCONTINUITY";
                Log.w(TAG, result.message);
                return result;
            }
            result.afterPts = ptsAfter;
            
            // 3. 计算 PTS 差异
            result.ptsDiff = Math.abs(ptsAfter - ptsBefore);
            result.hasPtsJump = result.ptsDiff > PTS_JUMP_THRESHOLD;
            result.shouldUseExoPlayer = result.hasPtsJump;
            result.success = true;
            
            if (result.hasPtsJump) {
                result.message = String.format("PTS jump detected: %.2fs -> %.2fs (diff=%.2fs), " +
                        "recommend using ExoPlayer", ptsBefore, ptsAfter, result.ptsDiff);
            } else {
                result.message = String.format("PTS continuous: %.2fs -> %.2fs (diff=%.2fs), " +
                        "IJK can handle this", ptsBefore, ptsAfter, result.ptsDiff);
            }
            
            Log.d(TAG, result.message);
            
        } catch (Exception e) {
            result.message = "PTS check failed: " + e.getMessage();
            Log.e(TAG, result.message, e);
        }
        
        return result;
    }
    
    /**
     * 提取 TS 片段的第一个 PTS（支持加密 TS）
     */
    private static Double extractFirstPts(String tsUrl, EncryptionInfo encryption) {
        byte[] tsData = downloadAndDecryptTsHeader(tsUrl, encryption);
        if (tsData == null || tsData.length < TS_PACKET_SIZE) {
            return null;
        }
        
        // 遍历 TS 包，找到第一个包含 PTS 的包
        for (int offset = 0; offset + TS_PACKET_SIZE <= tsData.length; offset += TS_PACKET_SIZE) {
            Double pts = extractPtsFromPacket(tsData, offset);
            if (pts != null) {
                Log.d(TAG, "Found first PTS: " + pts + "s at offset " + offset);
                return pts;
            }
        }
        
        Log.w(TAG, "No PTS found in first " + MAX_PACKETS_TO_CHECK + " packets");
        return null;
    }
    
    /**
     * 提取 TS 片段的最后一个 PTS（支持加密 TS）
     */
    private static Double extractLastPts(String tsUrl, EncryptionInfo encryption) {
        byte[] tsData = downloadAndDecryptTsHeader(tsUrl, encryption);
        if (tsData == null || tsData.length < TS_PACKET_SIZE) {
            return null;
        }
        
        // 从后往前遍历 TS 包，找到最后一个包含 PTS 的包
        Double lastPts = null;
        for (int offset = 0; offset + TS_PACKET_SIZE <= tsData.length; offset += TS_PACKET_SIZE) {
            Double pts = extractPtsFromPacket(tsData, offset);
            if (pts != null) {
                lastPts = pts;
            }
        }
        
        if (lastPts != null) {
            Log.d(TAG, "Found last PTS: " + lastPts + "s");
        } else {
            Log.w(TAG, "No PTS found in first " + MAX_PACKETS_TO_CHECK + " packets");
        }
        
        return lastPts;
    }
    
    /**
     * 下载并解密 TS 片段头部
     * 
     * @param tsUrl TS URL
     * @param encryption 加密信息
     * @return 解密后的 TS 数据，失败返回 null
     */
    private static byte[] downloadAndDecryptTsHeader(String tsUrl, EncryptionInfo encryption) {
        // 1. 下载 TS 头部数据
        byte[] encryptedData = downloadTsHeader(tsUrl);
        if (encryptedData == null || encryptedData.length == 0) {
            return null;
        }
        
        // 2. 如果没有加密，直接返回
        if (encryption == null || !encryption.isEncrypted()) {
            Log.d(TAG, "TS is not encrypted, using raw data");
            return encryptedData;
        }
        
        // 3. 下载密钥（如果还没有）
        if (encryption.keyData == null) {
            encryption.keyData = downloadEncryptionKey(encryption.keyUri);
            if (encryption.keyData == null) {
                Log.w(TAG, "Failed to download encryption key from: " + encryption.keyUri);
                return null;
            }
            Log.d(TAG, "Downloaded encryption key: " + encryption.keyData.length + " bytes");
        }
        
        // 4. 解密数据
        try {
            byte[] decryptedData = decryptAES128(encryptedData, encryption.keyData, encryption.iv);
            if (decryptedData != null) {
                Log.d(TAG, "Successfully decrypted TS data: " + decryptedData.length + " bytes");
            }
            return decryptedData;
        } catch (Exception e) {
            Log.e(TAG, "Failed to decrypt TS data", e);
            return null;
        }
    }
    
    /**
     * 下载加密密钥
     * 
     * @param keyUri 密钥 URI
     * @return 密钥数据，失败返回 null
     */
    private static byte[] downloadEncryptionKey(String keyUri) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(keyUri);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                Log.w(TAG, "HTTP response code: " + responseCode + " for key: " + keyUri);
                return null;
            }
            
            InputStream is = connection.getInputStream();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int bytesRead;
            
            while ((bytesRead = is.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            
            is.close();
            return baos.toByteArray();
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to download encryption key: " + keyUri, e);
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    
    /**
     * AES-128 解密
     * 
     * @param encryptedData 加密数据
     * @param key 密钥（16 字节）
     * @param ivHex IV 十六进制字符串（可选）
     * @return 解密后的数据，失败返回 null
     */
    private static byte[] decryptAES128(byte[] encryptedData, byte[] key, String ivHex) {
        try {
            // 1. 确保数据长度是 16 字节的倍数（AES 块大小）
            int blockSize = 16;
            int dataLength = encryptedData.length;
            int alignedLength = (dataLength / blockSize) * blockSize;
            
            if (alignedLength < blockSize) {
                Log.w(TAG, "Encrypted data too short: " + dataLength + " bytes");
                return null;
            }
            
            // 只解密对齐的部分
            byte[] alignedData = encryptedData;
            if (alignedLength < dataLength) {
                alignedData = new byte[alignedLength];
                System.arraycopy(encryptedData, 0, alignedData, 0, alignedLength);
                Log.d(TAG, "Trimmed data from " + dataLength + " to " + alignedLength + " bytes for AES decryption");
            }
            
            // 2. 准备 IV
            byte[] iv;
            if (ivHex != null && !ivHex.isEmpty()) {
                // 解析 IV（格式：0x00000000000000000000000000000000）
                String ivStr = ivHex.startsWith("0x") ? ivHex.substring(2) : ivHex;
                iv = hexStringToByteArray(ivStr);
            } else {
                // 如果没有指定 IV，使用全零 IV
                iv = new byte[16];
            }
            
            // 3. 创建 Cipher - HLS TS 使用 NoPadding
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            
            // 4. 解密
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            return cipher.doFinal(alignedData);
            
        } catch (Exception e) {
            Log.e(TAG, "AES-128 decryption failed", e);
            return null;
        }
    }
    
    /**
     * 十六进制字符串转字节数组
     */
    private static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }
    
    /**
     * 从 TS 包中提取 PTS
     * 
     * @param data TS 数据
     * @param offset TS 包起始位置
     * @return PTS（秒），如果没有 PTS 返回 null
     */
    private static Double extractPtsFromPacket(byte[] data, int offset) {
        try {
            // 检查同步字节
            if ((data[offset] & 0xFF) != 0x47) {
                return null;
            }
            
            // 检查是否有 adaptation field 和 payload
            int adaptationFieldControl = (data[offset + 3] >> 4) & 0x03;
            boolean hasAdaptationField = (adaptationFieldControl == 0x02) || (adaptationFieldControl == 0x03);
            boolean hasPayload = (adaptationFieldControl == 0x01) || (adaptationFieldControl == 0x03);
            
            if (!hasPayload) {
                return null;
            }
            
            // 计算 payload 起始位置
            int payloadStart = offset + 4;
            if (hasAdaptationField) {
                int adaptationFieldLength = data[offset + 4] & 0xFF;
                payloadStart += 1 + adaptationFieldLength;
            }
            
            // 检查是否有足够的数据
            if (payloadStart + 14 > offset + TS_PACKET_SIZE) {
                return null;
            }
            
            // 检查 PES 起始码（0x000001）
            if ((data[payloadStart] & 0xFF) != 0x00 ||
                (data[payloadStart + 1] & 0xFF) != 0x00 ||
                (data[payloadStart + 2] & 0xFF) != 0x01) {
                return null;
            }
            
            // 检查 PTS_DTS_flags
            int ptsDtsFlags = (data[payloadStart + 7] >> 6) & 0x03;
            if (ptsDtsFlags == 0x00) {
                return null; // 没有 PTS
            }
            
            // 提取 PTS（33 位）
            int ptsOffset = payloadStart + 9;
            long pts = 0;
            pts |= ((long)(data[ptsOffset] & 0x0E) << 29);
            pts |= ((long)(data[ptsOffset + 1] & 0xFF) << 22);
            pts |= ((long)(data[ptsOffset + 2] & 0xFE) << 14);
            pts |= ((long)(data[ptsOffset + 3] & 0xFF) << 7);
            pts |= ((long)(data[ptsOffset + 4] & 0xFE) >> 1);
            
            // 转换为秒（PTS 时钟频率 90kHz）
            double ptsSeconds = pts / 90000.0;
            
            return ptsSeconds;
            
        } catch (Exception e) {
            Log.e(TAG, "Error extracting PTS from packet at offset " + offset, e);
            return null;
        }
    }
    
    /**
     * 下载 TS 片段的头部数据（前 N 个包）
     * 
     * @param tsUrl TS URL
     * @return TS 数据，失败返回 null
     */
    private static byte[] downloadTsHeader(String tsUrl) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(tsUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            
            // 使用 Range 请求只下载前面的数据
            int bytesToDownload = TS_PACKET_SIZE * MAX_PACKETS_TO_CHECK;
            connection.setRequestProperty("Range", "bytes=0-" + (bytesToDownload - 1));
            
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK && 
                responseCode != HttpURLConnection.HTTP_PARTIAL) {
                Log.w(TAG, "HTTP response code: " + responseCode + " for " + tsUrl);
                return null;
            }
            
            InputStream is = connection.getInputStream();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;
            int totalRead = 0;
            
            while (totalRead < bytesToDownload && (bytesRead = is.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
                totalRead += bytesRead;
            }
            
            is.close();
            byte[] data = baos.toByteArray();
            
            Log.d(TAG, "Downloaded " + data.length + " bytes from " + tsUrl);
            return data;
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to download TS header: " + tsUrl, e);
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
