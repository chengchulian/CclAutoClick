package org.ccl.cclautoclick.engine;

/**
 * 输入标记 - 用于区分物理输入和模拟输入，防止回环
 */
public class InputMark {

    /**
     * 魔法值标识符 - 用于标记由本程序生成的输入事件
     * 使用 long 类型以兼容 Windows dwExtraInfo (ULONG_PTR)
     * 十六进制: 0xCCL2026, 十进制: 3435973830
     */
    public static final long MAGIC = 3435973830L;
}
