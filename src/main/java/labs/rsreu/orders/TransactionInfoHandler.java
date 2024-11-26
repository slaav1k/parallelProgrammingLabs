package labs.rsreu.orders;

import labs.rsreu.clients.Client;
import labs.rsreu.clients.ClientsList;
import labs.rsreu.currencies.Currency;

import java.util.concurrent.ConcurrentLinkedQueue;

public class TransactionInfoHandler {
    private ClientsList clientsList;
    private ConcurrentLinkedQueue<TransactionInfo> callbackQueue = new ConcurrentLinkedQueue<>();

    public TransactionInfoHandler(ClientsList clientsList, ConcurrentLinkedQueue<TransactionInfo> callbackQueue) {
        this.clientsList = clientsList;
        this.callbackQueue = callbackQueue;
    }

    public void processTransactions() {
        for (TransactionInfo transactionInfo : callbackQueue) {
            if (!transactionInfo.isHasError()) {
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
}
