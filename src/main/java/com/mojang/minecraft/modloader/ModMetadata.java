package com.mojang.minecraft.modloader.api;

/**
 * Rich metadata for a loaded mod.
 * Populated either from the IMod interface getters or from modinfo.json inside the jar.
 */
public class ModMetadata {

    public final String modId;
    public final String name;
    public final String version;
    public final String description;
    public final String author;
    public final String website;

    /** Whether the mod is enabled (can be toggled in the Mods GUI). */
    public boolean enabled = true;

    /** Source jar filename, set by ModLoader after loading. */
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

    /** Short constructor for mods that don't fill optional fields. */
    public ModMetadata(String modId, String name, String version) {
        this(modId, name, version, "", "Unknown", "");
    }

    @Override
    public String toString() {
        return name + " v" + version + " (" + modId + ")";
    }
}
