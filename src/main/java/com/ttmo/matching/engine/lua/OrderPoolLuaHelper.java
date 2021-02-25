package com.ttmo.matching.engine.lua;

import com.ttmo.matching.enums.OrderSide;
import com.ttmo.matching.support.Marker;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.util.Assert;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * @author Jover Zhang
 */
@SuppressWarnings("rawtypes")
@RequiredArgsConstructor
public class OrderPoolLuaHelper {

    private static final DefaultRedisScript<Long> ADD_ORDER;

    private static final DefaultRedisScript<Long> REMOVE_ORDER;

    private static final DefaultRedisScript<Long> UPDATE_ORDER;

    private static final DefaultRedisScript<String> PEEK_FIRST;

    @Deprecated
    private static final DefaultRedisScript<List> FETCH_ALL;

    static {
        ADD_ORDER = new DefaultRedisScript<Long>() {{
            setResultType(Long.class);
            setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/limit_order_pool/add_order.lua")));
        }};
        REMOVE_ORDER = new DefaultRedisScript<Long>() {{
            setResultType(Long.class);
            setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/limit_order_pool/remove_order.lua")));
        }};
        UPDATE_ORDER = new DefaultRedisScript<Long>() {{
            setResultType(Long.class);
            setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/limit_order_pool/update_order.lua")));
        }};
        PEEK_FIRST = new DefaultRedisScript<String>() {{
            setResultType(String.class);
            setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/limit_order_pool/peek_first.lua")));
        }};
        FETCH_ALL = new DefaultRedisScript<List>() {{
            setResultType(List.class);
            setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/limit_order_pool/fetch_all.lua")));
        }};
    }

    final StringRedisTemplate redisTemplate;

    public static Marker decodeMarker(@Nonnull String markerStr) {
        String[] markerInfo = markerStr.split(",");
        Assert.isTrue(markerInfo.length == 3);
        return Marker.builder()
                .id(markerInfo[0])
                .price(new BigDecimal(markerInfo[1]))
                .amount(new BigDecimal(markerInfo[2]))
                .build();
    }

    public void addOrder(String poolName, OrderSide side, Marker order) {
        String isBuy = side == OrderSide.SIDE_BUY ? "buy" : "sell";
        redisTemplate.execute(ADD_ORDER,
                Arrays.asList("poolName", "isBuy", "id", "price", "amount"),
                poolName, isBuy, order.getId(),
                order.getPrice().toPlainString(), order.getAmount().toPlainString());
    }

    public void removeOrder(String poolName, OrderSide side, Marker order) {
        String isBuy = side == OrderSide.SIDE_BUY ? "buy" : "sell";
        redisTemplate.execute(REMOVE_ORDER,
                Arrays.asList("poolName", "isBuy", "id", "price", "amount"),
                poolName, isBuy, order.getId(),
                order.getPrice().toPlainString(), order.getAmount().toPlainString());
    }

    public Marker peekFirst(String poolName, OrderSide side) {
        String isBuy = side == OrderSide.SIDE_BUY ? "buy" : "sell";
        String markerStr = redisTemplate.execute(PEEK_FIRST,
                Arrays.asList("poolName", "isBuy"),
                poolName, isBuy);
        if (markerStr == null) {
            return null;
        }
        return decodeMarker(markerStr);
    }

    public void update(String poolName, Marker order) {
        redisTemplate.execute(UPDATE_ORDER, Arrays.asList("poolName", "id", "price", "amount"),
                poolName, order.getId(),
                order.getPrice().toPlainString(), order.getAmount().toPlainString());
    }

    /**
     * Just for debug
     */
    @Deprecated
    @SuppressWarnings({"unchecked"})
    public ConcurrentSkipListMap<BigDecimal, LinkedList<Marker>> fetchOrderPool(String poolName) {
        ConcurrentSkipListMap<BigDecimal, LinkedList<Marker>> result = new ConcurrentSkipListMap<>();
        List<List<String>> orderPool = redisTemplate.execute(FETCH_ALL,
                Collections.singletonList("poolName"), poolName);
        if (orderPool == null || orderPool.isEmpty()) {
            return result;
        }

        for (List<String> samePriceOrders : orderPool) {
            for (String orderStr : samePriceOrders) {
                Marker order = decodeMarker(orderStr);
                result.compute(order.getPrice(), (k, v) -> {
                    if (v == null) {
                        v = new LinkedList<>();
                    }
                    v.add(order);
                    return v;
                });
            }
        }
        return result;
    }

}
