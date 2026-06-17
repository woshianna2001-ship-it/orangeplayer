package com.orange.playerlibrary.speech;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.orange.playerlibrary.R;

import java.util.List;

/**
 * 语音识别语言包管理对话框
 * 复刻 OCR 语言包管理界面风格，支持品质选择
 */
public class SpeechLanguagePackDialog {
    
    private final Context mContext;
    private final VoskModelManager mManager;
    private final SpeechLanguagePackAdapter mAdapter;
    private Dialog mDialog;
    private TextView mTvInstalledInfo;
    private OnLanguageChangedListener mOnLanguageChangedListener;
    private OnDismissListener mOnDismissListener;
    
    /**
     * 语言变化监听器
     */
    public interface OnLanguageChangedListener {
        void onLanguageInstalled(String languageCode);
        void onLanguageDeleted(String languageCode);
    }
    
    /**
     * 对话框关闭监听器
     */
    public interface OnDismissListener {
        void onDismiss();
    }
    
    public SpeechLanguagePackDialog(Context context) {
        mContext = context;
        mManager = new VoskModelManager(context);
        mAdapter = new SpeechLanguagePackAdapter();
    }
    
    /**
     * 设置语言变化监听器
     */
    public void setOnLanguageChangedListener(OnLanguageChangedListener listener) {
        mOnLanguageChangedListener = listener;
    }
    
    /**
     * 设置对话框关闭监听器
     */
    public void setOnDismissListener(OnDismissListener listener) {
        mOnDismissListener = listener;
    }

    /**
     * 显示对话框
     */
    public void show() {
        View view = LayoutInflater.from(mContext).inflate(R.layout.dialog_speech_language_pack, null);
        
        mTvInstalledInfo = view.findViewById(R.id.tv_installed_info);
        RecyclerView rvLanguages = view.findViewById(R.id.rv_languages);
        View btnClose = view.findViewById(R.id.btn_close);
        
        // 设置 RecyclerView
        rvLanguages.setLayoutManager(new LinearLayoutManager(mContext));
        rvLanguages.setAdapter(mAdapter);
        
        // 加载语言列表
        refreshLanguageList();
        
        // 设置操作监听
        mAdapter.setOnActionListener(new SpeechLanguagePackAdapter.OnActionListener() {
            @Override
            public void onDownload(VoskModelManager.LanguageModel model, VoskModelManager.ModelQuality quality) {
                downloadLanguage(model, quality);
            }
            
            @Override
            public void onDelete(VoskModelManager.LanguageModel model) {
                deleteLanguage(model);
            }
        });
        
        // 创建对话框
        mDialog = new AlertDialog.Builder(mContext)
                .setView(view)
                .create();
        
        btnClose.setOnClickListener(v -> mDialog.dismiss());
        
        // 设置对话框关闭监听
        mDialog.setOnDismissListener(dialog -> {
            if (mOnDismissListener != null) {
                mOnDismissListener.onDismiss();
            }
        });
        
        // 设置对话框属性
        Window window = mDialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setDimAmount(0.6f);
        }
        
        mDialog.show();
    }
    
    /**
     * 刷新语言列表
     */
    public void refreshLanguageList() {
        List<VoskModelManager.LanguageModel> languages = mManager.getSupportedLanguages();
        mAdapter.setLanguages(languages);
        updateInstalledInfo();
    }
    
    /**
     * 下载语言
     */
    private void downloadLanguage(VoskModelManager.LanguageModel model, VoskModelManager.ModelQuality quality) {
        mManager.downloadModel(model.languageCode, quality, new VoskModelManager.DownloadCallback() {
            @Override
            public void onProgress(int progress, String status) {
                mAdapter.setDownloadProgress(model.languageCode, progress);
            }
            
            @Override
            public void onSuccess() {
                mAdapter.setDownloadComplete(model.languageCode, true, quality);
                updateInstalledInfo();
                Toast.makeText(mContext, model.displayName + " 下载完成", Toast.LENGTH_SHORT).show();
                
                // 通知监听器
                if (mOnLanguageChangedListener != null) {
                    mOnLanguageChangedListener.onLanguageInstalled(model.languageCode);
                }
            }
            
            @Override
            public void onError(String error) {
                mAdapter.setDownloadComplete(model.languageCode, false, null);
                Toast.makeText(mContext, "下载失败: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 删除语言
     */
    private void deleteLanguage(VoskModelManager.LanguageModel model) {
        new AlertDialog.Builder(mContext)
                .setTitle("删除语言包")
                .setMessage("确定要删除 " + model.displayName + " 语言包吗？")
                .setPositiveButton("删除", (dialog, which) -> {
                    if (mManager.deleteLanguageModel(model.languageCode)) {
                        mAdapter.setDeleted(model.languageCode);
                        updateInstalledInfo();
                        Toast.makeText(mContext, model.displayName + " 已删除", Toast.LENGTH_SHORT).show();
                        
                        // 通知监听器
                        if (mOnLanguageChangedListener != null) {
                            mOnLanguageChangedListener.onLanguageDeleted(model.languageCode);
                        }
                    } else {
                        Toast.makeText(mContext, "删除失败", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }
    
    /**
     * 更新已安装信息
     */
    private void updateInstalledInfo() {
        List<String> installed = mManager.getInstalledLanguages();
        long totalSize = 0;
        
        for (String languageCode : installed) {
            totalSize += mManager.getLanguageModelSize(languageCode);
        }
        
        String sizeText;
        if (totalSize < 1024 * 1024) {
            sizeText = String.format("%.1f KB", totalSize / 1024.0);
        } else if (totalSize < 1024 * 1024 * 1024) {
            sizeText = String.format("%.1f MB", totalSize / (1024.0 * 1024));
        } else {
            sizeText = String.format("%.2f GB", totalSize / (1024.0 * 1024 * 1024));
        }
        
        mTvInstalledInfo.setText("已安装: " + installed.size() + " 个语言包，共 " + sizeText);
    }
    
    /**
     * 获取已安装语言数量
     */
    public int getInstalledCount() {
        return mManager.getInstalledLanguages().size();
    }
    
    /**
     * 关闭对话框
     */
    public void dismiss() {
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
        }
        mManager.release();
    }
    
    /**
     * 取消下载
     */
    public void cancelDownload() {
        mManager.cancelDownload();
    }
}
