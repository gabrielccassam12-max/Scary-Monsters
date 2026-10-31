package com.scarymonsters.client;

import com.scarymonsters.AnimPayload;
import com.scarymonsters.ModRegistry;
import com.scarymonsters.ScaryMod;
import com.scarymonsters.ScaryPayload;
import com.scarymonsters.ability.ScaryAttack;
import com.scarymonsters.item.ScaryItem;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class ScaryClient implements ClientModInitializer {
    public static KeyBinding switchForm;
    public static KeyBinding switchAttack;

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void onInitializeClient() {
        switchForm = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.scarymonsters.switch_form",
                InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_V, "key.categories.scarymonsters"));
        switchAttack = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.scarymonsters.switch_attack",
                InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_B, "key.categories.scarymonsters"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (switchForm.wasPressed()) ClientPlayNetworking.send(new ScaryPayload(ScaryPayload.SWITCH_FORM));
            while (switchAttack.wasPressed()) ClientPlayNetworking.send(new ScaryPayload(ScaryPayload.SWITCH_ATTACK));
        });

        // models
        EntityModelLayerRegistry.registerModelLayer(DinoGear.LAYER, DinoGear::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(DinoMinionModel.RAPTOR_LAYER, DinoMinionModel::raptorData);
        EntityModelLayerRegistry.registerModelLayer(DinoMinionModel.REX_LAYER, DinoMinionModel::rexData);

        // minion renderers
        EntityRendererRegistry.register(ModRegistry.RAPTOR, ctx -> new DinoMinionRenderer(ctx,
                DinoMinionModel.RAPTOR_LAYER, ScaryMod.id("textures/entity/dino_raptor.png"), 1.0f, 0.5f));
        EntityRendererRegistry.register(ModRegistry.REX, ctx -> new DinoMinionRenderer(ctx,
                DinoMinionModel.REX_LAYER, ScaryMod.id("textures/entity/dino_rex.png"), 1.45f, 1.4f));

        // the transformation: dino gear drawn on every player that holds the item
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((type, renderer, helper, ctx) -> {
            if (renderer instanceof PlayerEntityRenderer playerRenderer) {
                helper.register(new DinoGearFeature((net.minecraft.client.render.entity.feature.FeatureRendererContext) playerRenderer,
                        ctx.getModelLoader()));
            }
        });

        // attack poses coming from the server
        ClientPlayNetworking.registerGlobalReceiver(AnimPayload.ID,
                (payload, context) -> ClientAnims.set(payload.entityId(), payload.anim()));

        // item icon changes with the Form (see models/item/scary_monsters.json overrides)
        ModelPredicateProviderRegistry.register(ModRegistry.SCARY_MONSTERS, ScaryMod.id("form"),
                (stack, world, entity, seed) -> ScaryItem.getForm(stack) / 10f);

        HudRenderCallback.EVENT.register(ScaryClient::renderHud);
    }

    private static void renderHud(DrawContext ctx, RenderTickCounter tick) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) return;
        ItemStack stack = ScaryItem.heldItem(mc.player);
        if (stack.isEmpty()) return;

        int form = ScaryItem.getForm(stack);
        ScaryAttack selected = ScaryItem.getAttack(stack);
        List<ScaryAttack> list = ScaryAttack.forForm(form);

        int x = 10;
        int lineH = 11;
        int h = 18 + list.size() * lineH;
        int y = ctx.getScaledWindowHeight() - h - 40;
        int w = 150;

        ctx.fill(x - 4, y - 4, x + w, y + h - 2, 0x90000000);
        ctx.fill(x - 4, y - 4, x - 2, y + h - 2, 0xFF7CB342);
        ctx.drawTextWithShadow(mc.textRenderer, Text.translatable("hud.scarymonsters.form", form,
                Text.translatable("form.scarymonsters." + form)), x + 2, y, 0xFF9CCC65);
        for (int i = 0; i < list.size(); i++) {
            ScaryAttack a = list.get(i);
            boolean sel = a == selected;
            Text name = Text.literal(sel ? "> " : "  ").append(a.displayName());
            ctx.drawTextWithShadow(mc.textRenderer, name, x + 2, y + 14 + i * lineH, sel ? 0xFFFFE57A : 0xFF9A9A9A);
        }
    }
}
