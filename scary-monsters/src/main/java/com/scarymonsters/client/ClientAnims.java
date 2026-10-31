package com.scarymonsters.client;

import com.scarymonsters.ability.Anims;
import net.minecraft.client.MinecraftClient;

import java.util.HashMap;
import java.util.Map;

/** Remembers which attack pose each player (by entity id) started and when, so the gear can animate it. */
public final class ClientAnims {
    private ClientAnims() {}

    private static final Map<Integer, long[]> MAP = new HashMap<>(); // id -> {anim, startTick}

    public static void set(int entityId, int anim) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;
        long now = mc.world.getTime();
        MAP.entrySet().removeIf(en -> now - en.getValue()[1] > 200);
        MAP.put(entityId, new long[]{anim, now});
    }

    public static int anim(int entityId) {
        long[] a = MAP.get(entityId);
        return a == null ? Anims.IDLE : (int) a[0];
    }

    /** 0..1 progress of the running pose, or -1 when nothing is playing. */
    public static float progress(int entityId, float tickDelta) {
        long[] a = MAP.get(entityId);
        MinecraftClient mc = MinecraftClient.getInstance();
        if (a == null || mc.world == null) return -1f;
        float t = (mc.world.getTime() + tickDelta - a[1]) / (float) Anims.duration((int) a[0]);
        return t >= 0f && t < 1f ? t : -1f;
    }
}
