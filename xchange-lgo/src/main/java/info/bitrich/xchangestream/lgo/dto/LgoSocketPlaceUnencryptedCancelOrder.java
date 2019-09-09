package info.bitrich.xchangestream.lgo.dto;

public class LgoSocketPlaceUnencryptedCancelOrder {

    private final LgoCancelOrder order;

    public LgoSocketPlaceUnencryptedCancelOrder(LgoCancelOrder cancelOrder) {
        this.order = cancelOrder;
    }

    public String getType() {
        return "cancelorder";
    }

    public LgoCancelOrder getOrder() {
        return order;
    }

}
