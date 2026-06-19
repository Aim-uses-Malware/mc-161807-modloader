package com.mojang.minecraft.modloader;

/**
 * Метаданные мода — имя, версия, описание, автор.
 * Возвращается через IMod.getMetadata().
 */
public class ModMetadata {

    public final String modId;
    public final String name;
    public final String version;
    public final String description;
    public final String author;
    public final String website;

    /** Включён ли мод (можно переключать через GUI). */
    public boolean enabled = true;

    /** Имя jar-файла, устанавливается ModLoader'ом. */
    public String sourceFile = "";

    public ModMetadata(String modId, String name, String version,
                       String description, String author, String website) {
        this.modId       = modId;
        this.name        = name;
        this.version     = version;
        this.description = description;
        this.author      = author;
        this.website     = website;
    }

    public ModMetadata(String modId, String name, String version) {
        this(modId, name, version, "", "Unknown", "");
    }

    @Override
    public String toString() {
        return name + " v" + version + " (" + modId + ")"
               + (author.isEmpty() ? "" : " by " + author);
    }
}
