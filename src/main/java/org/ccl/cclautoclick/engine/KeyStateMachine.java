package org.ccl.cclautoclick.engine;

import org.ccl.cclautoclick.model.Key;
import org.ccl.cclautoclick.scheduler.TaskScheduler;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 按键状态机 - 每个按键独立的状态管理
 * 采用 pressedKeys 集合跟踪物理按键状态，防止 HOLD 模式抖动
 */
public class KeyStateMachine {

    /**
     * 当前被物理按下的按键集合（用于 HOLD 模式）
     */
    private final Set<Key> pressedKeys = ConcurrentHashMap.newKeySet();

    /**
     * 按键运行状态映射（用于 TOGGLE 模式）
     */
    private final Map<Key, KeyState> stateMap = new ConcurrentHashMap<>();

    private final TaskScheduler scheduler;

    public KeyStateMachine(TaskScheduler scheduler) {
        this.scheduler = scheduler;
    }

    /**
     * HOLD模式：按键按下
     * 只有当按键不在 pressedKeys 中时才启动任务（防抖）
     */
    public void onKeyDown(Key key) {
        // 如果已经在 pressedKeys 中，说明是重复的 keydown 事件，忽略
        if (pressedKeys.add(key)) {
            stateMap.put(key, KeyState.RUNNING);
            scheduler.startTask(key);
        }
    }

    /**
     * HOLD模式：按键释放
     * 只有当按键在 pressedKeys 中时才停止任务（确保是真实的 keyup）
     */
    public void onKeyUp(Key key) {
        // 只有从 pressedKeys 中成功移除才停止任务（防误触发）
        if (pressedKeys.remove(key)) {
            stateMap.put(key, KeyState.IDLE);
            scheduler.stopTask(key);
        }
    }

    /**
     * TOGGLE模式：切换状态
     */
    public void toggle(Key key) {
        KeyState currentState = stateMap.getOrDefault(key, KeyState.IDLE);
        if (currentState == KeyState.IDLE) {
            stateMap.put(key, KeyState.RUNNING);
            scheduler.startTask(key);
        } else {
            stateMap.put(key, KeyState.IDLE);
            scheduler.stopTask(key);
        }
    }

    /**
     * 获取按键当前状态
     */
    public KeyState getState(Key key) {
        return stateMap.getOrDefault(key, KeyState.IDLE);
    }

    /**
     * 检查按键是否正在运行
     */
    public boolean isRunning(Key key) {
        return stateMap.getOrDefault(key, KeyState.IDLE) == KeyState.RUNNING;
    }

    /**
     * 停止所有按键
     */
    public void stopAll() {
        pressedKeys.clear();
        for (Key key : stateMap.keySet()) {
            stateMap.put(key, KeyState.IDLE);
            scheduler.stopTask(key);
        }
    }
}
