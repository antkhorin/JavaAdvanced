package ru.ifmo.ctddev.khorin.rmi;

import java.rmi.*;

public interface Bank extends Remote {
    Person addClient(String firstName, String lastName, String passport)
        throws RemoteException;

    Person getClient(String passport, boolean type)
        throws RemoteException;

    void updateLocalClient(Person person, String accountId)
            throws RemoteException;
}
