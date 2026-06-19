package com.mojang.minecraft.modloader.event;

/** Базовый класс всех событий. */
public abstract class ModEvent {
    /** Тип события для удобного toString. */
    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}
