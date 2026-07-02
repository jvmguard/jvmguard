package com.jvmguard.integration.tests.jvmguard.classes.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface BaseRemote extends Remote {
    String simpleCall(String param) throws RemoteException;
}
