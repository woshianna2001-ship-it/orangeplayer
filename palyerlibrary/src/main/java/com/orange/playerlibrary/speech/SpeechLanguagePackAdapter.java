package com.orange.playerlibrary.speech;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.orange.playerlibrary.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 语音识别语言包列表适配器
 * 支持品质选择
 */
public class SpeechLanguagePackAdapter extends RecyclerView.Adapter<SpeechLanguagePackAdapter.ViewHolder> {
    
    public interface OnActionListener {
        void onDownload(VoskModelManager.LanguageModel model, VoskModelManager.ModelQuality quality);
        void onDelete(VoskModelManager.LanguageModel model);
    }
    
    private final List<VoskModelManager.LanguageModel> mLanguages = new ArrayList<>();
    private final Map<String, Integer> mDownloadProgress = new HashMap<>();
    private final Map<String, Boolean> mDownloading = new HashMap<>();
    private final Map<String, VoskModelManager.ModelQuality> mSelectedQuality = new HashMap<>();
    private OnActionListener mListener;
    
    public void setLanguages(List<VoskModelManager.LanguageModel> languages) {
        mLanguages.clear();
        mLanguages.addAll(languages);
        // 初始化默认选择的品质
        for (VoskModelManager.LanguageModel model : languages) {
            if (!mSelectedQuality.containsKey(model.languageCode)) {
                mSelectedQuality.put(model.languageCode, model.getDefaultQuality());
            }
        }
        notifyDataSetChanged();
    }
    
    public void setOnActionListener(OnActionListener listener) {
        mListener = listener;
    }

    public void setDownloadProgress(String languageCode, int progress) {
        mDownloadProgress.put(languageCode, progress);
        mDownloading.put(languageCode, true);
        int position = findPosition(languageCode);
        if (position >= 0) {
            notifyItemChanged(position);
        }
    }
    
    public void setDownloadComplete(String languageCode, boolean success, VoskModelManager.ModelQuality quality) {
        mDownloading.put(languageCode, false);
        mDownloadProgress.remove(languageCode);
        int position = findPosition(languageCode);
        if (position >= 0) {
            if (success) {
                mLanguages.get(position).isInstalled = true;
                mLanguages.get(position).installedQuality = quality;
            }
            notifyItemChanged(position);
        }
    }
    
    public void setDeleted(String languageCode) {
        int position = findPosition(languageCode);
        if (position >= 0) {
            mLanguages.get(position).isInstalled = false;
            mLanguages.get(position).installedQuality = null;
            notifyItemChanged(position);
        }
    }
    
