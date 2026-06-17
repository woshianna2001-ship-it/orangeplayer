package com.orange.playerlibrary.ocr;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * OCR 扫描区域选择控件
 * 支持用户拖拉调整识别区域
 * 
 * Requirements: 8.1, 8.2, 8.3, 8.4, 8.7, 9.2, 9.3, 9.4, 9.5
 */
public class OcrScanRegionView extends FrameLayout {
    
    private static final String TAG = "OcrScanRegionView";
    
    // 拖拽手柄大小（dp）
    private static final int HANDLE_SIZE_DP = 24;
    // 边缘触摸区域大小（dp）
    private static final int EDGE_TOUCH_SIZE_DP = 20;
    // 最小区域大小（相对于视频尺寸的比例）
    private static final float MIN_REGION_SIZE = 0.1f;
    
    // 颜色配置
    private static final int MASK_COLOR = 0x80000000; // 半透明黑色遮罩
    private static final int BORDER_COLOR = 0xFFFFFFFF; // 白色边框
    private static final int HANDLE_COLOR = 0xFFFFFFFF; // 白色手柄
    private static final int HANDLE_FILL_COLOR = 0xFF2196F3; // 蓝色手柄填充
    
    /**
     * 扫描区域数据类
     * 使用相对于视频尺寸的比例 (0-1)
     */
    public static class ScanRegion {
        public float left;   // 相对于视频宽度的比例 (0-1)
        public float top;    // 相对于视频高度的比例 (0-1)
        public float right;  // 相对于视频宽度的比例 (0-1)
        public float bottom; // 相对于视频高度的比例 (0-1)
        
        public ScanRegion() {
            this(0f, 0.8f, 1f, 1f);
        }
        
        public ScanRegion(float left, float top, float right, float bottom) {
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
        }
        
        /**
         * 获取默认区域（底部20%）
         */
        public static ScanRegion getDefault() {
            return new ScanRegion(0f, 0.8f, 1f, 1f);
        }
        
        /**
         * 复制区域
         */
        public ScanRegion copy() {
            return new ScanRegion(left, top, right, bottom);
        }
        
        /**
         * 检查区域是否有效
         */
        public boolean isValid() {
            return left >= 0 && left <= 1 &&
                   top >= 0 && top <= 1 &&
                   right >= 0 && right <= 1 &&
                   bottom >= 0 && bottom <= 1 &&
                   left < right && top < bottom;
        }
        
        @Override
        public String toString() {
            return String.format("ScanRegion[%.2f, %.2f, %.2f, %.2f]", left, top, right, bottom);
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            ScanRegion other = (ScanRegion) obj;
            return Float.compare(other.left, left) == 0 &&
                   Float.compare(other.top, top) == 0 &&
                   Float.compare(other.right, right) == 0 &&
                   Float.compare(other.bottom, bottom) == 0;
        }
    }
    
    /**
     * 区域变化监听器
     */
    public interface OnRegionChangedListener {
        void onRegionChanged(ScanRegion region);
        void onEditModeChanged(boolean isEditing);
    }
    
    // 拖拽类型
    private enum DragType {
        NONE,
        MOVE,           // 移动整个区域
        LEFT,           // 左边缘
        TOP,            // 上边缘
        RIGHT,          // 右边缘
        BOTTOM,         // 下边缘
        TOP_LEFT,       // 左上角
        TOP_RIGHT,      // 右上角
        BOTTOM_LEFT,    // 左下角
        BOTTOM_RIGHT    // 右下角
    }
    
    // 画笔
    private Paint mMaskPaint;
    private Paint mBorderPaint;
    private Paint mHandlePaint;
    private Paint mHandleFillPaint;
    private Paint mClearPaint;
    
    // 区域数据
    private ScanRegion mScanRegion;
    private ScanRegion mOriginalRegion; // 编辑前的原始区域
    
    // 视频尺寸
    private int mVideoWidth = 0;
    private int mVideoHeight = 0;
    
    // 编辑模式
    private boolean mIsEditMode = false;
    
    // 拖拽状态
    private DragType mCurrentDragType = DragType.NONE;
    private float mLastTouchX;
    private float mLastTouchY;
    
    // 尺寸（像素）
    private int mHandleSize;
    private int mEdgeTouchSize;
    
    // 按钮容器
    private LinearLayout mButtonContainer;
    private Button mConfirmButton;
    private Button mResetButton;
    
