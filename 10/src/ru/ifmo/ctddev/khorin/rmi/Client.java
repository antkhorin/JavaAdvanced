package ru.ifmo.ctddev.khorin.rmi;

import java.rmi.*;
import java.net.*;

public class Client {
    public static void main(String[] args) throws RemoteException {
        if (args.length != 5) {
            System.out.println("Please write 5 args");
            return;
        }
        for (String s : args) {
            if (s == null) {
                System.out.println("Args shouldn't be null");
                return;
            }
        }
        String firstName = args[0];
        String lastName = args[1];
        String passport = args[2];
        String accountId = args[3];
        int transaction;
        try {
            transaction = Integer.parseInt(args[4]);
        } catch (NumberFormatException e) {
            System.out.println("Use correct number in 5th argument");
            return;
        }
        Bank bank;
        try {
            bank = (Bank) Naming.lookup("//localhost/bank");
        } catch (NotBoundException e) {
            System.out.println("Bank is not bound");
            return;
        } catch (MalformedURLException e) {
            System.out.println("Bank URL is invalid");
            return;
        }
        Person client = bank.getClient(passport, true);
        if (client == null) {
            client = bank.addClient(firstName, lastName, passport);
        } else if (!client.getFirstName().equals(firstName) || !client.getLastName().equals(lastName)) {
            System.out.println("Invalid personal information");
        }
        if (client.getAmount(accountId) == null) {
            client.addAccount(accountId);
        } else {
            System.out.println("Account already exists");
        }
        System.out.println("Money: " + client.getAmount(accountId));
        System.out.println("Adding money " + transaction);
        client.transaction(accountId, transaction);
        System.out.println("Current: " + client.getAmount(accountId));
        //bank.updateLocalClient(client, accountId);
    }
}
