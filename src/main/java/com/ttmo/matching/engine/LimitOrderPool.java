package com.ttmo.matching.engine;

import com.ttmo.matching.engine.lua.OrderPoolLuaHelper;
import com.ttmo.matching.enums.OrderSide;
import com.ttmo.matching.support.Marker;
import com.ttmo.matching.support.Order;
import com.ttmo.matching.support.Trade;
import lombok.RequiredArgsConstructor;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * @author Jover Zhang
 */
@RequiredArgsConstructor
public class LimitOrderPool implements OrderPool {

    @Nonnull
    final OrderPoolLuaHelper helper;

    @Nonnull
    final TradedMessage message;

    final int PRECISION;

    String buyPoolName = "LIMIT_BUY";

    String sellPoolName = "LIMIT_SELL";

    public void addBuyOrder(Marker order) {
        helper.addOrder(buyPoolName, OrderSide.SIDE_BUY, order);
    }

    public void addSellOrder(Marker order) {
        helper.addOrder(sellPoolName, OrderSide.SIDE_SELL, order);
    }

    public void removeBuyOrder(Marker order) {
        helper.removeOrder(buyPoolName, OrderSide.SIDE_BUY, order);
    }

    public void removeSellOrder(Marker order) {
        helper.removeOrder(sellPoolName, OrderSide.SIDE_SELL, order);
    }

    public void updateBuyOrder(Marker order) {
        helper.update(buyPoolName, order);
    }

    public void updateSellOrder(Marker order) {
        helper.update(sellPoolName, order);
    }

    public Marker peekFirstBuyOrder() {
        return helper.peekFirst(buyPoolName, OrderSide.SIDE_BUY);
    }

    public Marker peekFirstSellOrder() {
        return helper.peekFirst(sellPoolName, OrderSide.SIDE_SELL);
    }

    public void dealLimitBuy(@Nonnull Order order) {
        Marker firstOrder;
        while ((firstOrder = peekFirstSellOrder()) != null &&
                order.getPrice().compareTo(firstOrder.getPrice()) >= 0 &&
                order.getAmount().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal curAmount;
            // 当 剩余数量 < 挂单的数量 时
            // 挂单的数量 -= 剩余数量, 剩余数量 = 0
            if (order.getAmount().compareTo(firstOrder.getAmount()) < 0) {
                curAmount = order.getAmount();
                updateSellOrder(Marker.builder()
                        .id(firstOrder.getId())
                        .price(firstOrder.getPrice())
                        .amount(firstOrder.getAmount().subtract(order.getAmount()))
                        .build());
                order.setAmount(BigDecimal.ZERO);
            }
            // 当 剩余数量 >= 挂单的数量 时
            // 删除第一个挂单, 剩余数量 -= 挂单的数量
            else {
                curAmount = firstOrder.getAmount();
                removeSellOrder(firstOrder);
                order.setAmount(order.getAmount().subtract(firstOrder.getAmount()));
            }

            Trade trade = Trade.builder()
                    .markerId(firstOrder.getId())
                    .traderId(order.getId())
                    .traderSide(order.getSide())
                    .price(firstOrder.getPrice())
                    .amount(curAmount)
                    .timestamp(System.currentTimeMillis())
                    .build();
            message.send(trade, order);
        }

        if (order.getAmount().compareTo(BigDecimal.ZERO) > 0) {
            addBuyOrder(order);
        }
    }

