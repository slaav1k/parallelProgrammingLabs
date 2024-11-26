package labs.rsreu.orders;

public class TransactionInfo {
    private boolean hasError;
    private String errorMessage;
    private Order buyOrder;
    private Order sellOrder;

    public TransactionInfo(Order buyOrder, Order sellOrder) {
        this.buyOrder = buyOrder;
        this.sellOrder = sellOrder;
    }

    public TransactionInfo(boolean hasError, String errorMessage) {
        this.hasError = hasError;
        this.errorMessage = errorMessage;
    }

    public boolean isHasError() {
        return hasError;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Order getBuyOrder() {
        return buyOrder;
    }

    public Order getSellOrder() {
        return sellOrder;
    }

    @Override
    public String toString() {
        if (this.hasError) {
            return this.errorMessage;
        } else {
            return this.buyOrder.toString() + " " + this.sellOrder.toString();
        }
    }
    
}
