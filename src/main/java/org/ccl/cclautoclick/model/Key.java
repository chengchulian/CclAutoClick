package org.ccl.cclautoclick.model;

/**
 * 按键枚举 - 统一表示键盘和鼠标按键
 */
public enum Key {
    // 鼠标按键（负数表示）
    MOUSE_LEFT(-1, "鼠标左键"),
    MOUSE_RIGHT(-2, "鼠标右键"),
    MOUSE_MIDDLE(-3, "鼠标中键"),

    // 键盘按键（虚拟键码）
    KEY_A(0x41, "A"),
    KEY_B(0x42, "B"),
    KEY_C(0x43, "C"),
    KEY_D(0x44, "D"),
    KEY_E(0x45, "E"),
    KEY_F(0x46, "F"),
    KEY_G(0x47, "G"),
    KEY_H(0x48, "H"),
    KEY_I(0x49, "I"),
    KEY_J(0x4A, "J"),
    KEY_K(0x4B, "K"),
    KEY_L(0x4C, "L"),
    KEY_M(0x4D, "M"),
    KEY_N(0x4E, "N"),
    KEY_O(0x4F, "O"),
    KEY_P(0x50, "P"),
    KEY_Q(0x51, "Q"),
    KEY_R(0x52, "R"),
    KEY_S(0x53, "S"),
    KEY_T(0x54, "T"),
    KEY_U(0x55, "U"),
    KEY_V(0x56, "V"),
    KEY_W(0x57, "W"),
    KEY_X(0x58, "X"),
    KEY_Y(0x59, "Y"),
    KEY_Z(0x5A, "Z"),

    KEY_0(0x30, "0"),
    KEY_1(0x31, "1"),
    KEY_2(0x32, "2"),
    KEY_3(0x33, "3"),
    KEY_4(0x34, "4"),
    KEY_5(0x35, "5"),
    KEY_6(0x36, "6"),
    KEY_7(0x37, "7"),
    KEY_8(0x38, "8"),
    KEY_9(0x39, "9"),

    KEY_F1(0x70, "F1"),
    KEY_F2(0x71, "F2"),
    KEY_F3(0x72, "F3"),
    KEY_F4(0x73, "F4"),
    KEY_F5(0x74, "F5"),
    KEY_F6(0x75, "F6"),
    KEY_F7(0x76, "F7"),
    KEY_F8(0x77, "F8"),
    KEY_F9(0x78, "F9"),
    KEY_F10(0x79, "F10"),
    KEY_F11(0x7A, "F11"),
    KEY_F12(0x7B, "F12"),

    KEY_ENTER(0x0D, "Enter"),
    KEY_SPACE(0x20, "Space"),
    KEY_TAB(0x09, "Tab"),
    KEY_ESCAPE(0x1B, "Esc"),
    KEY_BACKSPACE(0x08, "Backspace"),

    KEY_SHIFT(0x10, "Shift"),
    KEY_CTRL(0x11, "Ctrl"),
    KEY_ALT(0x12, "Alt");

    private final int code;
    private final String name;

    Key(int code, String name) {
        this.code = code;
        this.name = name;
    }

    /**
     * 根据虚拟键码查找 Key
     */
    public static Key fromCode(int code) {
        for (Key key : values()) {
            if (key.code == code) {
                return key;
            }
        }
        return null;
    }

    public int getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    /**
     * 判断是否为鼠标按键
     */
    public boolean isMouse() {
        return code < 0;
    }

    @Override
    public String toString() {
        return name;
    }
}