    private int findPosition(String languageCode) {
        for (int i = 0; i < mLanguages.size(); i++) {
            if (mLanguages.get(i).languageCode.equals(languageCode)) {
                return i;
            }
        }
        return -1;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_speech_language_pack, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        VoskModelManager.LanguageModel model = mLanguages.get(position);
        
        holder.tvName.setText(model.displayName);
        
        Boolean downloading = mDownloading.get(model.languageCode);
        boolean isDownloading = downloading != null && downloading;
        
        if (isDownloading) {
            // 下载中 - 显示进度条
            holder.progressBar.setVisibility(View.VISIBLE);
            Integer progress = mDownloadProgress.get(model.languageCode);
            holder.progressBar.setProgress(progress != null ? progress : 0);
            holder.btnAction.setText(progress != null ? progress + "%" : "0%");
            holder.btnAction.setEnabled(false);
            holder.spinnerQuality.setVisibility(View.GONE);
            holder.tvInstalledQuality.setVisibility(View.GONE);
            
            // 显示当前下载的品质大小
            VoskModelManager.ModelQuality selectedQuality = mSelectedQuality.get(model.languageCode);
            if (selectedQuality != null) {
                VoskModelManager.ModelInfo info = model.getModelInfo(selectedQuality);
                if (info != null) {
                    holder.tvSize.setText(info.getSizeDescription());
                    holder.tvSize.setVisibility(View.VISIBLE);
                }
            }
        } else if (model.isInstalled) {
            // 已安装
            holder.progressBar.setVisibility(View.GONE);
            holder.btnAction.setText("删除");
            holder.btnAction.setEnabled(true);
            holder.spinnerQuality.setVisibility(View.GONE);
            
            // 显示已安装的品质
            if (model.installedQuality != null) {
                holder.tvInstalledQuality.setText("已安装: " + model.installedQuality.displayName);
                holder.tvInstalledQuality.setVisibility(View.VISIBLE);
                
                // 显示已安装模型的大小
                VoskModelManager.ModelInfo info = model.getModelInfo(model.installedQuality);
                if (info != null) {
                    holder.tvSize.setText(info.getSizeDescription());
                    holder.tvSize.setVisibility(View.VISIBLE);
                }
            } else {
                holder.tvInstalledQuality.setVisibility(View.GONE);
                holder.tvSize.setVisibility(View.GONE);
            }
            
            holder.btnAction.setOnClickListener(v -> {
                if (mListener != null) {
                    mListener.onDelete(model);
                }
            });
        } else {
            // 未安装 - 显示品质选择
            holder.progressBar.setVisibility(View.GONE);
            holder.btnAction.setText("下载");
            holder.btnAction.setEnabled(true);
            holder.tvInstalledQuality.setVisibility(View.GONE);
            
            // 设置品质选择下拉框
            List<VoskModelManager.ModelQuality> qualities = model.getAvailableQualities();
            if (qualities.size() > 1) {
                holder.spinnerQuality.setVisibility(View.VISIBLE);
                setupQualitySpinner(holder, model, qualities);
            } else {
                holder.spinnerQuality.setVisibility(View.GONE);
            }
            
            // 显示选中品质的大小
            VoskModelManager.ModelQuality selectedQuality = mSelectedQuality.get(model.languageCode);
            if (selectedQuality == null) {
                selectedQuality = model.getDefaultQuality();
            }
            if (selectedQuality != null) {
                VoskModelManager.ModelInfo info = model.getModelInfo(selectedQuality);
                if (info != null) {
                    holder.tvSize.setText(info.getSizeDescription());
                    holder.tvSize.setVisibility(View.VISIBLE);
                }
            }
            
            final VoskModelManager.ModelQuality finalQuality = selectedQuality;
            holder.btnAction.setOnClickListener(v -> {
                if (mListener != null) {
                    VoskModelManager.ModelQuality quality = mSelectedQuality.get(model.languageCode);
                    if (quality == null) {
                        quality = finalQuality;
                    }
                    mListener.onDownload(model, quality);
                }
            });
        }
    }

    private void setupQualitySpinner(ViewHolder holder, VoskModelManager.LanguageModel model, 
            List<VoskModelManager.ModelQuality> qualities) {
        
        // 创建品质名称列表
        List<String> qualityNames = new ArrayList<>();
        for (VoskModelManager.ModelQuality quality : qualities) {
            qualityNames.add(quality.displayName);
        }
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                holder.itemView.getContext(),
                R.layout.spinner_item,
                qualityNames);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        holder.spinnerQuality.setAdapter(adapter);
        
        // 设置当前选中的品质
        VoskModelManager.ModelQuality currentQuality = mSelectedQuality.get(model.languageCode);
        if (currentQuality != null) {
            int index = qualities.indexOf(currentQuality);
            if (index >= 0) {
                holder.spinnerQuality.setSelection(index);
            }
        }
        
        // 监听品质选择变化
        holder.spinnerQuality.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < qualities.size()) {
                    VoskModelManager.ModelQuality selectedQuality = qualities.get(position);
                    mSelectedQuality.put(model.languageCode, selectedQuality);
                    
                    // 更新大小显示
                    VoskModelManager.ModelInfo info = model.getModelInfo(selectedQuality);
                    if (info != null) {
                        holder.tvSize.setText(info.getSizeDescription());
                    }
                }
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // 不处理
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return mLanguages.size();
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        TextView tvInstalledQuality;
        TextView tvSize;
        Spinner spinnerQuality;
        ProgressBar progressBar;
        TextView btnAction;
        
        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_language_name);
            tvInstalledQuality = itemView.findViewById(R.id.tv_installed_quality);
            tvSize = itemView.findViewById(R.id.tv_size);
            spinnerQuality = itemView.findViewById(R.id.spinner_quality);
            progressBar = itemView.findViewById(R.id.progress_bar);
            btnAction = itemView.findViewById(R.id.btn_action);
        }
    }
}
