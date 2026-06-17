package com.orange.playerlibrary.ocr;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.orange.playerlibrary.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 语言包列表适配器
 */
public class LanguagePackAdapter extends RecyclerView.Adapter<LanguagePackAdapter.ViewHolder> {
    
    public interface OnActionListener {
        void onDownload(LanguagePackManager.LanguagePack pack);
        void onDelete(LanguagePackManager.LanguagePack pack);
    }
    
    private final List<LanguagePackManager.LanguagePack> mLanguages = new ArrayList<>();
    private final Map<String, Integer> mDownloadProgress = new HashMap<>();
    private final Map<String, Boolean> mDownloading = new HashMap<>();
    private OnActionListener mListener;
    
    public void setLanguages(List<LanguagePackManager.LanguagePack> languages) {
        mLanguages.clear();
        mLanguages.addAll(languages);
        notifyDataSetChanged();
    }
    
    public void setOnActionListener(OnActionListener listener) {
        mListener = listener;
    }
    
    public void setDownloadProgress(String code, int progress) {
        mDownloadProgress.put(code, progress);
        mDownloading.put(code, true);
        int position = findPosition(code);
        if (position >= 0) {
            notifyItemChanged(position);
        }
    }
    
    public void setDownloadComplete(String code, boolean success) {
        mDownloading.put(code, false);
        mDownloadProgress.remove(code);
        int position = findPosition(code);
        if (position >= 0) {
            if (success) {
                mLanguages.get(position).installed = true;
            }
            notifyItemChanged(position);
        }
    }
    
    public void setDeleted(String code) {
        int position = findPosition(code);
        if (position >= 0) {
            mLanguages.get(position).installed = false;
            notifyItemChanged(position);
        }
    }
    
    private int findPosition(String code) {
        for (int i = 0; i < mLanguages.size(); i++) {
            if (mLanguages.get(i).code.equals(code)) {
                return i;
            }
        }
        return -1;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_language_pack, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LanguagePackManager.LanguagePack pack = mLanguages.get(position);
        
        holder.tvName.setText(pack.name);
        holder.tvDesc.setText(pack.description);
        holder.tvSize.setText(pack.getSizeText());
        
        Boolean downloading = mDownloading.get(pack.code);
        boolean isDownloading = downloading != null && downloading;
        
        if (isDownloading) {
            // 下载中 - 显示进度条
            holder.progressBar.setVisibility(View.VISIBLE);
            Integer progress = mDownloadProgress.get(pack.code);
            holder.progressBar.setProgress(progress != null ? progress : 0);
            holder.btnAction.setText(progress != null ? progress + "%" : "0%");
            holder.btnAction.setEnabled(false);
            holder.tvSize.setVisibility(View.VISIBLE);
        } else if (pack.installed) {
            // 已安装
            holder.progressBar.setVisibility(View.GONE);
            holder.btnAction.setText("删除");
            holder.btnAction.setEnabled(true);
            holder.tvSize.setVisibility(View.VISIBLE);
            holder.btnAction.setOnClickListener(v -> {
                if (mListener != null) {
                    mListener.onDelete(pack);
                }
            });
        } else {
            // 未安装
            holder.progressBar.setVisibility(View.GONE);
            holder.btnAction.setText("下载");
            holder.btnAction.setEnabled(true);
            holder.tvSize.setVisibility(View.VISIBLE);
            holder.btnAction.setOnClickListener(v -> {
                if (mListener != null) {
                    mListener.onDownload(pack);
                }
            });
        }
    }
    
    @Override
    public int getItemCount() {
        return mLanguages.size();
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        TextView tvDesc;
        TextView tvSize;
        ProgressBar progressBar;
        TextView btnAction;
        
        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_language_name);
            tvDesc = itemView.findViewById(R.id.tv_language_desc);
            tvSize = itemView.findViewById(R.id.tv_size);
            progressBar = itemView.findViewById(R.id.progress_bar);
            btnAction = itemView.findViewById(R.id.btn_action);
        }
    }
}
