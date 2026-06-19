package fancymodels;

import com.mojang.minecraft.Player;
import com.mojang.minecraft.level.Level;
import com.mojang.minecraft.modloader.IMod;
import com.mojang.minecraft.modloader.ModLoader;
import com.mojang.minecraft.modloader.logging.ModLogger;
import com.mojang.minecraft.modloader.model.CustomEntityModel;
import com.mojang.minecraft.modloader.model.ModelPart;
import com.mojang.minecraft.modloader.model.OBJLoader;
import org.lwjgl.BufferUtils;
import org.lwjgl.input.Keyboard;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.util.glu.GLU.gluBuild2DMipmaps;

public class FancyModels implements IMod {

    private static final File MODELS_DIR = new File("models");
    private final ModLogger log = ModLogger.forMod("fancymodels");
    private final Random random = new Random();

    private final List<File> objFiles = new ArrayList<>();
    private final Map<File, CustomEntityModel> modelCache = new HashMap<>();
    private final Map<File, Integer> textureCache = new HashMap<>();
    private final List<Spawned> spawned = new ArrayList<>();

    private Player playerRef;

    @Override public String getModId()   { return "fancymodels"; }
    @Override public String getName()    { return "FancyModels"; }
    @Override public String getVersion() { return "1.0.0"; }

    @Override
    public void onLoad(ModLoader loader) {
        if (!MODELS_DIR.exists()) {
            MODELS_DIR.mkdirs();
            log.info("Папка models/ создана — кидай туда .obj файлы");
        }
        refreshFileList();
        log.info("Найдено " + objFiles.size() + " .obj моделей. P рядом с игроком — спавн случайной.");
    }

    private void refreshFileList() {
        objFiles.clear();
        File[] files = MODELS_DIR.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".obj"));
        if (files != null) objFiles.addAll(Arrays.asList(files));
    }

    @Override
    public void onPlayerTick(Player player, Level level) {
        this.playerRef = player; // только так мод узнаёт позицию игрока в onKeyPress/onRenderPost
    }

    @Override
    public void onKeyPress(int key) {
        if (key != Keyboard.KEY_P || playerRef == null) return;

        refreshFileList(); // подхватываем новые файлы без перезапуска игры
        if (objFiles.isEmpty()) {
            log.warn("В models/ нет .obj файлов");
            return;
        }

        File chosen = objFiles.get(random.nextInt(objFiles.size()));
        try {
            CustomEntityModel model = modelCache.computeIfAbsent(chosen, this::loadModel);
            int texId = textureCache.computeIfAbsent(chosen, this::loadMatchingTexture); // -1 если нет png

            float ox = playerRef.x + (random.nextFloat() - 0.5f) * 4f;
            float oz = playerRef.z + (random.nextFloat() - 0.5f) * 4f;

            spawned.add(new Spawned(model, ox, playerRef.y, oz, texId));
            log.info("Заспавнена " + chosen.getName());
        } catch (Exception e) {
            log.error("Не удалось загрузить " + chosen.getName(), e);
        }
    }

    private CustomEntityModel loadModel(File file) {
        try {
            ModelPart root = OBJLoader.load(file.getPath()); // путь в FS — OBJLoader сам подхватит fallback
            CustomEntityModel m = new CustomEntityModel() {};
            m.parts.add(root); // parts protected — доступно из анонимного наследника
            return m;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /** Ищет текстуру рядом с моделью: sword.obj → sword.png. Textures.java умеет грузить только classpath, тут свой загрузчик под файловую систему. */
    private int loadMatchingTexture(File objFile) {
        String base = objFile.getName().replaceFirst("\\.obj$", "");
        File pngFile = new File(objFile.getParentFile(), base + ".png");
        if (!pngFile.exists()) return -1;

        try {
            BufferedImage img = ImageIO.read(pngFile);
            int w = img.getWidth(), h = img.getHeight();
            int[] pixels = new int[w * h];
            img.getRGB(0, 0, w, h, pixels, 0, w);
            for (int i = 0; i < pixels.length; i++) {
                int a = pixels[i] >> 24 & 0xFF, r = pixels[i] >> 16 & 0xFF,
                    g = pixels[i] >> 8 & 0xFF, b = pixels[i] & 0xFF;
                pixels[i] = a << 24 | b << 16 | g << 8 | r;
            }
            ByteBuffer buf = BufferUtils.createByteBuffer(w * h * 4);
            buf.asIntBuffer().put(pixels);

            int id = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, id);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            gluBuild2DMipmaps(GL_TEXTURE_2D, GL_RGBA, w, h, GL_RGBA, GL_UNSIGNED_BYTE, buf);
            return id;
        } catch (IOException e) {
            log.warn("Не удалось загрузить текстуру " + pngFile.getName());
            return -1;
        }
    }

    @Override
    public void onRenderPost(float partialTicks) {
        for (Spawned s : spawned) {
            glPushMatrix();
            glTranslatef(s.x, s.y, s.z);

            if (s.textureId >= 0) {
                glEnable(GL_TEXTURE_2D);
                glBindTexture(GL_TEXTURE_2D, s.textureId);
                glColor3f(1f, 1f, 1f);
            } else {
                glDisable(GL_TEXTURE_2D);
                glColor3f(0.6f, 0.8f, 1f); // голубым, если текстуры нет
            }

            s.model.render(System.currentTimeMillis() / 1000.0, partialTicks);
            glPopMatrix();
        }
        glEnable(GL_TEXTURE_2D);
        glColor3f(1f, 1f, 1f);
    }

    private static class Spawned {
        final CustomEntityModel model;
        final float x, y, z;
        final int textureId;
        Spawned(CustomEntityModel model, float x, float y, float z, int textureId) {
            this.model = model; this.x = x; this.y = y; this.z = z; this.textureId = textureId;
        }
    }
}
