package com.teardownvoxel.registry;

import com.teardownvoxel.TeardownVoxel;
import net.minecraft.block.Block;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;

public final class ModTags {
    /** Natural ground: a structure touching one of these is considered supported. */
    public static final TagKey<Block> FOUNDATION = of("foundation");
    /** Never treated as load-bearing (leaves, plants, ladders...). They are not collapsed either. */
    public static final TagKey<Block> NON_STRUCTURAL = of("non_structural");
    /** Shatters from a single projectile hit or light impact (glass, ice...). */
    public static final TagKey<Block> FRAGILE = of("fragile");
    /** What the blowtorch cuts through rapidly. */
    public static final TagKey<Block> TORCHABLE = of("torchable");
    /** Never destroyed by sledgehammer / mod explosives. */
    public static final TagKey<Block> INDESTRUCTIBLE = of("indestructible");

    private static TagKey<Block> of(String path) {
        return TagKey.of(RegistryKeys.BLOCK, TeardownVoxel.id(path));
    }

    public static void init() {
    }

    private ModTags() {
    }
}
