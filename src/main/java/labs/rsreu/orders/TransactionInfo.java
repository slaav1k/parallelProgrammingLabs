package labs.rsreu.orders;

public class TransactionInfo {
    private boolean hasError;
    private boolean hasMessage;
    private int isOpen;
    private String message;
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

    public TransactionInfo(int isOpen, String message) {
        this.isOpen = isOpen;
        this.message = message;
    }

    public TransactionInfo(String message) {
        this.hasMessage = true;
        this.message = message;
    }

    public boolean isHasMessage() {
        return hasMessage;
    }

    public boolean isOrderOpen() {
        return isOpen == 1;
    }

    public String getMessage() {
        return message;
    }

    public void setIsOpen(int isOpen) {
        this.isOpen = isOpen;
    }

    public void setMessage(String message) {
        this.message = message;
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
            return "ERROR " + this.errorMessage;
        } else {
            return this.buyOrder.toString() + " " + this.sellOrder.toString();
        }
    }
    
}
