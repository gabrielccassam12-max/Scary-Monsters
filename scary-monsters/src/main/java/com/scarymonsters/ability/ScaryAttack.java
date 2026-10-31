package com.scarymonsters.ability;

import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/** All 16 attacks, four per Form. */
public enum ScaryAttack {
    // ---- FORM 1: Raptor
    CLAW_RAKE(1, "claw_rake", 12, 0.3f, Anims.CLAW),
    POUNCE(1, "pounce", 40, 0.6f, Anims.POUNCE),
    TAIL_WHIP(1, "tail_whip", 45, 0.7f, Anims.WHIP),
    BITE(1, "bite", 35, 0.6f, Anims.BITE),

    // ---- FORM 2: Predator
    RAPTOR_RUSH(2, "raptor_rush", 70, 1.2f, Anims.RUSH),
    DINO_ROAR(2, "dino_roar", 120, 1.5f, Anims.ROAR),
    DINOSAURIZE(2, "dinosaurize", 160, 2.0f, Anims.SUMMON),
    CAMOUFLAGE(2, "camouflage", 240, 1.0f, Anims.SUMMON),

    // ---- FORM 3: Tyrant
    TYRANT_BITE(3, "tyrant_bite", 60, 2.0f, Anims.BITE),
    STOMP(3, "stomp", 100, 2.5f, Anims.STOMP),
    PACK_CALL(3, "pack_call", 300, 3.0f, Anims.SUMMON),
    FOSSILIZE(3, "fossilize", 200, 2.5f, Anims.ROAR),

    // ---- FORM 4: Primordial
    METEOR_STORM(4, "meteor_storm", 360, 5.0f, Anims.DOOM),
    EXTINCTION_AURA(4, "extinction_aura", 400, 5.0f, Anims.DOOM),
    RAMPAGE(4, "rampage", 500, 6.0f, Anims.RUSH),
    DINO_ARMY(4, "dino_army", 600, 6.0f, Anims.SUMMON);

    public final int form;
    public final String key;
    public final int cooldown;
    public final float exhaustion;
    public final int anim;

    ScaryAttack(int form, String key, int cooldown, float exhaustion, int anim) {
        this.form = form;
        this.key = key;
        this.cooldown = cooldown;
        this.exhaustion = exhaustion;
        this.anim = anim;
    }

    public Text displayName() {
        return Text.translatable("attack.scarymonsters." + key);
    }

    public static List<ScaryAttack> forForm(int form) {
        List<ScaryAttack> list = new ArrayList<>();
        for (ScaryAttack a : values()) {
            if (a.form == form) list.add(a);
        }
        return list;
    }
}
