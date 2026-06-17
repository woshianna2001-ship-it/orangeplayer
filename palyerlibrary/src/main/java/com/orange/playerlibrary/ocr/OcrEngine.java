package com.orange.playerlibrary.ocr;

import android.content.Context;
import android.graphics.Bitmap;

/**
 * OCR 引擎接口
 */
public interface OcrEngine {
    
    /**
     * 初始化 OCR 引擎
     * @param context 上下文
     * @param language 语言代码 (chi_sim, eng, jpn, kor)
     * @return 是否初始化成功
     */
    boolean init(Context context, String language);
    
    /**
     * 识别图片中的文字
     * @param bitmap 图片
     * @return 识别结果
     */
    String recognize(Bitmap bitmap);
    
    /**
     * 释放资源
     */
    void release();
    
    /**
     * 是否已初始化
     */
    boolean isInitialized();
}
