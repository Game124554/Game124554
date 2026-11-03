package net.scpfoundation.world;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.scpfoundation.ScpFoundation;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * SCP-106's pocket dimension (data/scpfoundation/dimension/pocket_dimension.json): a dark, endless corroded floor.
 * Wander more than 40 blocks from the centre and the corridor either lets you out (60%) or loops you back.
 */
public final class PocketDimension {
    public static final RegistryKey<World> KEY = RegistryKey.of(RegistryKeys.WORLD, ScpFoundation.id("pocket_dimension"));
    private static final double ESCAPE_RADIUS = 40.0;
    private static final double FLOOR_Y = 4.0;

    private record Return(RegistryKey<World> world, Vec3d pos) {}

    private static final Map<UUID, Return> RETURNS = new HashMap<>();

    private PocketDimension() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(PocketDimension::tick);
    }

    public static void send(ServerPlayerEntity player) {
        MinecraftServer server = player.getServer();
        if (server == null) return;
        ServerWorld pocket = server.getWorld(KEY);
        if (pocket == null || player.getWorld() == pocket) return;
        RETURNS.put(player.getUuid(), new Return(player.getWorld().getRegistryKey(), player.getPos()));
        double a = player.getRandom().nextDouble() * Math.PI * 2.0;
        player.teleport(pocket, Math.cos(a) * 6.0, FLOOR_Y, Math.sin(a) * 6.0, player.getYaw(), player.getPitch());
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 200, 0));
        pocket.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_SCULK_SHRIEKER_SHRIEK, SoundCategory.HOSTILE, 1.0f, 0.5f);
        player.sendMessage(Text.translatable("message.scpfoundation.pocket_enter").formatted(Formatting.DARK_RED), true);
    }

    private static void tick(MinecraftServer server) {
        if (server.getTicks() % 20 != 0) return;
        ServerWorld pocket = server.getWorld(KEY);
        if (pocket == null) return;
        for (ServerPlayerEntity player : pocket.getPlayers()) {
            if (player.isSpectator()) continue;
            if (server.getTicks() % 100 == 0 && !player.isCreative()) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 40, 0));
            }
            if (player.getY() < 0.0) {
                player.teleport(pocket, 0.5, FLOOR_Y, 0.5, player.getYaw(), player.getPitch());
                continue;
            }
            double dist = Math.sqrt(player.getX() * player.getX() + player.getZ() * player.getZ());
            if (dist < ESCAPE_RADIUS) continue;
            if (player.getRandom().nextFloat() < 0.6f) {
                escape(server, player);
            } else {
                player.teleport(pocket, 0.5, FLOOR_Y, 0.5, player.getYaw() + 180.0f, player.getPitch());
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 100, 1));
                player.sendMessage(Text.translatable("message.scpfoundation.pocket_loop").formatted(Formatting.DARK_RED), true);
            }
        }
    }

    private static void escape(MinecraftServer server, ServerPlayerEntity player) {
        Return ret = RETURNS.remove(player.getUuid());
        ServerWorld target = ret != null ? server.getWorld(ret.world()) : null;
        Vec3d pos;
        if (target == null) {
            target = server.getOverworld();
            BlockPos spawn = target.getSpawnPos();
            pos = new Vec3d(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5);
        } else {
            // Exit somewhere near where you were taken, on the surface.
            double x = ret.pos().x + (player.getRandom().nextDouble() - 0.5) * 40.0;
            double z = ret.pos().z + (player.getRandom().nextDouble() - 0.5) * 40.0;
            int y = target.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(x), (int) Math.floor(z));
            pos = new Vec3d(x, y, z);
        }
        player.teleport(target, pos.x, pos.y, pos.z, player.getYaw(), player.getPitch());
        player.sendMessage(Text.translatable("message.scpfoundation.pocket_escape").formatted(Formatting.GRAY), true);
    }
}
