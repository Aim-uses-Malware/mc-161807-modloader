package fancymodels;

import com.mojang.minecraft.Player;
import com.mojang.minecraft.level.Level;
import com.mojang.minecraft.modloader.IMod;
import com.mojang.minecraft.modloader.ModLoader;
import com.mojang.minecraft.modloader.logging.ModLogger;
import com.mojang.minecraft.modloader.model.CustomEntityModel;
import com.mojang.minecraft.modloader.model.Material;
import com.mojang.minecraft.modloader.model.ModelPart;
import com.mojang.minecraft.modloader.model.MtlLoader;
import com.mojang.minecraft.modloader.model.OBJLoader;
import com.mojang.minecraft.modloader.model.TextureLoader;
import org.lwjgl.input.Keyboard;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static org.lwjgl.opengl.GL11.*;

/**
 * FancyModels — спавнит случайную .obj модель из папки models/ рядом с игроком по нажатию P.
 *
 * Текстуры теперь грузятся через общие апишки лоадера моделей (MtlLoader/TextureLoader),
 * те же самые, которыми OBJLoader сам читает mtllib/usemtl внутри .obj. Порядок поиска:
 *
 *  1) model.obj -> model.mtl уже подхвачен САМИМ OBJLoader'ом через строку "mtllib" в .obj —
 *     в этом случае текстуры уже висят на конкретных частях модели (ModelPart), отдельно
 *     грузить ничего не нужно. Проверяем это через ModelPart.hasOwnOrChildTexture().
 *  2) Если в .obj не было mtllib (модель без материалов) — ищем по соглашению об именах:
 *     a) model.obj -> model.png / model.jpg / model.jpeg (прямое совпадение имени)
 *     b) model.obj -> model.mtl -> map_Kd <файл> (тот же .mtl, но без ссылки из .obj)
 *  Если ничего не найдено — модель рендерится плоским голубым цветом без текстуры.
 */
public class FancyModels implements IMod {

    private static final File MODELS_DIR = new File("models");

    /** Масштаб моделей. OBJ-файлы бывают в любых единицах — подбери под свои модели. */
    private static final float MODEL_SCALE = 0.15f;

    /** Насколько выше игрока (по Y, в блоках) спавнить модель. */
    private static final float SPAWN_HEIGHT_OFFSET = 1.5f;

    private final ModLogger log = ModLogger.forMod("fancymodels");
    private final Random random = new Random();

    private final List<File> objFiles = new ArrayList<>();
    private final Map<File, CustomEntityModel> modelCache = new HashMap<>();
    private final Map<File, Integer> textureCache = new HashMap<>();
    /** true, если модель сама несёт свои текстуры (из mtllib внутри .obj) — см. loadModel(). */
    private final Map<File, Boolean> modelHasOwnMaterials = new HashMap<>();
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
            boolean hasOwnMaterials = modelHasOwnMaterials.getOrDefault(chosen, false);

            // Если модель уже сама несёт текстуры (из mtllib), глобальный textureId не нужен —
            // только если её НЕТ, пробуем найти текстуру по соглашению об именах.
            int texId = hasOwnMaterials ? -1 : textureCache.computeIfAbsent(chosen, this::loadMatchingTexture);

            float ox = (float) (playerRef.x + (random.nextFloat() - 0.5f) * 4f);
            float oz = (float) (playerRef.z + (random.nextFloat() - 0.5f) * 4f);
            float oy = (float) playerRef.y + SPAWN_HEIGHT_OFFSET;

