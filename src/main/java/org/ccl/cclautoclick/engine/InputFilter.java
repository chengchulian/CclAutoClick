package org.ccl.cclautoclick.engine;

import org.ccl.cclautoclick.jna.WinUser;

/**
 * 输入过滤器 - 防回环核心组件
 * 过滤掉由本程序生成的模拟输入事件，防止无限循环
 */
public class InputFilter {

    /**
     * 判断是否应该忽略该键盘事件
     *
     * @param info 键盘钩子结构体
     * @return true-应该忽略（是模拟输入），false-应该处理（是物理输入）
     */
    public static boolean shouldIgnore(WinUser.KBDLLHOOKSTRUCT info) {
        // 1. 系统注入标记（最重要）
        // LLKHF_INJECTED = 0x10 - 表示该事件是由 SendInput 等 API 注入的
        if ((info.flags & 0x10) != 0) {
            return true;
        }

        // 2. 自定义标记（精准识别本程序的输出）
        if (info.dwExtraInfo == (int) InputMark.MAGIC) {
            return true;
        }

        return false;
    }

    /**
     * 判断是否应该忽略该鼠标事件
     *
     * @param info 鼠标钩子结构体
     * @return true-应该忽略（是模拟输入），false-应该处理（是物理输入）
     */
    public static boolean shouldIgnore(WinUser.MSLLHOOKSTRUCT info) {
        // 1. 系统注入标记
        // LLMHF_INJECTED = 0x01 - 表示该事件是由 SendInput 等 API 注入的
        if ((info.flags & 0x01) != 0) {
            return true;
        }

        // 2. 自定义标记（精准识别本程序的输出）
        if (info.dwExtraInfo == (int) InputMark.MAGIC) {
            return true;
        }

        return false;
    }
}
