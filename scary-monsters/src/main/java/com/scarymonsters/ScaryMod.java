package com.scarymonsters;

import com.scarymonsters.ability.Scheduler;
import com.scarymonsters.item.ScaryItem;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class ScaryMod implements ModInitializer {
    public static final String MOD_ID = "scarymonsters";

    // ---- simple "config" (edit and rebuild) ----
    /** Meteors / drill-like effects may destroy blocks. */
    public static final boolean BREAK_BLOCKS = false;
    /** If true every Form is available from the start (otherwise gated by XP level). */
    public static final boolean UNLOCK_ALL = false;
    /** XP levels needed for Form 1, 2, 3 and 4 (creative mode ignores this). */
    public static final int[] FORM_LEVELS = {0, 5, 15, 30};
    /** Passive buffs (speed, jump, strength...) while holding the item. */
    public static final boolean PASSIVE_BUFFS = true;
    /** Lifetime of summoned dinosaurs in ticks (20 ticks = 1 second). */
    public static final int MINION_LIFETIME = 900;

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        ModRegistry.init();
        Scheduler.init();

        PayloadTypeRegistry.playC2S().register(ScaryPayload.ID, ScaryPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(AnimPayload.ID, AnimPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(ScaryPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            ItemStack stack = ScaryItem.heldItem(player);
            if (stack.isEmpty()) return;
            if (payload.action() == ScaryPayload.SWITCH_FORM) {
                ScaryItem.cycleForm(player, stack);
            } else if (payload.action() == ScaryPayload.SWITCH_ATTACK) {
                ScaryItem.cycleAttack(player, stack);
            }
        });
    }
}
