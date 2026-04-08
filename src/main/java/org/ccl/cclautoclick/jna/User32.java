package org.ccl.cclautoclick.jna;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.Union;
import com.sun.jna.win32.W32APIOptions;

import java.util.Arrays;
import java.util.List;

/**
 * Win32 API 接口定义
 */
public interface User32 extends com.sun.jna.Library {
    User32 INSTANCE = Native.load("user32", User32.class, W32APIOptions.UNICODE_OPTIONS);
    // 鼠标事件标志
    int MOUSEEVENTF_MOVE = 0x0001;
    int MOUSEEVENTF_LEFTDOWN = 0x0002;
    int MOUSEEVENTF_LEFTUP = 0x0004;
    int MOUSEEVENTF_RIGHTDOWN = 0x0008;
    int MOUSEEVENTF_RIGHTUP = 0x0010;
    int MOUSEEVENTF_MIDDLEDOWN = 0x0020;
    int MOUSEEVENTF_MIDDLEUP = 0x0040;
    int MOUSEEVENTF_WHEEL = 0x0800;
    // 键盘事件标志
    int KEYEVENTF_KEYDOWN = 0x0000;
    int KEYEVENTF_KEYUP = 0x0002;
    int KEYEVENTF_SCANCODE = 0x0008;  // 使用扫描码
    // 挂钩类型
    int WH_KEYBOARD_LL = 13;
    int WH_MOUSE_LL = 14;
    // 消息
    int WM_KEYDOWN = 0x0100;
    int WM_KEYUP = 0x0101;
    int WM_SYSKEYDOWN = 0x0104;
    int WM_SYSKEYUP = 0x0105;
    // 鼠标消息
    int WM_LBUTTONDOWN = 0x0201;
    int WM_LBUTTONUP = 0x0202;
    int WM_RBUTTONDOWN = 0x0204;
    int WM_RBUTTONUP = 0x0205;
    int WM_MBUTTONDOWN = 0x0207;
    int WM_MBUTTONUP = 0x0208;

    /**
     * SendInput - 合成键盘、鼠标和硬件输入事件
     */
    int SendInput(int nInputs, INPUT[] pInputs, int cbSize);

    /**
     * SetWindowsHookEx - 安装应用程序定义的挂钩程序
     */
    Pointer SetWindowsHookEx(int idHook, LowLevelKeyboardProc lpfn, Pointer hMod, int dwThreadId);

    Pointer SetWindowsHookEx(int idHook, LowLevelMouseProc lpfn, Pointer hMod, int dwThreadId);

    /**
     * UnhookWindowsHookEx - 删除挂钩程序
     */
    boolean UnhookWindowsHookEx(Pointer hhk);

    /**
     * CallNextHookEx - 将挂钩信息传递给下一个挂钩程序
     */
    Pointer CallNextHookEx(Pointer hhk, int nCode, Pointer wParam, Pointer lParam);

    /**
     * MapVirtualKey - 转换虚拟键码为字符
     */
    int MapVirtualKey(int uCode, int uMapType);

    /**
     * GetKeyNameText - 获取按键名称
     */
    int GetKeyNameText(int lParam, char[] lpString, int nSize);

    /**
     * GetAsyncKeyState - 确定在调用时键是向上还是向下，以及是否在上一次调用 GetAsyncKeyState 之后按下该键
     *
     * @param vKey 虚拟键码
     * @return 如果最高位设置为1，则键被按下；如果最低位设置为1，则键在上一次调用后被按下
     */
    short GetAsyncKeyState(int vKey);
    /**
     * 低级键盘挂钩回调接口
     */
    interface LowLevelKeyboardProc extends com.sun.jna.Callback {
        Pointer callback(int nCode, Pointer wParam, Pointer lParam);
    }
    /**
     * 低级鼠标挂钩回调接口
     */
    interface LowLevelMouseProc extends com.sun.jna.Callback {
        Pointer callback(int nCode, Pointer wParam, Pointer lParam);
    }

    /**
     * INPUT 结构体
     */
    @Structure.FieldOrder({"type", "inputUnion"})
    class INPUT extends Structure {
        public static final int INPUT_MOUSE = 0;
        public static final int INPUT_KEYBOARD = 1;

        public int type;
        public InputUnion inputUnion;

        public INPUT() {
            inputUnion = new InputUnion();
        }
    }

    /**
     * InputUnion 联合体
     */
    public static class InputUnion extends Union {
        public MOUSEINPUT mi;
        public KEYBDINPUT ki;
        public HARDWAREINPUT hi;

        public InputUnion() {
            super();
        }

        public InputUnion(Pointer peer) {
            super(peer);
        }

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("mi", "ki", "hi");
        }
    }

    /**
     * MOUSEINPUT 结构体
     */
    @Structure.FieldOrder({"dx", "dy", "mouseData", "dwFlags", "time", "dwExtraInfo"})
    class MOUSEINPUT extends Structure {
        public int dx;
        public int dy;
        public int mouseData;
        public int dwFlags;
        public int time;
        public Pointer dwExtraInfo;
    }

    /**
     * KEYBDINPUT 结构体
     */
    @Structure.FieldOrder({"wVk", "wScan", "dwFlags", "time", "dwExtraInfo"})
    class KEYBDINPUT extends Structure {
        public short wVk;
        public short wScan;
        public int dwFlags;
        public int time;
        public Pointer dwExtraInfo;
    }

    /**
     * HARDWAREINPUT 结构体
     */
    @Structure.FieldOrder({"uMsg", "wParamL", "wParamH"})
    class HARDWAREINPUT extends Structure {
        public int uMsg;
        public short wParamL;
        public short wParamH;
    }

    /**
     * KBDLLHOOKSTRUCT 结构体 - 低级键盘挂钩数据
     */
    @Structure.FieldOrder({"vkCode", "scanCode", "flags", "time", "dwExtraInfo"})
    class KBDLLHOOKSTRUCT extends Structure {
        public int vkCode;
        public int scanCode;
        public int flags;
        public int time;
        public int dwExtraInfo;

        public KBDLLHOOKSTRUCT() {
            super();
        }

        public KBDLLHOOKSTRUCT(Pointer pointer) {
            super(pointer);
            read();
        }
    }

    /**
     * MSLLHOOKSTRUCT 结构体 - 低级鼠标挂钩数据
     */
    @Structure.FieldOrder({"pt", "mouseData", "flags", "time", "dwExtraInfo"})
    class MSLLHOOKSTRUCT extends Structure {
        public POINT pt;
        public int mouseData;
        public int flags;
        public int time;
        public int dwExtraInfo;

        public MSLLHOOKSTRUCT() {
            super();
        }

        public MSLLHOOKSTRUCT(Pointer pointer) {
            super(pointer);
            read();
        }
    }

    /**
     * POINT 结构体
     */
    @Structure.FieldOrder({"x", "y"})
    class POINT extends Structure {
        public int x;
        public int y;
    }
}
