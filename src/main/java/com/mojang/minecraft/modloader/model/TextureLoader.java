package com.mojang.minecraft.modloader.model;

import static org.lwjgl.opengl.GL11.*;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import org.lwjgl.BufferUtils;

/**
 * Грузит PNG/JPG в OpenGL текстуру через "сырой" GL11 — так же, как остальной
 * рендер в этом проекте (display lists в ModelPart). Используется OBJLoader'ом
 * для текстур, на которые ссылается .mtl (map_Kd).
 *
 * Кэширует по пути к файлу: если несколько материалов ссылаются на одну и ту же
 * текстуру (частый случай для моделей мобов), она грузится в видеопамять один раз.
 */
public class TextureLoader {

    private static final Map<String, Integer> cache = new HashMap<>();

    private TextureLoader() {}

    /**
     * Грузит текстуру из classpath (если ресурс есть) или из файловой системы.
     * Должно вызываться из основного GL-треда (как и весь остальной рендер-код).
     *
     * @param path путь к файлу текстуры, например "/assets/mymod/models/dragon.png"
     * @return GL texture id, готовый для glBindTexture(GL_TEXTURE_2D, id)
     */
    public static int load(String path) throws IOException {
        Integer cached = cache.get(path);
        if (cached != null) return cached;

        BufferedImage img = readImage(path);
        int id = upload(img);
        cache.put(path, id);
        return id;
    }

    private static BufferedImage readImage(String path) throws IOException {
        InputStream in = TextureLoader.class.getResourceAsStream(path);
        if (in == null) {
            File f = new File(path.startsWith("/") ? path.substring(1) : path);
            if (!f.exists()) throw new IOException("Texture not found: " + path);
            in = new FileInputStream(f);
        }
        try (InputStream stream = in) {
            BufferedImage img = ImageIO.read(stream);
            if (img == null) throw new IOException("Не удалось декодировать изображение: " + path);
            return img;
        }
    }

    private static int upload(BufferedImage img) {
        int width = img.getWidth();
        int height = img.getHeight();

        ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * 4);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = img.getRGB(x, y);
                buffer.put((byte) ((argb >> 16) & 0xFF)); // R
                buffer.put((byte) ((argb >> 8) & 0xFF));  // G
                buffer.put((byte) (argb & 0xFF));         // B
                buffer.put((byte) ((argb >> 24) & 0xFF)); // A
            }
        }
        buffer.flip();

        int id = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, id);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);
        return id;
    }

    /** Удаляет все загруженные текстуры из видеопамяти и чистит кэш. Звать при выгрузке мода. */
    public static void freeAll() {
        for (int id : cache.values()) {
            glDeleteTextures(id);
        }
        cache.clear();
    }
}
