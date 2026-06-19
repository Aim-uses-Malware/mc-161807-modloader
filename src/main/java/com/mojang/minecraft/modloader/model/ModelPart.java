package com.mojang.minecraft.modloader.model;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;

/**
 * Универсальная часть 3D-модели с поддержкой OBJ-вершин, UV и дочерних частей.
 * Моды могут строить произвольные скелетные модели из ModelPart-ов.
 */
public class ModelPart {

    public final String name;

    // Transform
    public float x, y, z;
    public float rotX, rotY, rotZ;
    public float scaleX = 1f, scaleY = 1f, scaleZ = 1f;

    private final List<float[]> vertices = new ArrayList<>();  // x,y,z,u,v per vertex
    private final List<int[]>   faces    = new ArrayList<>();  // triangle indices (v0,v1,v2)
    private final List<ModelPart> children = new ArrayList<>();

    /** Отдельный Display List (GL11). -1 = не скомпилирован. */
    private int displayList = -1;
    private boolean dirty   = true;

    public ModelPart(String name) {
        this.name = name;
    }

    // ─── Geometry builders ────────────────────────────────────────────────────

    /** Добавляет вершину с UV. */
    public ModelPart addVertex(float x, float y, float z, float u, float v) {
        vertices.add(new float[]{x, y, z, u, v});
        dirty = true;
        return this;
    }

    /** Добавляет треугольник по индексам вершин. */
    public ModelPart addTriangle(int v0, int v1, int v2) {
        faces.add(new int[]{v0, v1, v2});
        dirty = true;
        return this;
    }

    /**
     * Быстрый хелпер: добавляет прямоугольную грань (quad → 2 треугольника).
     * v0-v3 — 4 вершины по часовой стрелке.
     */
    public ModelPart addQuad(float x0,float y0,float z0,float u0,float v0,
                             float x1,float y1,float z1,float u1,float v1,
                             float x2,float y2,float z2,float u2,float v2,
                             float x3,float y3,float z3,float u3,float v3) {
        int base = vertices.size();
        addVertex(x0,y0,z0,u0,v0);
        addVertex(x1,y1,z1,u1,v1);
        addVertex(x2,y2,z2,u2,v2);
        addVertex(x3,y3,z3,u3,v3);
        addTriangle(base, base+1, base+2);
        addTriangle(base, base+2, base+3);
        return this;
    }

    /** Добавляет box (Cube-аналог из оригинального ZombieModel). */
    public ModelPart addBox(float minX, float minY, float minZ,
                            int sizeX,  int sizeY,  int sizeZ) {
        float maxX = minX + sizeX;
        float maxY = minY + sizeY;
        float maxZ = minZ + sizeZ;

        // Top
        addQuad(minX,maxY,maxZ, 0,0, maxX,maxY,maxZ, 1,0,
                maxX,maxY,minZ, 1,1, minX,maxY,minZ, 0,1);
        // Bottom
        addQuad(minX,minY,minZ, 0,1, maxX,minY,minZ, 1,1,
                maxX,minY,maxZ, 1,0, minX,minY,maxZ, 0,0);
        // Front (+Z)
        addQuad(minX,maxY,maxZ, 0,0, minX,minY,maxZ, 0,1,
                maxX,minY,maxZ, 1,1, maxX,maxY,maxZ, 1,0);
        // Back (-Z)
        addQuad(maxX,maxY,minZ, 0,0, maxX,minY,minZ, 0,1,
                minX,minY,minZ, 1,1, minX,maxY,minZ, 1,0);
        // Right (+X)
        addQuad(maxX,maxY,maxZ, 0,0, maxX,minY,maxZ, 0,1,
                maxX,minY,minZ, 1,1, maxX,maxY,minZ, 1,0);
        // Left (-X)
        addQuad(minX,maxY,minZ, 0,0, minX,minY,minZ, 0,1,
                minX,minY,maxZ, 1,1, minX,maxY,maxZ, 1,0);
        return this;
    }

    /** Добавляет дочернюю часть. */
    public ModelPart addChild(ModelPart child) {
        children.add(child);
        return this;
    }

    // ─── Rendering ────────────────────────────────────────────────────────────

    /**
     * Рендерит эту часть и всех детей.
     * Перед вызовом должна быть привязана текстура.
     */
    public void render() {
        glPushMatrix();

        glTranslatef(x, y, z);
        if (rotZ != 0) glRotatef((float) Math.toDegrees(rotZ), 0, 0, 1);
        if (rotY != 0) glRotatef((float) Math.toDegrees(rotY), 0, 1, 0);
        if (rotX != 0) glRotatef((float) Math.toDegrees(rotX), 1, 0, 0);
        if (scaleX != 1f || scaleY != 1f || scaleZ != 1f)
            glScalef(scaleX, scaleY, scaleZ);

        drawGeometry();

        for (ModelPart child : children) {
            child.render();
        }

        glPopMatrix();
    }

    private void drawGeometry() {
        if (vertices.isEmpty() || faces.isEmpty()) return;

        if (dirty || displayList == -1) {
            if (displayList != -1) glDeleteLists(displayList, 1);
            displayList = glGenLists(1);
            glNewList(displayList, GL_COMPILE);
            glBegin(GL_TRIANGLES);
            for (int[] face : faces) {
                for (int idx : face) {
                    float[] v = vertices.get(idx);
                    glTexCoord2f(v[3], v[4]);
                    glVertex3f(v[0], v[1], v[2]);
                }
            }
            glEnd();
            glEndList();
            dirty = false;
        }

        glCallList(displayList);
    }

    /** Освобождает GPU-ресурсы. */
    public void free() {
        if (displayList != -1) {
            glDeleteLists(displayList, 1);
            displayList = -1;
        }
        for (ModelPart child : children) child.free();
    }

    public void setPosition(float x, float y, float z) {
        this.x = x; this.y = y; this.z = z;
    }
    public void setRotation(float rx, float ry, float rz) {
        this.rotX = rx; this.rotY = ry; this.rotZ = rz;
    }
}
