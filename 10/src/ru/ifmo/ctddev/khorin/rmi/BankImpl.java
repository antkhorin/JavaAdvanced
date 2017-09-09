package ru.ifmo.ctddev.khorin.rmi;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class BankImpl implements Bank {
    private ConcurrentMap<String, Person> clients = new ConcurrentHashMap<>();
    private int port;

    public BankImpl(final int port) {
        this.port = port;
    }

    public Person addClient(String firstName, String lastName, String passport) throws RemoteException {
        RemotePerson client = new RemotePerson(firstName, lastName, passport);
        boolean exist = (clients.putIfAbsent(passport, client) != null);
        if (!exist) {
            UnicastRemoteObject.exportObject(client, port);
        }
        return client;
}

    public Person getClient(String passport, boolean type) throws RemoteException {
        if (clients.containsKey(passport)) {
            return type ? clients.get(passport) : new LocalPerson(clients.get(passport));
        } else {
            return null;
        }
    }

    public void updateLocalClient(Person person, String accountId) throws RemoteException {
        Person client = clients.get(person.getPassport());
        if (client != null) {
            if (client.getAmount(accountId) == null) {
                client.addAccount(accountId);
            }
            client.transaction(accountId, person.getAmount(accountId) - client.getAmount(accountId));
        }
    }
}