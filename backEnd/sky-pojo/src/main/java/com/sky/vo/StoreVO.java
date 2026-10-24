package com.sky.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreVO implements Serializable {
    private Long id;
    private String name;
    private String address;
    private String distance;
    private String contact;
    private String contactName;
    private String phone;
    private String contactPhone;
    private String image;
    private String pic;
    private String service;
    private String serviceTags;
    private String status; // "open" or "closed"
    private BigDecimal latitude;
    private BigDecimal longitude;
}
