package labs.rsreu.orders;

import com.lmax.disruptor.RingBuffer;
import labs.rsreu.clients.Client;
import labs.rsreu.clients.ClientsList;
import labs.rsreu.currencies.Currency;
import labs.rsreu.exchanges.ResponseEvent;

import java.util.concurrent.ConcurrentLinkedQueue;

public class TransactionInfoHandler {
    private ClientsList clientsList;
    private RingBuffer<ResponseEvent> responseBuffer;
    private ConcurrentLinkedQueue<TransactionInfo> callbackQueue = new ConcurrentLinkedQueue<>();

    public TransactionInfoHandler(ClientsList clientsList, ConcurrentLinkedQueue<TransactionInfo> callbackQueue) {
        this.clientsList = clientsList;
        this.callbackQueue = callbackQueue;
    }

    public TransactionInfoHandler(ClientsList clientsList, RingBuffer<ResponseEvent> responseBuffer) {
        this.clientsList = clientsList;
        this.responseBuffer = responseBuffer;
    }

    public void processTransactions() {
        for (TransactionInfo transactionInfo : callbackQueue) {
            if (!transactionInfo.isHasError() && !transactionInfo.isOrderOpen() && !transactionInfo.isHasMessage()) {
                Order sellOrder = transactionInfo.getSellOrder();
                Order buyOrder = transactionInfo.getBuyOrder();
                Client seller = clientsList.getClient(sellOrder.getClientId());
                Client buyer = clientsList.getClient(buyOrder.getClientId());

                Currency buyCurrency = buyOrder.getCurrencyPair().getCurrencyFirst();
                Currency sellCurrency = buyOrder.getCurrencyPair().getCurrencySecond();

                buyer.updateBalance(sellCurrency, buyOrder.getAmountSecond(), false);
                buyer.updateBalance(buyCurrency, buyOrder.getAmountFirst(), true);

                seller.updateBalance(sellCurrency, sellOrder.getAmountSecond(), true);
                seller.updateBalance(buyCurrency, sellOrder.getAmountFirst(), false);
            }
        }
    }

    public void processTransactionsDisruptor() {
        long sequence = responseBuffer.getCursor();  // Получаем текущий курсор в буфере

        for (long i = 0; i < sequence; i++) {
            // Получаем событие для обработки
            ResponseEvent responseEvent = responseBuffer.get(i);
            TransactionInfo transactionInfo = responseEvent.getTransactionInfo();

            // Проверяем, что данные валидны, и обрабатываем их
            if (transactionInfo != null && !transactionInfo.isHasError() && !transactionInfo.isOrderOpen() && !transactionInfo.isHasMessage()) {
                Order sellOrder = transactionInfo.getSellOrder();
                Order buyOrder = transactionInfo.getBuyOrder();
                Client seller = clientsList.getClient(sellOrder.getClientId());
                Client buyer = clientsList.getClient(buyOrder.getClientId());

                Currency buyCurrency = buyOrder.getCurrencyPair().getCurrencyFirst();
                Currency sellCurrency = buyOrder.getCurrencyPair().getCurrencySecond();

                // Обновляем балансы покупателя и продавца
                buyer.updateBalance(sellCurrency, buyOrder.getAmountSecond(), false);
                buyer.updateBalance(buyCurrency, buyOrder.getAmountFirst(), true);

                seller.updateBalance(sellCurrency, sellOrder.getAmountSecond(), true);
                seller.updateBalance(buyCurrency, sellOrder.getAmountFirst(), false);
            }
        }
    }

}
