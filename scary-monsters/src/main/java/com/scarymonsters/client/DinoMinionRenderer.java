package com.scarymonsters.client;

import com.scarymonsters.entity.DinoMinionEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class DinoMinionRenderer extends MobEntityRenderer<DinoMinionEntity, DinoMinionModel> {
    private final Identifier texture;
    private final float scale;

    public DinoMinionRenderer(EntityRendererFactory.Context ctx, EntityModelLayer layer, Identifier texture,
                              float scale, float shadow) {
        super(ctx, new DinoMinionModel(ctx.getPart(layer)), shadow);
        this.texture = texture;
        this.scale = scale;
    }

    @Override
    protected void scale(DinoMinionEntity entity, MatrixStack matrices, float amount) {
        matrices.scale(scale, scale, scale);
    }

    @Override
    public Identifier getTexture(DinoMinionEntity entity) {
        return texture;
    }
}
