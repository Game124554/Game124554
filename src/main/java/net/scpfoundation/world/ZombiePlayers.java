package net.scpfoundation.world;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityGroup;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.scpfoundation.entity.Scp0492Entity;
import net.scpfoundation.entity.Scp049Entity;
import net.scpfoundation.registry.ModEffects;
import net.scpfoundation.registry.ModEntities;
import net.scpfoundation.registry.ModItems;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The SCP-049 / SCP-008 plague, for players.
 *
 * Game rules:
 *  - scpDeadPlayersRise (default true): when a player dies, an SCP-049-2 carrying their name rises where they fell.
 *  - scpPlayableZombies (default true): a player killed by SCP-049, SCP-049-2 or a zombie player, or who dies from
 *    SCP-008, respawns AS SCP-049-2 where they died. Zombie players are stronger, never hungry, ignored by the undead,
 *    and every hit they land spreads SCP-008. SCP-500 (or /scp cure) turns them back.
 */
public final class ZombiePlayers {
    public static final String TAG = "scp049_2_player";
    public static final GameRules.Key<GameRules.BooleanRule> DEAD_PLAYERS_RISE =
            GameRuleRegistry.register("scpDeadPlayersRise", GameRules.Category.MOBS, GameRuleFactory.createBooleanRule(true));
    public static final GameRules.Key<GameRules.BooleanRule> PLAYABLE_ZOMBIES =
            GameRuleRegistry.register("scpPlayableZombies", GameRules.Category.PLAYER, GameRuleFactory.createBooleanRule(true));

    private record Grave(RegistryKey<World> world, Vec3d pos, float yaw) {}

    private static final Map<UUID, Grave> RISING = new HashMap<>();

    private ZombiePlayers() {}

    public static boolean isZombie(PlayerEntity player) {
        return player.getCommandTags().contains(TAG);
    }

    public static void infect(LivingEntity target, int ticks) {
        if (target instanceof PlayerEntity p && (isZombie(p) || p.isCreative() || p.isSpectator())) return;
        if (target.getGroup() == EntityGroup.UNDEAD) return;
        StatusEffectInstance current = target.getStatusEffect(ModEffects.SCP_008_INFECTION);
        if (current == null) {
            target.addStatusEffect(new StatusEffectInstance(ModEffects.SCP_008_INFECTION, ticks, 0));
            if (target instanceof PlayerEntity p) {
                p.sendMessage(Text.translatable("message.scpfoundation.008_infected").formatted(Formatting.DARK_GREEN), true);
            }
        }
    }

