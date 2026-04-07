package org.ccl.cclautoclick.scheduler;

import org.ccl.cclautoclick.model.KeyConfig;
import org.ccl.cclautoclick.sender.InputSender;

/**
 * 点击任务 - 执行具体的连点操作
 */
public class ClickTask implements Runnable {

    private final KeyConfig config;

    public ClickTask(KeyConfig config) {
        this.config = config;
    }

    @Override
    public void run() {
        try {
            // 执行点击操作
            InputSender.execute(config.getKey());
        } catch (Exception e) {
            System.err.println("点击任务异常: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
