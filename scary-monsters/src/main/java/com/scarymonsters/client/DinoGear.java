package com.scarymonsters.client;

import com.scarymonsters.ScaryMod;
import com.scarymonsters.ability.Anims;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;

/**
 * The dinosaur body parts (tail, snout, claws, spikes, scales) drawn over the player.
 * Each group of parts follows the matching vanilla player part, so everything moves with
 * the player's own animations (walking, sneaking, swinging...).
 * Geometry comes from the generated {@link GearParts}; this class shows the right parts for the Form and animates tail and jaw.
 */
public class DinoGear {
    public static final EntityModelLayer LAYER = new EntityModelLayer(ScaryMod.id("dino_gear"), "main");

    private final ModelPart gHead, gBody, gRArm, gLArm, gRLeg, gLLeg;
    private final ModelPart[] visParts;
    private final ModelPart[] tailA, tailB, jaws;

    public DinoGear(ModelPart root) {
        gHead = root.getChild("g_head");
        gBody = root.getChild("g_body");
        gRArm = root.getChild("g_rarm");
        gLArm = root.getChild("g_larm");
        gRLeg = root.getChild("g_rleg");
        gLLeg = root.getChild("g_lleg");
        visParts = resolve(root, GearParts.VIS_PATH);
        tailA = resolve(root, new String[]{"g_body/tailA_1", "g_body/tailA_1/tailA_2", "g_body/tailA_1/tailA_2/tailA_3"});
        tailB = resolve(root, new String[]{"g_body/tailB_1", "g_body/tailB_1/tailB_2", "g_body/tailB_1/tailB_2/tailB_3"});
        jaws = resolve(root, new String[]{"g_head/jaw_a", "g_head/jaw_b", "g_head/jaw_c"});
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData data = new ModelData();
        GearParts.build(data.getRoot());
        return TexturedModelData.of(data, 128, 128);
    }

    private static ModelPart[] resolve(ModelPart root, String[] paths) {
        ModelPart[] out = new ModelPart[paths.length];
        for (int i = 0; i < paths.length; i++) {
            ModelPart p = root;
            for (String s : paths[i].split("/")) p = p.getChild(s);
            out[i] = p;
        }
        return out;
    }

    private static float env(float t, float in, float out) {
        return MathHelper.clamp(Math.min(t / in, (1f - t) / out), 0f, 1f);
    }

    /** @param t progress of the running pose (0..1) or -1 */
    public void pose(int form, float age, float limbAngle, float limbDist, int anim, float t) {
        for (int i = 0; i < visParts.length; i++) {
            visParts[i].visible = form >= GearParts.VIS_LO[i] && form <= GearParts.VIS_HI[i];
        }
        boolean playing = anim != Anims.IDLE && t >= 0f;
        float bell = playing ? MathHelper.sin(t * (float) Math.PI) : 0f;

        // ---- tail
        float sway = MathHelper.sin(age * 0.1f) * 0.12f + MathHelper.sin(limbAngle * 0.6662f) * 0.3f * limbDist;
        for (ModelPart[] chain : new ModelPart[][]{tailA, tailB}) {
            for (int i = 0; i < chain.length; i++) {
                ModelPart part = chain[i];
                part.resetTransform();
                part.yaw += sway * (0.6f + 0.5f * i);
                part.pitch += MathHelper.sin(age * 0.07f + i) * 0.04f;
                if (!playing) continue;
                switch (anim) {
                    case Anims.WHIP -> part.yaw += (t * 2f - 1f) * (1.1f + 0.4f * i) * 1.6f;
                    case Anims.POUNCE -> part.pitch += 0.5f * env(t, 0.15f, 0.5f);
                    case Anims.RUSH -> {
                        part.yaw *= 0.2f;
                        part.pitch += 0.12f;
                    }
                    case Anims.STOMP -> part.pitch += t < 0.4f ? 0.7f * (t / 0.4f) : -0.5f * (1f - t);
                    case Anims.ROAR, Anims.DOOM, Anims.SUMMON -> part.pitch += 0.4f * env(t, 0.2f, 0.2f);
                    default -> {}
                }
            }
        }

        // ---- jaw
        float open = 0.05f + 0.04f * MathHelper.sin(age * 0.12f);
        if (playing) {
            switch (anim) {
                case Anims.BITE -> open = t < 0.35f ? 0.95f * (t / 0.35f) : Math.max(0f, 0.95f * (1f - (t - 0.35f) / 0.12f));
                case Anims.ROAR -> open = 0.95f * env(t, 0.1f, 0.15f);
                case Anims.DOOM -> open = 0.85f * env(t, 0.1f, 0.25f);
                case Anims.SUMMON -> open = 0.55f * env(t, 0.15f, 0.25f);
                case Anims.CLAW, Anims.POUNCE, Anims.RUSH -> open = 0.35f * bell;
                default -> {}
            }
        }
        for (ModelPart j : jaws) {
            j.resetTransform();
            j.pitch += open;
        }
    }

    public void render(MatrixStack m, VertexConsumer vc, int light, PlayerEntityModel<?> pm) {
        int ov = OverlayTexture.DEFAULT_UV;
        m.push();
        pm.head.rotate(m);
        gHead.render(m, vc, light, ov, -1);
        m.pop();

        m.push();
        pm.body.rotate(m);
        gBody.render(m, vc, light, ov, -1);
        m.pop();

        m.push();
        pm.rightArm.rotate(m);
        gRArm.render(m, vc, light, ov, -1);
        m.pop();

        m.push();
        pm.leftArm.rotate(m);
        gLArm.render(m, vc, light, ov, -1);
        m.pop();

        m.push();
        pm.rightLeg.rotate(m);
        gRLeg.render(m, vc, light, ov, -1);
        m.pop();

        m.push();
        pm.leftLeg.rotate(m);
        gLLeg.render(m, vc, light, ov, -1);
        m.pop();
    }
}
