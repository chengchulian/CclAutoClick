package org.ccl.cclautoclick.model;

/**
 * 按键配置 - 每个按键的连点配置
 */
public class KeyConfig {

    private Key key;
    private boolean enabled;
    private int interval;      // 点击间隔（毫秒）
    private int delay;         // 启动延迟（毫秒）
    private TriggerMode triggerMode; // 触发模式
    public KeyConfig(Key key) {
        this.key = key;
        this.enabled = false;
        this.interval = 50;
        this.delay = 0;
        this.triggerMode = TriggerMode.TOGGLE; // 默认单击激活
    }

    public Key getKey() {
        return key;
    }

    public void setKey(Key key) {
        this.key = key;
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
        if (interval > 0) {
            this.interval = interval;
        }
    }

    public int getDelay() {
        return delay;
    }

    public void setDelay(int delay) {
        if (delay >= 0) {
            this.delay = delay;
        }
    }

    public TriggerMode getTriggerMode() {
        return triggerMode;
    }

    public void setTriggerMode(TriggerMode triggerMode) {
        if (triggerMode != null) {
            this.triggerMode = triggerMode;
        }
    }

    @Override
    public String toString() {
        return String.format("KeyConfig{key=%s, enabled=%b, interval=%dms, delay=%dms, triggerMode=%s}",
                key, enabled, interval, delay, triggerMode);
    }

    public enum TriggerMode {
        HOLD,     // 长按激活
        TOGGLE    // 单击激活
    }
}
