package ru.ifmo.ctddev.khorin.rmi;

import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClientTest {
    private ExecutorService service = Executors.newFixedThreadPool(10);

    @Test
    public void test() throws Exception {
        List<Map<Integer, Integer>> sum = new ArrayList<>();
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
        for (int i = 0; i < 10; i++) {
            sum.add(new HashMap<>());
        }
        for (int i = 0; i < 10; i++) {
            Person client = bank.getClient(i + "", true);
            if (client != null) {
                for (int accountId = 0; accountId < 10; accountId++) {
                    if (client.getAmount(accountId + "") != null) {
                        sum.get(i).put(accountId, client.getAmount(accountId + ""));
                    }
                }
            }
        }
        List<Callable<Void>> runnables = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            int passport = (int)(Math.random() * 10);
            int accountId = (int)(Math.random() * 10);
            int amount = (int)(Math.random() * 1000);
            sum.get(passport).putIfAbsent(accountId, 0);
            sum.get(passport).put(accountId, sum.get(passport).get(accountId) + amount);
            runnables.add(() -> {
                String[] args = {passport + "", passport + "", passport + "", accountId + "", amount + ""};
                try {
                    Client.main(args);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                return null;
            });
        }
        service.invokeAll(runnables);
        boolean error = false;
        for (int i = 0; i < 10; i++) {
            for (Integer accountId : sum.get(i).keySet()) {
                if (!(sum.get(i).get(accountId).equals(bank.getClient(i + "", true).getAmount(accountId +"")))) {
                    error = true;
                    System.out.println("Error person " + i + " account " + accountId);
                }
            }
        }
        if (!error) {
            System.out.println("Correct");
        }
    }

    public static void main(String[] args) throws Exception {
        JUnitCore runner = new JUnitCore();
        Result result = runner.run(ClientTest.class);
    }
}
