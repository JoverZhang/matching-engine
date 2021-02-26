package com.ttmo.matching.engine;

import com.ttmo.matching.enums.OrderSide;
import com.ttmo.matching.support.Marker;
import com.ttmo.matching.support.Message;
import com.ttmo.matching.support.Order;
import com.ttmo.matching.support.Trade;
import lombok.RequiredArgsConstructor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.BlockingQueue;

/**
 * 撮合入口
 *
 * @author Jover Zhang
 */
@RequiredArgsConstructor
public class Entry {

    @Nonnull
    final LimitOrderPool limitOrderPool;

    @Nonnull
    final BlockingQueue<Order> orderQueue;

    @Nonnull
    final Message message;

    @Nullable
    final BigDecimal lastPrice;

    /**
     * 开启撮合入口, 分发挂单事件
     * <p>使用 {@link #orderQueue} 接收挂单推送
     */
    public void start() {
        for (; ; ) {
            Order order = orderQueue.poll();
            if (order != null) {
                switch (order.getAction()) {
                    case CREATE:
                        create(order);
                        break;
                    case CANCEL:
                        cancel(order);
                        break;
                    default:
                        throw new RuntimeException("Unsupported order action: " + order.getAction());
                }
            }
        }
    }

    /**
     * 创建挂单
     */
    private void create(@Nonnull Order order) {
        switch (order.getType()) {
            case LIMIT:
                dealLimit(order);
                break;
            case MARKET:
                dealMarket(order);
                break;
            default:
                throw new RuntimeException("Unsupported order type: " + order.getType());
        }
    }

    /**
     * 取消挂单
     */
    private void cancel(@Nonnull Order order) {
        Marker marker;
        if (order.getSide() == OrderSide.SIDE_BUY) {
            marker = limitOrderPool.removeBuyOrder(order);
        } else {
            marker = limitOrderPool.removeSellOrder(order);
        }
        if (marker != null) {
            message.completedCancelOrder(marker);
        }
    }

    /**
     * 处理限价单
     */
    private void dealLimit(@Nonnull Order order) {
        List<Trade> trades;

        if (order.getSide() == OrderSide.SIDE_BUY) {
            trades = limitOrderPool.dealLimitBuy(order);
        } else {
            trades = limitOrderPool.dealLimitSell(order);
        }

        for (Trade trade : trades) {
            message.completedTraded(trade, order);
        }
    }

    /**
     * 处理市价单
     */
    private void dealMarket(@Nonnull Order order) {
        List<Trade> trades;

        if (order.getSide() == OrderSide.SIDE_BUY) {
            trades = limitOrderPool.dealMarketBuy(order);
            // 小概率事件
            if (order.getPrice().compareTo(BigDecimal.ZERO) > 0) {
                message.completedCancelOrder(order);
            }
        } else {
            trades = limitOrderPool.dealMarketSell(order);
            // 小概率事件
            if (order.getAmount().compareTo(BigDecimal.ZERO) > 0) {
                message.completedCancelOrder(order);
            }
        }

        for (Trade trade : trades) {
            message.completedTraded(trade, order);
        }
    }

}
