package org.ccl.cclautoclick.sender;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import org.ccl.cclautoclick.engine.InputMark;
import org.ccl.cclautoclick.jna.User32;
import org.ccl.cclautoclick.model.Key;

public class InputSender {

    public static void sendMouse(int flags) {
        User32.INPUT input = new User32.INPUT();
        input.type = User32.INPUT.INPUT_MOUSE;

        input.inputUnion.setType(User32.MOUSEINPUT.class);
        input.inputUnion.mi = new User32.MOUSEINPUT();
        input.inputUnion.mi.dwFlags = flags;
        input.inputUnion.mi.time = 0;
        // 【关键】设置自定义标记，防止回环
        input.inputUnion.mi.dwExtraInfo = Pointer.createConstant(InputMark.MAGIC);

        input.inputUnion.write();
        input.write();

        send(new User32.INPUT[]{input});
    }

    public static void sendKey(short vk, boolean keyUp) {
        User32.INPUT input = new User32.INPUT();
        input.type = User32.INPUT.INPUT_KEYBOARD;

        input.inputUnion.setType(User32.KEYBDINPUT.class);
        input.inputUnion.ki = new User32.KEYBDINPUT();
        input.inputUnion.ki.wVk = vk;
        input.inputUnion.ki.wScan = 0;
        input.inputUnion.ki.dwFlags = keyUp ? User32.KEYEVENTF_KEYUP : 0;
        input.inputUnion.ki.time = 0;
        // 【关键】设置自定义标记，防止回环
        input.inputUnion.ki.dwExtraInfo = Pointer.createConstant(InputMark.MAGIC);

        input.inputUnion.write();
        input.write();

        send(new User32.INPUT[]{input});
    }

    public static void sendKeyClick(short vk) {
        sendKey(vk, false);
        sendKey(vk, true);
    }

    public static void sendWheel(int delta) {
        User32.INPUT input = new User32.INPUT();
        input.type = User32.INPUT.INPUT_MOUSE;

        input.inputUnion.setType(User32.MOUSEINPUT.class);
        input.inputUnion.mi = new User32.MOUSEINPUT();
        input.inputUnion.mi.mouseData = delta;
        input.inputUnion.mi.dwFlags = User32.MOUSEEVENTF_WHEEL;

        input.inputUnion.write();
        input.write();

        send(new User32.INPUT[]{input});
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
                downFlag = User32.MOUSEEVENTF_LEFTDOWN;
                upFlag = User32.MOUSEEVENTF_LEFTUP;
                break;
            case MOUSE_RIGHT:
                downFlag = User32.MOUSEEVENTF_RIGHTDOWN;
                upFlag = User32.MOUSEEVENTF_RIGHTUP;
                break;
            case MOUSE_MIDDLE:
                downFlag = User32.MOUSEEVENTF_MIDDLEDOWN;
                upFlag = User32.MOUSEEVENTF_MIDDLEUP;
                break;
            default:
                return;
        }

        // 按下
        sendMouse(downFlag);
        // 释放
        sendMouse(upFlag);
    }

    private static void send(User32.INPUT[] inputs) {

        for (User32.INPUT input : inputs) {
            input.write();
        }

        int size = new User32.INPUT().size();

        int sent = User32.INSTANCE.SendInput(inputs.length, inputs, size);

        if (sent != inputs.length) {
            int err = Native.getLastError();
            throw new RuntimeException(
                    "SendInput失败: sent=" + sent + ", err=" + err
            );
        }
    }
}