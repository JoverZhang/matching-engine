package com.ttmo.matching.engine;

import com.ttmo.matching.support.Order;
import com.ttmo.matching.support.Trade;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 交易结束消息
 *
 * @author Jover Zhang
 */
public class TradedMessage {

    AtomicInteger i = new AtomicInteger(0);

    long start;

    public void send(Trade trade, Order order) {
        System.out.println("trade = " + trade + ", order = " + order);
//        int c = this.i.incrementAndGet();
//        if (c == 1) {
//            start = System.currentTimeMillis();
//        }
//        if (c == 2000) {
//            System.out.println(System.currentTimeMillis() - start);
//        }
    }

    public void cancel(Order order) {
        System.out.println("cancel order = " + order);
    }

}
