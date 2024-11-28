package labs.rsreu.exchanges;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.lmax.disruptor.util.DaemonThreadFactory;
import labs.rsreu.currencies.CurrencyPairRegistry;
import labs.rsreu.orders.Order;
import labs.rsreu.orders.OrderType;
import labs.rsreu.orders.TransactionInfo;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

public class DisruptorExchange implements IExchange {
    private final CurrencyPairRegistry currencyPairRegistry;

    private final RingBuffer<OrderEvent> orderBuffer;  // Входная очередь заявок
    private final RingBuffer<ResponseEvent> responseBuffer;  // Выходная очередь ответов
    private final int bufferSize = 2048 * 128;
    private final DisruptorExchangeOrderHandler orderHandler;
    private  final Disruptor<OrderEvent> orderDisruptor;
    private final Lock lock = new ReentrantLock();
    private boolean isExchangeClosed = false;

    public DisruptorExchange(CurrencyPairRegistry currencyPairRegistry, RingBuffer<ResponseEvent> responseBuffer) {
        this.currencyPairRegistry = currencyPairRegistry;
        this.responseBuffer = responseBuffer;

        // Инициализация входного буфера для заявок
        EventFactory<OrderEvent> orderFactory = OrderEvent::new;
        orderDisruptor = new Disruptor<>(orderFactory, bufferSize, DaemonThreadFactory.INSTANCE);
        orderBuffer = orderDisruptor.getRingBuffer();

//        // Инициализация выходного буфера для ответов
//        EventFactory<ResponseEvent> responseFactory = ResponseEvent::new;
//        Disruptor<ResponseEvent> responseDisruptor = new Disruptor<>(responseFactory, bufferSize, DaemonThreadFactory.INSTANCE);
//        responseBuffer = responseDisruptor.getRingBuffer();

        // Создаем обработчик ордеров
        orderHandler = new DisruptorExchangeOrderHandler(responseBuffer);
        orderDisruptor.handleEventsWith(orderHandler);

//        // Создаем обработчик ответов
//        ResponseHandler responseHandler = new ResponseHandler();
//        responseDisruptor.handleEventsWith(responseHandler);

        // Запускаем disruptor'ы
        orderDisruptor.start();
//        responseDisruptor.start();
    }


    @Override
    public void createOrder(Order order, Consumer<TransactionInfo> resultCallback) {
        ResponseEvent responseEvent = new ResponseEvent();


        if (isExchangeClosed) {
//            resultCallback.accept(new TransactionInfo(true, "Order cannot be created: Exchange is closed."));
            responseEvent.setTransactionInfo(new TransactionInfo(true, "Order cannot be created: Exchange is closed."));
            responseBuffer.publishEvent((event, sequence) -> event.setTransactionInfo(responseEvent.getTransactionInfo()));
            return;
        }

        if (!currencyPairRegistry.isValidCurrencyPair(order.getCurrencyPair())) {
//            resultCallback.accept(new TransactionInfo(true, "Order " + order.getId() + " has invalid currency pair."));
            responseEvent.setTransactionInfo(new TransactionInfo(true, "Order " + order.getId() + " has invalid currency pair."));
            responseBuffer.publishEvent((event, sequence) -> event.setTransactionInfo(responseEvent.getTransactionInfo()));
            return;
        }

        // Добавляем callback для уведомления о статусе
        order.addStatusCallback(resultCallback);

        try {
            // Публикуем событие в буфер
            orderBuffer.publishEvent((event, sequence) -> event.setOrder(new Order(order)));
//            resultCallback.accept(new TransactionInfo(1, "Order " + order.getId() + " successfully submitted."));
            responseEvent.setTransactionInfo(new TransactionInfo(1, "Order " + order.getId() + " successfully submitted."));
            responseBuffer.publishEvent((event, sequence) -> event.setTransactionInfo(responseEvent.getTransactionInfo()));
        } catch (Exception e) {
//            resultCallback.accept(new TransactionInfo(true, "Order submission failed: " + e.getMessage()));
            responseEvent.setTransactionInfo(new TransactionInfo(true, "Order submission failed: " + e.getMessage()));
            responseBuffer.publishEvent((event, sequence) -> event.setTransactionInfo(responseEvent.getTransactionInfo()));
        }
    }


    @Override
    public List<Order> getOpenOrders() {
        return List.of();
    }

    @Override
    public void closeExchange() {
        isExchangeClosed = true;
        orderHandler.closeExchange();
        orderDisruptor.shutdown();
//        responseDisruptor.shutdown();
    }

//    public RingBuffer<ResponseEvent> getResponseBuffer() {
//        lock.lock();
//        try {
//            return responseBuffer;
//        } finally {
//            lock.unlock();
//        }
//    }
}
