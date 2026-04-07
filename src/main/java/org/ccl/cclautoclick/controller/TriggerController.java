package org.ccl.cclautoclick.controller;

import org.ccl.cclautoclick.engine.KeyStateMachine;
import org.ccl.cclautoclick.model.Key;
import org.ccl.cclautoclick.model.KeyConfig;
import org.ccl.cclautoclick.model.KeyEvent;
import org.ccl.cclautoclick.scheduler.TaskScheduler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 触发控制层 - 处理HOLD和TOGGLE模式
 */
public class TriggerController {

    private final Map<Key, KeyConfig> configMap = new ConcurrentHashMap<>();
    private final KeyStateMachine stateMachine;
    private final TaskScheduler scheduler;
    private TriggerMode mode = TriggerMode.TOGGLE;
    public TriggerController(TaskScheduler scheduler, KeyStateMachine stateMachine) {
        this.scheduler = scheduler;
        this.stateMachine = stateMachine;
    }

    /**
     * 注册按键配置
     */
    public void registerConfig(Key key, KeyConfig config) {
        configMap.put(key, config);
        scheduler.registerConfig(key, config);
    }

    /**
     * 获取触发模式
     */
    public TriggerMode getMode() {
        return mode;
    }

    /**
     * 设置触发模式
     */
    public void setMode(TriggerMode mode) {
        this.mode = mode;
    }

    /**
     * 处理按键按下事件
     */
    public void handleKeyDown(Key key) {
        KeyConfig config = configMap.get(key);
        if (config == null || !config.isEnabled()) {
            return;
        }

        if (mode == TriggerMode.HOLD) {
            stateMachine.onKeyDown(key);
        } else if (mode == TriggerMode.TOGGLE) {
            stateMachine.toggle(key);
        }
    }

    /**
     * 处理按键释放事件
     */
    public void handleKeyUp(Key key) {
        KeyConfig config = configMap.get(key);
        if (config == null || !config.isEnabled()) {
            return;
        }

        if (mode == TriggerMode.HOLD) {
            stateMachine.onKeyUp(key);
        }
        // TOGGLE模式下不处理keyup
    }

    /**
     * 处理按键事件
     */
    public void handleKeyEvent(KeyEvent event) {
        if (event.isKeyDown()) {
            handleKeyDown(event.getKey());
        } else {
            handleKeyUp(event.getKey());
        }
    }

    /**
     * 停止所有任务
     */
    public void stopAll() {
        stateMachine.stopAll();
    }

    /**
     * 检查按键是否正在运行
     */
    public boolean isRunning(Key key) {
        return stateMachine.isRunning(key);
    }

    public enum TriggerMode {
        HOLD,     // 按住激活
        TOGGLE    // 单击激活
    }
}