            spawned.add(new Spawned(model, ox, oy, oz, texId, hasOwnMaterials));
            log.info("Заспавнена " + chosen.getName()
                    + (hasOwnMaterials ? " (материалы из .obj)" : texId >= 0 ? " (с текстурой)" : " (без текстуры)"));
        } catch (Exception e) {
            log.error("Не удалось загрузить " + chosen.getName(), e);
        }
    }

    private CustomEntityModel loadModel(File file) {
        try {
            ModelPart root = OBJLoader.load(file.getPath()); // путь в FS — OBJLoader сам подхватит mtllib, если он есть
            root.scaleX = root.scaleY = root.scaleZ = MODEL_SCALE;

            // OBJLoader уже разобрал mtllib/usemtl (если они были в файле) и развесил
            // текстуры по конкретным ModelPart. Запоминаем это для onKeyPress/onRenderPost.
            modelHasOwnMaterials.put(file, root.hasOwnOrChildTexture());

            return new CustomEntityModel() {
                { parts.add(root); } // код внутри тела подкласса — protected parts доступен
            };
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // ─── Текстуры (фоллбэк для моделей без mtllib внутри .obj) ──────────────

    private int loadMatchingTexture(File objFile) {
        File dir = objFile.getParentFile();
        String base = objFile.getName().replaceFirst("\\.obj$", "");

        // a) Прямое совпадение имени: model.obj -> model.png/.jpg/.jpeg
        File direct = findImageByBaseName(dir, base);
        if (direct != null) {
            return tryLoadTexture(direct.getPath());
        }

        // b) Через .mtl, который .obj сам не ссылается (нет mtllib) — берём первый
        //    материал с map_Kd через тот же MtlLoader, которым пользуется OBJLoader.
        File mtl = new File(dir, base + ".mtl");
        if (mtl.exists()) {
            Material material = firstMaterialWithTexture(mtl);
            if (material != null && material.diffuseTexturePath != null) {
                File texFile = new File(dir, material.diffuseTexturePath);
                if (texFile.exists()) {
                    return tryLoadTexture(texFile.getPath());
                }
                log.warn("В " + mtl.getName() + " указана текстура " + material.diffuseTexturePath
                        + ", но файла нет рядом с моделью");
            } else {
                log.warn(mtl.getName() + " не содержит материала с map_Kd");
            }
        }

        log.warn("Текстура для " + objFile.getName() + " не найдена (искал " + base
                + ".png/.jpg/.jpeg и через " + base + ".mtl) — спавню без текстуры");
        return -1;
    }

    private File findImageByBaseName(File dir, String base) {
        for (String ext : new String[]{".png", ".jpg", ".jpeg"}) {
            File f = new File(dir, base + ext);
            if (f.exists()) return f;
        }
        return null;
    }

    /** Берёт первый материал из .mtl, у которого задана diffuse-текстура (map_Kd). */
    private Material firstMaterialWithTexture(File mtlFile) {
        try (InputStream in = new FileInputStream(mtlFile)) {
            Map<String, Material> materials = MtlLoader.load(in);
            for (Material m : materials.values()) {
                if (m.diffuseTexturePath != null) return m;
            }
        } catch (IOException e) {
            log.warn("Не удалось прочитать " + mtlFile.getName() + ": " + e.getMessage());
        }
        return null;
    }

    /** Грузит файл изображения с диска в OpenGL-текстуру через общий TextureLoader. */
    private int tryLoadTexture(String path) {
        try {
            int id = TextureLoader.load(path);
            log.info("Текстура загружена: " + path);
            return id;
        } catch (IOException e) {
            log.warn("Не удалось загрузить текстуру " + path + ": " + e.getMessage());
            return -1;
        }
    }

    // ─── Рендер ─────────────────────────────────────────────────────────────

    @Override
    public void onRenderPost(float partialTicks) {
        for (Spawned s : spawned) {
            glPushMatrix();
            glTranslatef(s.x, s.y, s.z);

            if (s.textureId >= 0) {
                // Глобальная текстура по соглашению об именах (модель без mtllib).
                glEnable(GL_TEXTURE_2D);
                glBindTexture(GL_TEXTURE_2D, s.textureId);
                glColor3f(1f, 1f, 1f);
            } else if (s.hasOwnMaterials) {
                // У модели свои текстуры на конкретных ModelPart (из mtllib) —
                // НЕ гасим GL_TEXTURE_2D и не перекрашиваем, иначе перекроем то,
                // что каждая часть сама забиндит при рендере.
                glEnable(GL_TEXTURE_2D);
                glColor3f(1f, 1f, 1f);
            } else {
                glDisable(GL_TEXTURE_2D);
                glColor3f(0.6f, 0.8f, 1f); // голубым, если текстуры нет вообще никакой
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
        final boolean hasOwnMaterials;
        Spawned(CustomEntityModel model, float x, float y, float z, int textureId, boolean hasOwnMaterials) {
            this.model = model; this.x = x; this.y = y; this.z = z;
            this.textureId = textureId; this.hasOwnMaterials = hasOwnMaterials;
        }
    }
}
