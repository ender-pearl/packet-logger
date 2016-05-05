package net.minecraft.packetlogger.agent;

import com.google.gson.Gson;
import io.netty.util.AttributeKey;
import net.minecraft.packetlogger.gui.GuiApplication;
import net.minecraft.packetlogger.hook.Hooks;
import net.minecraft.packetlogger.info.ProtocolInfo;

import java.io.InputStreamReader;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;

public class Agent {
    public static Instrumentation instrumentation;
    public static AgentTransformer transformer = new AgentTransformer();

    public static void agentmain(String args, Instrumentation instrumentation) throws Exception {
        premain(args, instrumentation);
    }

    public static void premain(String args, Instrumentation instrumentation) throws Exception {
        Agent.instrumentation = instrumentation;

        try {
            Gson gson = new Gson();

            Hooks.protocolInfo = gson.fromJson(new InputStreamReader(Agent.class.getResourceAsStream("/protocol_1.9.json")), ProtocolInfo.class);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        String[] retransformBlacklist = new String[]{
                "java.", "org.", "com.", "sun.", "javax.", "io.", "jdk.",
                "joptsimple.",
                "paulscode.",
                "net.minecraft.packetlogger."
        };

        if (args == null) {
            args = "";
        }

        final String[] splitArgs = args.split(" ");
        new Thread(() -> GuiApplication.run(splitArgs)).start();

        instrumentation.addTransformer(transformer, true);
        Class[] loadedClasses = instrumentation.getAllLoadedClasses();
        for (Class loadedClass : loadedClasses) {
            String className = loadedClass.getName();

            boolean skip = false;
            for (String entry : retransformBlacklist) {
                skip = className.startsWith(entry);
                if (skip) {
                    break;
                }
            }

            if (skip) {
                continue;
            }

            if (instrumentation.isModifiableClass(loadedClass)) {
                try {
                    instrumentation.retransformClasses(loadedClass);
                } catch (InternalError error) {
                    System.out.println("Error Transforming: " + loadedClass);
                }
            }

            if (transformer.hasInjected()) {
                break;
            }
        }

        Field[] networkManagerFields = Class.forName(transformer.getNetworkManagerClass()).getFields();
        for (Field field : networkManagerFields) {
            if (field.getType() == AttributeKey.class) {
                field.setAccessible(true);
                Hooks.attributeProtocol = (AttributeKey) field.get(null);
            }
        }
    }
}
