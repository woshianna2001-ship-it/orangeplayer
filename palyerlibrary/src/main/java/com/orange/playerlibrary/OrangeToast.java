package com.orange.playerlibrary;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;

/**
 * 播放器自定义Toast
 * 显示在播放器中央，不会被系统Toast队列影响
 */
public class OrangeToast {

    private static final int DEFAULT_DURATION = 2000;
    private static final int SHORT_DURATION = 1500;
    private static final int LONG_DURATION = 3000;

    private static PopupWindow sCurrentToast;
    private static Handler sHandler = new Handler(Looper.getMainLooper());
    private static Runnable sDismissRunnable;

    /**
     * 显示Toast（默认时长）
     */
    public static void show(View anchorView, String message) {
        show(anchorView, message, DEFAULT_DURATION, 0);
    }

    /**
     * 显示短Toast
     */
    public static void showShort(View anchorView, String message) {
        show(anchorView, message, SHORT_DURATION, 0);
    }

    /**
     * 显示长Toast
     */
    public static void showLong(View anchorView, String message) {
        show(anchorView, message, LONG_DURATION, 0);
    }

    /**
     * 显示带图标的Toast
     */
    public static void show(View anchorView, String message, int iconResId) {
        show(anchorView, message, DEFAULT_DURATION, iconResId);
    }

    /**
     * 显示Toast
     * @param anchorView 锚点View（播放器View）
     * @param message 消息内容
     * @param duration 显示时长
     * @param iconResId 图标资源ID，0表示不显示图标
     */
    public static void show(View anchorView, String message, int duration, int iconResId) {
        if (anchorView == null || message == null) return;

        // 确保在主线程执行
        if (Looper.myLooper() != Looper.getMainLooper()) {
            sHandler.post(() -> show(anchorView, message, duration, iconResId));
            return;
        }

        // 取消之前的Toast
        dismiss();

        Context context = anchorView.getContext();
        if (context == null) return;
        
        // 获取实际的锚点View - 如果当前View尺寸为0，尝试使用DecorView
        View actualAnchor = anchorView;
        int anchorWidth = anchorView.getWidth();
        int anchorHeight = anchorView.getHeight();
        
        if (anchorWidth == 0 || anchorHeight == 0) {
            // 尝试获取Activity的DecorView
            if (context instanceof android.app.Activity) {
                android.app.Activity activity = (android.app.Activity) context;
                View decorView = activity.getWindow().getDecorView();
                if (decorView != null && decorView.getWidth() > 0 && decorView.getHeight() > 0) {
                    actualAnchor = decorView;
                    anchorWidth = decorView.getWidth();
                    anchorHeight = decorView.getHeight();
                } else {
                    // DecorView也不可用，使用系统Toast
                    android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show();
                    return;
                }
            } else {
                // 非Activity上下文，使用系统Toast
                android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // 创建Toast视图
        View toastView = LayoutInflater.from(context).inflate(R.layout.orange_toast_layout, null);
        TextView textView = toastView.findViewById(R.id.toast_text);
        ImageView iconView = toastView.findViewById(R.id.toast_icon);

        textView.setText(message);

        if (iconResId != 0) {
            iconView.setImageResource(iconResId);
            iconView.setVisibility(View.VISIBLE);
        } else {
            iconView.setVisibility(View.GONE);
        }

        // 创建PopupWindow
        sCurrentToast = new PopupWindow(toastView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                false);
        sCurrentToast.setOutsideTouchable(false);
        sCurrentToast.setTouchable(false);
        sCurrentToast.setClippingEnabled(false);

        // 测量Toast尺寸
        toastView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int toastWidth = toastView.getMeasuredWidth();
        int toastHeight = toastView.getMeasuredHeight();

        // 计算显示位置（播放器中央）
        int[] location = new int[2];
        actualAnchor.getLocationOnScreen(location);

        // 居中显示
        int x = location[0] + (anchorWidth - toastWidth) / 2;
        int y = location[1] + (anchorHeight - toastHeight) / 2;

        try {
            sCurrentToast.showAtLocation(actualAnchor, Gravity.NO_GRAVITY, x, y);
        } catch (Exception e) {
            // 窗口可能已销毁，使用系统Toast
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        // 延迟隐藏
        sDismissRunnable = OrangeToast::dismiss;
        sHandler.postDelayed(sDismissRunnable, duration);
    }

    /**
     * 立即隐藏Toast
     */
    public static void dismiss() {
        if (sDismissRunnable != null) {
            sHandler.removeCallbacks(sDismissRunnable);
            sDismissRunnable = null;
        }
        if (sCurrentToast != null && sCurrentToast.isShowing()) {
            try {
                sCurrentToast.dismiss();
            } catch (Exception ignored) {
            }
            sCurrentToast = null;
        }
    }

    /**
     * 使用Context显示Toast（兼容方法，会使用系统Toast）
     */
    public static void show(Context context, String message) {
        if (context == null || message == null) return;
        sHandler.post(() -> {
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show();
        });
    }
}
