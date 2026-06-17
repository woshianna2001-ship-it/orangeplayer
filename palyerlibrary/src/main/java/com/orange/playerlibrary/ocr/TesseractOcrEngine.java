package com.orange.playerlibrary.ocr;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Tesseract OCR 引擎实现
 * 使用反射调用，避免强依赖
 */
public class TesseractOcrEngine implements OcrEngine {
    
    private static final String TAG = "TesseractOcrEngine";
    private static final String TESSDATA_DIR = "tessdata";
    
    private Object mTessBaseAPI;
    private boolean mInitialized = false;
    private Context mContext;
    
    @Override
    public boolean init(Context context, String language) {
        Log.d(TAG, "init() called, language=" + language);
        
        if (!OcrAvailabilityChecker.isTesseractAvailable()) {
            Log.e(TAG, "Tesseract library not available");
            return false;
        }
        
        mContext = context.getApplicationContext();
        
        try {
            // 准备语言数据文件
            String dataPath = prepareTrainedData(language);
            Log.d(TAG, "prepareTrainedData returned: " + dataPath);
            
            if (dataPath == null) {
                Log.e(TAG, "Failed to prepare trained data");
                return false;
            }
            
            // 使用反射创建 TessBaseAPI
            Class<?> tessClass = Class.forName("com.googlecode.tesseract.android.TessBaseAPI");
            Log.d(TAG, "TessBaseAPI class found: " + tessClass);
            
            mTessBaseAPI = tessClass.newInstance();
            Log.d(TAG, "TessBaseAPI instance created: " + mTessBaseAPI);
            
            // 调用 init 方法
            java.lang.reflect.Method initMethod = tessClass.getMethod("init", String.class, String.class);
            Log.d(TAG, "Calling TessBaseAPI.init(" + dataPath + ", " + language + ")");
            
            boolean result = (Boolean) initMethod.invoke(mTessBaseAPI, dataPath, language);
            Log.d(TAG, "TessBaseAPI.init() returned: " + result);
            
            if (result) {
                mInitialized = true;
                Log.d(TAG, "Tesseract initialized successfully for language: " + language);
            } else {
                Log.e(TAG, "Tesseract init failed");
            }
            
            return result;
        } catch (Exception e) {
            Log.e(TAG, "Failed to init Tesseract", e);
            return false;
        }
    }
    
    @Override
    public String recognize(Bitmap bitmap) {
        Log.d(TAG, "recognize() called, bitmap=" + (bitmap != null ? bitmap.getWidth() + "x" + bitmap.getHeight() : "null"));
        
        if (!mInitialized || mTessBaseAPI == null) {
            Log.e(TAG, "recognize: not initialized, mInitialized=" + mInitialized + ", mTessBaseAPI=" + mTessBaseAPI);
            return null;
        }
        
        try {
            // 图像预处理：优化复杂背景识别
            Bitmap processedBitmap = preprocessImage(bitmap);
            
            Class<?> tessClass = mTessBaseAPI.getClass();
            
            // setImage
            java.lang.reflect.Method setImageMethod = tessClass.getMethod("setImage", Bitmap.class);
            setImageMethod.invoke(mTessBaseAPI, processedBitmap);
            Log.d(TAG, "setImage() done");
            
            // getUTF8Text
            java.lang.reflect.Method getTextMethod = tessClass.getMethod("getUTF8Text");
            String result = (String) getTextMethod.invoke(mTessBaseAPI);
            Log.d(TAG, "getUTF8Text() returned: [" + (result != null ? result.replace("\n", "\\n") : "null") + "]");
            
            // clear
            java.lang.reflect.Method clearMethod = tessClass.getMethod("clear");
            clearMethod.invoke(mTessBaseAPI);
            
            // 释放处理后的图像
            if (processedBitmap != bitmap) {
                processedBitmap.recycle();
            }
            
            // 过滤并清理识别结果
            return filterOcrResult(result);
        } catch (Exception e) {
            Log.e(TAG, "Failed to recognize", e);
            return null;
        }
    }
    
