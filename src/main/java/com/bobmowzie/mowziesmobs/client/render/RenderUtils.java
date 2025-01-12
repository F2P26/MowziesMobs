package com.bobmowzie.mowziesmobs.client.render;

import com.ilexiconn.llibrary.client.model.tools.AdvancedModelRenderer;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.renderer.Matrix4f;
import net.minecraft.client.renderer.Quaternion;
import net.minecraft.client.renderer.Vector4f;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

public class RenderUtils {
    public static void matrixStackFromModel(MatrixStack matrixStack, AdvancedModelRenderer modelRenderer) {
        AdvancedModelRenderer parent = modelRenderer.getParent();
        if (parent != null) matrixStackFromModel(matrixStack, parent);
        modelRenderer.translateRotate(matrixStack);
    }

    public static Vec3d getWorldPosFromModel(Entity entity, float entityYaw, AdvancedModelRenderer modelRenderer) {
        MatrixStack matrixStack = new MatrixStack();
        matrixStack.translate(entity.getPosX(), entity.getPosY(), entity.getPosZ());
        matrixStack.rotate(new Quaternion(0, -entityYaw + 180, 0, true));
        matrixStack.scale(-1, -1, 1);
        matrixStack.translate(0, -1.5f, 0);
        RenderUtils.matrixStackFromModel(matrixStack, modelRenderer);
        MatrixStack.Entry matrixEntry = matrixStack.getLast();
        Matrix4f matrix4f = matrixEntry.getMatrix();

        Vector4f vec = new Vector4f(0, 0, 0, 1);
        vec.transform(matrix4f);
        return new Vec3d(vec.getX(), vec.getY(), vec.getZ());
    }
}
