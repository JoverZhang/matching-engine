package com.ttmo.matching.support;

import com.ttmo.matching.enums.OrderSide;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 交易信息
 *
 * @author Jover Zhang
 */
@Data
@Builder
@AllArgsConstructor
public class Trade {

    /**
     * 挂单 id
     */
    String markerId;

    /**
     * 吃单 id
     */
    String traderId;

    /**
     * 吃单方向
     */
    OrderSide traderSide;

    /**
     * 成交价格
     */
    BigDecimal price;

    /**
     * 成交数量
     */
    BigDecimal amount;

    /**
     * 成交时间戳
     */
    Long timestamp;

}
