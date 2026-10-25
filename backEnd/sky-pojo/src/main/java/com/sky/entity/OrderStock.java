package com.sky.entity;

import lombok.Data;

/** Immutable reservation quantities, including the original components of a set meal. */
@Data
public class OrderStock {
    private Long orderId;
    private Long dishId;
    private Integer number;
}
