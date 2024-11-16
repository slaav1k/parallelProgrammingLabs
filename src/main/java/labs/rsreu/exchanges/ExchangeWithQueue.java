//package labs.rsreu.exchanges;
//
//import labs.rsreu.clients.Client;
//import labs.rsreu.currencies.Currency;
//import labs.rsreu.currencies.CurrencyPairRegistry;
//import labs.rsreu.orders.Order;
//import labs.rsreu.orders.OrderType;
//
//import java.math.BigDecimal;
//import java.math.RoundingMode;
//import java.util.*;
//import java.util.concurrent.*;
//import java.util.concurrent.locks.Lock;
//import java.util.concurrent.locks.ReentrantLock;
//import java.util.function.Consumer;
//import java.util.stream.Collectors;
//
//public class ExchangeWithQueue implements IExchange
//{
//    private final Map<Integer, Client> clients = new HashMap<>();
//    private final CurrencyPairRegistry currencyPairRegistry; // Валютные пары
//    private List<Order> buyOrders = new ArrayList<>(); // Список ордеров на покупку
//    private List<Order> sellOrders = new ArrayList<>(); // Список ордеров на продажу
//    private final Lock lock = new ReentrantLock();
//    private final BlockingQueue<Order> orderQueue = new LinkedBlockingQueue<>();
//    private final ExecutorService executorService;
//
//    public ExchangeWithQueue(CurrencyPairRegistry currencyPairRegistry) {
//        this.currencyPairRegistry = currencyPairRegistry;
//        this.executorService = Executors.newSingleThreadExecutor();
//        this.startOrderProcessor();
//    }
//
//    private void startOrderProcessor() {
//        executorService.submit(() -> {
//            while (!Thread.currentThread().isInterrupted()) {
//                try {
//                    Order order = orderQueue.take();
//                    processOrder(order);
//                } catch (InterruptedException e) {
//                    Thread.currentThread().interrupt();
//                    // оповестить клиентов, заявки которых остались
//                    // что биржа закрылась
//                    break; // илт return
//
//                }
//            }
//        });
//    }
//
//    private void processOrder(Order order) {
//        if (order.getType() == OrderType.BUY) {
//            addBuyOrder(order);
//        }
//        else {
//            addSellOrder(order);
//        }
//    }
//
//    private void addSellOrder(Order sellOrder) {
//        buyOrders = getBuyOrders();
//        boolean isMatched = false;
//        Iterator<Order> iterator = buyOrders.iterator();
//        while (iterator.hasNext()) {
//            Order eachBuyOrder = iterator.next();
//
//            // Убедимся, что ордера принадлежат разным клиентам, что валютные пары совпадают и что цена удовлетворяет условиям
//            if (eachBuyOrder.getClient().getId() != sellOrder.getClient().getId()
//                    && eachBuyOrder.getCurrencyPair().equals(sellOrder.getCurrencyPair())
//                    && eachBuyOrder.getPrice().compareTo(sellOrder.getPrice()) >= 0) {
//
//                isMatched = true;
//
//                BigDecimal transactionPrice = eachBuyOrder.getPrice();
//                List<BigDecimal> transactionsAmountCurrency = processTransaction(eachBuyOrder, sellOrder, transactionPrice);
//                BigDecimal transactionAmountBuyCurrency = transactionsAmountCurrency.get(1);
//                BigDecimal transactionAmountSellCurrency = transactionsAmountCurrency.get(0);
//
//
//                // Обновляем остатки в ордерах
//                BigDecimal remainingAmountSellCurrency = sellOrder.getAmountFirst().subtract(transactionAmountBuyCurrency);
//                BigDecimal remainingAmountBuyCurrency = sellOrder.getAmountSecond().subtract(transactionAmountSellCurrency);
//
//                BigDecimal remainingAmountBuyOrderSellCurrency = eachBuyOrder.getAmountFirst().subtract(transactionAmountBuyCurrency);
//                BigDecimal remainingAmountBuyOrderBuyCurrency = eachBuyOrder.getAmountSecond().subtract(transactionAmountSellCurrency);
//
//                // Обновление ордера на продажу, если остаток больше нуля
//                if (remainingAmountSellCurrency.compareTo(BigDecimal.ZERO) == 0) {
//                    System.out.println("Продавец продал все, заявка удалена.");
////                    resultCallback.accept("Продавец продал все, заявка удалена.");
//                } else {
//                    sellOrder.setAmountFirst(remainingAmountSellCurrency);
//                    sellOrder.setAmountSecond(remainingAmountBuyCurrency);
//                    sellOrders.add(sellOrder);
//                    sellOrders.sort(Comparator.comparing(Order::getPrice).reversed());
//                }
//
//                // Обновление ордера на покупку, если остаток больше нуля
//                if (remainingAmountBuyOrderSellCurrency.compareTo(BigDecimal.ZERO) == 0) {
//                    iterator.remove();
////                    System.out.println("Покупатель купил все, заявка удалена.");
//                    resultCallback.accept("Покупатель купил все, заявка удалена.");
//                } else {
//                    eachBuyOrder.setAmountFirst(remainingAmountBuyOrderSellCurrency);
//                    eachBuyOrder.setAmountSecond(remainingAmountBuyOrderBuyCurrency);
//                }
//            }
//        }
//
//        // Если ордер не исполнен, добавляем в очередь
//        if (!isMatched) {
//            sellOrders.add(sellOrder);
//            sellOrders.sort(Comparator.comparing(Order::getPrice).reversed()); // Сортировка по цене
//        }
//    }
//
//    private void addBuyOrder(Order buyOrder) {
//        boolean isMatched = false;
//        sellOrders = getSellOrders();
//        Iterator<Order> iterator = sellOrders.iterator();
//        while (iterator.hasNext()) {
//            Order eachSellOrder = iterator.next();
//            if (eachSellOrder.getClientId() != buyOrder.getClientId()
//                    && eachSellOrder.getCurrencyPair().equals(buyOrder.getCurrencyPair())
//                    && eachSellOrder.getPrice().compareTo(buyOrder.getPrice()) <= 0) {
//
//                isMatched = true;
//
//                BigDecimal transactionPrice = eachSellOrder.getPrice();
//                List<BigDecimal> transactionsAmountCurrency = processTransaction(buyOrder, eachSellOrder, transactionPrice);
//
//                BigDecimal transactionAmountBuyCurrency = transactionsAmountCurrency.get(1);
//                BigDecimal transactionAmountSellCurrency = transactionsAmountCurrency.get(0);
//
//                // Обновляем остатки в ордерах, но не устанавливаем значения равные нулю
//                BigDecimal remainingAmountBuyCurrency = buyOrder.getAmountFirst().subtract(transactionAmountBuyCurrency);
//                BigDecimal remainingAmountSellCurrency = buyOrder.getAmountSecond().subtract(transactionAmountSellCurrency);
//
//                BigDecimal remainingAmountSellOrderBuyCurrency = eachSellOrder.getAmountFirst().subtract(transactionAmountBuyCurrency);
//                BigDecimal remainingAmountSellOrderSellCurrency = eachSellOrder.getAmountSecond().subtract(transactionAmountSellCurrency);
//
//                // Обновление ордера на покупку, если остаток больше нуля
//                if (remainingAmountBuyCurrency.compareTo(BigDecimal.ZERO) == 0) {
//                    System.out.println("Покупатель купил все, заявка удалена.");
////                    resultCallback.accept("Покупатель купил все, заявка удалена.");
//
//                } else {
//                    buyOrder.setAmountFirst(remainingAmountBuyCurrency);
//                    buyOrder.setAmountSecond(remainingAmountSellCurrency);
//                    buyOrders.add(buyOrder);
//                    buyOrders.sort(Comparator.comparing(Order::getPrice));
//                }
//
//                // Обновление ордера на продажу, если остаток больше нуля
//                if (remainingAmountSellOrderBuyCurrency.compareTo(BigDecimal.ZERO) == 0) {
//                    iterator.remove();
//                    System.out.println("Продавец продал все, заявка удалена.");
////                    resultCallback.accept("Продавец продал все, заявка удалена.");
//
//                } else {
//                    eachSellOrder.setAmountFirst(remainingAmountSellOrderBuyCurrency);
//                    eachSellOrder.setAmountSecond(remainingAmountSellOrderSellCurrency);
//                    orderQueue.add(eachSellOrder);
//                }
//            }
//        }
//    }
//
//    private List<BigDecimal> processTransaction(Order buyOrder, Order sellOrder, BigDecimal transactionPrice) {
//        Client buyer = clients.get(buyOrder.getClientId());
//        Client seller = clients.get(sellOrder.getClientId());
//
//        labs.rsreu.currencies.Currency buyCurrency = buyOrder.getCurrencyPair().getCurrencyFirst();
//        Currency sellCurrency = buyOrder.getCurrencyPair().getCurrencySecond();
//
//        BigDecimal buyAmountBuyer = buyOrder.getAmountFirst();
//        BigDecimal sellAmountSeller = sellOrder.getAmountFirst();
//        BigDecimal buyAmountOrderTmp = buyAmountBuyer.min(sellAmountSeller);
//        BigDecimal priceBuyAmountOrderTmp = buyAmountOrderTmp.multiply(transactionPrice);
//
//        BigDecimal buyerOldBalanceSellCurrency = buyer.getBalance(sellCurrency);
//        BigDecimal sellerOldBalanceBuyCurrency = seller.getBalance(buyCurrency);
//        BigDecimal buyerOldBalanceBuyCurrency = buyer.getBalance(buyCurrency);
//        BigDecimal sellerOldBalanceSellCurrency = seller.getBalance(sellCurrency);
//
//        // Проверка баланса покупателя
//        BigDecimal transactionAmountSellCurrency;
//        BigDecimal transactionAmountBuyCurrency;
//        if (buyer.getBalance(sellCurrency).compareTo(priceBuyAmountOrderTmp) >= 0) {
//            transactionAmountSellCurrency = priceBuyAmountOrderTmp;
//            transactionAmountBuyCurrency = buyAmountOrderTmp;
//        } else {
//            transactionAmountSellCurrency = buyer.getBalance(sellCurrency);
//            transactionAmountBuyCurrency = transactionAmountSellCurrency.divide(transactionPrice, RoundingMode.DOWN);
//        }
//
//        // Обновление балансов
//        buyer.updateBalance(sellCurrency, transactionAmountSellCurrency, false);
//        buyer.updateBalance(buyCurrency, transactionAmountBuyCurrency, true);
//
//        seller.updateBalance(sellCurrency, transactionAmountSellCurrency, true);
//        seller.updateBalance(buyCurrency, transactionAmountBuyCurrency, false);
//
//        // Логирование сделки
//        String transactionLog = "|----------TRANSACTION COMPLETE----------|\n" +
//                "ID ордера (покупка): " + buyOrder.getId() +
//                ", Покупатель ID: " + buyer.getId() + "\n" +
//                "ID ордера (продажа): " + sellOrder.getId() +
//                ", Продавец ID: " + seller.getId() + "\n" +
//                "Валютная пара: " + buyOrder.getCurrencyPair() + "\n" +
//                "Сумма транзакции " + buyCurrency + ": " + transactionAmountBuyCurrency + "\n" +
//                "Сумма транзакции " + sellCurrency + ": " + transactionAmountSellCurrency + "\n" +
//                "Баланс покупателя до сделки: " + buyerOldBalanceSellCurrency + " " +
//                sellCurrency + ", после сделки: " + buyer.getBalance(sellCurrency) + "\n" +
//                "Баланс покупателя до сделки: " + buyerOldBalanceBuyCurrency + " " +
//                buyCurrency + ", после сделки: " + buyer.getBalance(buyCurrency) + "\n" +
//                "Баланс продавца до сделки: " + sellerOldBalanceBuyCurrency + " " +
//                buyCurrency + ", после сделки: " + seller.getBalance(buyCurrency) + "\n" +
//                "Баланс продавца до сделки: " + sellerOldBalanceSellCurrency + " " +
//                sellCurrency + ", после сделки: " + seller.getBalance(sellCurrency) + "\n" +
//                "|----------------------------------------|";
//        System.out.println(transactionLog);
//        return Arrays.asList(transactionAmountSellCurrency, transactionAmountBuyCurrency);
//
//    }
//
//    @Override
//    public Client createClient(EnumMap<Currency, BigDecimal> balance) {
//        return null;
//    }
//
//    @Override
//    public void createOrder(Order order, Consumer<String> resultCallback) {
//
//    }
//
//    @Override
//    public List<Order> getOpenOrders() {
//        return List.of();
//    }
//
//    @Override
//    public Client getClientState(int clientId) {
//        return null;
//    }
//
//    @Override
//    public EnumMap<Currency, BigDecimal> getTotalBalances() {
//        return null;
//    }
//
//    private List<Order> getSellOrders() {
//        return orderQueue.stream()
//                .filter(order -> order.getType() == OrderType.SELL) // Фильтруем ордера на продажу
//                .sorted(Comparator.comparing(Order::getPrice).reversed()) // Сортируем по цене (убывание)
//                .collect(Collectors.toList());
//    }
//
//    private List<Order> getBuyOrders() {
//        return orderQueue.stream()
//                .filter(order -> order.getType() == OrderType.BUY) // Фильтруем ордера на покупку
//                .sorted(Comparator.comparing(Order::getPrice)) // Сортируем по цене
//                .collect(Collectors.toList());
//    }
//
//}
