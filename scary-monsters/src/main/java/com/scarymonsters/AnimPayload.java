package com.scarymonsters;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/** Server -> clients: "entity X started attack animation Y" (drives tail / jaw poses on every client). */
public record AnimPayload(int entityId, int anim) implements CustomPayload {
    public static final CustomPayload.Id<AnimPayload> ID = new CustomPayload.Id<>(ScaryMod.id("anim"));
    public static final PacketCodec<RegistryByteBuf, AnimPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT, AnimPayload::entityId,
            PacketCodecs.VAR_INT, AnimPayload::anim,
            AnimPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
