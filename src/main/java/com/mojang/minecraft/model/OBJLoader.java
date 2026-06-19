package com.mojang.minecraft.modloader.model;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Загружает .obj файлы (Wavefront) из classpath или файловой системы в ModelPart.
 *
 * Поддерживается:
 *  - вершины (v x y z)
 *  - UV-координаты (vt u v)
 *  - нормали (vn) — игнорируются, так как движок использует flat shading
 *  - грани (f v/vt v/vt v/vt) — триангуляция квадов автоматически
 *  - группы объектов (o, g) → каждая группа = отдельный ModelPart-ребёнок
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
        if (stream == null) {
            File f = new File(path.startsWith("/") ? path.substring(1) : path);
            if (!f.exists()) throw new IOException("OBJ not found: " + path);
            stream = new FileInputStream(f);
        }
        return parse(stream, path);
    }

    /**
     * Загружает .obj из произвольного InputStream.
     */
    public static ModelPart load(InputStream stream, String debugName) throws IOException {
        return parse(stream, debugName);
    }

    // ─── Parser ───────────────────────────────────────────────────────────────

    private static ModelPart parse(InputStream in, String name) throws IOException {
        ModelPart root = new ModelPart("root@" + name);

        List<float[]> verts = new ArrayList<>();  // x,y,z
        List<float[]> uvs   = new ArrayList<>();  // u,v

        ModelPart current = new ModelPart("default");
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
                case "o":
                case "g": {
                    // Flush current group
                    flush(current, partVerts, partTris);
                    partVerts.clear(); partTris.clear();

                    String groupName = tokens.length > 1 ? tokens[1] : "group";
                    current = new ModelPart(groupName);
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
}
