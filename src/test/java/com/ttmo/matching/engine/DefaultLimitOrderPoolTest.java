package com.ttmo.matching.engine;

import com.ttmo.matching.MatchingApplicationTests;
import com.ttmo.matching.engine.lua.OrderPoolLuaHelper;
import com.ttmo.matching.enums.OrderSide;
import com.ttmo.matching.enums.OrderType;
import com.ttmo.matching.support.Marker;
import com.ttmo.matching.support.Order;
import com.ttmo.matching.support.Trade;
import lombok.var;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static com.ttmo.matching.engine.DefaultLimitOrderPool.LIMIT_BUY_NAME;
import static com.ttmo.matching.engine.DefaultLimitOrderPool.LIMIT_SELL_NAME;
import static com.ttmo.matching.enums.OrderSide.SIDE_BUY;
import static com.ttmo.matching.enums.OrderSide.SIDE_SELL;
import static com.ttmo.matching.enums.OrderType.LIMIT;
import static com.ttmo.matching.enums.OrderType.MARKET;

@SpringBootTest(classes = MatchingApplicationTests.class)
class DefaultLimitOrderPoolTest extends Assertions {

    final static int PRECISION = 6;

    @Autowired
    StringRedisTemplate redisTemplate;

    DefaultLimitOrderPool limitOrderPool;

    OrderPoolLuaHelper helper;

    private static Marker newMarker(String id, String price, String amount) {
        return Marker.builder()
                .id(id)
                .price(new BigDecimal(price))
                .amount(new BigDecimal(amount))
                .build();
    }

    private static Order newOrder(String id, String price, String amount, OrderType type, OrderSide side) {
        return Order.builder()
                .id(id)
                .price(price == null ? null : new BigDecimal(price))
                .amount(amount == null ? null : new BigDecimal(amount))
                .type(type)
                .side(side)
                .build();
    }

    private static Trade newTrade(String markerId, String traderId, OrderSide traderSide, String price, String amount) {
        return Trade.builder()
                .markerId(markerId)
                .traderId(traderId)
                .traderSide(traderSide)
                .price(new BigDecimal(price))
                .amount(new BigDecimal(amount))
                .build();
    }

    private static void assertTradesEquals(List<Trade> expect, List<Trade> actual) {
        assertEquals(expect.size(), actual.size());
        for (int i = 0; i < expect.size(); i++) {
            Trade a = expect.get(i), b = actual.get(i);
            String msg = "\nexpect: " + a.toString() + ",\nactual: " + b.toString();
            assertEquals(b.getMarkerId(), a.getMarkerId(), msg);
            assertEquals(a.getTraderId(), b.getTraderId(), msg);
            assertEquals(a.getTraderSide(), b.getTraderSide(), msg);
            assertEquals(a.getPrice().compareTo(b.getPrice()), 0, msg);
            assertEquals(a.getAmount().compareTo(b.getAmount()), 0, msg);
        }
    }

    @BeforeEach
    void before() {
        helper = new OrderPoolLuaHelper(redisTemplate);
        limitOrderPool = new DefaultLimitOrderPool(helper, PRECISION);
    }

    @Test
    void dealLimitBuy() {
        {
            List<Marker> limitSellMarkers = new ArrayList<Marker>() {{
                add(newMarker("s1", "1", "10"));
                add(newMarker("s2", "2", "10"));
            }};
            limitSellMarkers.forEach(limitOrderPool::addSellOrder);

            Order order = newOrder("b1", "2", "30", LIMIT, SIDE_BUY);
            List<Trade> trades = limitOrderPool.dealLimitBuy(order);
            assertTradesEquals(Arrays.asList(
                    newTrade("s1", "b1", SIDE_BUY, "1", "10"),
                    newTrade("s2", "b1", SIDE_BUY, "2", "10")
            ), trades);
            assertEquals(newOrder("b1", "2", "10", LIMIT, SIDE_BUY), order);

            assertTrue(helper.fetchOrderPool(LIMIT_SELL_NAME).isEmpty());
        }
        {
            List<Marker> limitSellMarkers = new ArrayList<Marker>() {{
                add(newMarker("s1", "1", "10"));
                add(newMarker("s2", "2", "10"));
            }};
            limitSellMarkers.forEach(limitOrderPool::addSellOrder);

            Order order = newOrder("b1", "2", "15", LIMIT, SIDE_BUY);
            List<Trade> trades = limitOrderPool.dealLimitBuy(order);
            assertTradesEquals(Arrays.asList(
                    newTrade("s1", "b1", SIDE_BUY, "1", "10"),
                    newTrade("s2", "b1", SIDE_BUY, "2", "5")
            ), trades);
            assertEquals(newOrder("b1", "2", "0", LIMIT, SIDE_BUY), order);

            var actualOrderPool = helper.fetchOrderPool(LIMIT_SELL_NAME);
            LinkedList<Marker> queue = actualOrderPool.get(new BigDecimal("2"));
            assertEquals(1, queue.size());
            assertEquals(newMarker("s2", "2", "5"), queue.peekFirst());
        }
    }

