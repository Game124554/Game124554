package net.scpfoundation.block;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.scpfoundation.registry.ModBlockEntities;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Remembers how many pieces of candy each player has taken from this bowl. */
public class Scp330BlockEntity extends BlockEntity {
    private final Map<UUID, Integer> taken = new HashMap<>();

    public Scp330BlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SCP_330, pos, state);
    }

    public int take(UUID player) {
        int n = taken.getOrDefault(player, 0) + 1;
        taken.put(player, n);
        markDirty();
        return n;
    }

    @Override
    public void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        NbtCompound map = new NbtCompound();
        for (Map.Entry<UUID, Integer> e : taken.entrySet()) {
            map.putInt(e.getKey().toString(), e.getValue());
        }
        nbt.put("Taken", map);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        taken.clear();
        NbtCompound map = nbt.getCompound("Taken");
        for (String key : map.getKeys()) {
            try {
                taken.put(UUID.fromString(key), map.getInt(key));
            } catch (IllegalArgumentException ignored) {
                // skip malformed entries
            }
        }
    }
}
