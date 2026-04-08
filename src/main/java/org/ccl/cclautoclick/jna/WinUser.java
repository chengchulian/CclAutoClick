package org.ccl.cclautoclick.jna;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.Union;

import java.util.Arrays;
import java.util.List;

public interface WinUser extends com.sun.jna.Library {

    WinUser INSTANCE = Native.load("user32", WinUser.class);

    int INPUT_MOUSE = 0;
    int INPUT_KEYBOARD = 1;

    int MOUSEEVENTF_MOVE = 0x0001;
    int MOUSEEVENTF_LEFTDOWN = 0x0002;
    int MOUSEEVENTF_LEFTUP = 0x0004;
    int MOUSEEVENTF_RIGHTDOWN = 0x0008;
    int MOUSEEVENTF_RIGHTUP = 0x0010;
    int MOUSEEVENTF_MIDDLEDOWN = 0x0020;
    int MOUSEEVENTF_MIDDLEUP = 0x0040;
    int MOUSEEVENTF_WHEEL = 0x0800;

    int KEYEVENTF_KEYUP = 0x0002;
    int KEYEVENTF_SCANCODE = 0x0008;

    class INPUT extends Structure {
        public int type;
        public InputUnion inputUnion;

        public INPUT() {
            inputUnion = new InputUnion();
        }

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("type", "inputUnion");
        }
    }

    int SendInput(int nInputs, INPUT[] pInputs, int cbSize);

    class InputUnion extends Union {
        public MOUSEINPUT mi;
        public KEYBDINPUT ki;
    }

    class MOUSEINPUT extends Structure {
        public int dx;
        public int dy;
        public int mouseData;
        public int dwFlags;
        public int time;
        public Pointer dwExtraInfo;

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("dx", "dy", "mouseData", "dwFlags", "time", "dwExtraInfo");
        }
    }

    class KEYBDINPUT extends Structure {
        public short wVk;
        public short wScan;
        public int dwFlags;
        public int time;
        public Pointer dwExtraInfo;

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("wVk", "wScan", "dwFlags", "time", "dwExtraInfo");
        }
    }
}
