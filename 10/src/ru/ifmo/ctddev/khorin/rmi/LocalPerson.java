package ru.ifmo.ctddev.khorin.rmi;

import java.rmi.RemoteException;

public class LocalPerson extends AbstractPerson {
    public LocalPerson(Person person) throws RemoteException {
        this.firstName = person.getFirstName();
        this.lastName = person.getLastName();
        this.passport = person.getPassport();
        this.accounts = person.getAccounts();
    }
}
