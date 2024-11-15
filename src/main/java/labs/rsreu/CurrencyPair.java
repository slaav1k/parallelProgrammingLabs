package labs.rsreu;



public class CurrencyPair {
    private final Currency currencyFirst;
    private final Currency currencySecond;

    public CurrencyPair(Currency currencyFirst, Currency currencySecond) {
        if (currencyFirst.equals(currencySecond)) {
            throw new IllegalArgumentException("Currencies in the pair cannot be the same.");
        }
        this.currencyFirst = currencyFirst;
        this.currencySecond = currencySecond;
    }

    public Currency getCurrencyFirst() {
        return currencyFirst;
    }

    public Currency getCurrencySecond() {
        return currencySecond;
    }

    @Override
    public String toString() {
        return currencyFirst + "/" + currencySecond;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CurrencyPair that = (CurrencyPair) o;
        return currencyFirst.equals(that.currencyFirst) && currencySecond.equals(that.currencySecond);
    }

    @Override
    public int hashCode() {
        return 31 * currencyFirst.hashCode() + currencySecond.hashCode();
    }
}
