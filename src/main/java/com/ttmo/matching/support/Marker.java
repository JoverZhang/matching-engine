package com.ttmo.matching.support;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * 挂单
 * <p>
 * 因该实体会在内存中存留较长时间,
 * 因此仅保留挂单中的关键信息
 *
 * @author Jover Zhang
 */
@Data
@SuperBuilder
@AllArgsConstructor
public class Marker {

    /**
     * 挂单号
     */
    String id;

    /**
     * 价格
     */
    BigDecimal price;

    /**
     * 数量
     */
    BigDecimal amount;

}
