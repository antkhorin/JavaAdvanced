#!/bin/bash
export CLASSPATH=src

javac src/ru/ifmo/ctddev/khorin/rmi/Person.java src/ru/ifmo/ctddev/khorin/rmi/Server.java src/ru/ifmo/ctddev/khorin/rmi/Client.java &
rmiregistry
