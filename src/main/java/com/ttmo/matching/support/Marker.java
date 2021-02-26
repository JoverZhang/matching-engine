package com.ttmo.matching.support;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Marker marker = (Marker) o;
        if (!Objects.equals(id, marker.id)) {
            return false;
        }
        if (price != null ? price.compareTo(marker.price) != 0 : marker.price != null) {
            return false;
        }
        return amount != null ? amount.compareTo(marker.amount) == 0 : marker.amount == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (price != null ? price.hashCode() : 0);
        result = 31 * result + (amount != null ? amount.hashCode() : 0);
        return result;
    }

}
