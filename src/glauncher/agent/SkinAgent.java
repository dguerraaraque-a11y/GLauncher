package glauncher.agent;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;

/**
 * Agente Java para inyectar skins en Minecraft Offline.
 * En lugar de sobrescribir a Steve/Alex globalmente, intercepta el método 
 * de carga de texturas para aplicarla solo al jugador local.
 */
public class SkinAgent {

    public static void premain(String agentArgs, Instrumentation inst) {
        System.out.println("[GLauncher Skin Agent] Iniciando inyeccion selectiva...");
        
        String skinPath = System.getProperty("glauncher.skin.path");
        String myName = System.getProperty("glauncher.username");

        if (skinPath == null || myName == null) {
            System.err.println("[SkinAgent] Faltan parametros (skin o username). Abortando.");
            return;
        }

        inst.addTransformer(new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader, String className, Class classBeingRedefined,
                                    ProtectionDomain protectionDomain, byte[] classfileBuffer) {
                
                if (className == null) return null;
                String normalizedClassName = className.replace("/", ".");

                try {
                    // 1. SOPORTE PARA VERSIONES MODERNAS (1.8 - 1.21)
                    // bew = 1.8.9, bub = 1.12.2, bex = 1.16.5, AbstractClientPlayer = Mapeado
                    if (normalizedClassName.endsWith("AbstractClientPlayer") || 
                        normalizedClassName.equals("bew") || normalizedClassName.equals("bub") || normalizedClassName.equals("bex")) {
                        
                        ClassPool cp = ClassPool.getDefault();
                        cp.appendClassPath(new javassist.LoaderClassPath(loader));
                        CtClass cc = cp.get(normalizedClassName);
                        CtMethod m = findSkinMethod(cc);
                        
                        if (m != null) {
                            String resLocClass = detectResourceLocationClass(loader);
                            if (resLocClass != null) {
                                // Inyección: Si el nombre coincide, devolvemos nuestra ruta interna
                                String body = "{" +
                                    "if ($0.getGameProfile().getName().equals(\"" + myName + "\")) {" +
                                    "    return new " + resLocClass + "(\"glauncher\", \"skins/current.png\");" +
                                    "}" +
                                    "}";
                                m.insertBefore(body);
                                
                                System.out.println("[SkinAgent] Hook aplicado a: " + normalizedClassName);
                                byte[] byteCode = cc.toBytecode();
                                cc.detach();
                                return byteCode;
                            }
                        }
                    }

                    // 2. INYECCIÓN DEL ARCHIVO PNG (Crucial para que se vea)
                    // Interceptamos la carga de recursos para entregar el archivo que el usuario eligió
                    if (normalizedClassName.endsWith("DefaultResourcePack") || 
                        normalizedClassName.equals("bpe") || normalizedClassName.equals("ceb") || normalizedClassName.equals("cky")) {
                        
                        ClassPool cp = ClassPool.getDefault();
                        cp.appendClassPath(new javassist.LoaderClassPath(loader));
                        CtClass cc = cp.get(normalizedClassName);
                        
                        injectResourceLoading(cc, skinPath);
                        
                        System.out.println("[SkinAgent] Hook de texturas aplicado a: " + normalizedClassName);
                        byte[] byteCode = cc.toBytecode();
                        cc.detach();
                        return byteCode;
                    }

                } catch (Exception e) {
                    // Silencioso para evitar spam en consola
                }
                return null;
            }
        });
    }

    private static String detectResourceLocationClass(ClassLoader loader) {
        try {
            loader.loadClass("net.minecraft.resources.ResourceLocation");
            return "net.minecraft.resources.ResourceLocation"; // 1.17+
        } catch (Exception e) {
            try {
                loader.loadClass("net.minecraft.util.ResourceLocation");
                return "net.minecraft.util.ResourceLocation"; // 1.7 - 1.16
            } catch (Exception e2) { return null; }
        }
    }

    private static void injectResourceLoading(CtClass cc, String skinPath) throws Exception {
        String escapedPath = skinPath.replace("\\", "\\\\");

        // Engañamos al juego para que crea que el archivo "glauncher:skins/current.png" existe
        String[] existsNames = {"resourceExists", "func_110589_b", "b", "c"}; 
        for (String name : existsNames) {
            try {
                cc.getDeclaredMethod(name).insertBefore("{ if ($1.getNamespace().equals(\"glauncher\")) return true; }");
                break;
            } catch (Exception ignored) {}
        }

        // Cuando el juego pide el Stream de datos, le pasamos nuestro archivo PNG del disco
        String[] streamNames = {"getInputStream", "func_110590_a", "a"};
        for (String name : streamNames) {
            try {
                cc.getDeclaredMethod(name).insertBefore("{ if ($1.getNamespace().equals(\"glauncher\")) {" +
                               "  return new java.io.FileInputStream(\"" + escapedPath + "\");" +
                               "} }");
                break;
            } catch (Exception ignored) {}
        }
    }

    private static CtMethod findSkinMethod(CtClass cc) {
        String[] names = {"getLocationSkin", "m_6261_", "func_110306_p", "r", "x"};
        for (String n : names) {
            try { return cc.getDeclaredMethod(n); } catch (Exception ignored) {}
        }
        return null;
    }
}
