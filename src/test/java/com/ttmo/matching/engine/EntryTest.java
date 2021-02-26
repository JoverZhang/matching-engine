package com.ttmo.matching.engine;

import com.ttmo.matching.MatchingApplicationTests;
import com.ttmo.matching.common.Randomizer;
import com.ttmo.matching.engine.lua.OrderPoolLuaHelper;
import com.ttmo.matching.enums.OrderAction;
import com.ttmo.matching.enums.OrderSide;
import com.ttmo.matching.enums.OrderType;
import com.ttmo.matching.support.Marker;
import com.ttmo.matching.support.Message;
import com.ttmo.matching.support.Order;
import com.ttmo.matching.support.Trade;
import lombok.Builder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import static com.ttmo.matching.enums.OrderSide.SIDE_BUY;
import static com.ttmo.matching.enums.OrderSide.SIDE_SELL;
import static com.ttmo.matching.enums.OrderType.LIMIT;
import static com.ttmo.matching.enums.OrderType.MARKET;

@SpringBootTest(classes = MatchingApplicationTests.class)
class EntryTest extends Assertions {

    @Autowired
    StringRedisTemplate redisTemplate;

    final int PRECISION = 6;

    @Test
    void stressTesting() {
        final int END_COUNT = 5000;
        int[] counts = {0, 0};
        final long[] preTimes = {0};

        LinkedBlockingQueue<Order> queue = new LinkedBlockingQueue<>();
        Message message = new Message() {
            @Override
            public void successTraded(Trade trade, Order order) {
                if (counts[0]++ % 1000 == 0) {
                    System.out.println("traded 1000, used: " +
                            (~(preTimes[0] - (preTimes[0] = System.currentTimeMillis())) + 1));
                }
            }

            @Override
            public void successCancelOrder(Marker order) {
                if (counts[1]++ % 1000 == 0) {
                    System.out.println("canceled 1000");
                }
            }
        };

        new Thread(() ->
                new Entry(new DefaultLimitOrderPool(
                        new OrderPoolLuaHelper(redisTemplate), PRECISION),
                        queue, message, null)
                        .start())
                .start();

        List<Order> orders = OrderRandomizer.builder()
                .numberOfPrice(100)
                .numberOfSamePrice(100)
                .priceRandomizer(new Randomizer("0.0001", "100000", PRECISION))
                .amountRandomizer(new Randomizer("0.0001", "100000", PRECISION))
                .build().get(LIMIT, SIDE_BUY);
        orders.addAll(OrderRandomizer.builder()
                .numberOfPrice(100)
                .numberOfSamePrice(100)
                .priceRandomizer(new Randomizer("0.0001", "100000", PRECISION))
                .amountRandomizer(new Randomizer("0.0001", "100000", PRECISION))
                .build().get(LIMIT, SIDE_SELL));
        orders.addAll(OrderRandomizer.builder()
                .numberOfPrice(100)
                .numberOfSamePrice(100)
                .priceRandomizer(new Randomizer("0.0001", "100000", PRECISION))
                .build().get(MARKET, SIDE_BUY));
        orders.addAll(OrderRandomizer.builder()
                .numberOfPrice(100)
                .numberOfSamePrice(100)
                .amountRandomizer(new Randomizer("0.0001", "100000", PRECISION))
                .build().get(MARKET, SIDE_SELL));

        new HashSet<>(orders).forEach(queue::offer);
        while (counts[0] < END_COUNT) {
            Thread.yield();
        }
    }

    @AfterEach
    void after() {
        redisTemplate.execute((RedisCallback<Object>) connection -> connection.execute("FLUSHALL"));
    }

    @Builder
    static class OrderRandomizer {

        int numberOfPrice;

        /**
         * Default of {@link #numberOfSamePriceRandomizer}.getInt()
         */
        int numberOfSamePrice;

        Randomizer numberOfSamePriceRandomizer;

        Randomizer priceRandomizer;

        Randomizer amountRandomizer;

        public List<Order> get(OrderType type, OrderSide side) {
            List<Order> orders = get();
            orders.forEach(order -> {
                order.setType(type);
                order.setSide(side);
                order.setAction(OrderAction.CREATE);
            });
            return orders;
        }

        public List<Order> get() {
            assertTrue(numberOfPrice > 0);
            return new LinkedList<Order>() {{
                for (int i = 0; i < numberOfPrice; i++) {
                    BigDecimal price = priceRandomizer == null ? null : priceRandomizer.getBigDecimal();
                    int count = numberOfSamePriceRandomizer != null ?
                            numberOfSamePriceRandomizer.getInt() :
                            numberOfSamePrice;
                    for (int j = 0; j < count; j++) {
                        add(Order.builder()
                                .id(i + ":" + j)
                                .price(price)
                                .amount(amountRandomizer == null ? null : amountRandomizer.getBigDecimal())
                                .build());
                    }
                }
            }};
        }

    }

}
