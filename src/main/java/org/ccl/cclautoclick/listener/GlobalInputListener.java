package org.ccl.cclautoclick.listener;

import org.ccl.cclautoclick.model.KeyEvent;

/**
 * 全局输入监听器接口
 */
public interface GlobalInputListener {

    /**
     * 开始监听
     */
    void start();

    /**
     * 停止监听
     */
    void stop();

    /**
     * 是否正在监听
     */
    boolean isListening();

    /**
     * 设置事件监听器
     */
    void setEventListener(EventListener listener);

    /**
     * 事件监听器接口
     */
    interface EventListener {
        void onKeyEvent(KeyEvent event);
    }
}
