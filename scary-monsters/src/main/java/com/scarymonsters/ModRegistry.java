package com.scarymonsters;

import com.mojang.serialization.Codec;
import com.scarymonsters.entity.DinoMinionEntity;
import com.scarymonsters.item.ScaryItem;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.component.ComponentType;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Rarity;

public final class ModRegistry {
    private ModRegistry() {}

    /** Current Form (1-4), stored on the item stack. */
    public static final ComponentType<Integer> FORM = Registry.register(Registries.DATA_COMPONENT_TYPE,
            ScaryMod.id("form"),
            ComponentType.<Integer>builder().codec(Codec.INT).packetCodec(PacketCodecs.VAR_INT).build());

    /** Selected attack index inside the current Form. */
    public static final ComponentType<Integer> ATTACK = Registry.register(Registries.DATA_COMPONENT_TYPE,
            ScaryMod.id("attack"),
            ComponentType.<Integer>builder().codec(Codec.INT).packetCodec(PacketCodecs.VAR_INT).build());

    public static final Item SCARY_MONSTERS = Registry.register(Registries.ITEM, ScaryMod.id("scary_monsters"),
            new ScaryItem(new Item.Settings().maxCount(1).rarity(Rarity.EPIC).component(FORM, 1).component(ATTACK, 0)));

    public static final EntityType<DinoMinionEntity> RAPTOR = Registry.register(Registries.ENTITY_TYPE,
            ScaryMod.id("raptor"),
            FabricEntityTypeBuilder.<DinoMinionEntity>create(SpawnGroup.CREATURE, DinoMinionEntity::new)
                    .dimensions(EntityDimensions.fixed(0.9f, 1.3f))
                    .trackRangeBlocks(80)
                    .disableSummon()
                    .build());

    public static final EntityType<DinoMinionEntity> REX = Registry.register(Registries.ENTITY_TYPE,
            ScaryMod.id("rex"),
            FabricEntityTypeBuilder.<DinoMinionEntity>create(SpawnGroup.CREATURE, DinoMinionEntity::new)
                    .dimensions(EntityDimensions.fixed(2.2f, 3.0f))
                    .trackRangeBlocks(80)
                    .disableSummon()
                    .build());

    public static void init() {
        FabricDefaultAttributeRegistry.register(RAPTOR, DinoMinionEntity.raptorAttributes());
        FabricDefaultAttributeRegistry.register(REX, DinoMinionEntity.rexAttributes());
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(entries -> entries.add(SCARY_MONSTERS));
    }
}
