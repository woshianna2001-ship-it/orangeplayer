package com.orange.playerlibrary;

import static android.content.Context.MODE_PRIVATE;

import android.app.Activity;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class OrangeSharedSqlite {
    private Activity activity;
    private SharedPreferences sharedPreferences;
    
    // 初始化
    public void init(Activity mactivity, String name) {
        activity = mactivity;
        sharedPreferences = activity.getSharedPreferences(name, MODE_PRIVATE);
    }
    
    // 获取SharedPreferences
    public SharedPreferences getSharedPreferences() {
        return sharedPreferences;
    }
    
    public SharedPreferences SharedPreferences() {
        return sharedPreferences;
    }
    
    // 写入数据
    public void put(String name, Boolean key) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(name, key);
        editor.apply();
    }
    
    public void put(String name, Float key) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putFloat(name, key);
        editor.apply();
    }
    
    public void put(String name, int key) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(name, key);
        editor.apply();
    }
    
    public void put(String name, long key) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(name, key);
        editor.apply();
    }
    
    public void put(String name, String key) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(name, key);
        editor.apply();
    }
    
    public void putBoolean(String name, Boolean key) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(name, key);
        editor.apply();
    }
    
    public void putFloat(String name, Float key) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putFloat(name, key);
        editor.apply();
    }
    
    public void putInt(String name, int key) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(name, key);
        editor.apply();
    }
    
    public void putLong(String name, long key) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(name, key);
        editor.apply();
    }
    
    public void putString(String name, String key) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(name, key);
        editor.apply();
    }
    
    // 写入一个合集
    public void putStringList(String name, StringSet set) {
        Set<String> hobbies = new HashSet<>();
        set.put(hobbies);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putStringSet(name, hobbies);
        editor.apply();
    }
    
    public interface StringSet {
        void put(Set<String> name);
    }
    
    // 删除
    public void remove(String name) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(name);
        editor.apply();
    }
    
    // 清除全部数据
    public void clear() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
    }
    
    // 修改数据
    public boolean setString(String name, String key) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(name, key);
        boolean success = editor.commit();
        return success;
    }
    
    public String getString(String key, String defaultValue) {
        String value = this.sharedPreferences.getString(key, null);
        return value != null ? value : defaultValue;
    }
    
    public boolean getBoolean(String key, boolean defaultValue) {
        return this.sharedPreferences.getBoolean(key, defaultValue);
    }
    
    public int getInt(String key, int defaultValue) {
        return this.sharedPreferences.getInt(key, defaultValue);
    }
    
    public long getLong(String key, long defaultValue) {
        return this.sharedPreferences.getLong(key, defaultValue);
    }
    
    public float getFloat(String key, float defaultValue) {
        return this.sharedPreferences.getFloat(key, defaultValue);
    }
    
    public boolean setBoolean(String name, boolean key) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(name, key);
        return editor.commit();
    }
    
    public boolean setFloat(String name, float key) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putFloat(name, key);
        return editor.commit();
    }
    
    public boolean setInt(String name, int key) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(name, key);
        return editor.commit();
    }
    
    public boolean setLong(String name, long key) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(name, key);
        return editor.commit();
    }
    
    // 异步提交修改
    public void apply() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.apply();
    }
    
    // 获取数据
    public boolean getBoolean(String name) {
        return sharedPreferences.getBoolean(name, false);
    }
    
    public float getFloat(String name) {
        return sharedPreferences.getFloat(name, 0.0f);
    }
    
    public int getInt(String name) {
        return sharedPreferences.getInt(name, 0);
    }
    
    public long getLong(String name) {
        return sharedPreferences.getLong(name, 0);
    }
    
    public String getString(String name) {
        return sharedPreferences.getString(name, "未命名");
    }
    
    // 获取全部数据
    public void getAllList(AllList list) {
        Map<String, ?> allEntries = sharedPreferences.getAll();
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            list.getList(entry.getKey(), entry.getValue().toString(), entry);
        }
    }
    
    public interface AllList {
        void getList(String name, String key, Map.Entry<String, ?> entry);
    }
    
    // 注册监听器
    private SharedPreferences.OnSharedPreferenceChangeListener listener;
    
    public void setOnChangeListener(OnChangeListener listener) {
        removeOnChangeListener(); // 先移除旧监听器
        this.listener = (sharedPreferences, key) -> {
            Object value = sharedPreferences.getAll().get(key);
            listener.onChanged(key, value);
        };
        sharedPreferences.registerOnSharedPreferenceChangeListener(this.listener);
    }
    
    public interface OnChangeListener {
        void onChanged(String key, Object value);
    }
    
    // 注销监听器
    public void removeOnChangeListener() {
        if (listener != null) {
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener);
        }
    }
}
