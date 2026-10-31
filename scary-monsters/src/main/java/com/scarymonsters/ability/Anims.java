package com.scarymonsters.ability;

/** Pose ids shared by the server (which triggers them) and the client gear model (which plays them). */
public final class Anims {
    private Anims() {}

    public static final int IDLE = 0;
    public static final int CLAW = 1;
    public static final int POUNCE = 2;
    public static final int WHIP = 3;
    public static final int BITE = 4;
    public static final int ROAR = 5;
    public static final int RUSH = 6;
    public static final int STOMP = 7;
    public static final int SUMMON = 8;
    public static final int DOOM = 9;

    private static final int[] DURATION = {1, 10, 16, 22, 14, 34, 16, 20, 28, 50};

    public static int duration(int anim) {
        return anim >= 0 && anim < DURATION.length ? DURATION[anim] : 1;
    }
}
