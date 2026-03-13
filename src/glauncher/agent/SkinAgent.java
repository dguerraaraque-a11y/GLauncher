package glauncher.agent;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;

public class SkinAgent {

    public static void premain(String agentArgs, Instrumentation inst) {
        System.out.println("[GLauncher Skin Agent] Agente iniciado.");
        
        final String skinType = System.getProperty("glauncher.skin.type", "default");

        inst.addTransformer(new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                                    ProtectionDomain protectionDomain, byte[] classfileBuffer) {
                
                // Estos son nombres comunes para la clase del jugador en diferentes versiones.
                // 'bex' es para ~1.16, 'net/minecraft/client/player/AbstractClientPlayer' para versiones más nuevas con mappings.
                String[] targetClasses = { "bex", "net/minecraft/client/player/AbstractClientPlayer" };
                
                for (String targetClass : targetClasses) {
                    if (className.replace('.', '/').equals(targetClass)) {
                        try {
                            System.out.println("[GLauncher Skin Agent] Encontrada clase objetivo: " + className);
                            ClassPool cp = ClassPool.getDefault();
                            CtClass cc = cp.get(className.replace('/', '.'));

                            // --- Interceptar el método que obtiene el tipo de skin (slim/default) ---
                            // El nombre del método cambia con cada versión. 'getModelName' o 'm_104230_' son comunes.
                            CtMethod getModelMethod = findMethod(cc, new String[]{"getModelName", "m_104230_", "h"});
                            if (getModelMethod != null) {
                                System.out.println("[GLauncher Skin Agent] Interceptando método de modelo: " + getModelMethod.getName());
                                getModelMethod.setBody("{ return \"" + skinType + "\"; }");
                                System.out.println("[GLauncher Skin Agent] Modelo de skin forzado a: " + skinType);
                                return cc.toBytecode(); // Devolver la clase modificada
                            }
                            
                            cc.detach();
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            return null;
                        }
                    }
                }
                return null; // No transformar otras clases
            }
            
            private CtMethod findMethod(CtClass cc, String[] names) {
                for (String name : names) {
                    try {
                        // Buscamos un método sin parámetros
                        return cc.getDeclaredMethod(name, new CtClass[0]);
                    } catch (javassist.NotFoundException e) {
                        // Método no encontrado, probar el siguiente
                    }
                }
                return null;
            }
        });
    }
}
