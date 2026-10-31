package com.scarymonsters;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/** Client -> server: 0 = switch form, 1 = switch attack. */
public record ScaryPayload(int action) implements CustomPayload {
    public static final int SWITCH_FORM = 0;
    public static final int SWITCH_ATTACK = 1;

    public static final CustomPayload.Id<ScaryPayload> ID = new CustomPayload.Id<>(ScaryMod.id("action"));
    public static final PacketCodec<RegistryByteBuf, ScaryPayload> CODEC =
            PacketCodecs.VAR_INT.xmap(ScaryPayload::new, ScaryPayload::action).cast();

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
