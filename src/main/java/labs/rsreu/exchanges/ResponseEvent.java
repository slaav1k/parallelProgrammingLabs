package labs.rsreu.exchanges;

import labs.rsreu.orders.TransactionInfo;
import java.util.function.Consumer;

public class ResponseEvent {
    private TransactionInfo transactionInfo;
    private Consumer<TransactionInfo> callback;

    public TransactionInfo getTransactionInfo() {
        return transactionInfo;
    }

    public void setTransactionInfo(TransactionInfo transactionInfo) {
        this.transactionInfo = transactionInfo;
    }

    public Consumer<TransactionInfo> getCallback() {
        return callback;
    }

    public void setCallback(Consumer<TransactionInfo> callback) {
        this.callback = callback;
    }

    public void clear() {
        this.transactionInfo = null;
        this.callback = null;
    }
}
