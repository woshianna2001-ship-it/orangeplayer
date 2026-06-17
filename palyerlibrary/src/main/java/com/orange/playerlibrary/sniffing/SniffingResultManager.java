package com.orange.playerlibrary.sniffing;

import android.content.Context;
import android.content.SharedPreferences;

import com.orange.playerlibrary.VideoSniffing;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * 嗅探结果持久化管理器
 * 使用 SharedPreferences 存储嗅探结果
 */
public class SniffingResultManager {
    
    private static final String PREF_NAME = "orange_sniffing_results";
    private static final String KEY_RESULTS = "results";
    private static final int MAX_RESULTS = 50; // 最多保存 50 个结果
    
    private static SniffingResultManager sInstance;
    private final SharedPreferences mPreferences;
    
    private SniffingResultManager(Context context) {
        mPreferences = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
    
    public static synchronized SniffingResultManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new SniffingResultManager(context);
        }
        return sInstance;
    }
    
    /**
     * 保存嗅探结果
     */
    public void saveResult(VideoSniffing.VideoInfo videoInfo) {
        if (videoInfo == null || videoInfo.url == null) {
            return;
        }
        
        try {
            List<VideoSniffing.VideoInfo> results = getAllResults();
            
            // 去重：如果已存在相同 URL，先删除
            Iterator<VideoSniffing.VideoInfo> iterator = results.iterator();
            while (iterator.hasNext()) {
                VideoSniffing.VideoInfo info = iterator.next();
                if (info.url.equals(videoInfo.url)) {
                    iterator.remove();
                    break;
                }
            }
            
            // 添加到列表开头
            results.add(0, videoInfo);
            
            // 限制数量
            if (results.size() > MAX_RESULTS) {
                results = results.subList(0, MAX_RESULTS);
            }
            
            // 保存
            saveResults(results);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 删除指定 URL 的结果
     */
    public void deleteResult(String url) {
        if (url == null) {
            return;
        }
        
        try {
            List<VideoSniffing.VideoInfo> results = getAllResults();
            Iterator<VideoSniffing.VideoInfo> iterator = results.iterator();
            while (iterator.hasNext()) {
                VideoSniffing.VideoInfo info = iterator.next();
                if (info.url.equals(url)) {
                    iterator.remove();
                    break;
                }
            }
            saveResults(results);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 获取所有嗅探结果
     */
    public List<VideoSniffing.VideoInfo> getAllResults() {
        List<VideoSniffing.VideoInfo> results = new ArrayList<>();
        
        try {
            String json = mPreferences.getString(KEY_RESULTS, "[]");
            JSONArray jsonArray = new JSONArray(json);
            
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                VideoSniffing.VideoInfo videoInfo = fromJson(jsonObject);
                if (videoInfo != null) {
                    results.add(videoInfo);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        
        return results;
    }
    
    /**
     * 清空所有结果
     */
    public void clearAll() {
        mPreferences.edit().remove(KEY_RESULTS).apply();
    }
    
    /**
     * 保存结果列表
     */
    private void saveResults(List<VideoSniffing.VideoInfo> results) {
        try {
            JSONArray jsonArray = new JSONArray();
            for (VideoSniffing.VideoInfo info : results) {
                JSONObject jsonObject = toJson(info);
                if (jsonObject != null) {
                    jsonArray.put(jsonObject);
                }
            }
            mPreferences.edit().putString(KEY_RESULTS, jsonArray.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 将 VideoInfo 转换为 JSON
     */
    private JSONObject toJson(VideoSniffing.VideoInfo videoInfo) {
        if (videoInfo == null) {
            return null;
        }
        
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("url", videoInfo.url);
            jsonObject.put("contentType", videoInfo.contentType);
            jsonObject.put("title", videoInfo.title);
            
            // 保存 headers
            if (videoInfo.headers != null && !videoInfo.headers.isEmpty()) {
                JSONObject headersJson = new JSONObject();
                for (String key : videoInfo.headers.keySet()) {
                    headersJson.put(key, videoInfo.headers.get(key));
                }
                jsonObject.put("headers", headersJson);
            }
            
            return jsonObject;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * 从 JSON 转换为 VideoInfo
     */
    private VideoSniffing.VideoInfo fromJson(JSONObject jsonObject) {
        if (jsonObject == null) {
            return null;
        }
        
        try {
            String url = jsonObject.optString("url");
            String contentType = jsonObject.optString("contentType");
            String title = jsonObject.optString("title");
            
            // 解析 headers
            HashMap<String, String> headers = new HashMap<>();
            if (jsonObject.has("headers")) {
                JSONObject headersJson = jsonObject.getJSONObject("headers");
                Iterator<String> keys = headersJson.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    headers.put(key, headersJson.getString(key));
                }
            }
            
            return new VideoSniffing.VideoInfo(url, contentType, title, headers);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }
}