    public static void register() {
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayerEntity player) {
                if (net.scpfoundation.item.Scp963Item.tryRevive(player)) return false;
                onPlayerDeath(player, source);
            }
            return true;
        });

        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            Grave grave = RISING.remove(newPlayer.getUuid());
            if (grave != null || isZombie(oldPlayer)) {
                zombify(newPlayer, false);
                if (grave != null && newPlayer.getServer() != null) {
                    ServerWorld world = newPlayer.getServer().getWorld(grave.world());
                    if (world != null) {
                        newPlayer.teleport(world, grave.pos().x, grave.pos().y, grave.pos().z, grave.yaw(), 0.0f);
                    }
                }
            }
        });

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (!world.isClient && isZombie(player) && entity instanceof LivingEntity living) {
                infect(living, 20 * 120);
            }
            return ActionResult.PASS;
        });

        ServerTickEvents.END_SERVER_TICK.register(ZombiePlayers::tick);
    }

    private static boolean killedByPlague(ServerPlayerEntity player, DamageSource source) {
        Entity attacker = source.getAttacker();
        return attacker instanceof Scp049Entity || attacker instanceof Scp0492Entity
                || (attacker instanceof PlayerEntity p && isZombie(p))
                || player.hasStatusEffect(ModEffects.SCP_008_INFECTION);
    }

    private static void onPlayerDeath(ServerPlayerEntity player, DamageSource source) {
        if (player.isSpectator()) return;
        ServerWorld world = (ServerWorld) player.getWorld();
        GameRules rules = world.getGameRules();
        if (isZombie(player)) return; // already one of them: they simply come back as SCP-049-2
        if (rules.getBoolean(PLAYABLE_ZOMBIES) && killedByPlague(player, source)) {
            RISING.put(player.getUuid(), new Grave(world.getRegistryKey(), player.getPos(), player.getYaw()));
            return;
        }
        if (rules.getBoolean(DEAD_PLAYERS_RISE)) {
            Scp0492Entity corpse = ModEntities.SCP_049_2.create(world);
            if (corpse == null) return;
            corpse.refreshPositionAndAngles(player.getX(), player.getY(), player.getZ(), player.getYaw(), 0.0f);
            corpse.setCustomName(Text.literal(player.getName().getString() + " (SCP-049-2)"));
            corpse.setPersistent();
            world.spawnEntity(corpse);
            world.spawnParticles(ParticleTypes.SOUL, player.getX(), player.getBodyY(0.5), player.getZ(), 15, 0.3, 0.6, 0.3, 0.02);
        }
    }

    public static ItemStack mask() {
        ItemStack mask = new ItemStack(ModItems.SCP_049_2_MASK);
        mask.addEnchantment(Enchantments.BINDING_CURSE, 1);
        mask.addEnchantment(Enchantments.VANISHING_CURSE, 1);
        return mask;
    }

    public static void zombify(ServerPlayerEntity player, boolean announce) {
        player.addCommandTag(TAG);
        player.removeStatusEffect(ModEffects.SCP_008_INFECTION);
        ItemStack head = player.getEquippedStack(EquipmentSlot.HEAD);
        if (!head.isOf(ModItems.SCP_049_2_MASK)) {
            if (!head.isEmpty()) player.getInventory().offerOrDrop(head.copy());
            player.equipStack(EquipmentSlot.HEAD, mask());
        }
        applyBuffs(player);
        ServerWorld world = (ServerWorld) player.getWorld();
        world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENTITY_ZOMBIE_VILLAGER_CONVERTED, SoundCategory.PLAYERS, 1.0f, 0.7f);
        world.spawnParticles(ParticleTypes.SOUL, player.getX(), player.getBodyY(0.5), player.getZ(), 20, 0.4, 0.8, 0.4, 0.02);
        player.sendMessage(Text.translatable("message.scpfoundation.zombie_became").formatted(Formatting.DARK_GREEN, Formatting.BOLD), false);
        if (announce && player.getServer() != null) {
            player.getServer().getPlayerManager().broadcast(
                    Text.translatable("message.scpfoundation.zombie_announce", player.getName()).formatted(Formatting.DARK_GREEN), false);
        }
    }

    public static void cure(ServerPlayerEntity player) {
        if (!isZombie(player)) return;
        player.getCommandTags().remove(TAG);
        if (player.getEquippedStack(EquipmentSlot.HEAD).isOf(ModItems.SCP_049_2_MASK)) {
            player.equipStack(EquipmentSlot.HEAD, ItemStack.EMPTY);
        }
        player.removeStatusEffect(StatusEffects.STRENGTH);
        player.removeStatusEffect(StatusEffects.NIGHT_VISION);
        player.sendMessage(Text.translatable("message.scpfoundation.zombie_cured").formatted(Formatting.GREEN), false);
    }

    private static void applyBuffs(ServerPlayerEntity p) {
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 60, 0, true, false, true));
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 400, 0, true, false, true));
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.SATURATION, 40, 0, true, false, false));
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 60, 0, true, false, true));
    }

    private static void tick(MinecraftServer server) {
        if (server.getTicks() % 20 != 0) return;
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            if (p.isSpectator() || !p.isAlive()) continue;
            if (isZombie(p)) {
                applyBuffs(p);
                if (!p.getEquippedStack(EquipmentSlot.HEAD).isOf(ModItems.SCP_049_2_MASK)) {
                    ItemStack head = p.getEquippedStack(EquipmentSlot.HEAD);
                    if (!head.isEmpty()) p.getInventory().offerOrDrop(head.copy());
                    p.equipStack(EquipmentSlot.HEAD, mask());
                }
                // the undead (and SCP-049) recognise one of their own
                for (MobEntity mob : p.getWorld().getEntitiesByClass(MobEntity.class, p.getBoundingBox().expand(32.0),
                        m -> m.getTarget() == p && (m.getGroup() == EntityGroup.UNDEAD || m instanceof Scp049Entity))) {
                    mob.setTarget(null);
                }
                continue;
            }
            StatusEffectInstance infection = p.getStatusEffect(ModEffects.SCP_008_INFECTION);
            if (infection == null) continue;
            int left = infection.getDuration();
            if (left <= 25) {
                if (p.getWorld().getGameRules().getBoolean(PLAYABLE_ZOMBIES)) {
                    zombify(p, true);
                } else {
                    p.damage(p.getDamageSources().magic(), Float.MAX_VALUE);
                }
            } else if (left <= 600 && left > 580) {
                p.sendMessage(Text.translatable("message.scpfoundation.008_stage3").formatted(Formatting.DARK_GREEN), true);
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 600, 0));
            } else if (left <= 1200 && left > 1180) {
                p.sendMessage(Text.translatable("message.scpfoundation.008_stage2").formatted(Formatting.DARK_GREEN), true);
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.HUNGER, 600, 1));
            }
        }
    }
}