    /**
     * 图像预处理：优化复杂背景的识别
     * 1. 灰度化
     * 2. 二值化（自适应阈值）
     * 3. 去噪
     */
    private Bitmap preprocessImage(Bitmap original) {
        try {
            int width = original.getWidth();
            int height = original.getHeight();
            
            // 创建可变的 Bitmap
            Bitmap processed = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            
            int[] pixels = new int[width * height];
            original.getPixels(pixels, 0, width, 0, 0, width, height);
            
            // 第一步：灰度化
            int[] grayPixels = new int[width * height];
            for (int i = 0; i < pixels.length; i++) {
                int pixel = pixels[i];
                int r = (pixel >> 16) & 0xff;
                int g = (pixel >> 8) & 0xff;
                int b = pixel & 0xff;
                
                // 使用加权平均法灰度化
                int gray = (int) (0.299 * r + 0.587 * g + 0.114 * b);
                grayPixels[i] = gray;
            }
            
            // 第二步：自适应二值化（Otsu算法）
            int threshold = calculateOtsuThreshold(grayPixels);
            Log.d(TAG, "Otsu threshold: " + threshold);
            
            // 应用二值化
            for (int i = 0; i < grayPixels.length; i++) {
                int gray = grayPixels[i];
                // 大于阈值的设为白色（255），小于的设为黑色（0）
                int binary = gray > threshold ? 255 : 0;
                pixels[i] = 0xFF000000 | (binary << 16) | (binary << 8) | binary;
            }
            
            // 第三步：简单去噪（中值滤波）
            pixels = medianFilter(pixels, width, height);
            
            processed.setPixels(pixels, 0, width, 0, 0, width, height);
            
            Log.d(TAG, "Image preprocessing completed");
            return processed;
        } catch (Exception e) {
            Log.e(TAG, "Image preprocessing failed, using original", e);
            return original;
        }
    }
    
    /**
     * 计算 Otsu 自适应阈值
     */
    private int calculateOtsuThreshold(int[] grayPixels) {
        // 计算灰度直方图
        int[] histogram = new int[256];
        for (int gray : grayPixels) {
            histogram[gray]++;
        }
        
        int total = grayPixels.length;
        float sum = 0;
        for (int i = 0; i < 256; i++) {
            sum += i * histogram[i];
        }
        
        float sumB = 0;
        int wB = 0;
        int wF = 0;
        
        float varMax = 0;
        int threshold = 0;
        
        for (int i = 0; i < 256; i++) {
            wB += histogram[i];
            if (wB == 0) continue;
            
            wF = total - wB;
            if (wF == 0) break;
            
            sumB += i * histogram[i];
            
            float mB = sumB / wB;
            float mF = (sum - sumB) / wF;
            
            float varBetween = (float) wB * (float) wF * (mB - mF) * (mB - mF);
            
            if (varBetween > varMax) {
                varMax = varBetween;
                threshold = i;
            }
        }
        
        return threshold;
    }
    
