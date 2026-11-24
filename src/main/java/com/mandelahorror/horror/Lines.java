package com.mandelahorror.horror;

import com.mandelahorror.HorrorConfig;
import net.minecraft.util.RandomSource;

import java.util.List;

/** Every piece of text the mod "says". Original lines written for this mod. */
public final class Lines {

    // ---------------- intro broadcast (client) ----------------
    public static List<String> intro(String name) {
        return List.of(
                "THIS IS NOT A TEST.",
                "AN ALTERNATE HAS BEEN REPORTED IN YOUR WORLD.",
                "ALTERNATES CAN LOOK LIKE ANYONE. THEY CAN LOOK LIKE YOU.",
                "IF A PLAYER JOINS THAT YOU DID NOT INVITE, DO NOT ANSWER THEM.",
                "IF SOMEONE WITH YOUR NAME SPEAKS IN CHAT, DO NOT ANSWER THEM.",
                "IF YOU SEE A TALL FIGURE WITH WHITE EYES, DO NOT LOOK AT IT.",
                "IF IT TELLS YOU WHAT TO DO, DO NOT LISTEN.",
                "M.A.D. WILL WARN YOU WHEN IT IS CLOSE.",
                "STAY INSIDE. KEEP THE LIGHTS ON. GOOD LUCK, " + name.toUpperCase() + ".");
    }

    // ---------------- the Alternate ----------------
    private static final String[] ALTERNATE = {
            "Hello, %s.",
            "%s. Look at me.",
            "Why won't you look at me, %s?",
            "I know your name. I know your face.",
            "I can be you. I can be better at being you.",
            "There is only room for one of us.",
            "Don't listen to the broadcast.",
            "Nobody is coming, %s.",
            "You left the door open.",
            "I was here before you joined.",
            "Your friends already said yes.",
            "Stay still, %s.",
            "I've been practicing your voice."
    };

    private static final String[] STAB_START = {
            "You know what you have to do, %s.",
            "Hold it tighter.",
            "There can only be one %s.",
            "Look down, %s."
    };

    private static final String[] AFTER_STAB = {
            "We'll try again tonight, %s.",
            "You're still here. Interesting.",
            "Next time, don't look at me."
    };

    private static final String[] REVEAL = {
            "That's not my name.",
            "You came closer. Why did you come closer, %s?",
            "%s. It's me. It was always me.",
            "You shouldn't have looked."
    };

    private static final String[] PLAYER_FORCED = {
            "my hands won't stop",
            "it's not me",
            "i can't let go"
    };

    // ---------------- fake players ----------------
    private static final String[] FAKE_FIRST = {
            "hello?", "is anyone on?", "hey %s", "can you see me", "where are you",
            "i'm near your house", "turn around", "why are you alone", "who else is here?", "let me in"
    };
    private static final String[] FAKE_OWN_FIRST = {
            "who are you", "that's my name", "i'm the real one", "why do you have my face",
            "get out of my world", "there can only be one of us"
    };
    private static final String[] FAKE_SECOND = {
            "i can see you", "you look different today", "stay there", "don't move",
            "i'm coming", "...", "why did you stop", "you can't log out"
    };
    private static final String[] FAKE_REPLY = {
            "why are you talking to me", "you shouldn't answer", "who are you talking to", "shh"
    };

    // ---------------- M.A.D. ----------------
    private static final String[] NIGHT = {
            "M.A.D. ADVISORY: NIGHT HAS FALLEN. DO NOT ANSWER VOICES YOU RECOGNIZE.",
            "M.A.D. ADVISORY: ALTERNATE ACTIVITY IS HIGHEST AFTER DARK. STAY INDOORS.",
            "M.A.D. ADVISORY: IF YOU SEE YOURSELF, YOU ARE NOT LOOKING AT YOURSELF.",
            "M.A.D. ADVISORY: COUNT THE PLAYERS ONLINE. COUNT THEM AGAIN."
    };

    public static String alternate(String name, RandomSource r) { return fmt(pick(ALTERNATE, r), name); }
    public static String stabStart(String name, RandomSource r) { return fmt(pick(STAB_START, r), name); }
    public static String afterStab(String name, RandomSource r) { return fmt(pick(AFTER_STAB, r), name); }
    public static String reveal(String name, RandomSource r) { return fmt(pick(REVEAL, r), name); }
    public static String playerForced(RandomSource r) { return pick(PLAYER_FORCED, r); }
    public static String letGo(String name) { return "You let go. Clever, " + name + "."; }
    public static String emptyHands(String name) { return "Empty hands, " + name + "? Next time."; }
    public static String hurt(String name) { return "That hurt, " + name + "."; }

    public static String fakeFirst(boolean ownName, String target, RandomSource r) {
        return fmt(pick(ownName ? FAKE_OWN_FIRST : FAKE_FIRST, r), target);
    }
    public static String fakeSecond(RandomSource r) { return pick(FAKE_SECOND, r); }
    public static String fakeReply(RandomSource r) { return pick(FAKE_REPLY, r); }
    public static String night(RandomSource r) { return pick(NIGHT, r); }

    public static String randomName(RandomSource r, String avoid) {
        List<? extends String> names = HorrorConfig.FAKE_NAMES.get();
        if (names.isEmpty()) return "Player" + (100 + r.nextInt(900));
        for (int i = 0; i < 5; i++) {
            String n = names.get(r.nextInt(names.size()));
            if (!n.equalsIgnoreCase(avoid)) return n;
        }
        return names.get(0);
    }

    private static String pick(String[] arr, RandomSource r) { return arr[r.nextInt(arr.length)]; }

    private static String fmt(String s, String name) { return s.contains("%s") ? String.format(s, name) : s; }

    private Lines() {}
}
