package net.nauq;

import net.fabricmc.loader.api.FabricLoader;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class NauqConfig {
    public static final float[] THRESHOLDS = {0.80f, 0.85f, 0.90f, 0.95f, 1.00f};

    public static boolean autoHit = false;   // oto vurma ac/kapa
    public static boolean sound = true;      // ses ac/kapa
    public static int soundIdx = 0;          // ses secimi
    public static float critThreshold = 0.90f; // saldiri barinin en az bu kadar dolmasi gerekir

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("nauq.properties");
    }

    public static void load() {
        Path f = file();
        if (!Files.exists(f)) { save(); return; }
        try (InputStream in = Files.newInputStream(f)) {
            Properties p = new Properties();
            p.load(in);
            autoHit = Boolean.parseBoolean(p.getProperty("autoHit", "false"));
            sound = Boolean.parseBoolean(p.getProperty("sound", "true"));
            soundIdx = Math.max(0, Integer.parseInt(p.getProperty("soundIdx", "0")));
            critThreshold = Float.parseFloat(p.getProperty("critThreshold", "0.90"));
        } catch (Throwable ignored) {}
    }

    public static void save() {
        try (OutputStream out = Files.newOutputStream(file())) {
            Properties p = new Properties();
            p.setProperty("autoHit", String.valueOf(autoHit));
            p.setProperty("sound", String.valueOf(sound));
            p.setProperty("soundIdx", String.valueOf(soundIdx));
            p.setProperty("critThreshold", String.valueOf(critThreshold));
            p.store(out, "Nauq config");
        } catch (Throwable ignored) {}
    }
}
