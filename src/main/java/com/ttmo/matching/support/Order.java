package com.ttmo.matching.support;

import com.ttmo.matching.enums.OrderAction;
import com.ttmo.matching.enums.OrderSide;
import com.ttmo.matching.enums.OrderType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * @author Jover Zhang
 */
@Data
@ToString(callSuper = true)
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class Order extends Marker {

    /**
     * 挂单类型 限价 | 市价 ...
     */
    OrderType type;

    /**
     * 挂单方向 买 | 卖
     */
    OrderSide side;

    /**
     * 行为 创建挂单 | 取消挂单
     */
    OrderAction action;

}
