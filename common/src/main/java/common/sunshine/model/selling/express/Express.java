package common.sunshine.model.selling.express;

import common.sunshine.model.Entity;

/**
 * 该类为快递抽象类
 * Created by sunshine on 6/22/16.
 */
public class Express extends Entity implements Comparable<Express> {
    private String expressId;

    /* 快递单号 */
    private String expressNumber;

    /* 寄件人姓名 */
    private String senderName;

    /* 赠送人手机号 */
    private String senderPhone;

    /* 赠送人地址 */
    private String senderAddress;

    /* 受赠人姓名 */
    private String receiverName;

    /* 受赠人手机号 */
    private String receiverPhone;

    /* 受赠人地址 */
    private String receiverAddress;

    /* 商品名称 */
    private String goodsName;

    /* 物流单描述 */
    private String description;

    /* linkId 为order_item_id 或 customer_order_id*/
    private String linkId;

    /* 商品数量 */
    private int goodsQuantity;

    public Express() {
        super();
    }

    public Express(String expressNumber) {
        this();
        this.expressNumber = expressNumber;
    }

    public Express(String senderName, String senderPhone, String senderAddress, String receiverName, String receiverPhone, String receiverAddress) {
        this();
        this.senderName = senderName;
        this.senderPhone = senderPhone;
        this.senderAddress = senderAddress;
        this.receiverName = receiverName;
        this.receiverPhone = receiverPhone;
        this.receiverAddress = receiverAddress;
    }

    public Express(String senderName, String senderPhone, String senderAddress, String receiverName, String receiverPhone, String receiverAddress, String goodsName) {
        this(senderName, senderPhone, senderAddress, receiverName, receiverPhone, receiverAddress);
        this.goodsName = goodsName;
    }

    public Express(String expressNumber, String senderName, String senderPhone, String senderAddress, String receiverName, String receiverPhone, String receiverAddress, String goodsName) {
        this(senderName, senderPhone, senderAddress, receiverName, receiverPhone, receiverAddress, goodsName);
        this.expressNumber = expressNumber;
    }

    @Override
    public int compareTo(Express o) {
        int result;
        if (this.goodsQuantity > o.goodsQuantity) {
            result = 1;
        } else if (this.goodsQuantity == o.getGoodsQuantity()) {
            result = 0;
        } else {
            result = -1;
        }
        return result;
    }

    public String getExpressId() {
        return expressId;
    }

    public void setExpressId(String expressId) {
        this.expressId = expressId;
    }

    public String getExpressNumber() {
        return expressNumber;
    }

    public void setExpressNumber(String expressNumber) {
        this.expressNumber = expressNumber;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getSenderPhone() {
        return senderPhone;
    }

    public void setSenderPhone(String senderPhone) {
        this.senderPhone = senderPhone;
    }

    public String getSenderAddress() {
        return senderAddress;
    }

    public void setSenderAddress(String senderAddress) {
        this.senderAddress = senderAddress;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    public String getReceiverPhone() {
        return receiverPhone;
    }

    public void setReceiverPhone(String receiverPhone) {
        this.receiverPhone = receiverPhone;
    }

    public String getReceiverAddress() {
        return receiverAddress;
    }

    public void setReceiverAddress(String receiverAddress) {
        this.receiverAddress = receiverAddress;
    }

    public String getGoodsName() {
        return goodsName;
    }

    public void setGoodsName(String goodsName) {
        this.goodsName = goodsName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLinkId() {
        return linkId;
    }

    public void setLinkId(String linkId) {
        this.linkId = linkId;
    }

    public int getGoodsQuantity() {
        return goodsQuantity;
    }

    public void setGoodsQuantity(int goodsQuantity) {
        this.goodsQuantity = goodsQuantity;
    }
}
