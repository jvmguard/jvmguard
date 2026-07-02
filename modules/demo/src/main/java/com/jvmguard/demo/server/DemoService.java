package com.jvmguard.demo.server;

import com.jvmguard.demo.server.mbean.DemoMBean;
import com.jvmguard.demo.server.services.*;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;

/**
 * Entry point of a single demo workload JVM. The {@link JvmGuardDemoServerStarter} launches one such JVM per
 * service role, passing the role as the first argument; the group / pool / VM name are passed to the
 * jvmguard agent through the {@code -javaagent} argument.
 */
public class DemoService {

    public static void main(String[] args) throws Exception {
        System.setProperty("demo.jvmguard", "true");
        if (args.length < 1) {
            System.err.println("Usage: DemoService <role>");
            System.exit(1);
        }
        String role = args[0];
        TrafficProfile profile = new TrafficProfile(role, peakHourFor(role));
        registerMBeans(profile, role);
        Service service = createService(role, profile);
        service.start();
        System.err.println("Demo service [" + role + "] running with " + profile);
        Thread.currentThread().join();
    }

    private static double peakHourFor(String role) {
        return switch (role) {
            case "Storefront" -> 13;
            case "Catalog" -> 13;
            case "Recommendation" -> 19;
            case "Currency" -> 13;
            case "Cart" -> 14;
            case "Checkout" -> 15;
            case "Payment" -> 15;
            case "Shipping" -> 14;
            case "Notification" -> 16;
            default -> 13;
        };
    }

    private static Service createService(String role, TrafficProfile profile) {
        return switch (role) {
            case "Storefront" -> new Storefront(profile);
            case "Catalog" -> new Catalog(profile);
            case "Recommendation" -> new Recommendation(profile);
            case "Currency" -> new Currency(profile);
            case "Cart" -> new Cart(profile);
            case "Checkout" -> new Checkout(profile);
            case "Payment" -> new Payment(profile);
            case "Shipping" -> new Shipping(profile);
            case "Notification" -> new Notification(profile);
            default -> throw new IllegalArgumentException("Unknown demo service role: " + role);
        };
    }

    private static void registerMBeans(TrafficProfile profile, String role) {
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        try {
            server.registerMBean(profile, new ObjectName("com.jvmguard.demo:type=TrafficProfile,name=" + ObjectName.quote(role)));
            server.registerMBean(new DemoMBean(), new ObjectName("com.jvmguard.demo:type=DemoService,name=" + ObjectName.quote(role)));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