    public void dealLimitSell(@Nonnull Order order) {
        Marker firstOrder;
        while ((firstOrder = peekFirstBuyOrder()) != null &&
                order.getPrice().compareTo(firstOrder.getPrice()) <= 0 &&
                order.getAmount().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal curAmount;
            // 当 剩余数量 < 挂单的数量 时
            // 挂单的数量 -= 剩余数量, 剩余数量 = 0
            if (order.getAmount().compareTo(firstOrder.getAmount()) < 0) {
                curAmount = order.getAmount();
                updateBuyOrder(Marker.builder()
                        .id(firstOrder.getId())
                        .price(firstOrder.getPrice())
                        .amount(firstOrder.getAmount().subtract(order.getAmount()))
                        .build());
                order.setAmount(BigDecimal.ZERO);
            }
            // 当 剩余数量 >= 挂单的数量 时
            // 删除第一个挂单, 剩余数量 -= 挂单的数量
            else {
                curAmount = firstOrder.getAmount();
                removeBuyOrder(firstOrder);
                order.setAmount(order.getAmount().subtract(firstOrder.getAmount()));
            }

            Trade trade = Trade.builder()
                    .markerId(firstOrder.getId())
                    .traderId(order.getId())
                    .traderSide(order.getSide())
                    .price(firstOrder.getPrice())
                    .amount(curAmount)
                    .timestamp(System.currentTimeMillis())
                    .build();
            message.send(trade, order);
        }

        if (order.getAmount().compareTo(BigDecimal.ZERO) > 0) {
            addSellOrder(order);
        }
    }

    /**
     * 市价买单
     * <p>仅有 price
     */
    public void dealMarketBuy(@Nonnull Order order) {
        Marker firstOrder;
        while ((firstOrder = peekFirstSellOrder()) != null &&
                order.getPrice().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal curPrice, curAmount;
            // 计算以当前挂单的 `price` 可成交的 `amount`
            BigDecimal canTradeAmount = order.getPrice().divide(firstOrder.getPrice(), PRECISION, RoundingMode.DOWN);

            // 当 可交易数量 < 挂单的数量 时
            if (canTradeAmount.compareTo(firstOrder.getAmount()) < 0) {
                curPrice = order.getPrice();
                curAmount = canTradeAmount;
                updateSellOrder(Marker.builder()
                        .id(firstOrder.getId())
                        .price(firstOrder.getPrice())
                        .amount(firstOrder.getAmount().subtract(canTradeAmount))
                        .build());
                order.setPrice(BigDecimal.ZERO);
            }
            // 当 可交易数量 >= 挂单的数量 时
            else {
                curPrice = firstOrder.getPrice();
                curAmount = firstOrder.getAmount();
                removeSellOrder(firstOrder);
                order.setPrice(canTradeAmount
                        .subtract(firstOrder.getAmount())
                        .multiply(firstOrder.getPrice()));
            }

            Trade trade = Trade.builder()
                    .markerId(firstOrder.getId())
                    .traderId(order.getId())
                    .traderSide(order.getSide())
                    .price(curPrice)
                    .amount(curAmount)
                    .timestamp(System.currentTimeMillis())
                    .build();
            message.send(trade, order);
        }
        message.cancel(order);
    }

    /**
     * 市价卖单
     * <p>仅有 amount
     */
    public void dealMarketSell(@Nonnull Order order) {
        Marker firstOrder;
        while ((firstOrder = peekFirstBuyOrder()) != null &&
                order.getAmount().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal curAmount;
            // 当 剩余数量 < 挂单的数量 时
            // 挂单的数量 -= 剩余数量, 剩余数量 = 0
            if (order.getAmount().compareTo(firstOrder.getAmount()) < 0) {
                curAmount = order.getAmount();
                updateBuyOrder(Marker.builder()
                        .id(firstOrder.getId())
                        .price(firstOrder.getPrice())
                        .amount(firstOrder.getAmount().subtract(order.getAmount()))
                        .build());
                order.setAmount(BigDecimal.ZERO);
            }
            // 当 剩余数量 >= 挂单的数量 时
            // 删除第一个挂单, 剩余数量 -= 挂单的数量
            else {
                curAmount = firstOrder.getAmount();
                removeBuyOrder(firstOrder);
                order.setAmount(order.getAmount().subtract(firstOrder.getAmount()));
            }

            Trade trade = Trade.builder()
                    .markerId(firstOrder.getId())
                    .traderId(order.getId())
                    .traderSide(order.getSide())
                    .price(firstOrder.getPrice())
                    .amount(curAmount)
                    .timestamp(System.currentTimeMillis())
                    .build();
            message.send(trade, order);
        }
        message.cancel(order);
    }

}
