package com.ttmo.matching.engine.lua;

import com.ttmo.matching.MatchingApplicationTests;
import com.ttmo.matching.support.Marker;
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
import java.util.LinkedList;

import static com.ttmo.matching.enums.OrderSide.SIDE_BUY;

@SpringBootTest(classes = MatchingApplicationTests.class)
class OrderPoolLuaHelperTest extends Assertions {

    final String POOL_NAME = "TEST_POOL";

    @Autowired
    StringRedisTemplate redisTemplate;

    OrderPoolLuaHelper helper;

    @BeforeEach
    void before() {
        helper = new OrderPoolLuaHelper(redisTemplate);
    }

    @Test
    void addOrder() {
        assertTrue(helper.fetchOrderPool(POOL_NAME).isEmpty());

        Marker orderB1 = Marker.builder()
                .id("b1")
                .price(BigDecimal.TEN)
                .amount(BigDecimal.TEN)
                .build();
        helper.addOrder(POOL_NAME, SIDE_BUY, orderB1);

        var orderPool = helper.fetchOrderPool(POOL_NAME);
        assertEquals(1, orderPool.size());

        LinkedList<Marker> queue = orderPool.get(BigDecimal.TEN);
        assertEquals(1, queue.size());
        assertEquals(orderB1, queue.peekFirst());
    }

    @Test
    void removeOrder() {
        assertTrue(helper.fetchOrderPool(POOL_NAME).isEmpty());

        Marker orderB1 = Marker.builder()
                .id("orderB1")
                .price(BigDecimal.TEN)
                .amount(BigDecimal.TEN)
                .build();
        helper.addOrder(POOL_NAME, SIDE_BUY, orderB1);
        assertEquals(1, helper.fetchOrderPool(POOL_NAME).size());

        assertEquals(orderB1, helper.removeOrder(POOL_NAME, SIDE_BUY, orderB1));
        assertTrue(helper.fetchOrderPool(POOL_NAME).isEmpty());
    }

    @Test
    void peekFirst() {
        assertTrue(helper.fetchOrderPool(POOL_NAME).isEmpty());

        Marker orderB1 = Marker.builder()
                .id("orderB1")
                .price(BigDecimal.TEN)
                .amount(BigDecimal.TEN)
                .build();
        helper.addOrder(POOL_NAME, SIDE_BUY, orderB1);
        assertEquals(1, helper.fetchOrderPool(POOL_NAME).size());

        Marker firstOrder = helper.peekFirst(POOL_NAME, SIDE_BUY);
        assertEquals(orderB1, firstOrder);
    }

    @Test
    void update() {
        assertTrue(helper.fetchOrderPool(POOL_NAME).isEmpty());

        Marker orderB1 = Marker.builder()
                .id("orderB1")
                .price(BigDecimal.TEN)
                .amount(BigDecimal.TEN)
                .build();
        helper.addOrder(POOL_NAME, SIDE_BUY, orderB1);
        assertEquals(1, helper.fetchOrderPool(POOL_NAME).size());

        Marker updatedOrderB1 = Marker.builder()
                .id(orderB1.getId())
                .price(orderB1.getPrice())
                .amount(BigDecimal.ONE)
                .build();
        helper.update(POOL_NAME, updatedOrderB1);

        var orderPool = helper.fetchOrderPool(POOL_NAME);
        LinkedList<Marker> queue = orderPool.get(BigDecimal.TEN);
        assertEquals(updatedOrderB1, queue.getFirst());
    }

    @AfterEach
    void after() {
        redisTemplate.execute((RedisCallback<Object>) connection -> connection.execute("FLUSHALL"));
    }

}
