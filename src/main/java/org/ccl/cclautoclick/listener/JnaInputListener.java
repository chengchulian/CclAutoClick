package org.ccl.cclautoclick.listener;

import com.sun.jna.Pointer;
import org.ccl.cclautoclick.engine.InputFilter;
import org.ccl.cclautoclick.jna.User32;
import org.ccl.cclautoclick.model.Key;
import org.ccl.cclautoclick.model.KeyEvent;

/**
 * JNA实现的全局输入监听器 - 使用Windows Hook
 */
public class JnaInputListener implements GlobalInputListener {

    private Pointer keyboardHook = null;
    private Pointer mouseHook = null;
    private User32.LowLevelKeyboardProc keyboardProc;
    private User32.LowLevelMouseProc mouseProc;
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
            User32.INSTANCE.UnhookWindowsHookEx(keyboardHook);
            keyboardHook = null;
        }

        if (mouseHook != null) {
            User32.INSTANCE.UnhookWindowsHookEx(mouseHook);
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

                if (message == User32.WM_KEYDOWN || message == User32.WM_SYSKEYDOWN ||
                        message == User32.WM_KEYUP || message == User32.WM_SYSKEYUP) {

                    User32.KBDLLHOOKSTRUCT struct = new User32.KBDLLHOOKSTRUCT(lParam);

                    // 【关键】防回环过滤：忽略模拟输入事件
                    if (InputFilter.shouldIgnore(struct)) {
                        return User32.INSTANCE.CallNextHookEx(keyboardHook, nCode, wParam, lParam);
                    }

                    Key key = Key.fromCode(struct.vkCode);

                    if (key != null) {
                        KeyEvent.EventType type = (message == User32.WM_KEYDOWN || message == User32.WM_SYSKEYDOWN)
                                ? KeyEvent.EventType.DOWN
                                : KeyEvent.EventType.UP;

                        KeyEvent event = new KeyEvent(key, type);
                        eventListener.onKeyEvent(event);
                    }
                }
            }

            return User32.INSTANCE.CallNextHookEx(keyboardHook, nCode, wParam, lParam);
        };

        keyboardHook = User32.INSTANCE.SetWindowsHookEx(
                User32.WH_KEYBOARD_LL,
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
                    case User32.WM_LBUTTONDOWN:
                        key = Key.MOUSE_LEFT;
                        type = KeyEvent.EventType.DOWN;
                        break;
                    case User32.WM_LBUTTONUP:
                        key = Key.MOUSE_LEFT;
                        type = KeyEvent.EventType.UP;
                        break;
                    case User32.WM_RBUTTONDOWN:
                        key = Key.MOUSE_RIGHT;
                        type = KeyEvent.EventType.DOWN;
                        break;
                    case User32.WM_RBUTTONUP:
                        key = Key.MOUSE_RIGHT;
                        type = KeyEvent.EventType.UP;
                        break;
                    case User32.WM_MBUTTONDOWN:
                        key = Key.MOUSE_MIDDLE;
                        type = KeyEvent.EventType.DOWN;
                        break;
                    case User32.WM_MBUTTONUP:
                        key = Key.MOUSE_MIDDLE;
                        type = KeyEvent.EventType.UP;
                        break;
                }

                if (key != null && type != null) {
                    // 【关键】防回环过滤：需要读取鼠标结构体进行判断
                    User32.MSLLHOOKSTRUCT struct = new User32.MSLLHOOKSTRUCT(lParam);
                    if (!InputFilter.shouldIgnore(struct)) {
                        KeyEvent event = new KeyEvent(key, type);
                        eventListener.onKeyEvent(event);
                    }
                }
            }

            return User32.INSTANCE.CallNextHookEx(mouseHook, nCode, wParam, lParam);
        };

        mouseHook = User32.INSTANCE.SetWindowsHookEx(
                User32.WH_MOUSE_LL,
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
