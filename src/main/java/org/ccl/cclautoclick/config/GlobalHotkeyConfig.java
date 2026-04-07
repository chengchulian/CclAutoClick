package org.ccl.cclautoclick.config;

import org.ccl.cclautoclick.model.Key;

/**
 * 全局快捷键配置
 */
public class GlobalHotkeyConfig {

    private Key startStopHotkey; // 启动/停止快捷键

    public GlobalHotkeyConfig() {
        this.startStopHotkey = null; // 默认无快捷键
    }

    public Key getStartStopHotkey() {
        return startStopHotkey;
    }

    public void setStartStopHotkey(Key startStopHotkey) {
        this.startStopHotkey = startStopHotkey;
    }

    public boolean hasHotkey() {
        return startStopHotkey != null;
    }
}
