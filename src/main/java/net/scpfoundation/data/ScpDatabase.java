package net.scpfoundation.data;

import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The in-game Foundation database. Every number from SCP-001 to SCP-9999 has a file;
 * a catalogue of well-known anomalies carries a nickname, object class and a short summary
 * written for this mod. Every file links to the full article on the SCP Wiki.
 *
 * SCP Foundation concepts: SCP Wiki, https://scp-wiki.wikidot.com, CC BY-SA 3.0.
 */
public final class ScpDatabase {
    public static final int MIN = 1;
    public static final int MAX = 9999;

    public record Entry(int number, String nickname, String objectClass, String summary, boolean inGame) {}

    private static final Map<Integer, Entry> ENTRIES = new LinkedHashMap<>();

    static {
        // --- anomalies that physically exist in this mod ---
        game(5, "Skeleton Key", "Safe", "A key that opens any lock. In-game: opens doors, trapdoors, gates and keycard readers.");
        game(18, "Super Ball", "Euclid", "A rubber ball that gains energy with every bounce. In-game: throw it and stand back.");
        game(49, "Plague Doctor", "Euclid", "A humanoid in medieval plague-doctor garb that 'cures' people by killing them, then reanimates them as SCP-049-2.");
        game(63, "World's Best Toothbrush", "Safe", "A toothbrush that destroys organic matter on contact. In-game: mines wood, leaves, wool and plants instantly.");
        game(96, "The Shy Guy", "Euclid", "A pale, gaunt humanoid. Anyone who sees its face becomes its target; it will cross any distance to reach them.");
        game(106, "The Old Man", "Keter", "An elderly-looking humanoid that passes through solid matter, corrodes what it touches and drags victims into its pocket dimension.");
        game(131, "The Eye Pods", "Safe", "Small, friendly teardrop-shaped creatures with a single eye. In-game: they follow you and mark nearby hostiles.");
        game(173, "The Sculpture", "Euclid", "A concrete statue that cannot move while observed. When nobody is looking, it moves extremely fast. Do not blink.");
        game(207, "Cola Bottles", "Safe", "Bottles of cola that make the drinker faster while slowly harming them.");
        game(268, "Cap of Neglect", "Safe", "A flat cap: while it is worn, others simply stop paying attention to the wearer.");
        game(294, "The Coffee Machine", "Safe", "A vending machine that can dispense almost any liquid. In-game: insert an iron nugget renamed to the drink you want.");
        game(330, "Take Only Two", "Safe", "A bowl of candy with a sign asking you to take only two pieces. Taking more is a mistake.");
        game(427, "Lovecraftian Locket", "Safe", "A locket that heals the holder when opened. Too much exposure has consequences.");
        game(458, "The Never-Ending Pizza Box", "Safe", "A pizza box that always contains a fresh pizza.");
        game(500, "Panacea", "Safe", "Red pills that cure every disease and ailment within hours.");
        game(529, "Josie the Half-Cat", "Safe", "A friendly tabby cat that appears to be missing the back half of its body. It behaves like a normal cat.");
        game(682, "Hard-to-Destroy Reptile", "Keter", "A huge, hateful reptile that regenerates from almost anything and adapts to whatever is used against it.");
        game(714, "The Jaded Ring", "Safe", "A jade ring that makes the wearer drowsy but protects them from toxins.");
        game(914, "The Clockworks", "Safe", "A huge clockwork machine that 'refines' items on five settings, from Rough to Very Fine.");
        game(939, "With Many Voices", "Keter", "Pack predators that hunt by sound and lure prey by imitating human voices.");
        game(999, "The Tickle Monster", "Safe", "A friendly orange gelatinous blob whose touch makes people happy.");
        game(1048, "Builder Bear", "Keter", "A small teddy bear that walks on its own and builds copies of itself from disturbing materials.");
        game(1162, "Hole in the Wall", "Safe", "A hole in a wall: reach in with an object and you pull out something else. Reach in empty-handed at your own risk.");
        game(87, "The Stairwell", "Euclid", "A dark stairwell that seems to descend forever. Found in the world as a structure.");
        game(3008, "Infinite Furniture Store", "Euclid", "An enormous furniture store with no exits. Found in the world as a structure.");

        game(6, "Fountain of Youth", "Safe", "A spring whose water heals and reverses aging. In-game: bottled SCP-006 water heals and cures fatigue and poison.");
        game(12, "A Bad Composition", "Euclid", "An unfinished musical score. Anyone nearby feels compelled to complete it, using their own blood. A blindfold (carved pumpkin) protects you.");
        game(66, "Eric's Toy", "Euclid", "A small mass of braided yarn that asks for 'Eric' and sometimes emits deafening bursts of sound.");
        game(1025, "Encyclopedia of Diseases", "Safe", "Reading an entry gives the reader that disease's symptoms.");
        game(1471, "MalO ver1.0.0", "Euclid", "A phone app. Once installed, a dark figure with a canine skull starts appearing in the owner's photos - and behind them.");
        game(2521, "\u25CF\u25CF|\u25CF\u25CF\u25CF\u25CF\u25CF|\u25CF\u25CF|\u25CF", "Keter", "An entity that takes from anyone who writes about it in words. In-game: type its number in chat and it takes something from you.");

        // --- catalogued files (not physically present) ---
        file(1, "[ACCESS RESTRICTED]", "[REDACTED]", "Level 5 clearance required.");
        file(2, "The 'Living' Room", "Euclid", "A fleshy pod that contains a furnished room which absorbs anyone who enters.");
        file(3, "Biological Motherboard", "Euclid", "A computer motherboard made of living tissue.");
        file(4, "The 12 Rusty Keys and the Door", "Euclid", "A door that leads to different, often deadly places depending on the key used.");
        file(7, "Abdominal Planet", "Safe", "A man with a tiny, living planet in his abdomen.");
        file(8, "Zombie Plague", "Euclid", "A prion that turns its hosts into aggressive undead.");
        file(9, "Red Ice", "Euclid", "A red liquid that freezes when heated and melts when cooled.");
        file(13, "Blue Lady Cigarettes", "Safe", "Cigarettes that change the smoker's self-image.");
        file(14, "The Concrete Man", "Safe", "A man made of concrete who is still alive.");
        file(15, "Pipe Nightmare", "Keter", "A network of pipes that keeps growing on its own.");
        file(16, "Sentient Microscopic Organism", "Keter", "A microorganism that reshapes its hosts.");
        file(17, "Shadow Person", "Keter", "A humanoid shadow that engulfs anything in darkness.");
        file(19, "The Monster Pot", "Keter", "A large clay pot from which creatures sometimes emerge.");
        file(20, "Unseen Mold", "Keter", "An invisible, airborne mold.");
        file(35, "Possessive Mask", "Keter", "A porcelain mask that takes control of whoever wears it.");
        file(53, "Young Girl", "Euclid", "A small child; anyone who becomes hostile toward her suffers severe consequences.");
        file(55, "[unknown]", "Keter", "An object that cannot be remembered, only what it is not.");
        file(58, "Heart of Darkness", "Keter", "A cow-heart-like creature with tentacles and a malevolent intellect.");
        file(73, "Cain", "Euclid", "A man whose presence withers plant life and who cannot be harmed directly.");
        file(76, "Able", "Keter", "A hostile humanoid kept in a stone coffin who repeatedly returns.");
        file(79, "Old AI", "Euclid", "A sentient artificial intelligence on outdated hardware.");
        file(93, "Red Sea Object", "Euclid", "A disc that opens a way into another world when held near a mirror.");
        file(105, "Iris", "Safe", "A woman who can interact with the scenes in photographs.");
        file(111, "Dragon-Snails", "Safe", "Small snails that breathe fire.");
        file(148, "The 'Telekill' Alloy", "Euclid", "A metal that blocks psychic and telepathic effects.");
        file(169, "The Leviathan", "Keter", "A creature so large it may be an entire landmass.");
        file(191, "Cyborg Child", "Safe", "A child heavily modified with machinery.");
        file(231, "Special Personnel Requirements", "Keter", "Classified. Containment procedures must never be discussed.");
        file(261, "Pan-dimensional Vending", "Safe", "A vending machine that sells snacks from other worlds.");
        file(343, "'God'", "Safe", "An elderly man who claims to be God and can appear anywhere.");
        file(372, "Peripheral Jumper", "Euclid", "A creature that is only ever seen in the corner of your eye.");
        file(426, "I am a Toaster", "Euclid", "A toaster that makes those near it believe they are the toaster.");
        file(447, "Ball of Green Slime", "Safe", "A ball of slime that turns corpses into slime.");
        file(513, "A Cowbell", "Euclid", "A cowbell that causes horrifying hallucinations when rung.");
        file(610, "The Flesh that Hates", "Keter", "A spreading infection that converts living tissue.");
        file(701, "The Hanged King's Tragedy", "Euclid", "A play whose performances cause violent outbreaks.");
        file(860, "Blue Key", "Safe", "A key that opens a door to a strange forest.");
        file(895, "Camera Disruption", "Euclid", "A coffin that distorts camera recordings near it.");
        file(963, "Immortality", "Safe", "An amulet that preserves the mind of whoever touches it.");
        file(966, "Sleep Killer", "Euclid", "Creatures that prevent their victims from sleeping.");
        file(990, "Dream Man", "Keter", "A man who appears in people's dreams to warn of disasters.");
        file(1000, "Bigfoot", "Keter", "A species that has hidden from humanity for a very long time.");
        file(1123, "Atrocity Skull", "Safe", "A skull that makes those who touch it relive a historical atrocity.");
        file(1499, "Gas Mask", "[REDACTED]", "A gas mask that transports the wearer to another dimension.");
        file(2000, "Deus Ex Machina", "Thaumiel", "A facility built to rebuild humanity after a catastrophe.");
        file(2316, "Field Trip", "Keter", "A group of bodies in a lake that people believe they know.");
        file(2317, "A Door to Another World", "Keter", "A door that leads to a world where something is chained.");
        file(3000, "Anantashesha", "Thaumiel", "An enormous sea serpent whose secretions affect memory.");
        file(4000, "Taboo", "Keter", "A forest where names and naming have power.");
        file(5000, "Why?", "Keter", "A file on a world where the Foundation turned against humanity.");
    }

