package com.uaoanlao.m3u8down;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.ImageView;
import android.view.View;
import android.os.Build;
import android.graphics.Color;
import android.view.WindowManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.orange.downloader.VideoDownloadManager;
import com.orange.downloader.listener.DownloadListener;
import com.orange.downloader.listener.IDownloadInfosCallback;
import com.orange.downloader.model.VideoTaskItem;
import com.orange.downloader.utils.LogUtils;
import java.util.List;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView;
import android.widget.Adapter;
import java.util.List;
import com.orange.downloader.VideoDownloadManager;
import com.orange.downloader.listener.IDownloadInfosCallback;
import com.orange.downloader.model.VideoTaskItem;
import com.kongzue.dbv3.DB;
import java.util.List;
import android.content.Intent;
import android.content.Context;
import android.widget.TextView;

public class VideoDownloadListActivity extends AppCompatActivity {
  
  private ListView mDownloadListView;
  public static int yes;
  public Intent tz = new Intent();
  private com.jeffmony.videodemo.download.VideoDownloadListAdapter mAdapter;
  private VideoTaskItem[] items = new VideoTaskItem[getVideoTaskIte()];
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_downloadlist);
    
    
    //设置状态栏
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
      getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
      getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
      getWindow().setStatusBarColor(Color.TRANSPARENT);
      getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
    } else {
      getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
      getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
    }
    
    com.kongzue.dialogx.DialogX.init(this); //初始化
    com.kongzue.dialogx.DialogX.globalStyle = com.kongzue.dialogx.style.MaterialStyle.style(); //设置样式
    
    //限制多线程数量
    VideoDownloadManager.getInstance().setConcurrentCount(3);
    
    //设置返回按钮
    ImageView end=findViewById(R.id.tx1);
    end.setImageResource(R.mipmap.end);
    end.setOnClickListener(new View.OnClickListener() {
      
      @Override
      public void onClick(View view) {
        finish();
      }
    });
    
    //删除全部按钮
    TextView dele=(TextView)findViewById(R.id.wb4);
    dele.setOnClickListener(new View.OnClickListener() {
      
      @Override
      public void onClick(View view) {
        
        com.kongzue.dialogx.dialogs.PopTip.show(R.mipmap.dele, "是否删除全部下载任务？", "删除").setButton(new com.kongzue.dialogx.interfaces.OnDialogButtonClickListener<com.kongzue.dialogx.dialogs.PopTip>() {
          @Override
          public boolean onClick(com.kongzue.dialogx.dialogs.PopTip popTip, View v) {
            //点击“撤回”按钮回调
            VideoDownloadManager.getInstance().deleteAllVideoFiles();
            
            List<com.kongzue.dbv3.data.DBData> result = com.kongzue.dbv3.DB.getTable("m3u8").find();
            com.kongzue.dbv3.DB.getTable("m3u8")
            .delete(result);
            
            setVideoTaskIte(0);
            mDownloadListView.setAdapter(null);
            mAdapter.notifyDataSetChanged();
            
            com.kongzue.dialogx.dialogs.PopTip.show("已全部删除");
            return false;
          }
        }).setTintIcon(true).showLong();
        
      }
    });
    
    //全部暂停按钮
    TextView stop=(TextView)findViewById(R.id.wb6);
    stop.setOnClickListener(new View.OnClickListener() {
      
      @Override
      public void onClick(View view) {
        VideoDownloadManager.getInstance().pauseAllDownloadTasks();
      }
    });
    
    
    mDownloadListView=findViewById(R.id.lb1);
    
    VideoDownloadManager.getInstance().setGlobalDownloadListener(mListener);
    List<com.kongzue.dbv3.data.DBData> result = DB.getTable("m3u8").find();
    for (com.kongzue.dbv3.data.DBData name : result){
      items[(int)result.indexOf(name)]=new VideoTaskItem(name.getString("link"),name.getString("imageurl"),name.getString("name"),"group-1");
      mAdapter = new com.jeffmony.videodemo.download.VideoDownloadListAdapter(this, R.layout.download_item, items);
    }
    
    mDownloadListView.setAdapter(mAdapter);
    
    VideoDownloadManager.getInstance().fetchDownloadItems(mInfosCallback);
    
    
  }
  
  
  public void StartDownloadActivity(android.app.Activity ei){
    List<com.kongzue.dbv3.data.DBData> result = DB.getTable("m3u8").find();
    for (com.kongzue.dbv3.data.DBData name : result){
      VideoDownloadListActivity.setVideoTaskIte(result.size());
    }
    tz.setClass(ei, VideoDownloadListActivity.class);
    ei.startActivity(tz);
  }
  
  public static void setVideoTaskIte(int ye){
    yes=ye;
  }
  public static int getVideoTaskIte(){
    return yes;
  }
  
  
  public static VideoTaskItem addVideoTaskIte(String link,String imageurl,String name){
    VideoTaskItem item = new VideoTaskItem(link, imageurl, name, "group-1");
    com.kongzue.dbv3.DB.getTable("m3u8") 
    .add(                               //添加数据
    new com.kongzue.dbv3.data.DBData()
    .set("link",link)
    .set("imageurl",imageurl)
    .set("name",name)
    );
    return item;
  }
  
  private long mLastProgressTimeStamp;
  private long mLastSpeedTimeStamp;
  
  private DownloadListener mListener = new DownloadListener() {
    
    @Override
    public void onDownloadDefault(VideoTaskItem item) {
      notifyChanged(item);
    }
    
    @Override
    public void onDownloadPending(VideoTaskItem item) {
      notifyChanged(item);
    }
    
    @Override
    public void onDownloadPrepare(VideoTaskItem item) {
      notifyChanged(item);
    }
    
    @Override
    public void onDownloadStart(VideoTaskItem item) {
      notifyChanged(item);
    }
    
    @Override
    public void onDownloadProgress(VideoTaskItem item) {
      long currentTimeStamp = System.currentTimeMillis();
      if (currentTimeStamp - mLastProgressTimeStamp > 1000) {
        notifyChanged(item);
        mLastProgressTimeStamp = currentTimeStamp;
      }
    }
    
    @Override
    public void onDownloadSpeed(VideoTaskItem item) {
      long currentTimeStamp = System.currentTimeMillis();
      if (currentTimeStamp - mLastSpeedTimeStamp > 1000) {
        notifyChanged(item);
        mLastSpeedTimeStamp = currentTimeStamp;
      }
    }
    
    @Override
    public void onDownloadPause(VideoTaskItem item) {
      notifyChanged(item);
    }
    
    @Override
    public void onDownloadError(VideoTaskItem item) {
      notifyChanged(item);
    }
    
    @Override
    public void onDownloadSuccess(VideoTaskItem item) {
      notifyChanged(item);
    }
  };
  
  private void notifyChanged(final VideoTaskItem item) {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        mAdapter.notifyChanged(items, item);
      }
    });
  }
  
  private IDownloadInfosCallback mInfosCallback =
  new IDownloadInfosCallback() {
    @Override
    public void onDownloadInfos(List<VideoTaskItem> items) {
      for (VideoTaskItem item : items) {
        notifyChanged(item);
      }
    }
  };
  
  
  
  
}


//QQ 1823565614 七桐
//QQ 1823565614 七桐
//QQ交流群598138277
/*
github地址 https://github.com/JeffMony/VideoDownloader.git
更多方法自行去github查看
移植到项目请注意看清单文件的代码
*/
