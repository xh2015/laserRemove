package com.topsky.laserremove.manager;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;

import com.blankj.utilcode.util.LogUtils;
import com.topsky.laserremove.LaserRemoveApp;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DataJsonManager {
    private Map<String, Integer> jsonMap;
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private boolean fromAssets = false;

    private DataJsonManager() {
        jsonMap = new HashMap<>();
    }

    private static class SingletonHolder {
        private static final DataJsonManager INSTANCE = new DataJsonManager();
    }

    public static DataJsonManager getInstance() {
        return SingletonHolder.INSTANCE;
    }

    public void getJsonFromLocal() {
        EXECUTOR.execute(() -> {
            Context context = LaserRemoveApp.getInstance();
            if (context == null) {
                return;
            }

            File documentsFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "data.json");

            String jsonString = null;

            if (documentsFile.exists() && documentsFile.isFile() && !fromAssets) {
                try {
                    FileInputStream fis = new FileInputStream(documentsFile);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    reader.close();
                    jsonString = sb.toString();
                    LogUtils.d("DataJsonManager: Successfully read from Documents directory");
                } catch (Exception e) {
                    LogUtils.e("DataJsonManager: Failed to read from Documents directory", e);
                }
            } else {
                LogUtils.d("DataJsonManager: File not found in Documents, reading from assets");
            }

            if (TextUtils.isEmpty(jsonString)) {
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(context.getAssets().open("data.json"), StandardCharsets.UTF_8));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    reader.close();
                    jsonString = sb.toString();
                    fromAssets = true;
                    LogUtils.d("DataJsonManager: Successfully read from assets directory");
                } catch (Exception e) {
                    LogUtils.e("DataJsonManager: Failed to read from assets directory", e);
                    return;
                }
            }

            if (!TextUtils.isEmpty(jsonString)) {
                try {
                    JSONObject jsonObject = new JSONObject(jsonString);
                    jsonMap.clear();
                    Iterator<String> keys = jsonObject.keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        Integer value = jsonObject.getInt(key);
                        jsonMap.put(key, value);
                    }
                    LogUtils.d("DataJsonManager: Successfully parsed JSON, count:", jsonMap.size());
                } catch (Exception e) {
                    LogUtils.e("DataJsonManager: Failed to parse JSON", e);
                    if (!fromAssets) {
                        getJsonFromLocal();
                    }
                }
            }
        });
    }

    public Map<String, Integer> getJsonMap() {
        return jsonMap;
    }

    public Integer getValue(String key) {
        return jsonMap.get(key);
    }
}
