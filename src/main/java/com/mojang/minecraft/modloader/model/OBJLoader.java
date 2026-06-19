package com.mojang.minecraft.modloader.model;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Загружает .obj файлы (Wavefront) из classpath или файловой системы в ModelPart.
 *
 * Поддерживается:
 *  - вершины (v x y z)
 *  - UV-координаты (vt u v)
 *  - нормали (vn) — игнорируются, так как движок использует flat shading
 *  - грани (f v/vt v/vt v/vt) — триангуляция квадов автоматически
 *  - группы объектов (o, g) → каждая группа = отдельный ModelPart-ребёнок
 *  - материалы (mtllib имя.mtl, usemtl ИмяМатериала) → текстура из .mtl
 *    применяется к ModelPart-ребёнку, созданному на момент смены материала
 *
 * Материалы и их diffuse-текстуры (map_Kd) резолвятся относительно той же
 * директории, где лежит .obj файл (и тем же способом — classpath или файловая
 * система, в зависимости от того, как был загружен сам .obj).
 *
 * Если .obj грузится через load(InputStream, debugName) без явного пути,
 * mtllib не может быть резолвлен (неизвестна базовая директория) — в этом
 * случае текстуры материалов не загружаются, об этом пишется в лог,
 * остальная геометрия парсится как обычно.
 *
 * Использование из мода:
 * <pre>
 *   ModelPart root = OBJLoader.load("/assets/mymod/models/custom_entity.obj");
 * </pre>
 */
public class OBJLoader {

    private OBJLoader() {}

    /**
     * Загружает .obj из classpath (начиная с '/') или из файловой системы.
     *
     * @param path путь к файлу, например "/assets/mymod/models/sword.obj"
     * @return корневой ModelPart, который содержит все group-ы как детей
     */
    public static ModelPart load(String path) throws IOException {
        InputStream stream = OBJLoader.class.getResourceAsStream(path);
        boolean classpath = stream != null;
        if (stream == null) {
            File f = new File(path.startsWith("/") ? path.substring(1) : path);
            if (!f.exists()) throw new IOException("OBJ not found: " + path);
            stream = new FileInputStream(f);
        }

        int slash = path.lastIndexOf('/');
        String baseDir = slash >= 0 ? path.substring(0, slash + 1) : "";

        return parse(stream, path, baseDir, classpath);
    }

    /**
     * Загружает .obj из произвольного InputStream.
     * mtllib внутри файла НЕ будет резолвлен — нет базовой директории, чтобы найти .mtl рядом.
     */
    public static ModelPart load(InputStream stream, String debugName) throws IOException {
        return parse(stream, debugName, null, false);
    }

    // ─── Parser ───────────────────────────────────────────────────────────────

