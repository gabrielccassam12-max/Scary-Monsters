package com.scarymonsters.client;

import com.scarymonsters.ScaryMod;
import com.scarymonsters.item.ScaryItem;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.EntityModelLoader;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

/** Draws the dinosaur gear on any player that is currently holding Scary Monsters. */
public class DinoGearFeature extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {
    private static final Identifier TEXTURE = ScaryMod.id("textures/entity/dino_gear.png");
    private final DinoGear gear;

    public DinoGearFeature(FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> context,
                           EntityModelLoader loader) {
        super(context);
        this.gear = new DinoGear(loader.getModelPart(DinoGear.LAYER));
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vcp, int light, AbstractClientPlayerEntity player,
                       float limbAngle, float limbDistance, float tickDelta, float animationProgress,
                       float headYaw, float headPitch) {
        int form = ScaryItem.activeForm(player);
        if (form == 0 || player.isInvisible() || player.isSpectator()) return;

        int id = player.getId();
        gear.pose(form, animationProgress, limbAngle, limbDistance,
                ClientAnims.anim(id), ClientAnims.progress(id, tickDelta));
        gear.render(matrices, vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(TEXTURE)), light, getContextModel());
    }
}
