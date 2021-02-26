package com.ttmo.matching.engine;

import com.ttmo.matching.support.Marker;
import com.ttmo.matching.support.Order;
import com.ttmo.matching.support.Trade;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * @author Jover Zhang
 */
public interface LimitOrderPool extends OrderPool {

    void addBuyOrder(@Nonnull Marker order);

    void addSellOrder(@Nonnull Marker order);

    Marker removeBuyOrder(@Nonnull Marker order);

    Marker removeSellOrder(@Nonnull Marker order);

    /**
     * 处理限价买单
     *
     * @param order 限价买单
     */
    List<Trade> dealLimitBuy(@Nonnull Order order);

    /**
     * 处理限价卖单
     *
     * @param order 限价卖单
     */
    List<Trade> dealLimitSell(@Nonnull Order order);

    /**
     * 处理市价买单
     *
     * @param order 市价买单 (仅有 price)
     */
    List<Trade> dealMarketBuy(@Nonnull Order order);

    /**
     * 处理市价卖单
     *
     * @param order 市价卖单 (仅有 amount)
     */
    List<Trade> dealMarketSell(@Nonnull Order order);

}
