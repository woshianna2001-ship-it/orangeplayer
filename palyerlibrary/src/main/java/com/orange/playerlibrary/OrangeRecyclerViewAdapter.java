package com.orange.playerlibrary;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * RecyclerView 适配器
 */
public class OrangeRecyclerViewAdapter extends RecyclerView.Adapter<OrangeRecyclerViewAdapter.ViewHolder> {
    
    private final int mItemLayoutId;
    private final ArrayList<HashMap<String, Object>> mData;
    private final OrangeRecyclerView.OnRecyclerViewAdapter mCallback;
    
    public OrangeRecyclerViewAdapter(int itemLayoutId,
                                    ArrayList<HashMap<String, Object>> data,
                                    OrangeRecyclerView.OnRecyclerViewAdapter callback) {
        mItemLayoutId = itemLayoutId;
        mData = data;
        mCallback = callback;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(mItemLayoutId, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (mCallback != null) {
            mCallback.bindView(holder, mData, position);
        }
    }
    
    @Override
    public int getItemCount() {
        return mData != null ? mData.size() : 0;
    }
    
    /**
     * ViewHolder
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final View itemView;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            this.itemView = itemView;
        }
    }
}
