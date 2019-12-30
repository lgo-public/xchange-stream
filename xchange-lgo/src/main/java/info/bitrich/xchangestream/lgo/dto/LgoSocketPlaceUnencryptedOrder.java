package info.bitrich.xchangestream.lgo.dto;

import org.knowm.xchange.lgo.dto.order.*;

public class LgoSocketPlaceUnencryptedOrder {

    private final Payload payload;

    public LgoSocketPlaceUnencryptedOrder(LgoUnencryptedOrder order, Long ref, LgoOrderSignature signatureValue) {
        payload = new Payload(order, ref, signatureValue);
    }

    public String getType() {
        return "placeunencryptedorder";
    }

    public Payload getPayload() {
        return payload;
    }

    public static class Payload {
        public Payload(LgoUnencryptedOrder order, Long ref, LgoOrderSignature signatureValue) {
            this.order = order;
            signature = signatureValue;
            this.reference = ref;
        }

        private LgoUnencryptedOrder order;
        private LgoOrderSignature signature;
        private long reference;

        public LgoUnencryptedOrder getOrder() {
            return order;
        }

        public LgoOrderSignature getSignature() {
            return signature;
        }

        public long getReference() {
            return reference;
        }
    }
}
