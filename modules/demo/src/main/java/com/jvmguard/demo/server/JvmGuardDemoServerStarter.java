package com.jvmguard.demo.server;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Launches the jvmguard demo cluster: a fleet of monitored JVMs that simulate an e-commerce platform
 * ("Online Boutique"). Each child JVM runs a single {@link DemoService} role under a jvmguard group.
 * <p>
 * Topology:
 * <ul>
 *   <li>{@code Demo/Storefront}: a pool of 3 storefront VMs (the customer facing edge)</li>
 *   <li>{@code Demo/Browse}: Catalog, Recommendation, Currency (finding things / price display)</li>
 *   <li>{@code Demo/Purchase}: Cart, Checkout, Payment (transactions)</li>
 *   <li>{@code Demo/Fulfillment}: Shipping, Notification (after the order)</li>
 * </ul>
 * Stopping this starter shuts down the whole cluster.
 */
public class JvmGuardDemoServerStarter {

    /** A JVM to launch: a service role, its group path, and (optionally) a VM name. No name means a pool. */
    private record VmSpec(String role, String groupPath, String name) {
        boolean isPool() {
            return name == null;
        }
    }

    public static void main(String[] args) throws Exception {
        List<VmSpec> vms = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            vms.add(new VmSpec("Storefront", "Demo/Storefront", null));
        }
        vms.add(new VmSpec("Catalog", "Demo/Browse", "Catalog"));
        vms.add(new VmSpec("Recommendation", "Demo/Browse", "Recommendation"));
        vms.add(new VmSpec("Currency", "Demo/Browse", "Currency"));
        vms.add(new VmSpec("Cart", "Demo/Purchase", "Cart"));
        vms.add(new VmSpec("Checkout", "Demo/Purchase", "Checkout"));
        vms.add(new VmSpec("Payment", "Demo/Purchase", "Payment"));
        vms.add(new VmSpec("Shipping", "Demo/Fulfillment", "Shipping"));
        vms.add(new VmSpec("Notification", "Demo/Fulfillment", "Notification"));

        List<Process> processes = new ArrayList<>();
        for (VmSpec vm : vms) {
            startVm(vm, processes);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.err.println("Shutting down demo cluster ...");
            for (Process process : processes) {
                process.destroy();
            }
            System.err.println("Shut down completed.");
        }));

        for (Process process : processes) {
            try {
                process.waitFor();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static void startVm(VmSpec vm, List<Process> processes) {
        String executable = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
        if (System.getProperty("os.name").toLowerCase().startsWith("win")) {
            executable += ".exe";
        }

        File javaAgentJar = new File("agent/jvmguard.jar");
        if (!javaAgentJar.exists()) { // development mode
            javaAgentJar = new File("dist/agent/jvmguard.jar");
        }

        StringBuilder agent = new StringBuilder();
        agent.append("-javaagent:").append(javaAgentJar.getPath()).append("=");
        String vmPort = System.getProperty("vmPort");
        if (vmPort != null) {
            agent.append(",port=").append(vmPort);
        }
        if (vm.isPool()) {
            agent.append(",pool=").append(vm.groupPath());
        } else {
            agent.append(",name=").append(vm.name()).append(",group=").append(vm.groupPath());
        }
        agent.append(",keyStore=");

        List<String> commands = new ArrayList<>();
        commands.add(executable);
        commands.add(agent.toString());
        commands.add("-classpath");
        commands.add(System.getProperty("java.class.path"));

        if (Boolean.getBoolean("jvmguard.logRmi")) {
            commands.add("-Djvmguard.logRmi=true");
        }
        if (Boolean.getBoolean("jvmguard.debugBootstrap")) {
            commands.add("-Djvmguard.debugBootstrap=true");
        }
        addLoggingParameter("User", commands);
        addLoggingParameter("Common", commands);
        addLoggingParameter("Communication", commands);
        commands.add("-Djdk.xml.entityExpansionLimit=0");

        commands.add(DemoService.class.getName());
        commands.add(vm.role());

        String identity = vm.isPool() ? "pool=" + vm.groupPath() : "name=" + vm.name() + " group=" + vm.groupPath();
        System.err.println("Starting demo VM [" + vm.role() + "] " + identity);
        try {
            Process process = new ProcessBuilder(commands).inheritIO().start();
            processes.add(process);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void addLoggingParameter(String subSystem, List<String> commands) {
        String value = System.getProperty("jvmguard.log" + subSystem);
        if (value != null) {
            commands.add("-Djvmguard.log" + subSystem + "=" + value);
        }
    }
}
