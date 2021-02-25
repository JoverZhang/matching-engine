package com.ttmo.matching.engine;

import com.ttmo.matching.MatchingApplicationTests;
import com.ttmo.matching.common.Randomizer;
import com.ttmo.matching.engine.lua.OrderPoolLuaHelper;
import com.ttmo.matching.enums.OrderAction;
import com.ttmo.matching.enums.OrderSide;
import com.ttmo.matching.enums.OrderType;
import com.ttmo.matching.support.Order;
import lombok.Builder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

@SpringBootTest(classes = MatchingApplicationTests.class)
class EntryTest extends Assertions {

    @Autowired
    StringRedisTemplate redisTemplate;

    @Test
    void start() throws InterruptedException {
        OrderPoolLuaHelper helper = new OrderPoolLuaHelper(redisTemplate);

        LinkedBlockingQueue<Order> queue = new LinkedBlockingQueue<>();

        Entry entry = new Entry(new LimitOrderPool(helper, new TradedMessage(), 6), queue, null);
        Thread thread = new Thread(() -> entry.start());
        thread.start();

//        List<Order> orders = OrderRandomizer.builder()
//                .numberOfPrice(100)
//                .numberOfSamePrice(100)
//                .priceRandomizer(new Randomizer("0.0001", "100000", 4))
//                .amountRandomizer(new Randomizer("0.0001", "100000", 4))
//                .build().get(OrderType.LIMIT, OrderSide.SIDE_SELL);
//        orders.addAll(OrderRandomizer.builder()
//                .numberOfPrice(100)
//                .numberOfSamePrice(100)
//                .priceRandomizer(new Randomizer("0.0001", "100000", 4))
//                .amountRandomizer(new Randomizer("0.0001", "100000", 4))
//                .build().get(OrderType.LIMIT, OrderSide.SIDE_BUY));
//
//        System.out.println("orders.size() = " + orders.size());
//        new HashSet<>(orders).forEach(queue::offer);

//        queue.offer(Order.builder()
//                .id("1")
//                .price(new BigDecimal("999"))
//                .amount(new BigDecimal("100"))
//                .type(OrderType.LIMIT)
//                .side(OrderSide.SIDE_BUY)
//                .action(OrderAction.CREATE)
//                .build());
//        queue.offer(Order.builder()
//                .id("2")
//                .price(new BigDecimal("1"))
//                .amount(new BigDecimal("200"))
//                .type(OrderType.LIMIT)
//                .side(OrderSide.SIDE_BUY)
//                .action(OrderAction.CREATE)
//                .build());
        queue.offer(Order.builder()
                .id("3")
//                .price(new BigDecimal("30"))
                .amount(new BigDecimal("301"))
                .type(OrderType.MARKET)
                .side(OrderSide.SIDE_SELL)
                .action(OrderAction.CREATE)
                .build());


        System.out.println("end");
        thread.join();
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
                    BigDecimal price = priceRandomizer.getBigDecimal();
                    int count = numberOfSamePriceRandomizer != null ?
                            numberOfSamePriceRandomizer.getInt() :
                            numberOfSamePrice;
                    for (int j = 0; j < count; j++) {
                        add(Order.builder()
                                .id(i + ":" + j)
                                .price(price)
                                .amount(amountRandomizer.getBigDecimal())
                                .build());
                    }
                }
            }};
        }

    }

}
