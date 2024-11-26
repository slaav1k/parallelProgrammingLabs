package labs.rsreu.clients;

import labs.rsreu.currencies.Currency;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ClientsList {
    private final Map<Integer, Client> clients;
    private final Lock lock = new ReentrantLock();

    public ClientsList() {
        this.clients = new HashMap<>();
    }

    public Client createClient(EnumMap<Currency, BigDecimal> balance) {
        lock.lock();
        try {
            Client client = new Client(balance);
            clients.put(client.getId(), client);
            return client;
        } finally {
            lock.unlock();
        }
    }

    public void updateBalanceClient(int clientId, Currency currency, BigDecimal amount, boolean isDeposit) {
        lock.lock();
        try {
            Client tmpClient = this.getClient(clientId);
            if (tmpClient != null) {
                tmpClient.updateBalance(currency, amount, isDeposit);
            }
        } finally {
            lock.unlock();
        }
    }

    public ArrayList<Client> getAllClients() {
        lock.lock();
        try {
            return new ArrayList<>(clients.values());
        } finally {
            lock.unlock();
        }
    }

    public Client getClient(int clientId) {
        lock.lock();
        try {
            return clients.get(clientId);
        } finally {
            lock.unlock();
        }
    }

    public int size() {
        lock.lock();
        try {
            return clients.size();
        } finally {
            lock.unlock();
        }
    }

//    public void setClients(Map<Integer, Client> clients) {
//        this.clients = clients;
//    }

    public EnumMap<Currency, BigDecimal> getTotalBalances() {
        lock.lock();
        try {
            EnumMap<Currency, BigDecimal> totalBalances = new EnumMap<>(Currency.class);

            for (Client client : clients.values()) {
                // Для каждой валюты в балансе клиента
                for (Currency currency : client.getAllBalances().keySet()) {
                    BigDecimal clientBalance = client.getBalance(currency);
                    totalBalances.put(currency, totalBalances.getOrDefault(currency, BigDecimal.ZERO).add(clientBalance));
                }
            }
            return totalBalances;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public String toString() {
        lock.lock();
        StringBuilder sb = new StringBuilder();
        for (Client client : clients.values()) {
            sb.append(client.toString()).append("\n");
        }
        lock.unlock();
        return sb.toString();
    }
}