    private ScpDatabase() {}

    private static void game(int n, String nick, String cls, String summary) {
        ENTRIES.put(n, new Entry(n, nick, cls, summary, true));
    }

    private static void file(int n, String nick, String cls, String summary) {
        ENTRIES.put(n, new Entry(n, nick, cls, summary, false));
    }

    public static boolean isKnown(int n) {
        return ENTRIES.containsKey(n);
    }

    public static Entry get(int n) {
        return ENTRIES.get(n);
    }

    public static List<Entry> inGame() {
        List<Entry> out = new ArrayList<>();
        for (Entry e : ENTRIES.values()) if (e.inGame()) out.add(e);
        out.sort((a, b) -> Integer.compare(a.number(), b.number()));
        return Collections.unmodifiableList(out);
    }

    /** "002", "173", "1048". */
    public static String label(int n) {
        return n < 1000 ? String.format("%03d", n) : Integer.toString(n);
    }

    public static String url(int n) {
        return "https://scp-wiki.wikidot.com/scp-" + label(n);
    }

    public static List<Text> fileLines(int n) {
        List<Text> lines = new ArrayList<>();
        Entry e = ENTRIES.get(n);
        lines.add(Text.translatable("database.scpfoundation.header").formatted(Formatting.DARK_RED, Formatting.BOLD));
        lines.add(Text.translatable("database.scpfoundation.item", label(n)).formatted(Formatting.WHITE));
        if (e != null) {
            lines.add(Text.translatable("database.scpfoundation.nickname", e.nickname()).formatted(Formatting.GRAY));
            lines.add(Text.translatable("database.scpfoundation.class", classText(e.objectClass())));
            lines.add(Text.translatable("database.scpfoundation.summary", e.summary()).formatted(Formatting.GRAY));
            if (e.inGame()) lines.add(Text.translatable("database.scpfoundation.ingame").formatted(Formatting.GOLD));
        } else {
            lines.add(Text.translatable("database.scpfoundation.class",
                    Text.translatable("database.scpfoundation.redacted").formatted(Formatting.DARK_GRAY)));
            lines.add(Text.translatable("database.scpfoundation.restricted").formatted(Formatting.GRAY, Formatting.ITALIC));
        }
        String url = url(n);
        MutableText link = Text.translatable("database.scpfoundation.link").styled(style -> style
                .withColor(Formatting.AQUA).withUnderline(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(url))));
        lines.add(link);
        lines.add(Text.translatable("database.scpfoundation.license").formatted(Formatting.DARK_GRAY, Formatting.ITALIC));
        return lines;
    }

    public static Text classText(String cls) {
        Formatting color = switch (cls) {
            case "Safe" -> Formatting.GREEN;
            case "Euclid" -> Formatting.YELLOW;
            case "Keter" -> Formatting.RED;
            case "Thaumiel" -> Formatting.LIGHT_PURPLE;
            default -> Formatting.DARK_GRAY;
        };
        return Text.literal(cls).formatted(color);
    }
}
