package org.xxpay.shop.vo;


import javax.validation.constraints.NotNull;

/**
 * 支付订单请求类
 * @author oliver
 */
public class PayOrderReqVO {

    @NotNull(message = "订单id不能为空")
    private Long orderId;
    @NotNull(message = "订单名称不能为空")
    private String orderName;
    @NotNull(message = "渠道id不能为空")
    private String channelId;
    @NotNull(message = "付款方名称不能为空")
    private String payerUserName;
    @NotNull(message = "付款方id不能为空")
    private String payerUserId;
    @NotNull(message = "付款方用户类型不能为空")
    private String payerUserType;
    @NotNull(message = "订单金额不能为空")
    private Long orderPayMoney;
    @NotNull(message = "收款方id不能为空")
    private String payeeUserId;
    @NotNull(message = "收款方用户类型不能为空")
    private String payeeUserType;
    @NotNull(message = "收款方用户名称不能为空")
    private String payeeUserName;

    public String getPayerUserId() {
        return payerUserId;
    }

    public void setPayerUserId(String payerUserId) {
        this.payerUserId = payerUserId;
    }

    public String getPayerUserType() {
        return payerUserType;
    }

    public void setPayerUserType(String payerUserType) {
        this.payerUserType = payerUserType;
    }

    public String getPayeeUserId() {
        return payeeUserId;
    }

    public void setPayeeUserId(String payeeUserId) {
        this.payeeUserId = payeeUserId;
    }

    public String getPayeeUserType() {
        return payeeUserType;
    }

    public void setPayeeUserType(String payeeUserType) {
        this.payeeUserType = payeeUserType;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getOrderName() {
        return orderName;
    }

    public void setOrderName(String orderName) {
        this.orderName = orderName;
    }

    public Long getOrderPayMoney() {
        return orderPayMoney;
    }

    public void setOrderPayMoney(Long orderPayMoney) {
        this.orderPayMoney = orderPayMoney;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public String getPayerUserName() {
        return payerUserName;
    }

    public void setPayerUserName(String payerUserName) {
        this.payerUserName = payerUserName;
    }

    public String getPayeeUserName() {
        return payeeUserName;
    }

    public void setPayeeUserName(String payeeUserName) {
        this.payeeUserName = payeeUserName;
    }
}
