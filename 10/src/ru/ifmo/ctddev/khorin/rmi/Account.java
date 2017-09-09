package ru.ifmo.ctddev.khorin.rmi;

import java.io.Serializable;
import java.rmi.*;

public interface Account extends Serializable {
    String getId()
        throws RemoteException;

    int getAmount()
        throws RemoteException;

    void setAmount(int amount)
        throws RemoteException;
}