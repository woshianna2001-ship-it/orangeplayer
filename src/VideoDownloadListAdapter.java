package com.jeffmony.videodemo.download;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.orange.downloader.model.VideoTaskItem;
import com.orange.downloader.model.VideoTaskState;
import com.orange.downloader.utils.LogUtils;
import java.io.File;
import android.widget.Toast;
import com.bumptech.glide.Glide;
import com.orange.downloader.VideoDownloadManager;
import java.util.List;
import com.orange.downloader.model.VideoTaskItem;
import com.kongzue.dialogx.DialogX;
import com.kongzue.dbv3.DB;
import android.widget.LinearLayout;

import com.uaoanlao.m3u8down.R;



public class VideoDownloadListAdapter extends ArrayAdapter<VideoTaskItem> {
  
  private static final String TAG = "VideoListAdapter";
  
  private Context mContext;
  
  private static DownItem downitem;
  
  public VideoDownloadListAdapter(Context context, int resource, VideoTaskItem[] items) {
    super(context, resource, items);
    mContext = context;
  }
  @NonNull
  @Override
  public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
    
    View view = LayoutInflater.from(getContext()).inflate(R.layout.download_item, null);
    final VideoTaskItem item = getItem(position);
    TextView urlTextView = (TextView) view.findViewById(R.id.url);
    urlTextView.setText(item.getUrl());
    TextView stateTextView = (TextView) view.findViewById(R.id.zt);
    TextView playBtn = (TextView) view.findViewById(R.id.zt);
    TextView title = (TextView) view.findViewById(R.id.title);
    title.setText(item.getTitle());
    android.widget.ImageView imageView=(android.widget.ImageView)view.findViewById(R.id.tx1);
    Glide.with(mContext).load(item.getCoverUrl()).into(imageView);
    
    final android.widget.LinearLayout linear=(android.widget.LinearLayout)view.findViewById(R.id.xxbj6);
    linear.setOnClickListener(new View.OnClickListener(){
      @Override
      public void onClick(View v) {
        LogUtils.i(TAG, "[UI_CLICK] Task clicked, url=" + item.getUrl() + ", state=" + item.getTaskState());
        if (item.isInitialTask()) {
          LogUtils.i(TAG, "[UI_CLICK] isInitialTask=true, calling startDownload");
          VideoDownloadManager.getInstance().startDownload(item);
        } else if (item.isRunningTask()) {
          LogUtils.i(TAG, "[UI_CLICK] isRunningTask=true, calling pauseDownloadTask");
          VideoDownloadManager.getInstance().pauseDownloadTask(item.getUrl());
        } else if (item.isInterruptTask()) {
          LogUtils.i(TAG, "[UI_CLICK] isInterruptTask=true, calling resumeDownload");
          VideoDownloadManager.getInstance().resumeDownload(item.getUrl());
        } else if (item.isPendingTask()) {
          // 任务在等待中，可能是并发限制导致，尝试启动
          LogUtils.i(TAG, "[UI_CLICK] isPendingTask=true, task is waiting in queue");
        } else if (item.isCompleted()){
          LogUtils.i(TAG, "[UI_CLICK] isCompleted=true, opening file");
          //下载完成点击
          String filePath = item.getFilePath();
          File file = new File(filePath);
          if (file.exists()) {
            //Toast.makeText(mContext,item.getFilePath(),Toast.LENGTH_SHORT).show();
            downitem.onclick(item.getFilePath());
          } else {
            LogUtils.w(TAG, "[UI_CLICK] File not exists: " + filePath);
          }
        } else {
          LogUtils.w(TAG, "[UI_CLICK] Unknown state: " + item.getTaskState());
        }
      }
    });
    
