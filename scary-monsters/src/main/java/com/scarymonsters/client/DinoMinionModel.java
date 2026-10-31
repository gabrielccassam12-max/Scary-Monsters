package com.scarymonsters.client;

import com.scarymonsters.ScaryMod;
import com.scarymonsters.entity.DinoMinionEntity;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;

/** Walk cycle, tail sway, breathing and a snapping jaw for both the Raptor and the Rex. */
public class DinoMinionModel extends EntityModel<DinoMinionEntity> {
    public static final EntityModelLayer RAPTOR_LAYER = new EntityModelLayer(ScaryMod.id("raptor"), "main");
    public static final EntityModelLayer REX_LAYER = new EntityModelLayer(ScaryMod.id("rex"), "main");

    private final ModelPart root, body, neck, head, jaw, tail1, tail2, tail3, armR, armL;
    private final ModelPart thighR, shinR, footR, thighL, shinL, footL;
    private final ModelPart[] animated;

    public DinoMinionModel(ModelPart root) {
        this.root = root;
        body = root.getChild("body");
        neck = find(root, "body/neck");
        head = find(root, "body/neck/head");
        jaw = find(root, "body/neck/head/jaw");
        tail1 = find(root, "body/tail1");
        tail2 = find(root, "body/tail1/tail2");
        tail3 = find(root, "body/tail1/tail2/tail3");
        armR = find(root, "body/arm_r");
        armL = find(root, "body/arm_l");
        thighR = find(root, "body/thigh_r");
        shinR = find(root, "body/thigh_r/shin_r");
        footR = find(root, "body/thigh_r/shin_r/foot_r");
        thighL = find(root, "body/thigh_l");
        shinL = find(root, "body/thigh_l/shin_l");
        footL = find(root, "body/thigh_l/shin_l/foot_l");
        animated = new ModelPart[]{body, neck, head, jaw, tail1, tail2, tail3, armR, armL,
                thighR, shinR, footR, thighL, shinL, footL};
    }

    public static TexturedModelData raptorData() {
        ModelData data = new ModelData();
        RaptorParts.build(data.getRoot());
        return TexturedModelData.of(data, 128, 128);
    }

    public static TexturedModelData rexData() {
        ModelData data = new ModelData();
        RexParts.build(data.getRoot());
        return TexturedModelData.of(data, 128, 128);
    }

    private static ModelPart find(ModelPart root, String path) {
        ModelPart p = root;
        for (String s : path.split("/")) p = p.getChild(s);
        return p;
    }

    private static void swingLeg(ModelPart thigh, ModelPart shin, ModelPart foot, float s) {
        thigh.pitch += s * 0.8f;
        shin.pitch += -Math.max(0f, s) * 0.9f + Math.min(0f, s) * 0.2f;
        foot.pitch += Math.max(0f, s) * 0.6f;
    }

    @Override
    public void setAngles(DinoMinionEntity e, float limbAngle, float limbDistance, float age, float headYaw, float headPitch) {
        for (ModelPart p : animated) p.resetTransform();
        float rad = (float) (Math.PI / 180.0);

        // walk cycle
        float swing = limbAngle * 0.6662f;
        float amp = Math.min(1f, limbDistance * 1.6f);
        swingLeg(thighR, shinR, footR, MathHelper.cos(swing) * amp);
        swingLeg(thighL, shinL, footL, MathHelper.cos(swing + (float) Math.PI) * amp);
        body.pivotY -= Math.abs(MathHelper.sin(swing)) * 0.9f * amp;
        body.pitch += MathHelper.sin(swing * 2f) * 0.04f * amp + MathHelper.sin(age * 0.1f) * 0.02f;

        // head follows the target
        head.yaw += MathHelper.clamp(headYaw, -40f, 40f) * rad * 0.8f;
        head.pitch += MathHelper.clamp(headPitch, -30f, 30f) * rad * 0.6f;
        neck.pitch += MathHelper.sin(swing * 2f) * 0.05f * amp;

        // tail balances the body
        float wag = MathHelper.sin(age * 0.12f) * 0.14f + MathHelper.sin(swing) * 0.25f * amp;
        tail1.yaw += wag * 0.6f;
        tail2.yaw += wag * 0.9f;
        tail3.yaw += wag * 1.2f;
        tail1.pitch += MathHelper.sin(swing * 2f) * 0.05f * amp;

        // little arms
        armR.pitch += MathHelper.sin(age * 0.11f) * 0.12f;
        armL.pitch += MathHelper.sin(age * 0.11f + 1.5f) * 0.12f;

        // jaw: snaps while attacking, breathes otherwise
        if (e.isAttacking()) {
            jaw.pitch += 0.55f + 0.35f * MathHelper.sin(age * 0.7f);
            body.pitch += 0.12f * MathHelper.sin(age * 0.7f);
        } else {
            jaw.pitch += 0.05f + 0.03f * MathHelper.sin(age * 0.1f);
        }
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, int color) {
        root.render(matrices, vertices, light, overlay, color);
    }
}
