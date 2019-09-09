package info.bitrich.xchangestream.lgo.dto;

public class LgoSocketPlaceUnencryptedOrder {

    private final LgoUnencryptedOrder order;

    public LgoSocketPlaceUnencryptedOrder(LgoUnencryptedOrder unencryptedOrder) {
        this.order = unencryptedOrder;
    }

    public String getType() {
        return "placeunencryptedorder";
    }

    public LgoUnencryptedOrder getOrder() {
        return order;
    }

}
