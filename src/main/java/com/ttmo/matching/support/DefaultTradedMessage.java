package com.ttmo.matching.support;

/**
 * 交易结束消息
 *
 * @author Jover Zhang
 */
public class DefaultTradedMessage implements Message {

    @Override
    public void completedTraded(Trade trade, Order order) {
        System.out.println("trade = " + trade + ", order = " + order);
    }

    @Override
    public void completedCancelOrder(Marker order) {
        System.out.println("cancel order = " + order);
    }

}
