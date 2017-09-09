package ru.ifmo.ctddev.khorin.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

public class RemotePerson extends AbstractPerson {
    RemotePerson(String firstName, String lastName, String passport) throws RemoteException {
        this.firstName = firstName;
        this.lastName = lastName;
        this.passport = passport;
    }

}