    private static ModelPart parse(InputStream in, String name, String baseDir, boolean classpath) throws IOException {
        ModelPart root = new ModelPart("root@" + name);

        List<float[]> verts = new ArrayList<>();  // x,y,z
        List<float[]> uvs   = new ArrayList<>();  // u,v

        Map<String, Material> materials = null;   // заполняется по mtllib
        Material currentMaterial = null;           // заполняется по usemtl
        String currentGroupName = "default";

        ModelPart current = new ModelPart(currentGroupName);
        root.addChild(current);

        // Temp lists for current group
        List<float[]> partVerts = new ArrayList<>();
        List<int[]>   partTris  = new ArrayList<>();

        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        String line;

        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;

            String[] tokens = line.split("\\s+");
            switch (tokens[0]) {
                case "v": {
                    verts.add(new float[]{
                        Float.parseFloat(tokens[1]),
                        Float.parseFloat(tokens[2]),
                        Float.parseFloat(tokens[3])
                    });
                    break;
                }
                case "vt": {
                    uvs.add(new float[]{
                        Float.parseFloat(tokens[1]),
                        tokens.length > 2 ? Float.parseFloat(tokens[2]) : 0f
                    });
                    break;
                }
                case "mtllib": {
                    if (baseDir == null) {
                        System.err.println("[OBJLoader] mtllib встречен при загрузке из InputStream без пути — "
                                + "текстуры материалов не будут загружены: " + line);
                        break;
                    }
                    if (tokens.length < 2) break;
                    String mtlFile = tokens[1];
                    try {
                        materials = loadMtl(baseDir, mtlFile, classpath);
                    } catch (IOException e) {
                        System.err.println("[OBJLoader] Не удалось загрузить mtllib '" + mtlFile + "': " + e.getMessage());
                    }
                    break;
                }
                case "usemtl": {
                    String matName = tokens.length > 1 ? tokens[1] : null;
                    currentMaterial = (materials != null && matName != null) ? materials.get(matName) : null;
                    if (matName != null && currentMaterial == null && materials != null) {
                        System.err.println("[OBJLoader] usemtl '" + matName + "' не найден в загруженном mtllib");
                    }

                    // Завершаем текущую часть, начинаем новую — со своей текстурой
                    flush(current, partVerts, partTris);
                    partVerts.clear(); partTris.clear();

                    String partName = currentGroupName + (matName != null ? ("@" + matName) : "");
                    current = new ModelPart(partName);
                    if (currentMaterial != null) {
                        current.setTexture(currentMaterial.textureId);
                    }
                    root.addChild(current);
                    break;
                }
                case "o":
                case "g": {
                    // Flush current group
                    flush(current, partVerts, partTris);
                    partVerts.clear(); partTris.clear();

                    currentGroupName = tokens.length > 1 ? tokens[1] : "group";
                    current = new ModelPart(currentGroupName);
                    // Если usemtl уже был объявлен раньше (нестандартный, но валидный порядок) —
                    // унаследуем текущий материал на новую группу.
                    if (currentMaterial != null) {
                        current.setTexture(currentMaterial.textureId);
                    }
                    root.addChild(current);
                    break;
                }
                case "f": {
                    // tokens[1..n] are face vertices
                    // Each can be: v, v/vt, v/vt/vn, v//vn
                    int n = tokens.length - 1;
                    int[] faceVIndices = new int[n];

                    for (int i = 0; i < n; i++) {
                        String[] parts = tokens[i + 1].split("/");
                        int vIdx  = Integer.parseInt(parts[0]) - 1;   // 1-based → 0-based
                        int uvIdx = (parts.length > 1 && !parts[1].isEmpty())
                                    ? Integer.parseInt(parts[1]) - 1 : -1;

                        float[] pos = verts.get(vIdx);
                        float u = uvIdx >= 0 && uvIdx < uvs.size() ? uvs.get(uvIdx)[0] : 0f;
                        float v = uvIdx >= 0 && uvIdx < uvs.size() ? uvs.get(uvIdx)[1] : 0f;

                        faceVIndices[i] = partVerts.size();
                        partVerts.add(new float[]{pos[0], pos[1], pos[2], u, v});
                    }

                    // Fan triangulation (works for convex polygons)
                    for (int i = 1; i < n - 1; i++) {
                        partTris.add(new int[]{faceVIndices[0], faceVIndices[i], faceVIndices[i+1]});
                    }
                    break;
                }
            }
        }

        reader.close();

        // Flush last group
        flush(current, partVerts, partTris);

        return root;
    }

    private static void flush(ModelPart part, List<float[]> verts, List<int[]> tris) {
        for (float[] v : verts) {
            part.addVertex(v[0], v[1], v[2], v[3], v[4]);
        }
        for (int[] t : tris) {
            part.addTriangle(t[0], t[1], t[2]);
        }
    }

    // ─── Material loading ───────────────────────────────────────────────────────

    /**
     * Загружает .mtl относительно той же директории, что и .obj, и сразу
     * подгружает в видеопамять все текстуры (map_Kd), на которые материалы ссылаются.
     */
    private static Map<String, Material> loadMtl(String baseDir, String mtlFileName, boolean preferClasspath) throws IOException {
        String fullPath = baseDir + mtlFileName;

        InputStream stream = preferClasspath ? OBJLoader.class.getResourceAsStream(fullPath) : null;
        if (stream == null) {
            File f = new File(fullPath.startsWith("/") ? fullPath.substring(1) : fullPath);
            if (f.exists()) {
                stream = new FileInputStream(f);
            }
        }
        if (stream == null) {
            // на случай, если .obj грузился из файловой системы, а .mtl лежит в classpath (или наоборот)
            stream = OBJLoader.class.getResourceAsStream(fullPath);
        }
        if (stream == null) {
            throw new IOException("MTL not found: " + fullPath);
        }

        Map<String, Material> materials;
        try {
            materials = MtlLoader.load(stream);
        } finally {
            stream.close();
        }

        for (Material mat : materials.values()) {
            if (mat.diffuseTexturePath == null) continue;
            String texPath = baseDir + mat.diffuseTexturePath;
            try {
                mat.textureId = TextureLoader.load(texPath);
            } catch (IOException e) {
                System.err.println("[OBJLoader] Не удалось загрузить текстуру '" + texPath
                        + "' для материала '" + mat.name + "': " + e.getMessage());
            }
        }

        return materials;
    }
}
