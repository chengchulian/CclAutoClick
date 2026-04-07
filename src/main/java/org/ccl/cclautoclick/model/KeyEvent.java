package org.ccl.cclautoclick.model;

/**
 * 按键事件 - 统一的输入事件模型
 */
public class KeyEvent {

    private final Key key;
    private final EventType type;
    private final long timestamp;
    public KeyEvent(Key key, EventType type) {
        this.key = key;
        this.type = type;
        this.timestamp = System.currentTimeMillis();
    }

    public Key getKey() {
        return key;
    }

    public EventType getType() {
        return type;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean isKeyDown() {
        return type == EventType.DOWN;
    }

    public boolean isKeyUp() {
        return type == EventType.UP;
    }

    @Override
    public String toString() {
        return String.format("KeyEvent{key=%s, type=%s, time=%d}", key, type, timestamp);
    }

    public enum EventType {
        DOWN,   // 按下
        UP      // 释放
    }
}
