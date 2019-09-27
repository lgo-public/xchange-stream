package info.bitrich.xchangestream.lgo.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import org.knowm.xchange.lgo.dto.order.LgoOrderSignature;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class LgoCancelOrder {
    private final LgoOrderSignature signature;
    private final String orderId;
    private final long timestamp;
    private final Long reference;

    public LgoCancelOrder(LgoOrderSignature signature, String orderId, long timestamp, Long reference) {
        this.signature = signature;
        this.orderId = orderId;
        this.timestamp = timestamp;
        this.reference = reference;
    }

    public LgoOrderSignature getSignature() {
        return signature;
    }

    public String getOrderId() {
        return orderId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public Long getReference() {
        return reference;
    }
}