    @Test
    void dealLimitSell() {
        {
            List<Marker> limitBuyMarkers = new ArrayList<Marker>() {{
                add(newMarker("b1", "1", "10"));
                add(newMarker("b2", "2", "10"));
            }};
            limitBuyMarkers.forEach(limitOrderPool::addBuyOrder);

            Order order = newOrder("s1", "1", "30", LIMIT, SIDE_SELL);
            List<Trade> trades = limitOrderPool.dealLimitSell(order);
            assertTradesEquals(Arrays.asList(
                    newTrade("b2", "s1", SIDE_SELL, "2", "10"),
                    newTrade("b1", "s1", SIDE_SELL, "1", "10")
            ), trades);
            assertEquals(newOrder("s1", "1", "10", LIMIT, SIDE_SELL), order);

            assertTrue(helper.fetchOrderPool(LIMIT_BUY_NAME).isEmpty());
        }
        {
            List<Marker> limitBuyMarkers = new ArrayList<Marker>() {{
                add(newMarker("b1", "1", "10"));
                add(newMarker("b2", "2", "10"));
            }};
            limitBuyMarkers.forEach(limitOrderPool::addBuyOrder);

            Order order = newOrder("s1", "1", "15", LIMIT, SIDE_SELL);
            List<Trade> trades = limitOrderPool.dealLimitSell(order);
            assertTradesEquals(Arrays.asList(
                    newTrade("b2", "s1", SIDE_SELL, "2", "10"),
                    newTrade("b1", "s1", SIDE_SELL, "1", "5")
            ), trades);
            assertEquals(newOrder("s1", "1", "0", LIMIT, SIDE_SELL), order);

            var actualOrderPool = helper.fetchOrderPool(LIMIT_BUY_NAME);
            LinkedList<Marker> queue = actualOrderPool.get(new BigDecimal("1"));
            assertEquals(1, queue.size());
            assertEquals(newMarker("b1", "1", "5"), queue.peekFirst());
        }
    }

    @Test
    void dealMarketBuy() {
        {
            List<Marker> limitSellMarkers = new ArrayList<Marker>() {{
                add(newMarker("ls1", "2", "10"));
                add(newMarker("ls2", "3", "10"));
            }};
            limitSellMarkers.forEach(limitOrderPool::addSellOrder);

            Order order = newOrder("mb1", "60", null, MARKET, SIDE_BUY);
            List<Trade> trades = limitOrderPool.dealMarketBuy(order);
            assertTradesEquals(Arrays.asList(
                    newTrade("ls1", "mb1", SIDE_BUY, "2", "10"),
                    newTrade("ls2", "mb1", SIDE_BUY, "3", "10")
            ), trades);
            assertEquals(newOrder("mb1", "9.999999", null, MARKET, SIDE_BUY), order);

            assertTrue(helper.fetchOrderPool(LIMIT_SELL_NAME).isEmpty());
        }
        {
            List<Marker> limitSellMarkers = new ArrayList<Marker>() {{
                add(newMarker("ls1", "2", "10"));
                add(newMarker("ls2", "3", "10"));
            }};
            limitSellMarkers.forEach(limitOrderPool::addSellOrder);

            Order order = newOrder("mb1", "30", null, MARKET, SIDE_BUY);
            List<Trade> trades = limitOrderPool.dealMarketBuy(order);
            assertTradesEquals(Arrays.asList(
                    newTrade("ls1", "mb1", SIDE_BUY, "2", "10"),
                    newTrade("ls2", "mb1", SIDE_BUY, "3", "3.333333")
            ), trades);
            assertEquals(newOrder("mb1", "0", null, MARKET, SIDE_BUY), order);

            var actualOrderPool = helper.fetchOrderPool(LIMIT_SELL_NAME);
            LinkedList<Marker> queue = actualOrderPool.get(new BigDecimal("3"));
            assertEquals(1, queue.size());
            assertEquals(newMarker("ls2", "3", "6.666667"), queue.peekFirst());
        }
    }

    @Test
    void dealMarketSell() {
        {
            ArrayList<Marker> limitBuyMarkers = new ArrayList<Marker>() {{
                add(newMarker("lb1", "2", "10"));
                add(newMarker("lb2", "3", "10"));
            }};
            limitBuyMarkers.forEach(limitOrderPool::addBuyOrder);

            Order order = newOrder("ms1", null, "30", MARKET, SIDE_SELL);
            List<Trade> trades = limitOrderPool.dealMarketSell(order);
            assertTradesEquals(Arrays.asList(
                    newTrade("lb2", "ms1", SIDE_SELL, "3", "10"),
                    newTrade("lb1", "ms1", SIDE_SELL, "2", "10")
            ), trades);
            assertEquals(newOrder("ms1", null, "10", MARKET, SIDE_SELL), order);

            assertTrue(helper.fetchOrderPool(LIMIT_BUY_NAME).isEmpty());
        }
        {
            ArrayList<Marker> limitBuyMarkers = new ArrayList<Marker>() {{
                add(newMarker("lb1", "2", "10"));
                add(newMarker("lb2", "3", "10"));
            }};
            limitBuyMarkers.forEach(limitOrderPool::addBuyOrder);

            Order order = newOrder("ms1", null, "15", MARKET, SIDE_SELL);
            List<Trade> trades = limitOrderPool.dealMarketSell(order);
            assertTradesEquals(Arrays.asList(
                    newTrade("lb2", "ms1", SIDE_SELL, "3", "10"),
                    newTrade("lb1", "ms1", SIDE_SELL, "2", "5")
            ), trades);
            assertEquals(newOrder("ms1", null, "0", MARKET, SIDE_SELL), order);

            var actualOrderPool = helper.fetchOrderPool(LIMIT_BUY_NAME);
            LinkedList<Marker> queue = actualOrderPool.get(new BigDecimal("2"));
            assertEquals(1, queue.size());
            assertEquals(newMarker("lb1", "2", "5"), queue.peekFirst());
        }
    }

    @AfterEach
    void after() {
        redisTemplate.execute((RedisCallback<Object>) connection -> connection.execute("FLUSHALL"));
    }

}
