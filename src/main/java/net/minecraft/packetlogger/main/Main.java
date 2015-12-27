package net.minecraft.packetlogger.main;

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;

public class Main {
    private static void injectFile(File file) throws Exception {
        Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
        method.setAccessible(true);
        method.invoke(ClassLoader.getSystemClassLoader(), file.toURI().toURL());
    }

    private static File getCurrentJarFile() throws URISyntaxException {
        return new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
    }

    public static void main(String[] args) throws Exception {
        injectFile(new File(System.getProperty("java.home") + "/../lib/tools.jar"));

        boolean auto = args.length > 0 && args[0].equals("auto");

        String target = "-1";
        for (VirtualMachineDescriptor descriptor : VirtualMachine.list()) {
            if (auto) {
                if (descriptor.displayName().startsWith("net.minecraft.client.main.Main")) {
                    target = descriptor.id();
                }
            } else {
                System.out.println(descriptor.id() + " - " + descriptor.displayName());
            }
        }

        if (!auto) {
            System.out.print("Id: ");
            target = new BufferedReader(new InputStreamReader(System.in)).readLine();
        }

        VirtualMachine vm = VirtualMachine.attach(target);

        String agentJar = getCurrentJarFile().getAbsolutePath();
        try {
            vm.loadAgent(agentJar);
        } finally {
            vm.detach();
        }
    }
}
