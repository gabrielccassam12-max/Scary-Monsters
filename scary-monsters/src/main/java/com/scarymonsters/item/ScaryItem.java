package com.scarymonsters.item;

import com.scarymonsters.ModRegistry;
import com.scarymonsters.ScaryMod;
import com.scarymonsters.ability.ScaryAbilities;
import com.scarymonsters.ability.ScaryAttack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;

/**
 * Scary Monsters. While held, the player visibly transforms into a dinosaur (client side gear),
 * gains passive buffs scaled by the Form, and right click fires the selected attack.
 */
public class ScaryItem extends Item {
    public ScaryItem(Settings settings) {
        super(settings);
    }

    // ------------------------------------------------------------ state helpers
    public static int getForm(ItemStack stack) {
        return Math.max(1, Math.min(4, stack.getOrDefault(ModRegistry.FORM, 1)));
    }

    public static ScaryAttack getAttack(ItemStack stack) {
        List<ScaryAttack> list = ScaryAttack.forForm(getForm(stack));
        int idx = Math.floorMod(stack.getOrDefault(ModRegistry.ATTACK, 0), list.size());
        return list.get(idx);
    }

    public static ItemStack heldItem(PlayerEntity player) {
        if (player.getMainHandStack().getItem() instanceof ScaryItem) return player.getMainHandStack();
        if (player.getOffHandStack().getItem() instanceof ScaryItem) return player.getOffHandStack();
        return ItemStack.EMPTY;
    }

    /** Form (1-4) the player is currently transformed into, or 0 if not holding the item. */
    public static int activeForm(PlayerEntity player) {
        ItemStack s = heldItem(player);
        return s.isEmpty() ? 0 : getForm(s);
    }

    public static int unlockedForms(PlayerEntity player) {
        if (player.isCreative() || ScaryMod.UNLOCK_ALL) return 4;
        int forms = 1;
        for (int f = 2; f <= 4; f++) {
            if (player.experienceLevel >= ScaryMod.FORM_LEVELS[f - 1]) forms = f;
        }
        return forms;
    }

    public static void cycleForm(ServerPlayerEntity player, ItemStack stack) {
        int form = getForm(stack);
        int unlocked = unlockedForms(player);
        int next = form % 4 + 1;
        if (next > unlocked) {
            if (unlocked == 1) {
                player.sendMessage(Text.translatable("message.scarymonsters.locked", next, ScaryMod.FORM_LEVELS[next - 1])
                        .formatted(Formatting.RED), true);
                return;
            }
            next = 1;
        }
        stack.set(ModRegistry.FORM, next);
        stack.set(ModRegistry.ATTACK, 0);
        announce(player, stack);
    }

    public static void cycleAttack(ServerPlayerEntity player, ItemStack stack) {
        int size = ScaryAttack.forForm(getForm(stack)).size();
        stack.set(ModRegistry.ATTACK, (stack.getOrDefault(ModRegistry.ATTACK, 0) + 1) % size);
        announce(player, stack);
    }

    private static void announce(ServerPlayerEntity player, ItemStack stack) {
        player.sendMessage(Text.translatable("message.scarymonsters.attack",
                Text.translatable("form.scarymonsters." + getForm(stack)), getAttack(stack).displayName())
                .formatted(Formatting.GREEN), true);
    }

    // ------------------------------------------------------------ behaviour
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (world.isClient) return TypedActionResult.success(stack);

        if (player instanceof ServerPlayerEntity sp) {
            ScaryAttack attack = getAttack(stack);
            if (attack.form >= 3 && !sp.isCreative() && sp.getHungerManager().getFoodLevel() <= 6) {
                sp.sendMessage(Text.translatable("message.scarymonsters.exhausted").formatted(Formatting.RED), true);
                return TypedActionResult.fail(stack);
            }
            ScaryAbilities.execute(sp, attack);
            sp.addExhaustion(attack.exhaustion);
            sp.getItemCooldownManager().set(this, attack.cooldown);
        }
        return TypedActionResult.success(stack);
    }

    /** Passive buffs and the "fear" aura that weakens nearby monsters. */
    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (world.isClient || !ScaryMod.PASSIVE_BUFFS || !(entity instanceof ServerPlayerEntity player)) return;
        if (player.age % 20 != 0) return;
        if (!selected && player.getOffHandStack() != stack) return;

        int form = getForm(stack);
        buff(player, StatusEffects.SPEED, form >= 4 ? 2 : form >= 2 ? 1 : 0);
        buff(player, StatusEffects.JUMP_BOOST, form >= 3 ? 1 : 0);
        if (form >= 2) buff(player, StatusEffects.STRENGTH, form - 2);
        if (form >= 3) buff(player, StatusEffects.RESISTANCE, form - 3);

        if (form >= 2) {
            double radius = 4 + form * 1.5;
            for (LivingEntity e : world.getEntitiesByClass(LivingEntity.class, player.getBoundingBox().expand(radius),
                    e -> e instanceof Monster && e.isAlive())) {
                e.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 60, form >= 4 ? 1 : 0, true, false, false));
                if (form >= 3) {
                    e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, form - 3, true, false, false));
                }
            }
        }
    }

    private static void buff(ServerPlayerEntity p, net.minecraft.registry.entry.RegistryEntry<net.minecraft.entity.effect.StatusEffect> effect, int amp) {
        p.addStatusEffect(new StatusEffectInstance(effect, 60, amp, true, false, false));
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return getForm(stack) >= 4;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("form.scarymonsters." + getForm(stack)).formatted(Formatting.GREEN));
        tooltip.add(Text.translatable("tooltip.scarymonsters.attack", getAttack(stack).displayName()).formatted(Formatting.YELLOW));
        tooltip.add(Text.translatable("tooltip.scarymonsters.use").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("tooltip.scarymonsters.keys", "V", "B").formatted(Formatting.DARK_GRAY));
    }
}