    /**
     * 中值滤波去噪
     */
    private int[] medianFilter(int[] pixels, int width, int height) {
        int[] result = new int[pixels.length];
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = y * width + x;
                
                // 边缘像素直接复制
                if (x == 0 || x == width - 1 || y == 0 || y == height - 1) {
                    result[index] = pixels[index];
                    continue;
                }
                
                // 3x3 窗口中值滤波
                int[] window = new int[9];
                int k = 0;
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        int idx = (y + dy) * width + (x + dx);
                        window[k++] = pixels[idx] & 0xFF;
                    }
                }
                
                // 排序找中值
                java.util.Arrays.sort(window);
                int median = window[4];
                
                result[index] = 0xFF000000 | (median << 16) | (median << 8) | median;
            }
        }
        
        return result;
    }
    
    /**
     * 过滤 OCR 识别结果，只保留中英文、数字和常用标点
     * 并去掉中文字符之间的多余空格
     */
    private String filterOcrResult(String text) {
        if (text == null || text.isEmpty()) {
            Log.d(TAG, "filterOcrResult: input is null or empty");
            return null;
        }
        
        Log.d(TAG, "filterOcrResult: input length=" + text.length() + ", text=[" + text.replace("\n", "\\n") + "]");
        
        // 第一步：只保留有效字符
        StringBuilder filtered = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (isValidChar(c)) {
                filtered.append(c);
            }
        }
        
        String result = filtered.toString().trim();
        
        // 第二步：去掉中文字符之间的空格
        result = removeSpacesBetweenChinese(result);
        
        // 第三步：合并多个连续空格为一个
        result = result.replaceAll("\\s+", " ").trim();
        
        Log.d(TAG, "filterOcrResult: filtered length=" + result.length() + ", text=[" + result.replace("\n", "\\n") + "]");
        
        // 如果过滤后太短，可能是噪音
        if (result.length() < 2) {
            Log.d(TAG, "filterOcrResult: result too short, returning null");
            return null;
        }
        
        return result;
    }
    
    /**
     * 去掉中文字符之间的空格
     * 例如："为 了 满 足" -> "为了满足"
     * 但保留中英文之间的空格："Hello 世界" -> "Hello 世界"
     */
    private String removeSpacesBetweenChinese(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        StringBuilder result = new StringBuilder();
        char[] chars = text.toCharArray();
        
        for (int i = 0; i < chars.length; i++) {
            char current = chars[i];
            
            // 如果当前是空格，检查前后字符
            if (current == ' ') {
                // 获取前一个非空格字符
                char prev = '\0';
                for (int j = i - 1; j >= 0; j--) {
                    if (chars[j] != ' ') {
                        prev = chars[j];
                        break;
                    }
                }
                
                // 获取后一个非空格字符
                char next = '\0';
                for (int j = i + 1; j < chars.length; j++) {
                    if (chars[j] != ' ') {
                        next = chars[j];
                        break;
                    }
                }
                
                // 如果前后都是中文，跳过这个空格
                if (isChinese(prev) && isChinese(next)) {
                    continue;
                }
            }
            
            result.append(current);
        }
        
        return result.toString();
    }
    
    /**
     * 判断字符是否是中文
     */
    private boolean isChinese(char c) {
        return c >= '\u4e00' && c <= '\u9fff';
    }
    
    /**
     * 检查字符是否有效
     */
    private boolean isValidChar(char c) {
        // 中文字符范围
        if (c >= '\u4e00' && c <= '\u9fff') {
            return true;
        }
        // 英文字母
        if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
            return true;
        }
        // 数字
        if (c >= '0' && c <= '9') {
            return true;
        }
        // 空格和换行
        if (c == ' ' || c == '\n') {
            return true;
        }
        // 中文标点：，。！？、；：""''（）—…·
        String chinesePunctuation = "\uff0c\u3002\uff01\uff1f\u3001\uff1b\uff1a\u201c\u201d\u2018\u2019\uff08\uff09\u2014\u2026\u00b7";
        if (chinesePunctuation.indexOf(c) >= 0) {
            return true;
        }
        // 英文标点
        String englishPunctuation = ",.:;!?'\"()-";
        if (englishPunctuation.indexOf(c) >= 0) {
            return true;
        }
        return false;
    }
    
    @Override
    public void release() {
        if (mTessBaseAPI != null) {
            try {
                java.lang.reflect.Method endMethod = mTessBaseAPI.getClass().getMethod("recycle");
                endMethod.invoke(mTessBaseAPI);
            } catch (Exception e) {
                Log.e(TAG, "Failed to release Tesseract", e);
            }
            mTessBaseAPI = null;
        }
        mInitialized = false;
    }
    
    @Override
    public boolean isInitialized() {
        return mInitialized;
    }
    
    /**
     * 准备训练数据文件
     * 优先从下载目录加载，如果没有则从 assets 复制
     */
    private String prepareTrainedData(String language) {
        File tessDataDir = new File(mContext.getFilesDir(), TESSDATA_DIR);
        if (!tessDataDir.exists()) {
            tessDataDir.mkdirs();
        }
        
        String trainedDataFileName = language + ".traineddata";
        File trainedDataFile = new File(tessDataDir, trainedDataFileName);
        
        // 如果文件已存在（可能是下载的），直接返回
        if (trainedDataFile.exists() && trainedDataFile.length() > 0) {
            Log.d(TAG, "Using existing trained data: " + trainedDataFile.getAbsolutePath());
            return mContext.getFilesDir().getAbsolutePath();
        }
        
        // 尝试从 assets 复制
        try {
            InputStream is = mContext.getAssets().open(TESSDATA_DIR + "/" + trainedDataFileName);
            FileOutputStream fos = new FileOutputStream(trainedDataFile);
            
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
            
            fos.close();
            is.close();
            
            Log.d(TAG, "Copied trained data from assets: " + trainedDataFileName);
            return mContext.getFilesDir().getAbsolutePath();
        } catch (IOException e) {
            Log.e(TAG, "Failed to copy trained data: " + trainedDataFileName, e);
            Log.w(TAG, "语言包未安装，请在 OCR 设置中下载对应的语言包");
            return null;
        }
    }
    
    /**
     * 获取语言包下载提示
     */
    public static String getTrainedDataDownloadHint(String language) {
        String fileName = language + ".traineddata";
        return "请下载语言包并放置到 app/src/main/assets/tessdata/ 目录：\n" +
               "下载地址: https://github.com/tesseract-ocr/tessdata/raw/main/" + fileName;
    }
}
