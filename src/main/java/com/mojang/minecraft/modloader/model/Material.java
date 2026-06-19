package com.mojang.minecraft.modloader.model;

/**
 * Один материал из .mtl файла (формат Wavefront MTL).
 * Хранит цвета (ambient/diffuse/specular), параметры блеска/прозрачности
 * и путь к диффузной текстуре (map_Kd), если она указана.
 */
public class Material {

    public final String name;

    // Ka, Kd, Ks — цвета в формате RGB (0.0f..1.0f)
    public float[] ambient  = {0.2f, 0.2f, 0.2f};
    public float[] diffuse  = {0.8f, 0.8f, 0.8f};
    public float[] specular = {0.0f, 0.0f, 0.0f};

    // Ns — показатель блеска (specular exponent)
    public float specularExponent = 0.0f;

    // d / Tr — прозрачность (1.0 = непрозрачный)
    public float opacity = 1.0f;

    // map_Kd — относительный путь к файлу диффузной текстуры (как указан в .mtl)
    public String diffuseTexturePath = null;

    // GL texture id после загрузки через TextureLoader. -1 = текстура не загружена/нет.
    public int textureId = -1;

    public Material(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "Material{" + name
                + ", Kd=" + arr(diffuse)
                + ", map_Kd=" + diffuseTexturePath
                + ", textureId=" + textureId
                + "}";
    }

    private static String arr(float[] a) {
        return "(" + a[0] + ", " + a[1] + ", " + a[2] + ")";
    }
}
