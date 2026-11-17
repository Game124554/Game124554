package com.thunderscience.changedtsc.util;

import com.thunderscience.changedtsc.config.TSCConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Turns config "entity specs" into real EntityTypes.
 * Supports exact ids ("changed:foo"), keyword search ("auto:puro") and fallbacks ("a|b|c").
 * Because Changed's entities are found by keyword, this add-on keeps working when Changed renames things.
 */
public final class EntityResolver {
    private static final Map<String, Optional<EntityType<?>>> CACHE = new ConcurrentHashMap<>();
    private static volatile Set<String> namespaces;
    private static volatile Set<EntityType<?>> friendly;
    private static volatile List<EntityType<?>> latexPool;

    private EntityResolver() {}

    public static void invalidate() {
        CACHE.clear();
        namespaces = null;
        friendly = null;
        latexPool = null;
        FurniturePlacer.invalidate();
    }

    public static Set<String> latexNamespaces() {
        Set<String> ns = namespaces;
        if (ns == null) {
            ns = new HashSet<>();
            for (String s : TSCConfig.LATEX_NAMESPACES.get()) ns.add(s.trim().toLowerCase(Locale.ROOT));
            namespaces = ns;
        }
        return ns;
    }

    /** First entity matching the spec (options separated by "|"). */
    public static Optional<EntityType<?>> resolve(String spec) {
        if (spec == null) return Optional.empty();
        return CACHE.computeIfAbsent(spec, EntityResolver::doResolve);
    }

    private static Optional<EntityType<?>> doResolve(String spec) {
        for (String option : spec.split("\\|")) {
            List<EntityType<?>> found = matches(option.trim());
            if (!found.isEmpty()) return Optional.of(found.get(0));
        }
        return Optional.empty();
    }

    /** Every entity matching ALL options of a spec. */
    public static List<EntityType<?>> resolveAll(String spec) {
        Set<EntityType<?>> out = new LinkedHashSet<>();
        for (String option : spec.split("\\|")) out.addAll(matches(option.trim()));
        return new ArrayList<>(out);
    }

    public static List<EntityType<?>> matches(String option) {
        if (option.isEmpty()) return List.of();
        if (option.startsWith("auto:")) {
            String keyword = option.substring(5).toLowerCase(Locale.ROOT);
            Set<String> ns = latexNamespaces();
            return ForgeRegistries.ENTITY_TYPES.getKeys().stream()
                    .filter(id -> ns.contains(id.getNamespace()))
                    .filter(id -> id.getPath().contains(keyword))
                    // avoid picking baby variants unless explicitly asked for
                    .filter(id -> keyword.contains("pup") || !id.getPath().contains("pup"))
                    .sorted(Comparator.comparingInt((ResourceLocation id) -> id.getPath().length())
                            .thenComparing(ResourceLocation::toString))
                    .map(ForgeRegistries.ENTITY_TYPES::getValue)
                    .filter(Objects::nonNull)
                    .filter(t -> t.getCategory() != MobCategory.MISC)
                    .toList();
        }
        ResourceLocation id = ResourceLocation.tryParse(option);
        if (id != null && ForgeRegistries.ENTITY_TYPES.containsKey(id)) {
            EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(id);
            if (type != null) return List.of(type);
        }
        return List.of();
    }

    /** Entity types that are friendly (Puro by default). */
    public static Set<EntityType<?>> friendlyTypes() {
        Set<EntityType<?>> f = friendly;
        if (f == null) {
            f = new HashSet<>();
            if (TSCConfig.FRIENDLY_PURO_ENABLED.get()) {
                for (String spec : TSCConfig.FRIENDLY_LATEX.get()) f.addAll(resolveAll(spec));
            }
            friendly = f;
        }
        return f;
    }

    /** Latex that structures may spawn for a "latex" marker. */
    public static List<EntityType<?>> latexPool() {
        List<EntityType<?>> pool = latexPool;
        if (pool == null) {
            Set<EntityType<?>> set = new LinkedHashSet<>();
            List<? extends String> configured = TSCConfig.STRUCTURE_LATEX_POOL.get();
            if (!configured.isEmpty()) {
                for (String spec : configured) set.addAll(resolveAll(spec));
            } else {
                Set<String> ns = latexNamespaces();
                List<EntityType<?>> any = new ArrayList<>();
                for (ResourceLocation id : ForgeRegistries.ENTITY_TYPES.getKeys()) {
                    if (!ns.contains(id.getNamespace())) continue;
                    EntityType<?> t = ForgeRegistries.ENTITY_TYPES.getValue(id);
                    if (t == null || t.getCategory() == MobCategory.MISC) continue;
                    if (t.getCategory() == MobCategory.MONSTER) set.add(t);
                    else any.add(t);
                }
                if (set.isEmpty()) set.addAll(any);
            }
            set.removeAll(friendlyTypes());
            pool = new ArrayList<>(set);
            latexPool = pool;
        }
        return pool;
    }
}
