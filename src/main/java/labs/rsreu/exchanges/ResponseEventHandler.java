package labs.rsreu.exchanges;

import com.lmax.disruptor.EventHandler;
import labs.rsreu.clients.Client;
import labs.rsreu.clients.ClientsList;
import labs.rsreu.currencies.Currency;
import labs.rsreu.exchanges.ResponseEvent;
import labs.rsreu.orders.Order;
import labs.rsreu.orders.TransactionInfo;

public class ResponseEventHandler implements EventHandler<ResponseEvent> {
    private final ClientsList clientsList;

    public ResponseEventHandler(ClientsList clientsList) {
        this.clientsList = clientsList;
    }

    @Override
    public void onEvent(ResponseEvent event, long sequence, boolean endOfBatch) {
        TransactionInfo transactionInfo = event.getTransactionInfo();

        if (transactionInfo != null && !transactionInfo.isHasError() && !transactionInfo.isOrderOpen() && !transactionInfo.isHasMessage()) {
            // Извлекаем ордера и клиентов
            Order sellOrder = transactionInfo.getSellOrder();
            Order buyOrder = transactionInfo.getBuyOrder();
            Client seller = clientsList.getClient(sellOrder.getClientId());
            Client buyer = clientsList.getClient(buyOrder.getClientId());

            // Получаем валюты из ордера
            Currency buyCurrency = buyOrder.getCurrencyPair().getCurrencyFirst();
            Currency sellCurrency = buyOrder.getCurrencyPair().getCurrencySecond();

            // Обновляем балансы покупателей и продавцов
            buyer.updateBalance(sellCurrency, buyOrder.getAmountSecond(), false);
            buyer.updateBalance(buyCurrency, buyOrder.getAmountFirst(), true);

            seller.updateBalance(sellCurrency, sellOrder.getAmountSecond(), true);
            seller.updateBalance(buyCurrency, sellOrder.getAmountFirst(), false);
        }
    }
}
