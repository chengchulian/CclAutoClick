package org.ccl.cclautoclick.listener;

import com.sun.jna.Pointer;
import org.ccl.cclautoclick.engine.InputFilter;
import org.ccl.cclautoclick.jna.WinUser;
import org.ccl.cclautoclick.model.Key;
import org.ccl.cclautoclick.model.KeyEvent;

/**
 * JNA实现的全局输入监听器 - 使用Windows Hook
 */
public class JnaInputListener implements GlobalInputListener {

    private Pointer keyboardHook = null;
    private Pointer mouseHook = null;
    private WinUser.LowLevelKeyboardProc keyboardProc;
    private WinUser.LowLevelMouseProc mouseProc;
    private volatile boolean listening = false;
    private EventListener eventListener;

    @Override
    public void start() {
        if (listening) {
            return;
        }

        listening = true;

        // 安装键盘钩子
        installKeyboardHook();

        // 安装鼠标钩子
        installMouseHook();
    }

    @Override
    public void stop() {
        if (!listening) {
            return;
        }

        // 卸载钩子
        if (keyboardHook != null) {
            WinUser.INSTANCE.UnhookWindowsHookEx(keyboardHook);
            keyboardHook = null;
        }

        if (mouseHook != null) {
            WinUser.INSTANCE.UnhookWindowsHookEx(mouseHook);
            mouseHook = null;
        }

        listening = false;
    }

    @Override
    public boolean isListening() {
        return listening;
    }

    @Override
    public void setEventListener(EventListener listener) {
        this.eventListener = listener;
    }

    /**
     * 安装键盘钩子
     */
    private void installKeyboardHook() {
        keyboardProc = (nCode, wParam, lParam) -> {
            if (nCode >= 0 && eventListener != null) {
                long wParamValue = wParam != null ? Pointer.nativeValue(wParam) : 0;
                int message = (int) wParamValue;

                if (message == WinUser.WM_KEYDOWN || message == WinUser.WM_SYSKEYDOWN ||
                        message == WinUser.WM_KEYUP || message == WinUser.WM_SYSKEYUP) {

                    WinUser.KBDLLHOOKSTRUCT struct = new WinUser.KBDLLHOOKSTRUCT(lParam);

                    // 【关键】防回环过滤：忽略模拟输入事件
                    if (InputFilter.shouldIgnore(struct)) {
                        return WinUser.INSTANCE.CallNextHookEx(keyboardHook, nCode, wParam, lParam);
                    }

                    Key key = Key.fromCode(struct.vkCode);

                    if (key != null) {
                        KeyEvent.EventType type = (message == WinUser.WM_KEYDOWN || message == WinUser.WM_SYSKEYDOWN)
                                ? KeyEvent.EventType.DOWN
                                : KeyEvent.EventType.UP;

                        KeyEvent event = new KeyEvent(key, type);
                        eventListener.onKeyEvent(event);
                    }
                }
            }

            return WinUser.INSTANCE.CallNextHookEx(keyboardHook, nCode, wParam, lParam);
        };

        keyboardHook = WinUser.INSTANCE.SetWindowsHookEx(
                WinUser.WH_KEYBOARD_LL,
                keyboardProc,
                null,
                0
        );

        if (keyboardHook == null) {
            System.err.println("无法安装键盘挂钩");
            listening = false;
        }
    }

    /**
     * 安装鼠标钩子
     */
    private void installMouseHook() {
        mouseProc = (nCode, wParam, lParam) -> {
            if (nCode >= 0 && eventListener != null) {
                long wParamValue = wParam != null ? Pointer.nativeValue(wParam) : 0;
                int message = (int) wParamValue;

                Key key = null;
                KeyEvent.EventType type = null;

                switch (message) {
                    case WinUser.WM_LBUTTONDOWN:
                        key = Key.MOUSE_LEFT;
                        type = KeyEvent.EventType.DOWN;
                        break;
                    case WinUser.WM_LBUTTONUP:
                        key = Key.MOUSE_LEFT;
                        type = KeyEvent.EventType.UP;
                        break;
                    case WinUser.WM_RBUTTONDOWN:
                        key = Key.MOUSE_RIGHT;
                        type = KeyEvent.EventType.DOWN;
                        break;
                    case WinUser.WM_RBUTTONUP:
                        key = Key.MOUSE_RIGHT;
                        type = KeyEvent.EventType.UP;
                        break;
                    case WinUser.WM_MBUTTONDOWN:
                        key = Key.MOUSE_MIDDLE;
                        type = KeyEvent.EventType.DOWN;
                        break;
                    case WinUser.WM_MBUTTONUP:
                        key = Key.MOUSE_MIDDLE;
                        type = KeyEvent.EventType.UP;
                        break;
                }

                if (key != null && type != null) {
                    // 【关键】防回环过滤：需要读取鼠标结构体进行判断
                    WinUser.MSLLHOOKSTRUCT struct = new WinUser.MSLLHOOKSTRUCT(lParam);
                    if (!InputFilter.shouldIgnore(struct)) {
                        KeyEvent event = new KeyEvent(key, type);
                        eventListener.onKeyEvent(event);
                    }
                }
            }

            return WinUser.INSTANCE.CallNextHookEx(mouseHook, nCode, wParam, lParam);
        };

        mouseHook = WinUser.INSTANCE.SetWindowsHookEx(
                WinUser.WH_MOUSE_LL,
                mouseProc,
                null,
                0
        );

        if (mouseHook == null) {
            System.err.println("无法安装鼠标挂钩");
            listening = false;
        }
    }
}
