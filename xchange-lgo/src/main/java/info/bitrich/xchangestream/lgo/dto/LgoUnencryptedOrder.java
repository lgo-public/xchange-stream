package info.bitrich.xchangestream.lgo.dto;


import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import org.knowm.xchange.lgo.dto.order.LgoOrderSignature;

public class LgoUnencryptedOrder {
    private final LgoUnencryptedOrderData order;
    private final LgoOrderSignature signature;
    private final long reference;

    public LgoUnencryptedOrder(LgoUnencryptedOrderData order, LgoOrderSignature signature, long reference) {
        this.order = order;
        this.signature = signature;
        this.reference = reference;
    }

    public LgoUnencryptedOrderData getOrder() {
        return this.order;
    }

    public LgoOrderSignature getSignature() {
        return this.signature;
    }

    public long getReference() {
        return this.reference;
    }

    @JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
    public static class LgoUnencryptedOrderData {
        private final String type;
        private final String side;
        private final String productId;
        private final String quantity;
        private final String price;
        private final long timestamp;

        public LgoUnencryptedOrderData(String type, String side, String productId, String quantity, String price, long timestamp) {
            this.type = type;
            this.side = side;
            this.productId = productId;
            this.quantity = quantity;
            this.price = price;
            this.timestamp = timestamp;
        }

        public String getType() {
            return type;
        }

        public String getSide() {
            return side;
        }

        public String getProductId() {
            return productId;
        }

        public String getQuantity() {
            return quantity;
        }

        public String getPrice() {
            return price;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }
}

