# mc-161807 ModLoader v2

**Модлоадер для Minecraft Pre-Classic mc-161807 (RubyDung era)**

Форк оригинальной [thecodeofnotch/mc-161807](https://github.com/thecodeofnotch/mc-161807) с полноценной системой модификаций.

---

## Возможности

| Функция | Статус |
|---------|--------|
| Загрузка .jar модов из папки `mods/` | ✅ |
| Регистрация кастомных блоков | ✅ |
| Регистрация кастомных предметов | ✅ |
| Инвентарь игрока (36 слотов + хотбар) | ✅ |
| Система крафта (shaped + shapeless) | ✅ |
| Биомы (кастомная генерация поверхности) | ✅ |
| Структуры (размещение при генерации) | ✅ |
| Звуки (.wav через javax.sound) | ✅ |
| 3D-модели из OBJ файлов | ✅ |
| Кастомные ModelPart-иерархии | ✅ |
| EventBus (typed events) | ✅ |
| Хуки рендера (pre/post/HUD) | ✅ |
| Конфиги модов (config/<modId>.properties) | ✅ |
| Метаданные мода (ModMetadata) | ✅ |
| Спринт, двойной прыжок (Player) | ✅ |
| Здоровье игрока | ✅ |
| Логирование (ModLogger) | ✅ |

---

## Быстрый старт

### 1. Создать мод

```java
package mymod;

import com.mojang.minecraft.modloader.IMod;
import com.mojang.minecraft.modloader.ModLoader;

public class MyMod implements IMod {

    @Override public String getModId()   { return "mymod"; }
    @Override public String getName()    { return "My Mod"; }
    @Override public String getVersion() { return "1.0.0"; }

    @Override
    public void onLoad(ModLoader loader) {
        System.out.println("Hello from MyMod!");
    }
}
```

### 2. Упаковать в .jar и положить в `mods/`

```
mods/
  MyMod-1.0.0.jar
  AnotherMod.jar
```

### 3. Запустить игру — ModLoader найдёт и загрузит моды автоматически

---

## Регистрация блоков

```java
// ID блока: 10-255 (0-9 зарезервированы ванилью)
Tile myBlock = new Tile(20, 17); // id=20, textureSlot=17 в terrain.png
loader.registerTile(myBlock);

// Кастомная логика блока
Tile glassBlock = new Tile(21, 49) {
    @Override
    public boolean blocksLight() { return false; } // Пропускает свет

    @Override
    public boolean isSolid() { return true; }
};
loader.registerTile(glassBlock);
```

---

## Регистрация предметов

```java
// ID предмета: 256-4095
Item ruby = new Item(300, "Ruby");
loader.registerItem(ruby);

// Инструмент с прочностью
Item drillItem = new Item(301, "Diamond Drill") {
    { maxDurability = 1000; maxStackSize = 1; }

    @Override
    public boolean isTool() { return true; }

    @Override
    public boolean onUse(ItemStack stack, Player player, Level level) {
        System.out.println("Drill activated!");
        return false;
    }
};
loader.registerItem(drillItem);
```

---

## Рецепты крафта

```java
CraftingManager crafting = loader.getCraftingManager();

// Бесформенный (shapeless) — любой порядок
crafting.addShapeless(
    new ItemStack(myItem, 1),
    ruby, Item.stick
);

// Произвольный IRecipe
crafting.addRecipe(myCustomRecipe);
```

---

## 3D-модели

### Из OBJ файла

```java
import com.mojang.minecraft.modloader.model.OBJLoader;
import com.mojang.minecraft.modloader.model.ModelPart;

// Загрузить .obj из jar-ресурсов мода
ModelPart myModel = OBJLoader.load("/assets/mymod/models/robot.obj");

// Рендер (вызывать в onRenderPost или в Entity.render())
glBindTexture(GL_TEXTURE_2D, myTextureId);
myModel.render();

// Освободить GPU-ресурсы когда не нужно
myModel.free();
```

### Программная ModelPart-иерархия

```java
import com.mojang.minecraft.modloader.model.ModelPart;

ModelPart body = new ModelPart("body");
body.addBox(-4f, 0f, -2f, 8, 12, 4);

ModelPart head = new ModelPart("head");
head.addBox(-4f, -8f, -4f, 8, 8, 8);
head.setPosition(0, -12, 0); // Относительно body

body.addChild(head);

// Анимация
head.rotY = (float) Math.sin(time * 0.5); // Вращение головой

// Рендер
body.render();
```

### Кастомная модель энтити

```java
import com.mojang.minecraft.modloader.model.CustomEntityModel;

public class DragonModel extends CustomEntityModel {
    public ModelPart head;
    public ModelPart body;
    public ModelPart wing;

    public DragonModel() {
        body = new ModelPart("body");
        body.addBox(-6f, 0f, -3f, 12, 14, 6);
        parts.add(body);

        head = new ModelPart("head");
        head.addBox(-4f, -8f, -4f, 8, 8, 8);
        head.setPosition(0, 0, -8);
        body.addChild(head);

        // Загрузить крыло из OBJ
        try {
            wing = OBJLoader.load("/assets/mymod/models/dragon_wing.obj");
            wing.setPosition(6, 2, 0);
            parts.add(wing);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setRotationAngles(double time, float pt) {
        head.rotY = (float) Math.sin(time * 0.4) * 0.5f;
        wing.rotZ = (float) Math.sin(time * 2.0) * 0.6f;
    }
}
```

---

## EventBus

```java
import com.mojang.minecraft.modloader.event.EventBus;
import com.mojang.minecraft.modloader.event.Events;

// Подписка на событие
EventBus.subscribe(Events.TileBreakEvent.class, e -> {
    System.out.println("Сломан: " + e.tile + " на " + e.x + "," + e.y + "," + e.z);
    // Отмена (блок не ломается)
    // e.setCancelled(true);
});

EventBus.subscribe(Events.PlayerDeathEvent.class, e -> {
    e.setCancelled(true); // Бессмертие!
});

EventBus.subscribe(Events.RenderHudEvent.class, e -> {
    // Рисовать GL в ortho-режиме
});
```

### Все события

| Событие | Cancellable | Описание |
|---------|-------------|----------|
| `PlayerTickEvent` | ❌ | Каждый тик игрока |
| `PlayerJumpEvent` | ❌ | Прыжок |
| `PlayerDeathEvent` | ✅ | Смерть |
| `KeyPressEvent` | ❌ | Клавиша |
| `TileBreakEvent` | ✅ | Ломание блока |
| `TilePlaceEvent` | ✅ | Установка блока |
| `TileRandomTickEvent` | ❌ | Случайный тик |
| `EntitySpawnEvent` | ❌ | Спавн зомби |
| `EntityRemoveEvent` | ❌ | Удаление зомби |
| `EntityTickEvent` | ❌ | Тик зомби |
| `LevelGeneratedEvent` | ❌ | Генерация мира |
| `LevelLoadedEvent` | ❌ | Загрузка мира |
| `LevelSaveEvent` | ✅ | Сохранение мира |
| `RenderPreEvent` | ❌ | До рендера |
| `RenderPostEvent` | ❌ | После рендера |
| `RenderHudEvent` | ❌ | HUD (ortho) |

---

## Хуки IMod

```java
// Рендер-хуки
void onRenderPre(float partialTicks)   // До рендера сцены
void onRenderPost(float partialTicks)  // После рендера, до HUD
void onRenderHud(int w, int h, float pt) // В ortho-режиме HUD

// Мировые
void onLevelGenerated(Level level)
void onLevelLoaded(Level level)
boolean onLevelSave(Level level)  // false = отменить сохранение

// Блоки
void onTileDestroy(Tile, Level, x, y, z)
void onTilePlace(Tile, Level, x, y, z)
void onTileRandomTick(Level, x, y, z, Tile)

// Энтити
void onEntitySpawn(Zombie, Level)
void onEntityTick(Zombie, Level)
void onEntityRemove(Zombie, Level)

// Игрок
void onPlayerTick(Player, Level)
void onPlayerJump(Player)
void onKeyPress(int key) // LWJGL key code
```

---

## Звуки

```java
// Регистрация (в onLoad)
loader.registerSound("mymod:explosion", "/assets/mymod/sounds/explosion.wav");

// Воспроизведение
SoundManager.play("mymod:explosion");
SoundManager.play("mymod:explosion", 0.5f); // С громкостью

// 3D-звук с затуханием по расстоянию
SoundManager.playAt("mymod:explosion", dx, dy, dz, 32f); // maxDist = 32 блока
```

---

## Биомы и структуры

```java
// Биом
Biome snowBiome = new Biome("snow", 0xffffff) {
    @Override public int getSurfaceTile()    { return Tile.rock.id; }
    @Override public int getSubSurfaceTile() { return Tile.rock.id; }
    @Override public float getTreeChance()   { return 0.0f; }
};
loader.registerBiome(snowBiome);

// Структура
Structure obelisk = new Structure("obelisk")
    .pillar(0, 0, 0, 8, Tile.stoneBrick.id)
    .layer(0, -1, -1, 1, 1, Tile.rock.id); // Основание
loader.registerStructure(obelisk, 0.003f);
```

---

## Конфиги

```java
ModConfig cfg = loader.getConfig(getModId());

// Чтение
int speed    = cfg.getInt("speed", 100);
boolean god  = cfg.getBoolean("godmode", false);
String name  = cfg.getString("username", "Player");

// Запись и сохранение
cfg.setInt("speed", 120);
cfg.save(); // Записывает в config/mymodid.properties
```

---

## Сборка

```bash
git clone https://github.com/Aim-uses-Malware/mc-161807-modloader
cd mc-161807-modloader
./gradlew run
```

Моды кладём в папку `mods/` (создаётся автоматически).

---

## Структура проекта

```
src/main/java/com/mojang/minecraft/
├── Minecraft.java              — Главный класс (game loop, рендер)
├── Player.java                 — Игрок + Inventory + спринт + здоровье
├── Entity.java                 — Базовый класс энтити
├── biome/
│   └── Biome.java             — Биомы
├── character/
│   ├── Zombie.java
│   └── ZombieModel.java
├── crafting/
│   └── CraftingManager.java   — Система крафта
├── inventory/
│   └── Inventory.java         — Инвентарь 36 слотов
├── item/
│   ├── Item.java              — Базовый предмет
│   └── ItemStack.java         — Стак предметов
├── level/
│   ├── Level.java
│   └── tile/
│       └── Tile.java
├── modloader/
│   ├── IMod.java              — Интерфейс мода (v2)
│   ├── ModLoader.java         — Загрузчик + регистратор
│   ├── ModMetadata.java       — Метаданные
│   ├── ModConfig.java         — Конфиги
│   ├── event/
│   │   ├── EventBus.java      — Шина событий
│   │   ├── Events.java        — Все события
│   │   └── CancellableEvent.java
│   ├── model/
│   │   ├── ModelPart.java     — 3D-часть модели
│   │   ├── OBJLoader.java     — Загрузчик .obj файлов
│   │   └── CustomEntityModel.java
│   └── sound/
│       └── SoundManager.java  — Звуковой движок
├── particle/
│   ├── Particle.java
│   └── ParticleEngine.java
└── structure/
    └── Structure.java         — Структуры мира
```

---

*Форк mc-161807 · Minecraft Pre-Classic modding · by AimRite2YT*
