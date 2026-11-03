package com.latexdrone.content;

/** One latex creature form. Every form is both a mob and a shape a player can become. */
public record LatexForm(int index, String id, String name, int primary, int secondary, int glow, int parts,
                        float scale, double health, double damage, double speed, double armor, float infect,
                        String ability, String sound, int weight) {
    public static final int EARS = 1, EARS_ROUND = 2, SNOUT = 4, TAIL = 8, HORNS = 16, FIN = 32, WINGS = 64, LONG_EARS = 128;

    public static LatexForm get(int index) {
        if (index < 0 || index >= FormData.FORMS.size()) return FormData.FORMS.get(0);
        return FormData.FORMS.get(index);
    }

    public static LatexForm byId(String id) {
        for (LatexForm f : FormData.FORMS) if (f.id.equals(id)) return f;
        return null;
    }
}
