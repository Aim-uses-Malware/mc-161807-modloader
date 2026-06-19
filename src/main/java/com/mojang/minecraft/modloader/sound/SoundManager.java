package com.mojang.minecraft.modloader.sound;

import java.io.*;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javax.sound.sampled.*;

/**
 * Простой звуковой движок для модов.
 *
 * Поддерживает .wav файлы из classpath или файловой системы.
 * Звуки регистрируются по имени, потом воспроизводятся:
 *
 * <pre>
 *   SoundManager.register("mymod:explosion", "/assets/mymod/sounds/explosion.wav");
 *   SoundManager.play("mymod:explosion");
 *   SoundManager.play("mymod:explosion", 0.8f); // с громкостью
 * </pre>
 *
 * Реализован через javax.sound.sampled (встроено в JRE) — без доп. зависимостей.
 */
public class SoundManager {

    /** Кэш загруженных буферов. */
    private static final Map<String, byte[]> soundData  = new HashMap<>();
    /** Кэш форматов. */
    private static final Map<String, AudioFormat> formats = new HashMap<>();

    /** Глобальная громкость (0.0–1.0). */
    public static float masterVolume = 1.0f;

    private SoundManager() {}

    // ─── Registration ─────────────────────────────────────────────────────────

    /**
     * Регистрирует звук. Загружает и кэширует данные немедленно.
     *
     * @param name   Уникальное имя, например "mymod:plop"
     * @param path   Путь в classpath ("/assets/mymod/sounds/plop.wav")
     *               или файловой системе ("sounds/plop.wav")
     */
    public static boolean register(String name, String path) {
        try {
            InputStream is = SoundManager.class.getResourceAsStream(path);
            if (is == null) {
                File f = new File(path.startsWith("/") ? path.substring(1) : path);
                if (!f.exists()) {
                    log("Sound file not found: " + path);
                    return false;
                }
                is = new FileInputStream(f);
            }

            AudioInputStream ais = AudioSystem.getAudioInputStream(new BufferedInputStream(is));
            AudioFormat fmt = ais.getFormat();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int read;
            while ((read = ais.read(buf)) != -1) baos.write(buf, 0, read);
            ais.close();

            soundData.put(name, baos.toByteArray());
            formats.put(name, fmt);
            log("Registered sound: " + name);
            return true;
        } catch (Exception e) {
            log("[ERROR] Failed to register sound '" + name + "': " + e.getMessage());
            return false;
        }
    }

    // ─── Playback ─────────────────────────────────────────────────────────────

    /**
     * Воспроизводит звук по имени в отдельном потоке.
     *
     * @param name   Имя звука
     * @param volume Громкость 0.0–1.0
     */
    public static void play(String name, float volume) {
        byte[] data = soundData.get(name);
        AudioFormat fmt = formats.get(name);
        if (data == null || fmt == null) {
            log("[WARN] Sound not registered: " + name);
            return;
        }

        final float v = Math.max(0f, Math.min(1f, volume * masterVolume));
        final byte[] d = data;
        final AudioFormat f = fmt;

        new Thread(() -> {
            try {
                InputStream is = new ByteArrayInputStream(d);
                AudioInputStream ais = new AudioInputStream(is, f, d.length / f.getFrameSize());
                SourceDataLine line = AudioSystem.getSourceDataLine(f);
                line.open(f);

                // Установить громкость через FloatControl
                if (line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                    FloatControl gain = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
                    float dB = v > 0 ? (float) (20.0 * Math.log10(v)) : gain.getMinimum();
                    gain.setValue(Math.max(gain.getMinimum(), dB));
                }

                line.start();
                byte[] buf = new byte[4096];
                int bytesRead;
                while ((bytesRead = ais.read(buf)) != -1) {
                    line.write(buf, 0, bytesRead);
                }
                line.drain();
                line.close();
                ais.close();
            } catch (Exception e) {
                log("[ERROR] Playback failed for '" + name + "': " + e.getMessage());
            }
        }, "SoundThread-" + name).start();
    }

    /** Воспроизводит звук на полной громкости. */
    public static void play(String name) {
        play(name, 1.0f);
    }

    /** Воспроизводит звук с 3D-затуханием (упрощённо, по расстоянию). */
    public static void playAt(String name, double dx, double dy, double dz, float maxDist) {
        float dist = (float) Math.sqrt(dx*dx + dy*dy + dz*dz);
        float vol  = Math.max(0f, 1f - dist / maxDist);
        play(name, vol);
    }

    public static boolean isRegistered(String name) {
        return soundData.containsKey(name);
    }

    private static void log(String msg) {
        System.out.println("[SoundManager] " + msg);
    }
}
