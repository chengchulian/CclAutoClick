package org.ccl.cclautoclick.sender;

import com.sun.jna.Native;
import org.ccl.cclautoclick.engine.InputMark;
import org.ccl.cclautoclick.jna.WinUser;
import org.ccl.cclautoclick.model.Key;

public class InputSender {

    public static void sendMouse(int flags) {
        WinUser.INPUT input = new WinUser.INPUT();
        input.type = WinUser.INPUT_MOUSE;

        input.inputUnion.mi = new WinUser.MOUSEINPUT();
        input.inputUnion.mi.dwFlags = flags;
        input.inputUnion.mi.time = 0;
        // 【关键】设置自定义标记，防止回环
        input.inputUnion.mi.dwExtraInfo = (int) InputMark.MAGIC;

        input.inputUnion.write();
        input.write();

        send(new WinUser.INPUT[]{input});
    }

    public static void sendKey(short vk, boolean keyUp) {
        WinUser.INPUT input = new WinUser.INPUT();
        input.type = WinUser.INPUT_KEYBOARD;

        input.inputUnion.ki = new WinUser.KEYBDINPUT();
        input.inputUnion.ki.wVk = vk;
        input.inputUnion.ki.wScan = 0;
        input.inputUnion.ki.dwFlags = keyUp ? WinUser.KEYEVENTF_KEYUP : 0;
        input.inputUnion.ki.time = 0;
        // 【关键】设置自定义标记，防止回环
        input.inputUnion.ki.dwExtraInfo = (int) InputMark.MAGIC;

        input.inputUnion.write();
        input.write();

        send(new WinUser.INPUT[]{input});
    }

    public static void sendKeyClick(short vk) {
        sendKey(vk, false);
        sendKey(vk, true);
    }

    public static void sendWheel(int delta) {
        WinUser.INPUT input = new WinUser.INPUT();
        input.type = WinUser.INPUT_MOUSE;

        input.inputUnion.mi = new WinUser.MOUSEINPUT();
        input.inputUnion.mi.mouseData = delta;
        input.inputUnion.mi.dwFlags = WinUser.MOUSEEVENTF_WHEEL;

        input.inputUnion.write();
        input.write();

        send(new WinUser.INPUT[]{input});
    }

    /**
     * 执行按键点击（键盘或鼠标）
     */
    public static void execute(Key key) {
        if (key.isMouse()) {
            executeMouse(key);
        } else {
            sendKeyClick((short) key.getCode());
        }
    }

    /**
     * 执行鼠标点击
     */
    private static void executeMouse(Key key) {
        int downFlag, upFlag;
        switch (key) {
            case MOUSE_LEFT:
                downFlag = WinUser.MOUSEEVENTF_LEFTDOWN;
                upFlag = WinUser.MOUSEEVENTF_LEFTUP;
                break;
            case MOUSE_RIGHT:
                downFlag = WinUser.MOUSEEVENTF_RIGHTDOWN;
                upFlag = WinUser.MOUSEEVENTF_RIGHTUP;
                break;
            case MOUSE_MIDDLE:
                downFlag = WinUser.MOUSEEVENTF_MIDDLEDOWN;
                upFlag = WinUser.MOUSEEVENTF_MIDDLEUP;
                break;
            default:
                return;
        }

        // 按下
        sendMouse(downFlag);
        // 释放
        sendMouse(upFlag);
    }

    private static void send(WinUser.INPUT[] inputs) {

        for (WinUser.INPUT input : inputs) {
            input.write();
        }

        int size = new WinUser.INPUT().size();

        int sent = WinUser.INSTANCE.SendInput(inputs.length, inputs, size);

        if (sent != inputs.length) {
            int err = Native.getLastError();
            throw new RuntimeException(
                    "SendInput失败: sent=" + sent + ", err=" + err
            );
        }
    }
}