    //列表项目长按
    linear.setOnLongClickListener(new View.OnLongClickListener() {
      
      @Override
      public boolean onLongClick(View view) {
        
        
        com.kongzue.dialogx.dialogs.PopTip.show(R.mipmap.dele, "是否要删除该任务？", "删除").setButton(new com.kongzue.dialogx.interfaces.OnDialogButtonClickListener<com.kongzue.dialogx.dialogs.PopTip>() {
          @Override
          public boolean onClick(com.kongzue.dialogx.dialogs.PopTip popTip, View v) {
            //点击“撤回”按钮回调
            VideoDownloadManager.getInstance().deleteVideoTask(item.getUrl(),true);
            
            com.kongzue.dbv3.DB.getTable("m3u8")
            .delete(new com.kongzue.dbv3.data.DBData()
            .set("link", item.getUrl())
            );
            linear.setVisibility(View.GONE);
            
            com.kongzue.dialogx.dialogs.PopTip.show("已删除");
            return false;
          }
        }).setTintIcon(true).showLong();
        
        return false;
      }
    });
    
    
    
    TextView infoTextView = (TextView) view.findViewById(R.id.seek);
    TextView ws = (TextView) view.findViewById(R.id.ws);
    TextView size = (TextView) view.findViewById(R.id.size);
    android.widget.LinearLayout xxbj5=(android.widget.LinearLayout)view.findViewById(R.id.xxbj5);
    android.widget.ProgressBar pro=(android.widget.ProgressBar)view.findViewById(R.id.jdt1);
    setStateText(playBtn,xxbj5, item);
    setDownloadInfoText(infoTextView,ws,size,pro,title, item);
    return view;
  }
  
  
  
  private void setStateText(TextView stateView, android.widget.LinearLayout playBtn, VideoTaskItem item) {
    switch (item.getTaskState()) {
      case VideoTaskState.PENDING:
      case VideoTaskState.PREPARE:
      playBtn.setVisibility(View.INVISIBLE);
      stateView.setText("等待中");
      break;
      case VideoTaskState.START:
      case VideoTaskState.DOWNLOADING:
      playBtn.setVisibility(View.VISIBLE);
      stateView.setText("下载中");
      break;
      case VideoTaskState.PAUSE:
      //playBtn.setVisibility(View.INVISIBLE);
      stateView.setText("已暂停");
      break;
      case VideoTaskState.SUCCESS:
      playBtn.setVisibility(View.VISIBLE);
      stateView.setText("已下载");
      stateView.setTextColor(0xff0082ec);
      break;
      case VideoTaskState.ERROR:
      playBtn.setVisibility(View.INVISIBLE);
      stateView.setText("下载错误");
      break;
      default:
      playBtn.setVisibility(View.INVISIBLE);
      stateView.setText("未下载");
      break;
    }
  }
  
  private void setDownloadInfoText(TextView infoView,TextView ws,TextView size,android.widget.ProgressBar pro,TextView title, VideoTaskItem item) {
    switch (item.getTaskState()) {
      case VideoTaskState.DOWNLOADING:
      infoView.setText(item.getPercentString());
      ws.setText(item.getSpeedString());
      size.setText(item.getDownloadSizeString());
      pro.setProgress((int)item.getPercent());
      //title.setText(item.getTitle());
      break;
      case VideoTaskState.SUCCESS:
      case VideoTaskState.PAUSE:
      infoView.setText(item.getPercentString());
      ws.setText(item.getSpeedString());
      size.setText(item.getDownloadSizeString());
      pro.setProgress((int)item.getPercent());
      //title.setText(item.getTitle());
      break;
      default:
      break;
    }
  }
  
  public void notifyChanged(VideoTaskItem[] items, VideoTaskItem item) {
    for (int index = 0; index < getCount(); index++) {
      if (getItem(index).equals(item)) {
        items[index] = item;
        notifyDataSetChanged();
      }
    }
  }
  
  
  
  public static void setDownloadCallback(DownItem item){
        downitem= item;
    }

    public static interface DownItem{
        void onclick(String nr);
    }
  
}


//QQ 1823565614 七桐
//QQ 1823565614 七桐
//QQ交流群598138277
/*
github地址 https://github.com/JeffMony/VideoDownloader.git
更多方法自行去github查看
移植到项目请注意看清单文件的代码
*/
