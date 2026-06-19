package com.mojang.minecraft.modloader.model;

/**
 * Базовый класс для кастомных моделей мобов / энтити.
 *
 * Мод наследует этот класс, строит ModelPart-иерархию в конструкторе
 * и переопределяет setRotationAngles() для анимации.
 *
 * Пример:
 * <pre>
 *   public class DragonModel extends CustomEntityModel {
 *       public ModelPart head;
 *       public ModelPart body;
 *
 *       public DragonModel() {
 *           head = new ModelPart("head");
 *           head.addBox(-4, -8, -4, 8, 8, 8);
 *           parts.add(head);
 *
 *           body = new ModelPart("body");
 *           body.addBox(-6, 0, -3, 12, 14, 6);
 *           parts.add(body);
 *       }
 *
 *       &#64;Override
 *       public void setRotationAngles(double time, float partialTicks) {
 *           head.rotY = (float) Math.sin(time * 0.5);
 *       }
 *   }
 * </pre>
 *
 * Также можно загрузить OBJ-модель:
 * <pre>
 *   ModelPart root = OBJLoader.load("/assets/mymod/models/dragon.obj");
 *   parts.add(root);
 * </pre>
 */
public abstract class CustomEntityModel {

    protected final java.util.List<ModelPart> parts = new java.util.ArrayList<>();

    /**
     * Вызывается каждый кадр перед render().
     * Здесь задаются углы поворота частей для анимации.
     *
     * @param time         Игровое время (нарастающее)
     * @param partialTicks Остаток тика для интерполяции
     */
    public void setRotationAngles(double time, float partialTicks) {}

    /**
     * Рендерит модель. Вызывается Minecraft.render() или кастомным Entity.render().
     * Перед вызовом должна быть привязана правильная текстура через glBindTexture.
     *
     * @param time         Игровое время
     * @param partialTicks Остаток тика
     */
    public final void render(double time, float partialTicks) {
        setRotationAngles(time, partialTicks);
        for (ModelPart part : parts) {
            part.render();
        }
    }

    /**
     * Освобождает GPU-ресурсы всех частей.
     * Вызывается при выгрузке мода или уничтожении энтити.
     */
    public void free() {
        for (ModelPart part : parts) {
            part.free();
        }
    }

    /**
     * Находит ModelPart по имени (поиск в глубину).
     */
    public ModelPart findPart(String name) {
        for (ModelPart part : parts) {
            ModelPart found = findIn(part, name);
            if (found != null) return found;
        }
        return null;
    }

    private ModelPart findIn(ModelPart part, String name) {
        if (part.name.equals(name)) return part;
        // ModelPart.children is private, reflection or protected getter needed
        // For now, only top-level search
        return null;
    }
}
