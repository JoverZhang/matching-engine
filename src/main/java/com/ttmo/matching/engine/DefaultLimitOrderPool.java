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
import java.util.LinkedList;
import java.util.List;

/**
 * @author Jover Zhang
 */
@RequiredArgsConstructor
public class DefaultLimitOrderPool implements LimitOrderPool {

    final static String LIMIT_BUY_NAME = "LIMIT_BUY";

    final static String LIMIT_SELL_NAME = "LIMIT_SELL";

    @Nonnull
    final OrderPoolLuaHelper helper;

    final int PRECISION;

    @Override
    public void addBuyOrder(@Nonnull Marker order) {
        helper.addOrder(LIMIT_BUY_NAME, OrderSide.SIDE_BUY, order);
    }

    @Override
    public void addSellOrder(@Nonnull Marker order) {
        helper.addOrder(LIMIT_SELL_NAME, OrderSide.SIDE_SELL, order);
    }

    @Override
    public Marker removeBuyOrder(@Nonnull Marker order) {
        return helper.removeOrder(LIMIT_BUY_NAME, OrderSide.SIDE_BUY, order);
    }

    @Override
    public Marker removeSellOrder(@Nonnull Marker order) {
        return helper.removeOrder(LIMIT_SELL_NAME, OrderSide.SIDE_SELL, order);
    }

    protected void updateBuyOrder(@Nonnull Marker order) {
        helper.update(LIMIT_BUY_NAME, order);
    }

    protected void updateSellOrder(@Nonnull Marker order) {
        helper.update(LIMIT_SELL_NAME, order);
    }

    protected Marker peekFirstBuyOrder() {
        return helper.peekFirst(LIMIT_BUY_NAME, OrderSide.SIDE_BUY);
    }

    protected Marker peekFirstSellOrder() {
        return helper.peekFirst(LIMIT_SELL_NAME, OrderSide.SIDE_SELL);
    }

    @Override
    public List<Trade> dealLimitBuy(@Nonnull Order order) {
        List<Trade> trades = new LinkedList<>();
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
            trades.add(trade);
        }

        if (order.getAmount().compareTo(BigDecimal.ZERO) > 0) {
            addBuyOrder(order);
        }
        return trades;
    }

    @Override
    public List<Trade> dealLimitSell(@Nonnull Order order) {
        List<Trade> trades = new LinkedList<>();
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
            trades.add(trade);
        }

        if (order.getAmount().compareTo(BigDecimal.ZERO) > 0) {
            addSellOrder(order);
        }
        return trades;
    }

    @Override
    public List<Trade> dealMarketBuy(@Nonnull Order order) {
        List<Trade> trades = new LinkedList<>();
        Marker firstOrder;
        while ((firstOrder = peekFirstSellOrder()) != null &&
                order.getPrice().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal curAmount;
            // 计算以当前挂单的 `price` 可成交的 `amount`
            BigDecimal canTradeAmount = order.getPrice().divide(firstOrder.getPrice(), PRECISION, RoundingMode.DOWN);

            // 当 可交易数量 < 挂单的数量 时
            if (canTradeAmount.compareTo(firstOrder.getAmount()) < 0) {
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
                    .price(firstOrder.getPrice())
                    .amount(curAmount)
                    .timestamp(System.currentTimeMillis())
                    .build();
            trades.add(trade);
        }
        return trades;
    }

    @Override
    public List<Trade> dealMarketSell(@Nonnull Order order) {
        List<Trade> trades = new LinkedList<>();
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
            trades.add(trade);
        }
        return trades;
    }

}
