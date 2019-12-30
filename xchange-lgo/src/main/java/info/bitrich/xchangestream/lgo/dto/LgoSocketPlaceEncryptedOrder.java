package info.bitrich.xchangestream.lgo.dto;

import org.knowm.xchange.lgo.dto.order.LgoEncryptedOrder;

public class LgoSocketPlaceEncryptedOrder {

    private final LgoEncryptedOrder order;

    public LgoSocketPlaceEncryptedOrder(LgoEncryptedOrder encryptedOrder) {
        this.order = encryptedOrder;
    }

    public String getType() {
        return "placeorder";
    }

    public LgoEncryptedOrder getOrder() {
        return order;
    }


}
