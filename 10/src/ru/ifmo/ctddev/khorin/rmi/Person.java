package ru.ifmo.ctddev.khorin.rmi;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.concurrent.ConcurrentMap;

public interface Person extends Serializable, Remote {
    String getFirstName() throws RemoteException;

    String getLastName() throws RemoteException;

    String getPassport() throws RemoteException;

    ConcurrentMap<String, Account> getAccounts() throws RemoteException;

    void addAccount(String accountId) throws RemoteException;

    Integer getAmount(String accountId) throws RemoteException;

    void transaction(String accountId, int amount) throws RemoteException;
}
