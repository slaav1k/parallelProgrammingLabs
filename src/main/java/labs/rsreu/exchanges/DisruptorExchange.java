package labs.rsreu.exchanges;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import labs.rsreu.currencies.CurrencyPairRegistry;
import labs.rsreu.orders.Order;
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
    private final int bufferSize = 2048;
    private final DisruptorExchangeOrderHandler orderHandler;
    private  final Disruptor<OrderEvent> orderDisruptor;
    private final Lock lock = new ReentrantLock();
    private boolean isExchangeClosed = false;

    public DisruptorExchange(CurrencyPairRegistry currencyPairRegistry) {
        this.currencyPairRegistry = currencyPairRegistry;

        // Инициализация входного буфера для заявок
        EventFactory<OrderEvent> orderFactory = OrderEvent::new;
        orderDisruptor = new Disruptor<>(orderFactory, bufferSize, Executors.defaultThreadFactory(), ProducerType.SINGLE, new BlockingWaitStrategy());
        orderBuffer = orderDisruptor.getRingBuffer();

        // Инициализация выходного буфера для ответов
        EventFactory<ResponseEvent> responseFactory = ResponseEvent::new;
        Disruptor<ResponseEvent> responseDisruptor = new Disruptor<>(responseFactory, bufferSize, Executors.defaultThreadFactory(), ProducerType.SINGLE, new BlockingWaitStrategy());
        responseBuffer = responseDisruptor.getRingBuffer();

        // Создаем обработчик ордеров
        orderHandler = new DisruptorExchangeOrderHandler();
        orderDisruptor.handleEventsWith(orderHandler);

        // Создаем обработчик ответов
        ResponseHandler responseHandler = new ResponseHandler();
        responseDisruptor.handleEventsWith(responseHandler);

        // Запускаем disruptor'ы
        orderDisruptor.start();
        responseDisruptor.start();
    }


    @Override
    public void createOrder(Order order, Consumer<TransactionInfo> resultCallback) {
        if (isExchangeClosed) {
            resultCallback.accept(new TransactionInfo(true, "Order cannot be created: Exchange is closed."));
            return;
        }

        if (!currencyPairRegistry.isValidCurrencyPair(order.getCurrencyPair())) {
            resultCallback.accept(new TransactionInfo(true, "Order " + order.getId() + " has invalid currency pair."));
            return;
        }

        // Добавляем callback для уведомления о статусе
        order.addStatusCallback(resultCallback);

        try {
            // Публикуем событие в буфер
            orderBuffer.publishEvent((event, sequence) -> event.setOrder(new Order(order)));
            resultCallback.accept(new TransactionInfo(1, "Order " + order.getId() + " successfully submitted."));
        } catch (Exception e) {
            resultCallback.accept(new TransactionInfo(true, "Order submission failed: " + e.getMessage()));
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
}
