package com.mojang.minecraft.modloader.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Загрузчик материалов Wavefront .mtl.
 *
 * Вызывается из {@link OBJLoader}, когда в .obj встречается строка
 * "mtllib имя.mtl". Сам по себе не грузит текстуры в OpenGL — только
 * парсит текст и отдаёт путь к текстуре (map_Kd) как строку.
 * Загрузка PNG/JPG в GL текстуру делается {@link TextureLoader} —
 * это вызывает уже сам OBJLoader после получения карты материалов.
 *
 * Поддерживаемые директивы .mtl:
 *   newmtl <name>      — начало нового материала
 *   Ka r g b           — ambient color
 *   Kd r g b           — diffuse color
 *   Ks r g b           — specular color
 *   Ns <float>         — specular exponent
 *   d <float>          — opacity (1.0 = непрозрачно)
 *   Tr <float>         — альтернативная запись прозрачности (Tr = 1 - d)
 *   map_Kd <path>      — путь к файлу диффузной текстуры
 *
 * Неизвестные директивы (illum, Ni, map_Bump, map_Ks и т.д.) пропускаются —
 * для базового рендера с одной diffuse-текстурой они не нужны.
 */
public class MtlLoader {

    private MtlLoader() {}

    /**
     * Загружает .mtl из InputStream и возвращает карту имя -> Material.
     * Порядок материалов сохраняется (LinkedHashMap), как в исходном файле.
     */
    public static Map<String, Material> load(InputStream in) throws IOException {
        Map<String, Material> materials = new LinkedHashMap<>();
        Material current = null;

        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        String line;
        int lineNo = 0;

        while ((line = reader.readLine()) != null) {
            lineNo++;
            line = stripComment(line).trim();
            if (line.isEmpty()) continue;

            String[] tokens = line.split("\\s+");
            String key = tokens[0];

            try {
                switch (key) {
                    case "newmtl": {
                        String name = line.substring(key.length()).trim();
                        current = new Material(name);
                        materials.put(name, current);
                        break;
                    }
                    case "Ka":
                        requireCurrent(current, lineNo);
                        current.ambient = parseColor(tokens);
                        break;
                    case "Kd":
                        requireCurrent(current, lineNo);
                        current.diffuse = parseColor(tokens);
                        break;
                    case "Ks":
                        requireCurrent(current, lineNo);
                        current.specular = parseColor(tokens);
                        break;
                    case "Ns":
                        requireCurrent(current, lineNo);
                        current.specularExponent = Float.parseFloat(tokens[1]);
                        break;
                    case "d":
                        requireCurrent(current, lineNo);
                        current.opacity = Float.parseFloat(tokens[1]);
                        break;
                    case "Tr":
                        requireCurrent(current, lineNo);
                        current.opacity = 1.0f - Float.parseFloat(tokens[1]);
                        break;
                    case "map_Kd": {
                        requireCurrent(current, lineNo);
                        // путь может содержать опции (-s, -o и т.п.) перед именем файла —
                        // берём последний токен строки как имя файла
                        current.diffuseTexturePath = tokens[tokens.length - 1];
                        break;
                    }
                    default:
                        // illum, Ni, map_Ks, map_Bump, map_d, bump и т.д. — пропускаем
                        break;
                }
            } catch (NumberFormatException nfe) {
                System.err.println("[MtlLoader] Не удалось распарсить строку " + lineNo + ": \"" + line + "\" — пропускаю");
            }
        }

        return materials;
    }

    private static void requireCurrent(Material current, int lineNo) throws IOException {
        if (current == null) {
            throw new IOException("Строка " + lineNo + ": атрибут материала встречен до 'newmtl'");
        }
    }

    private static float[] parseColor(String[] tokens) {
        if (tokens.length < 4) {
            return new float[]{0.8f, 0.8f, 0.8f};
        }
        float r = Float.parseFloat(tokens[1]);
        float g = Float.parseFloat(tokens[2]);
        float b = Float.parseFloat(tokens[3]);
        return new float[]{r, g, b};
    }

    private static String stripComment(String line) {
        int idx = line.indexOf('#');
        return idx >= 0 ? line.substring(0, idx) : line;
    }
}
