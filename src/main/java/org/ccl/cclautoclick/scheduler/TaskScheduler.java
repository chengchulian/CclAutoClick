package org.ccl.cclautoclick.scheduler;

import org.ccl.cclautoclick.model.Key;
import org.ccl.cclautoclick.model.KeyConfig;

import java.util.Map;
import java.util.concurrent.*;

/**
 * 任务调度器 - 管理所有按键的连点任务
 */
public class TaskScheduler {

    private final ScheduledExecutorService executor;
    private final Map<Key, ScheduledFuture<?>> taskMap = new ConcurrentHashMap<>();
    private final Map<Key, KeyConfig> configMap = new ConcurrentHashMap<>();

    public TaskScheduler() {
        // 使用线程池，最多8个线程
        this.executor = Executors.newScheduledThreadPool(8);
    }

    /**
     * 注册按键配置
     */
    public void registerConfig(Key key, KeyConfig config) {
        configMap.put(key, config);
    }

    /**
     * 启动任务
     */
    public void startTask(Key key) {
        KeyConfig config = configMap.get(key);
        if (config == null || !config.isEnabled()) {
            return;
        }

        // 如果任务已在运行，先停止
        if (taskMap.containsKey(key)) {
            System.out.println("任务已存在，跳过启动: " + key.getName());
            return;
        }

        System.out.println("启动任务: " + key.getName() + ", 配置: " + config);

        // 创建并调度任务
        ClickTask clickTask = new ClickTask(config);
        ScheduledFuture<?> future = executor.scheduleAtFixedRate(
                clickTask,
                config.getDelay(),
                config.getInterval(),
                TimeUnit.MILLISECONDS
        );

        taskMap.put(key, future);
    }

    /**
     * 停止任务
     */
    public void stopTask(Key key) {
        ScheduledFuture<?> future = taskMap.remove(key);
        if (future != null) {
            future.cancel(true);
            System.out.println("停止任务: " + key.getName());
        }
    }

    /**
     * 检查任务是否正在运行
     */
    public boolean isRunning(Key key) {
        return taskMap.containsKey(key);
    }

    /**
     * 停止所有任务
     */
    public void stopAll() {
        for (Key key : taskMap.keySet()) {
            stopTask(key);
        }
    }

    /**
     * 关闭调度器
     */
    public void shutdown() {
        stopAll();
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
