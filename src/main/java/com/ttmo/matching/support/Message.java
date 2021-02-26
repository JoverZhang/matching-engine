package com.ttmo.matching.support;

/**
 * @author Jover Zhang
 */
public interface Message {

    /**
     * 成功撮合通知
     *
     * @param trade 交易信息
     * @param order 吃单者
     */
    void completedTraded(Trade trade, Order order);

    /**
     * 成功取消挂单
     *
     * @param order 挂单
     */
    void completedCancelOrder(Marker order);

}
