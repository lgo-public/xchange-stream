package info.bitrich.xchangestream.lgo.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type",
        visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = LgoUserUpdate.class, name = "update"),
        @JsonSubTypes.Type(value = LgoUserSnapshot.class, name = "snapshot")
})
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class LgoUserMessage {

    private final long batchId;
    private final String type;
    private final String channel;
    private final String productId;

    public LgoUserMessage(long batchId, String type, String channel, String productId) {
        this.batchId = batchId;
        this.type = type;
        this.channel = channel;
        this.productId = productId;
    }

    public long getBatchId() {
        return batchId;
    }

    public String getType() {
        return type;
    }

    public String getChannel() {
        return channel;
    }

    public String getProductId() {
        return productId;
    }

}
