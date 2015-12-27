package net.minecraft.packetlogger.agent;

import net.minecraft.packetlogger.hook.Hooks;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.*;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

import static org.objectweb.asm.Opcodes.*;

public class AgentTransformer implements ClassFileTransformer {
    public static final String STRING_DECODE = "Bad packet id ";
    public static final String STRING_ENCODE = "Can't serialize unregistered packet";
    public static final String STRING_NETWORK_MANAGER = "disconnect.endOfStream";

    private boolean hookedDecode = false;
    private boolean hookedEncode = false;
    private boolean foundNetworkManager = false;

    private String networkManagerClass;

    public final boolean hasInjected() {
        return hookedDecode && hookedEncode && foundNetworkManager;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        if (hasInjected()) {
            return null;
        }
        ClassNode classNode = new ClassNode();

        ClassReader reader = new ClassReader(classfileBuffer);
        reader.accept(classNode, 0);

        for (MethodNode methodNode : classNode.methods) {
            if (processMethod(classNode, methodNode)) {
                ClassWriter classWriter = new ClassWriter(ASM5);
                classNode.accept(classWriter);
                return classWriter.toByteArray();
            }
        }

        return null;
    }

    public boolean processMethod(ClassNode classNode, MethodNode methodNode) {
        InsnList instructions = methodNode.instructions;
        AbstractInsnNode insnNode = instructions.getFirst();
        while (insnNode != null) {
            if (insnNode instanceof LdcInsnNode) {
                LdcInsnNode ldcInsnNode = (LdcInsnNode) insnNode;
                if (STRING_DECODE.equals(ldcInsnNode.cst)) {
                    // System.out.println(classNode.name + "." + methodNode.name + methodNode.desc);

                    hookMethod(methodNode, "onDecode");
                    hookedDecode = true;
                    return true;
                }
                if (STRING_ENCODE.equals(ldcInsnNode.cst)) {
                    // System.out.println(classNode.name + "." + methodNode.name + methodNode.desc);

                    hookMethod(methodNode, "onEncode");
                    hookedEncode = true;
                    return true;
                }
                if (STRING_NETWORK_MANAGER.equals(ldcInsnNode.cst)) {
                    networkManagerClass = classNode.name.replace('/', '.');
                    foundNetworkManager = true;
                    return false;
                }
            }

            insnNode = insnNode.getNext();
        }
        return false;
    }

    public void hookMethod(MethodNode methodNode, String callbackName) {
        InsnList instructions = methodNode.instructions;
        AbstractInsnNode lastReturn = instructions.getLast();

        AbstractInsnNode insnNode = instructions.getFirst();
        while (insnNode != null) {
            if (insnNode.getOpcode() == RETURN) {
                lastReturn = insnNode;
            }

            insnNode = insnNode.getNext();
        }

        String hooksClass = Hooks.class.getName().replace('.', '/');

        instructions.insertBefore(lastReturn, new VarInsnNode(ALOAD, 0));
        instructions.insertBefore(lastReturn, new VarInsnNode(ALOAD, 1));
        instructions.insertBefore(lastReturn, new VarInsnNode(ALOAD, 2));
        instructions.insertBefore(lastReturn, new VarInsnNode(ALOAD, 3));
        instructions.insertBefore(lastReturn, new MethodInsnNode(INVOKESTATIC, hooksClass, callbackName, "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V", false));
    }

    public String getNetworkManagerClass() {
        return networkManagerClass;
    }
}
