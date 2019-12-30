package info.bitrich.xchangestream.lgo.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import org.knowm.xchange.lgo.dto.order.*;

public class LgoSocketCancelOrder {

    private final Payload payload;

    public LgoSocketCancelOrder(LgoPlaceCancelOrder cancelOrder, LgoOrderSignature signature) {
        payload = new Payload(cancelOrder.getOrderId(), cancelOrder.getTimestamp().toEpochMilli(), cancelOrder.getReference(), signature);
    }

    public String getType() {
        return "cancelorder";
    }

    public Payload getPayload() {
        return payload;
    }

    @JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
    public static class Payload {

        private String orderId;
        private long timestamp;
        private LgoOrderSignature signature;
        private long reference;

        public Payload(String orderId, long timestamp, long reference, LgoOrderSignature signature) {
            this.orderId = orderId;
            this.timestamp = timestamp;
            this.reference = reference;
            this.signature = signature;
        }

        public String getOrderId() {
            return orderId;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public LgoOrderSignature getSignature() {
            return signature;
        }

        public long getReference() {
            return reference;
        }
    }
}
