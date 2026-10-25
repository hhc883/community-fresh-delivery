package com.sky.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 社区自提点：社区团购的履约地点
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PickupPoint implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    //自提点名称
    private String name;

    //所属社区
    private String community;

    //详细地址
    private String address;

    //联系人
    private String contact;

    //联系电话
    private String phone;

    //营业时间
    private String openTime;

    //0 停用 1 启用
    private Integer status;

    //纬度（m21 新增：小程序端按距离排序 / 地图打点）
    private BigDecimal latitude;

    //经度
    private BigDecimal longitude;

    //门头图片
    private String image;

    //距离展示（默认 260m）
    private String distance;

    //服务标签（默认 支持冷冻/冷藏）
    private String serviceTags;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    public String getContactName() {
        return contact;
    }

    public void setContactName(String contactName) {
        this.contact = contactName;
    }

    public String getContactPhone() {
        return phone;
    }

    public void setContactPhone(String contactPhone) {
        this.phone = contactPhone;
    }

    public String getPic() {
        return image;
    }

    public void setPic(String pic) {
        this.image = pic;
    }

    public String getService() {
        // 只返回数据库真实值，不编造默认服务标签
        return serviceTags;
    }

    public String getDistance() {
        // 只返回数据库真实值，距离需要由客户端按定位计算，不在服务端编造
        return distance;
    }
}
