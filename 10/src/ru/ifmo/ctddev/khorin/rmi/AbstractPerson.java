package ru.ifmo.ctddev.khorin.rmi;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public abstract class AbstractPerson implements Person {
    protected String firstName;
    protected String lastName;
    protected String passport;
    protected ConcurrentMap<String, Account> accounts = new ConcurrentHashMap<>();

    public String getFirstName() throws RemoteException {
        return firstName;
    }

    public String getLastName() throws RemoteException {
        return lastName;
    }

    public String getPassport() throws RemoteException {
        return passport;
    }

    public ConcurrentMap<String, Account> getAccounts() throws RemoteException {
        return accounts;
    }

    public void addAccount(String accountId) throws RemoteException {
        if (accounts.containsKey(accountId)) {
            System.out.println("Account with id" + accountId + "already exists");
        } else {
            System.out.println("Creating account");
            accounts.putIfAbsent(accountId, new AccountImpl(accountId));
        }
    }

    public Integer getAmount(String accountId) throws RemoteException {
        if (!accounts.containsKey(accountId)) {
            System.out.println("Account with id " + accountId + " doesn't exists");
            return null;
        } else {
            return accounts.get(accountId).getAmount();
        }
    }

    public void transaction(String accountId, int amount) throws RemoteException {
        if (!accounts.containsKey(accountId)) {
            addAccount(accountId);
        }
        Account account = accounts.get(accountId);
        account.setAmount(account.getAmount() + amount);
        System.out.println("Current amount " + account.getAmount());
    }

}
