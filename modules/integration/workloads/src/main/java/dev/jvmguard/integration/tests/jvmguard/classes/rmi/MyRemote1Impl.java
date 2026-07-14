package dev.jvmguard.integration.tests.jvmguard.classes.rmi;

import dev.jvmguard.integration.util.SleepHelper;

import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class MyRemote1Impl implements MyRemote1 {
    private static MyRemote1Impl impl;

    @Override
    public String simpleCall(String param) throws RemoteException {
        System.out.println("remote call " + param);
        try {
            if (param.startsWith("jvmguard")) {
                SleepHelper.sleep(200);
                RemoteCallee.call();
            } else {
                SleepHelper.sleep(300);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return param.toLowerCase();
    }

    public static void register(Registry registry) throws RemoteException {
        impl = new MyRemote1Impl();
        MyRemote1 stub = (MyRemote1)UnicastRemoteObject.exportObject(impl, 5077);
        registry.rebind(NAME, stub);
    }

}
