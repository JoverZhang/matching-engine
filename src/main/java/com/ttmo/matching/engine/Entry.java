package com.ttmo.matching.engine;

import com.ttmo.matching.enums.OrderSide;
import com.ttmo.matching.support.Order;
import lombok.RequiredArgsConstructor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigDecimal;
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

    @Nullable
    final BigDecimal lastPrice;

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

    private void cancel(@Nonnull Order order) {
        if (order.getSide() == OrderSide.SIDE_BUY) {
            limitOrderPool.removeBuyOrder(order);
        } else {
            limitOrderPool.removeSellOrder(order);
        }
    }

    private void dealLimit(@Nonnull Order order) {
        if (order.getSide() == OrderSide.SIDE_BUY) {
            limitOrderPool.dealLimitBuy(order);
        } else {
            limitOrderPool.dealLimitSell(order);
        }
    }

    private void dealMarket(@Nonnull Order order) {
        if (order.getSide() == OrderSide.SIDE_BUY) {
            limitOrderPool.dealMarketBuy(order);
        } else {
            limitOrderPool.dealMarketSell(order);
        }
    }

}
