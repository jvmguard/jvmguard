package dev.jvmguard.integration.tests.jvmguard.classes.rmi;

import dev.jvmguard.integration.util.SleepHelper;

import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class MyRemote3Impl implements MyRemote3 {
    private static MyRemote3Impl impl;


    @Override public String simpleCall(String param) throws RemoteException {
        System.out.println("remote call3 " + param);
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
        impl = new MyRemote3Impl();
        MyRemote3 stub = (MyRemote3)UnicastRemoteObject.exportObject(impl, 5077);
        registry.rebind(NAME, stub);
    }

}
