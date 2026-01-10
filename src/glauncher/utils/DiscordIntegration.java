package glauncher.utils;

import club.minnced.discord.rpc.DiscordEventHandlers;
import club.minnced.discord.rpc.DiscordRPC;
import club.minnced.discord.rpc.DiscordRichPresence;

public class DiscordIntegration {
    private static final String APP_ID = "1455766995374047344";
    private static DiscordRPC lib;
    private static Thread callbackThread;
    private static boolean running = false;
    private static long startTime = System.currentTimeMillis() / 1000; // Tiempo de inicio para "Elapsed"
    private static boolean showTime = true;
    private static String lastDetails = "En el Menú Principal";
    private static String lastState = "GLauncher";

    public static void start() {
        if (running) return;
        
        try {
            lib = DiscordRPC.INSTANCE;
            DiscordEventHandlers handlers = new DiscordEventHandlers();
            handlers.ready = (user) -> System.out.println("Discord RPC Conectado como " + user.username);
            handlers.disconnected = (code, message) -> System.out.println("Discord RPC Desconectado: " + message);
            handlers.errored = (code, message) -> System.out.println("Discord RPC Error: " + message);
            
            lib.Discord_Initialize(APP_ID, handlers, true, "");
            
            running = true;
            callbackThread = new Thread(() -> {
                while (running && !Thread.currentThread().isInterrupted()) {
                    lib.Discord_RunCallbacks();
                    try { Thread.sleep(2000); } catch (InterruptedException e) {}
                }
            });
            callbackThread.setDaemon(true);
            callbackThread.start();
            
            refresh();
        } catch (Throwable t) {
            System.err.println("Discord RPC no disponible (Falta librería java-discord-rpc en /lib): " + t.getMessage());
            running = false;
        }
    }

    public static void stop() {
        if (!running) return;
        running = false;
        if (lib != null) {
            lib.Discord_ClearPresence();
            lib.Discord_Shutdown();
        }
        if (callbackThread != null) callbackThread.interrupt();
    }

    public static void setShowTime(boolean show) {
        showTime = show;
        if (running) refresh();
    }

    public static void update(String details, String state) {
        lastDetails = details;
        lastState = state;
        if (running) refresh();
    }

    private static void refresh() {
        if (lib == null) return;
        DiscordRichPresence presence = new DiscordRichPresence();
        if (showTime) presence.startTimestamp = startTime;
        presence.details = lastDetails;
        presence.state = lastState;
        presence.largeImageKey = "logo"; // Asegúrate de subir una imagen llamada 'logo' en Art Assets
        presence.largeImageText = "GLauncher v1.0";
        
        lib.Discord_UpdatePresence(presence);
    }
}
