package labs.rsreu.currencies;



import java.util.HashSet;
import java.util.Set;


public class CurrencyPairRegistry {
    private final Set<CurrencyPair> availablePairs;

    public CurrencyPairRegistry() {
        availablePairs = new HashSet<>();
    }

    // Добавление валютной пары в реестр
    public void addCurrencyPair(CurrencyPair currencyPair) {
        availablePairs.add(currencyPair);
    }

    // Проверка, существует ли такая валютная пара
    public boolean isValidCurrencyPair(CurrencyPair currencyPair) {
        return availablePairs.contains(currencyPair);
    }

}
