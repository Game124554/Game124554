package com.thunderscience.changedtsc.compat;

import com.thunderscience.changedtsc.ChangedTSC;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Optional;

/**
 * Reads the player's state from the Changed mod without linking against it: is the player
 * transfurred, and how far along is Changed's own transfur progress.
 *
 * Everything is looked up by name at runtime. If Changed is missing, or a method was renamed in
 * another version, the answer is simply "not transfurred / 0" and nothing breaks.
 */
public final class ChangedCompat {
    private static final String PROCESS_CLASS = "net.ltxprogrammer.changed.process.ProcessTransfur";

    private static boolean looked;
    @Nullable private static Method isTransfurred;
    @Nullable private static Method getVariant;
    @Nullable private static Method getProgress;

    private ChangedCompat() {}

    public static boolean isLoaded() {
        ModList list = ModList.get();
        return list != null && list.isLoaded("changed");
    }

    private static void look() {
        if (looked) return;
        looked = true;
        if (!isLoaded()) return;
        try {
            Class<?> process = Class.forName(PROCESS_CLASS);
            isTransfurred = find(process, "isPlayerTransfurred");
            getVariant = find(process, "getPlayerTransfurVariant");
            getProgress = find(process, "getPlayerTransfurProgress");
        } catch (Throwable t) {
            ChangedTSC.LOGGER.debug("Changed's transfur class was not found: {}", t.toString());
        }
    }

    @Nullable
    private static Method find(Class<?> owner, String name) {
        for (Method m : owner.getMethods()) {
            if (m.getName().equals(name) && Modifier.isStatic(m.getModifiers()) && m.getParameterCount() == 1
                    && m.getParameterTypes()[0].isAssignableFrom(Player.class)) {
                return m;
            }
        }
        return null;
    }

    /** True if Changed says this player is currently in a transfurred form. */
    public static boolean isTransfurred(Player player) {
        look();
        try {
            if (isTransfurred != null && isTransfurred.invoke(null, player) instanceof Boolean b) return b;
            if (getVariant != null) {
                Object variant = getVariant.invoke(null, player);
                if (variant instanceof Optional<?> optional) return optional.isPresent();
                return variant != null;
            }
        } catch (Throwable t) {
            isTransfurred = null;
            getVariant = null;
        }
        return false;
    }

    /** Changed's own transfur progress as 0-100 (progress compared with the player's tolerance), or 0 if unknown. */
    public static float transfurPercent(Player player) {
        look();
        if (getProgress == null) return 0.0F;
        try {
            Object value = getProgress.invoke(null, player);
            float progress;
            if (value instanceof Number number) {
                progress = number.floatValue();
            } else if (value != null) {
                Object inner = value.getClass().getMethod("progress").invoke(value);
                if (!(inner instanceof Number number)) return 0.0F;
                progress = number.floatValue();
            } else {
                return 0.0F;
            }
            float percent = progress / tolerance(player) * 100.0F;
            return Float.isFinite(percent) ? Math.max(0.0F, Math.min(100.0F, percent)) : 0.0F;
        } catch (Throwable t) {
            getProgress = null;
            return 0.0F;
        }
    }

    private static float tolerance(Player player) {
        Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation("changed", "transfur_tolerance"));
        if (attribute != null) {
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance != null && instance.getValue() > 0.0) return (float) instance.getValue();
        }
        return 20.0F;
    }
}
