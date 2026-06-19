package com.mojang.minecraft.modloader;

import java.io.*;
import java.util.Properties;

/**
 * Простой персистентный конфиг мода.
 * Хранится в config/<modId>.properties.
 * Получают через ModLoader.getConfig("mymodid").
 */
public class ModConfig {

    private final String     modId;
    private final File       configFile;
    private final Properties props = new Properties();

    public ModConfig(String modId) {
        this.modId = modId;
        File dir = new File("config");
        dir.mkdirs();
        this.configFile = new File(dir, modId + ".properties");
        load();
    }

    private void load() {
        if (!configFile.exists()) return;
        try (InputStream in = new FileInputStream(configFile)) {
            props.load(in);
        } catch (IOException e) {
            System.err.println("[ModConfig] Failed to load config for " + modId + ": " + e.getMessage());
        }
    }

    public void save() {
        try (OutputStream out = new FileOutputStream(configFile)) {
            props.store(out, "Config for mod: " + modId);
        } catch (IOException e) {
            System.err.println("[ModConfig] Failed to save config for " + modId + ": " + e.getMessage());
        }
    }

    public String getString(String key, String def) { return props.getProperty(key, def); }
    public void setString(String key, String value) { props.setProperty(key, value); }

    public int getInt(String key, int def) {
        try { return Integer.parseInt(props.getProperty(key, String.valueOf(def))); }
        catch (NumberFormatException e) { return def; }
    }
    public void setInt(String key, int value) { props.setProperty(key, String.valueOf(value)); }

    public boolean getBoolean(String key, boolean def) {
        String v = props.getProperty(key);
        if (v == null) return def;
        return v.equalsIgnoreCase("true") || v.equals("1");
    }
    public void setBoolean(String key, boolean value) { props.setProperty(key, value ? "true" : "false"); }

    public float getFloat(String key, float def) {
        try { return Float.parseFloat(props.getProperty(key, String.valueOf(def))); }
        catch (NumberFormatException e) { return def; }
    }
    public void setFloat(String key, float value) { props.setProperty(key, String.valueOf(value)); }
}