    // 监听器
    private OnRegionChangedListener mListener;
    
    public OcrScanRegionView(@NonNull Context context) {
        this(context, null);
    }
    
    public OcrScanRegionView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }
    
    public OcrScanRegionView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }
    
    private void init(Context context) {
        // 设置为可绘制
        setWillNotDraw(false);
        
        // 计算尺寸
        float density = context.getResources().getDisplayMetrics().density;
        mHandleSize = (int) (HANDLE_SIZE_DP * density);
        mEdgeTouchSize = (int) (EDGE_TOUCH_SIZE_DP * density);
        
        // 初始化画笔
        initPaints();
        
        // 初始化默认区域
        mScanRegion = ScanRegion.getDefault();
        
        // 初始化按钮
        initButtons(context);
        
        // 默认隐藏
        setVisibility(GONE);
    }

    
    private void initPaints() {
        // 遮罩画笔
        mMaskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mMaskPaint.setColor(MASK_COLOR);
        mMaskPaint.setStyle(Paint.Style.FILL);
        
        // 边框画笔
        mBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBorderPaint.setColor(BORDER_COLOR);
        mBorderPaint.setStyle(Paint.Style.STROKE);
        mBorderPaint.setStrokeWidth(3f);
        
        // 手柄边框画笔
        mHandlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mHandlePaint.setColor(HANDLE_COLOR);
        mHandlePaint.setStyle(Paint.Style.STROKE);
        mHandlePaint.setStrokeWidth(2f);
        
        // 手柄填充画笔
        mHandleFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mHandleFillPaint.setColor(HANDLE_FILL_COLOR);
        mHandleFillPaint.setStyle(Paint.Style.FILL);
        
        // 清除画笔（用于挖空选区）
        mClearPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mClearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
    }
    
    private void initButtons(Context context) {
        // 创建按钮容器
        mButtonContainer = new LinearLayout(context);
        mButtonContainer.setOrientation(LinearLayout.HORIZONTAL);
        mButtonContainer.setVisibility(GONE);
        
        // 创建确认按钮（保存并关闭）
        mConfirmButton = new Button(context);
        mConfirmButton.setText("保存并关闭");
        mConfirmButton.setTextColor(Color.WHITE);
        mConfirmButton.setBackgroundColor(0xFF2196F3);
        mConfirmButton.setPadding(32, 16, 32, 16);
        mConfirmButton.setOnClickListener(v -> {
            Log.d(TAG, "Confirm button clicked");
            confirmSelection();
        });
        
        // 创建重置按钮
        mResetButton = new Button(context);
        mResetButton.setText("重置");
        mResetButton.setTextColor(Color.WHITE);
        mResetButton.setBackgroundColor(0xFF666666);
        mResetButton.setPadding(32, 16, 32, 16);
        mResetButton.setOnClickListener(v -> {
            Log.d(TAG, "Reset button clicked");
            resetToDefault();
        });
        
        // 添加按钮到容器
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        buttonParams.setMargins(16, 0, 16, 0);
        
        mButtonContainer.addView(mResetButton, buttonParams);
        mButtonContainer.addView(mConfirmButton, buttonParams);
        
        // 添加容器到视图
        FrameLayout.LayoutParams containerParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        containerParams.gravity = android.view.Gravity.CENTER_HORIZONTAL | android.view.Gravity.TOP;
        containerParams.topMargin = 32;
        
        addView(mButtonContainer, containerParams);
    }
    
    // ===== 公共方法 =====
    
    /**
     * 设置视频尺寸（用于计算实际像素区域）
     */
    public void setVideoSize(int width, int height) {
        mVideoWidth = width;
        mVideoHeight = height;
        Log.d(TAG, "setVideoSize: " + width + "x" + height);
        invalidate();
    }
    
    /**
     * 获取扫描区域
     */
    public ScanRegion getScanRegion() {
        return mScanRegion.copy();
    }
    
    /**
     * 设置扫描区域
     */
    public void setScanRegion(ScanRegion region) {
        if (region != null && region.isValid()) {
            mScanRegion = region.copy();
            constrainRegion();
            invalidate();
            Log.d(TAG, "setScanRegion: " + mScanRegion);
        }
    }
    
    /**
     * 重置为默认区域
     */
    public void resetToDefault() {
        Log.d(TAG, "resetToDefault called");
        mScanRegion = ScanRegion.getDefault();
        invalidate();
        
        // 不通知监听器，只有确认时才保存
        Log.d(TAG, "resetToDefault: " + mScanRegion);
    }
    
    /**
     * 进入编辑模式
     */
    public void enterEditMode() {
        Log.d(TAG, "enterEditMode: mIsEditMode=" + mIsEditMode + 
            ", visibility=" + getVisibility() + 
            ", isAttached=" + isAttachedToWindow() + 
            ", size=" + getWidth() + "x" + getHeight());
        
        if (mIsEditMode) {
            Log.w(TAG, "enterEditMode: already in edit mode");
            return;
        }
        
        mIsEditMode = true;
        mOriginalRegion = mScanRegion.copy();
        
        setVisibility(VISIBLE);
        mButtonContainer.setVisibility(VISIBLE);
        
        // 取消之前的动画，避免干扰
        animate().cancel();
        
        // 淡入动画
        setAlpha(0f);
        animate()
            .alpha(1f)
            .setDuration(200)
            .setListener(null)  // 清除之前的监听器，避免触发 exitEditMode
            .start();
        
        if (mListener != null) {
            mListener.onEditModeChanged(true);
        }
        
        Log.d(TAG, "enterEditMode: completed, visibility=" + getVisibility());
    }
    
    /**
     * 退出编辑模式
     */
    public void exitEditMode() {
        Log.d(TAG, "exitEditMode: mIsEditMode=" + mIsEditMode);
        
        if (!mIsEditMode) {
            Log.w(TAG, "exitEditMode: not in edit mode");
            return;
        }
        
        mIsEditMode = false;
        
        // 淡出动画
        animate().alpha(0f).setDuration(200).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                setVisibility(GONE);
                mButtonContainer.setVisibility(GONE);
                Log.d(TAG, "exitEditMode: animation ended, visibility=" + getVisibility());
            }
        }).start();
        
        if (mListener != null) {
            mListener.onEditModeChanged(false);
        }
        
        Log.d(TAG, "exitEditMode: started fade out animation");
    }
    
    /**
     * 是否处于编辑模式
     */
    public boolean isInEditMode() {
        return mIsEditMode;
    }
    
    /**
     * 确认选择
     */
    public void confirmSelection() {
        Log.d(TAG, "confirmSelection: " + mScanRegion);
        
        if (mListener != null) {
            mListener.onRegionChanged(mScanRegion.copy());
        }
        exitEditMode();
    }
    
    /**
     * 取消选择（恢复原始区域）
     */
    public void cancelSelection() {
        if (mOriginalRegion != null) {
            mScanRegion = mOriginalRegion.copy();
            invalidate();
        }
        exitEditMode();
        Log.d(TAG, "cancelSelection");
    }
    
    /**
     * 获取实际像素区域（用于截图）
     */
    public Rect getPixelRegion() {
        if (mVideoWidth <= 0 || mVideoHeight <= 0) {
            return null;
        }
        
        int left = (int) (mScanRegion.left * mVideoWidth);
        int top = (int) (mScanRegion.top * mVideoHeight);
        int right = (int) (mScanRegion.right * mVideoWidth);
        int bottom = (int) (mScanRegion.bottom * mVideoHeight);
        
        return new Rect(left, top, right, bottom);
    }
    
    /**
     * 设置区域变化监听器
     */
    public void setOnRegionChangedListener(OnRegionChangedListener listener) {
        mListener = listener;
    }

    
    // ===== 绘制方法 =====
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        if (!mIsEditMode) return;
        
        int width = getWidth();
        int height = getHeight();
        
        if (width <= 0 || height <= 0) return;
        
        // 计算选区的像素坐标
        float left = mScanRegion.left * width;
        float top = mScanRegion.top * height;
        float right = mScanRegion.right * width;
        float bottom = mScanRegion.bottom * height;
        
        // 保存图层用于挖空
        int saveCount = canvas.saveLayer(0, 0, width, height, null);
        
        // 绘制半透明遮罩
        canvas.drawRect(0, 0, width, height, mMaskPaint);
        
        // 挖空选区
        canvas.drawRect(left, top, right, bottom, mClearPaint);
        
        // 恢复图层
        canvas.restoreToCount(saveCount);
        
        // 绘制选区边框
        canvas.drawRect(left, top, right, bottom, mBorderPaint);
        
        // 绘制拖拽手柄
        drawHandles(canvas, left, top, right, bottom);
    }
    
    /**
     * 绘制拖拽手柄
     */
    private void drawHandles(Canvas canvas, float left, float top, float right, float bottom) {
        float halfHandle = mHandleSize / 2f;
        
        // 四个角的手柄
        drawHandle(canvas, left, top);           // 左上
        drawHandle(canvas, right, top);          // 右上
        drawHandle(canvas, left, bottom);        // 左下
        drawHandle(canvas, right, bottom);       // 右下
        
        // 四条边的中点手柄
        float centerX = (left + right) / 2;
        float centerY = (top + bottom) / 2;
        
        drawHandle(canvas, centerX, top);        // 上边中点
        drawHandle(canvas, centerX, bottom);     // 下边中点
        drawHandle(canvas, left, centerY);       // 左边中点
        drawHandle(canvas, right, centerY);      // 右边中点
    }
    
    /**
     * 绘制单个手柄
     */
    private void drawHandle(Canvas canvas, float x, float y) {
        float halfHandle = mHandleSize / 2f;
        
        // 绘制填充圆
        canvas.drawCircle(x, y, halfHandle - 2, mHandleFillPaint);
        
        // 绘制边框圆
        canvas.drawCircle(x, y, halfHandle - 2, mHandlePaint);
    }
    
    // ===== 触摸处理 =====
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!mIsEditMode) {
            return super.onTouchEvent(event);
        }
        
        float x = event.getX();
        float y = event.getY();
        
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mCurrentDragType = detectDragType(x, y);
                mLastTouchX = x;
                mLastTouchY = y;
                return mCurrentDragType != DragType.NONE;
                
            case MotionEvent.ACTION_MOVE:
                if (mCurrentDragType != DragType.NONE) {
                    handleDrag(x, y);
                    mLastTouchX = x;
                    mLastTouchY = y;
                    return true;
                }
                break;
                
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mCurrentDragType = DragType.NONE;
                break;
        }
        
        return super.onTouchEvent(event);
    }
    
    /**
     * 检测拖拽类型
     */
    private DragType detectDragType(float x, float y) {
        int width = getWidth();
        int height = getHeight();
        
        // 计算选区的像素坐标
        float left = mScanRegion.left * width;
        float top = mScanRegion.top * height;
        float right = mScanRegion.right * width;
        float bottom = mScanRegion.bottom * height;
        
        float centerX = (left + right) / 2;
        float centerY = (top + bottom) / 2;
        
        // 检测角落手柄
        if (isNearPoint(x, y, left, top)) return DragType.TOP_LEFT;
        if (isNearPoint(x, y, right, top)) return DragType.TOP_RIGHT;
        if (isNearPoint(x, y, left, bottom)) return DragType.BOTTOM_LEFT;
        if (isNearPoint(x, y, right, bottom)) return DragType.BOTTOM_RIGHT;
        
        // 检测边缘中点手柄
        if (isNearPoint(x, y, centerX, top)) return DragType.TOP;
        if (isNearPoint(x, y, centerX, bottom)) return DragType.BOTTOM;
        if (isNearPoint(x, y, left, centerY)) return DragType.LEFT;
        if (isNearPoint(x, y, right, centerY)) return DragType.RIGHT;
        
        // 检测是否在选区内部（移动整个区域）
        if (x >= left && x <= right && y >= top && y <= bottom) {
            return DragType.MOVE;
        }
        
        return DragType.NONE;
    }
    
    /**
     * 检测点是否靠近目标点
     */
    private boolean isNearPoint(float x, float y, float targetX, float targetY) {
        float distance = (float) Math.sqrt(Math.pow(x - targetX, 2) + Math.pow(y - targetY, 2));
        return distance <= mHandleSize;
    }
    
    /**
     * 处理拖拽
     */
    private void handleDrag(float x, float y) {
        int width = getWidth();
        int height = getHeight();
        
        if (width <= 0 || height <= 0) return;
        
        // 计算移动的比例
        float deltaX = (x - mLastTouchX) / width;
        float deltaY = (y - mLastTouchY) / height;
        
        Log.v(TAG, "handleDrag: deltaX=" + deltaX + ", deltaY=" + deltaY + ", dragType=" + mCurrentDragType);
        
        switch (mCurrentDragType) {
            case MOVE:
                // 移动整个区域
                float regionWidth = mScanRegion.right - mScanRegion.left;
                float regionHeight = mScanRegion.bottom - mScanRegion.top;
                
                mScanRegion.left += deltaX;
                mScanRegion.right += deltaX;
                mScanRegion.top += deltaY;
                mScanRegion.bottom += deltaY;
                break;
                
            case LEFT:
                mScanRegion.left += deltaX;
                break;
                
            case TOP:
                mScanRegion.top += deltaY;
                break;
                
            case RIGHT:
                mScanRegion.right += deltaX;
                break;
                
            case BOTTOM:
                mScanRegion.bottom += deltaY;
                break;
                
            case TOP_LEFT:
                mScanRegion.left += deltaX;
                mScanRegion.top += deltaY;
                break;
                
            case TOP_RIGHT:
                mScanRegion.right += deltaX;
                mScanRegion.top += deltaY;
                break;
                
            case BOTTOM_LEFT:
                mScanRegion.left += deltaX;
                mScanRegion.bottom += deltaY;
                break;
                
            case BOTTOM_RIGHT:
                mScanRegion.right += deltaX;
                mScanRegion.bottom += deltaY;
                break;
        }
        
        // 约束区域
        constrainRegion();
        
        // 重绘
        invalidate();
    }
    
    /**
     * 约束区域在有效范围内
     * Requirements: 8.4
     */
    private void constrainRegion() {
        // 确保区域在 0-1 范围内
        mScanRegion.left = Math.max(0f, Math.min(1f, mScanRegion.left));
        mScanRegion.top = Math.max(0f, Math.min(1f, mScanRegion.top));
        mScanRegion.right = Math.max(0f, Math.min(1f, mScanRegion.right));
        mScanRegion.bottom = Math.max(0f, Math.min(1f, mScanRegion.bottom));
        
        // 确保最小尺寸
        if (mScanRegion.right - mScanRegion.left < MIN_REGION_SIZE) {
            if (mCurrentDragType == DragType.LEFT || 
                mCurrentDragType == DragType.TOP_LEFT || 
                mCurrentDragType == DragType.BOTTOM_LEFT) {
                mScanRegion.left = mScanRegion.right - MIN_REGION_SIZE;
            } else {
                mScanRegion.right = mScanRegion.left + MIN_REGION_SIZE;
            }
        }
        
        if (mScanRegion.bottom - mScanRegion.top < MIN_REGION_SIZE) {
            if (mCurrentDragType == DragType.TOP || 
                mCurrentDragType == DragType.TOP_LEFT || 
                mCurrentDragType == DragType.TOP_RIGHT) {
                mScanRegion.top = mScanRegion.bottom - MIN_REGION_SIZE;
            } else {
                mScanRegion.bottom = mScanRegion.top + MIN_REGION_SIZE;
            }
        }
        
        // 确保 left < right, top < bottom
        if (mScanRegion.left >= mScanRegion.right) {
            float temp = mScanRegion.left;
            mScanRegion.left = mScanRegion.right;
            mScanRegion.right = temp;
        }
        
        if (mScanRegion.top >= mScanRegion.bottom) {
            float temp = mScanRegion.top;
            mScanRegion.top = mScanRegion.bottom;
            mScanRegion.bottom = temp;
        }
        
        // 移动时确保不超出边界
        if (mCurrentDragType == DragType.MOVE) {
            float regionWidth = mScanRegion.right - mScanRegion.left;
            float regionHeight = mScanRegion.bottom - mScanRegion.top;
            
            if (mScanRegion.left < 0) {
                mScanRegion.left = 0;
                mScanRegion.right = regionWidth;
            }
            if (mScanRegion.right > 1) {
                mScanRegion.right = 1;
                mScanRegion.left = 1 - regionWidth;
            }
            if (mScanRegion.top < 0) {
                mScanRegion.top = 0;
                mScanRegion.bottom = regionHeight;
            }
            if (mScanRegion.bottom > 1) {
                mScanRegion.bottom = 1;
                mScanRegion.top = 1 - regionHeight;
            }
        }
        
        // 再次确保在边界内
        mScanRegion.left = Math.max(0f, Math.min(1f - MIN_REGION_SIZE, mScanRegion.left));
        mScanRegion.top = Math.max(0f, Math.min(1f - MIN_REGION_SIZE, mScanRegion.top));
        mScanRegion.right = Math.max(MIN_REGION_SIZE, Math.min(1f, mScanRegion.right));
        mScanRegion.bottom = Math.max(MIN_REGION_SIZE, Math.min(1f, mScanRegion.bottom));
    }
}
