package org.ccl.cclautoclick.config;

import com.google.gson.*;
import org.ccl.cclautoclick.model.Key;
import org.ccl.cclautoclick.model.KeyConfig;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 配置管理器 - 负责配置的保存和加载
 */
public class ConfigManager {

    private static final String CONFIG_FILE = "config.json";
    private static final long AUTO_SAVE_DELAY_MS = 1000; // 自动保存延迟时间（1秒）
    private final Gson gson;
    private ScheduledExecutorService autoSaveExecutor;
    private ScheduledFuture<?> pendingSaveTask;
    private Object saveLock = new Object();

    public ConfigManager() {
        this.gson = createGson();
        this.autoSaveExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "AutoSave-Thread");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * 创建Gson实例，注册自定义序列化器
     */
    private Gson createGson() {
        return new GsonBuilder()
                .registerTypeAdapter(Key.class, new KeySerializer())
                .registerTypeAdapter(Key.class, new KeyDeserializer())
                .setPrettyPrinting()
                .create();
    }

    /**
     * 保存配置到本地文件
     */
    public void saveConfig(List<KeyConfig> configs, KeyConfig.TriggerMode triggerMode, org.ccl.cclautoclick.model.Key startStopHotkey) {
        try {
            ConfigData configData = new ConfigData();
            configData.setTriggerMode(triggerMode.name());

            // 设置全局快捷键
            if (startStopHotkey != null) {
                configData.setStartStopHotkey(startStopHotkey.name());
            } else {
                configData.setStartStopHotkey(null);
            }

            List<SavedKeyConfig> savedConfigs = new ArrayList<>();
            for (KeyConfig config : configs) {
                SavedKeyConfig saved = new SavedKeyConfig();
                saved.setKeyName(config.getKey().name());
                saved.setEnabled(config.isEnabled());
                saved.setInterval(config.getInterval());
                saved.setDelay(config.getDelay());
                saved.setTriggerMode(config.getTriggerMode().name());
                savedConfigs.add(saved);
            }
            configData.setConfigs(savedConfigs);

            String json = gson.toJson(configData);

            // 写入文件
            File file = new File(CONFIG_FILE);
            try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
                writer.write(json);
            }

            System.out.println("配置已保存到: " + file.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("保存配置失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 从本地文件加载配置
     */
    public ConfigData loadConfig() {
        File file = new File(CONFIG_FILE);
        if (!file.exists()) {
            System.out.println("配置文件不存在，使用默认配置");
            return null;
        }

        try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
            ConfigData configData = gson.fromJson(reader, ConfigData.class);
            System.out.println("配置已从: " + file.getAbsolutePath() + " 加载");
            return configData;
        } catch (IOException e) {
            System.err.println("加载配置失败: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 触发自动保存（带防抖功能）
     * 当频繁调用时，会取消之前的任务并重新调度，确保只在最后一次调用后保存
     */
    public void triggerAutoSave(List<KeyConfig> configs, KeyConfig.TriggerMode triggerMode, org.ccl.cclautoclick.model.Key startStopHotkey) {
        synchronized (saveLock) {
            // 取消之前待执行的保存任务
            if (pendingSaveTask != null && !pendingSaveTask.isDone()) {
                pendingSaveTask.cancel(false);
            }

            // 调度新的保存任务
            pendingSaveTask = autoSaveExecutor.schedule(() -> {
                try {
                    saveConfig(configs, triggerMode, startStopHotkey);
                    System.out.println("[自动保存] 配置已自动保存");
                } catch (Exception e) {
                    System.err.println("[自动保存] 保存失败: " + e.getMessage());
                    e.printStackTrace();
                }
            }, AUTO_SAVE_DELAY_MS, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * 关闭自动保存执行器
     */
    public void shutdown() {
        if (autoSaveExecutor != null && !autoSaveExecutor.isShutdown()) {
            // 等待当前任务完成
            autoSaveExecutor.shutdown();
            try {
                if (!autoSaveExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                    autoSaveExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                autoSaveExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 配置数据类
     */
    public static class ConfigData {
        private String triggerMode;
        private List<SavedKeyConfig> configs;
        private String startStopHotkey; // 全局启动/停止快捷键

        public String getTriggerMode() {
            return triggerMode;
        }

        public void setTriggerMode(String triggerMode) {
            this.triggerMode = triggerMode;
        }

        public List<SavedKeyConfig> getConfigs() {
            return configs;
        }

        public void setConfigs(List<SavedKeyConfig> configs) {
            this.configs = configs;
        }

        public String getStartStopHotkey() {
            return startStopHotkey;
        }

        public void setStartStopHotkey(String startStopHotkey) {
            this.startStopHotkey = startStopHotkey;
        }
    }

    /**
     * 保存的按键配置
     */
    public static class SavedKeyConfig {
        private String keyName;
        private boolean enabled;
        private int interval;
        private int delay;
        private String triggerMode;

        public String getKeyName() {
            return keyName;
        }

        public void setKeyName(String keyName) {
            this.keyName = keyName;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getInterval() {
            return interval;
        }

        public void setInterval(int interval) {
            this.interval = interval;
        }

        public int getDelay() {
            return delay;
        }

        public void setDelay(int delay) {
            this.delay = delay;
        }

        public String getTriggerMode() {
            return triggerMode;
        }

        public void setTriggerMode(String triggerMode) {
            this.triggerMode = triggerMode;
        }
    }

    /**
     * Key枚举的序列化器
     */
    private static class KeySerializer implements JsonSerializer<Key> {
        @Override
        public JsonElement serialize(Key src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.name());
        }
    }

    /**
     * Key枚举的反序列化器
     */
    private static class KeyDeserializer implements JsonDeserializer<Key> {
        @Override
        public Key deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            try {
                return Key.valueOf(json.getAsString());
            } catch (IllegalArgumentException e) {
                System.err.println("无效的按键名称: " + json.getAsString());
                return Key.KEY_A; // 默认返回KEY_A
            }
        }
    }
}
