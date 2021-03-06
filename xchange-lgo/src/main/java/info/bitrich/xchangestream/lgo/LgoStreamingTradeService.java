package info.bitrich.xchangestream.lgo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import info.bitrich.xchangestream.core.StreamingTradeService;
import info.bitrich.xchangestream.lgo.domain.*;
import info.bitrich.xchangestream.lgo.dto.*;
import info.bitrich.xchangestream.service.netty.StreamingObjectMapperHelper;
import io.reactivex.Observable;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.trade.*;
import org.knowm.xchange.lgo.LgoAdapters;
import org.knowm.xchange.lgo.dto.key.LgoKey;
import org.knowm.xchange.lgo.dto.order.*;
import org.knowm.xchange.lgo.service.*;
import si.mazi.rescu.SynchronizedValueFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class LgoStreamingTradeService implements StreamingTradeService {

    private final LgoStreamingService streamingService;
    private final LgoKeyService keyService;
    private final LgoSignatureService signatureService;
    private final SynchronizedValueFactory<Long> nonceFactory;
    private final boolean shouldEncryptOrders;
    private final Map<CurrencyPair, LgoUserBatchSubscription> batchSubscriptions = new ConcurrentHashMap<>();
    private Observable<LgoOrderEvent> afrSubscription;

    LgoStreamingTradeService(LgoStreamingService streamingService,
                             LgoKeyService keyService,
                             LgoSignatureService signatureService,
                             SynchronizedValueFactory<Long> nonceFactory,
                             boolean shouldEncryptOrders) {
        this.streamingService = streamingService;
        this.keyService = keyService;
        this.signatureService = signatureService;
        this.nonceFactory = nonceFactory;
        this.shouldEncryptOrders = shouldEncryptOrders;
    }

    /**
     * {@inheritDoc}
     * First sent orders will be current open orders.
     */
    @Override
    public Observable<Order> getOrderChanges(CurrencyPair currencyPair, Object... args) {
        return getOrderBatchChanges(currencyPair)
                .flatMap(Observable::fromIterable);
    }

    /**
     * Get an up-to-date view of all your open orders after each LGO batch execution.
     * First sending will be the actual open orders list.
     */
    public Observable<OpenOrders> getOpenOrders(CurrencyPair currencyPair) {
        return getOrderUpdates(currencyPair)
                .map(u -> u.getAllOpenOrders().values().stream()
                        .filter(order -> order instanceof LimitOrder)
                        .map(order -> (LimitOrder) order)
                        .collect(Collectors.toList()))
                .map(OpenOrders::new);
    }

    /**
     * Receive all updated orders, for each LGO batches.
     * First sending will be the actual open orders list.
     */
    public Observable<Collection<Order>> getOrderBatchChanges(CurrencyPair currencyPair) {
        return getOrderUpdates(currencyPair)
                .map(LgoGroupedUserUpdate::getUpdatedOrders);
    }

    private Observable<LgoGroupedUserUpdate> getOrderUpdates(CurrencyPair currencyPair) {
        return batchSubscriptions.computeIfAbsent(currencyPair, this::createBatchSubscription).getPublisher();
    }

    private LgoUserBatchSubscription createBatchSubscription(CurrencyPair currencyPair) {
        return LgoUserBatchSubscription.create(streamingService, currencyPair);
    }

    @Override
    public Observable<UserTrade> getUserTrades(CurrencyPair currencyPair, Object... args) {
        return getRawBatchOrderEvents(currencyPair)
                .filter(lgoOrderEvent -> "match".equals(lgoOrderEvent.getType()))
                .map(matchEvent -> LgoAdapter.adaptUserTrade(currencyPair, (LgoMatchOrderEvent) matchEvent));
    }

    /**
     * Receive all events for the selected currency pairs. Merges batch order events and ack (AFR)
     * events.
     */
    public Observable<LgoOrderEvent> getRawAllOrderEvents(Collection<CurrencyPair> currencyPairs) {
        Observable<LgoOrderEvent> ackObservable = getRawReceivedOrderEvents();
        return currencyPairs.stream()
                .map(this::getRawBatchOrderEvents)
                .reduce(Observable::mergeWith)
                .map(ackObservable::mergeWith)
                .orElse(ackObservable);
    }

    /**
     * Get ack for your placed orders. "received" events indicate the orderId associated to your
     * order, if you set a reference on order placement you will have it in the event. "failed" events
     * indicate that the order could not be read or was invalid and not added to a batch.
     */
    public Observable<LgoOrderEvent> getRawReceivedOrderEvents() {
        if (afrSubscription == null) {
            createAfrSubscription();
        }
        return afrSubscription;
    }

    private void createAfrSubscription() {
        final ObjectMapper mapper = StreamingObjectMapperHelper.getObjectMapper();
        afrSubscription = streamingService
                .subscribeChannel("afr")
                .map(s -> mapper.readValue(s.toString(), LgoAckUpdate.class))
                .map(LgoAckUpdate::getData)
                .flatMap(Observable::<LgoOrderEvent>fromIterable)
                .share();
    }

    /**
     * Get all events of your orders happened during batch execution, for a currency pair. "pending"
     * events indicate that the order was added to a batch and received by the execution engine.
     * "invalid" events indicate that the order was not suitable for execution. "match" events
     * indicate that the order did match against another order. "open" events indicate that the order
     * entered the order book. "done" events indicate that the order was filled, canceled or
     * rejected.
     */
    public Observable<LgoOrderEvent> getRawBatchOrderEvents(CurrencyPair currencyPair) {
        return getOrderUpdates(currencyPair)
                .map(LgoGroupedUserUpdate::getEvents)
                .flatMap(Observable::fromIterable);
    }

    /**
     * Place a market order
     *
     * @return the order reference
     */
    public String placeMarketOrder(MarketOrder marketOrder) throws IOException {
        Long ref = nonceFactory.createValue();
        if (shouldEncryptOrders) {
            LgoPlaceOrder lgoOrder = LgoAdapters.adaptEncryptedMarketOrder(marketOrder);
            return placeEncryptedOrder(ref, lgoOrder);
        }
        LgoUnencryptedOrder lgoUnencryptedOrder = LgoAdapters.adaptUnencryptedMarketOrder(marketOrder);
        return placeUnencryptedOrder(lgoUnencryptedOrder, ref);
    }

    /**
     * Place a limit order
     *
     * @return the order reference
     */
    public String placeLimitOrder(LimitOrder limitOrder) throws IOException {
        Long ref = nonceFactory.createValue();
        if (shouldEncryptOrders) {
            return placeEncryptedOrder(ref, LgoAdapters.adaptLimitOrder(limitOrder));
        }
        LgoUnencryptedOrder lgoUnencryptedOrder = LgoAdapters.adaptUnencryptedLimitOrder(limitOrder);
        return placeUnencryptedOrder(lgoUnencryptedOrder, ref);
    }

    /**
     * Place a cancel order
     *
     * @return true
     */
    public boolean cancelOrder(String orderId) throws IOException {
        Long ref = nonceFactory.createValue();
        LgoPlaceCancelOrder lgoOrder = new LgoPlaceCancelOrder(ref, orderId, new Date().toInstant());
        if (shouldEncryptOrders) {
            placeEncryptedOrder(ref, lgoOrder);
            return true;
        }
        return placeUnencryptedCancel(lgoOrder);
    }

    private boolean placeUnencryptedCancel(LgoPlaceCancelOrder cancelOrder) throws JsonProcessingException {
        String toSign = generateStringToSign(cancelOrder);
        LgoOrderSignature signature = signatureService.signOrder(toSign);
        LgoSocketCancelOrder placeOrder = new LgoSocketCancelOrder(cancelOrder, signature);
        String payload = StreamingObjectMapperHelper.getObjectMapper().writeValueAsString(placeOrder);
        streamingService.sendMessage(payload);
        return true;
    }

    private String generateStringToSign(LgoPlaceCancelOrder cancelOrder) {
        ObjectNode node = StreamingObjectMapperHelper.getObjectMapper().createObjectNode();
        node.put("order_id", cancelOrder.getOrderId());
        node.put("timestamp", cancelOrder.getTimestamp().toEpochMilli());
        return node.toString();
    }

    private String placeUnencryptedOrder(LgoUnencryptedOrder order, Long ref) throws JsonProcessingException {
        String toSign = generateStringToSign(order);
        LgoOrderSignature signatureValue = signatureService.signOrder(toSign);
        LgoSocketPlaceUnencryptedOrder placeOrder = new LgoSocketPlaceUnencryptedOrder(order, ref, signatureValue);
        String payload = StreamingObjectMapperHelper.getObjectMapper().writeValueAsString(placeOrder);
        streamingService.sendMessage(payload);
        return ref.toString();
    }

    private String generateStringToSign(LgoUnencryptedOrder order) {
        ObjectNode node = StreamingObjectMapperHelper.getObjectMapper().createObjectNode();
        node.put("type", order.type);
        node.put("side", order.side);
        node.put("product_id", order.productId);
        node.put("quantity", order.quantity);
        if ("L".equals(order.type)) node.put("price", order.price);
        node.put("timestamp", order.timestamp);
        return node.toString();
    }

    private String placeEncryptedOrder(Long ref, LgoPlaceOrder lgoOrder) throws JsonProcessingException {
        LgoKey lgoKey = keyService.selectKey();
        String encryptedOrder = CryptoUtils.encryptOrder(lgoKey, lgoOrder);
        LgoOrderSignature signature = signatureService.signOrder(encryptedOrder);
        LgoSocketPlaceEncryptedOrder placeOrder = new LgoSocketPlaceEncryptedOrder(new LgoEncryptedOrder(lgoKey.getId(), encryptedOrder, signature, ref));
        String payload = StreamingObjectMapperHelper.getObjectMapper().writeValueAsString(placeOrder);
        streamingService.sendMessage(payload);
        return ref.toString();
    }
}
