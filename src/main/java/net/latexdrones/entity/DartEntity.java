package net.latexdrones.entity;

import net.latexdrones.content.MobDef;
import net.latexdrones.infection.InfectionData;
import net.latexdrones.infection.InfectionManager;
import net.latexdrones.registry.ModBlocks;
import net.latexdrones.registry.ModEffects;
import net.latexdrones.registry.ModEntities;
import net.latexdrones.registry.ModItems;
import net.latexdrones.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Projectile for every dart-like shot. What it does on impact depends on the item it carries:
 * latex dart (infects / converts), tranquilizer dart, antidote dart or goo canister (splash).
 */
public class DartEntity extends ThrowableItemProjectile {
    private String sourceForm = "";
    private int hypnoTicks;

    public DartEntity(EntityType<? extends DartEntity> type, Level level) {
        super(type, level);
    }

    public DartEntity(Level level, LivingEntity shooter) {
        super(ModEntities.DART, shooter, level);
    }

    /** @param form mob form a player hit by this dart will turn into ("" = keep / default) */
    public void setSource(String form, int hypnoTicks) {
        this.sourceForm = form == null ? "" : form;
        this.hypnoTicks = hypnoTicks;
    }

    public static Item itemFor(int dartKind) {
        switch (dartKind) {
            case MobDef.DART_TRANQ:
                return ModItems.TRANQ_DART;
            case MobDef.DART_GOO:
                return ModItems.GOO_CANISTER;
            default:
                return ModItems.LATEX_DART;
        }
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.LATEX_DART;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        Level level = this.level();
        if (level.isClientSide) {
            return;
        }
        Entity hit = result.getEntity();
        Entity owner = this.getOwner();
        if (owner instanceof LatexDroneEntity && hit instanceof LatexDroneEntity) {
            return; // no friendly fire inside the hive
        }
        ItemStack stack = this.getItem();
        if (stack.is(ModItems.TRANQ_DART)) {
            hit.hurt(this.damageSources().thrown(this, owner), 2.0F);
            if (hit instanceof LivingEntity living) {
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 3));
                living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 1));
                if (living instanceof ServerPlayer) {
                    living.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
                }
            }
        } else if (stack.is(ModItems.ANTIDOTE_DART)) {
            if (hit instanceof LatexDroneEntity drone) {
                drone.hurt(this.damageSources().thrown(this, owner), drone.def().scientist ? 2.0F : 14.0F);
            } else if (hit instanceof ServerPlayer player) {
                if (InfectionData.isTransformed(player)) {
                    player.hurt(this.damageSources().thrown(this, owner), 6.0F);
                } else {
                    InfectionManager.reduce(player, 400);
                    player.addEffect(new MobEffectInstance(ModEffects.INOCULATED, 600, 0));
                }
            }
        } else if (stack.is(ModItems.GOO_CANISTER)) {
            hit.hurt(this.damageSources().thrown(this, owner), 1.0F);
        } else {
            if (hit instanceof LatexDroneEntity) {
                return; // already latex
            }
            hit.hurt(this.damageSources().thrown(this, owner), 2.0F);
            if (hit instanceof ServerPlayer player) {
                InfectionManager.addInfection(player, 250, this.sourceForm);
            } else if (hit instanceof LivingEntity living && living.isAlive() && InfectionManager.isConvertible(living)
                    && level instanceof ServerLevel server) {
                InfectionManager.spawnDroneFrom(server, living, this.sourceForm);
                living.discard();
            }
        }
        if (this.hypnoTicks > 0 && hit instanceof LivingEntity living && !(hit instanceof LatexDroneEntity)) {
            living.addEffect(new MobEffectInstance(ModEffects.HYPNOSIS, this.hypnoTicks, 0));
        }
        level.playSound(null, hit.getX(), hit.getY(), hit.getZ(), ModSounds.INJECT, SoundSource.NEUTRAL, 0.8F, 1.0F);
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        Level level = this.level();
        if (!level.isClientSide) {
            if (this.getItem().is(ModItems.GOO_CANISTER) && level instanceof ServerLevel server) {
                this.splash(server);
            }
            level.broadcastEntityEvent(this, (byte) 3);
            this.discard();
        }
    }

    /** Goo canisters burst: infect / slow everything close by and leave a puddle. */
    private void splash(ServerLevel level) {
        Entity owner = this.getOwner();
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(2.5D))) {
            if (living instanceof LatexDroneEntity) {
                continue;
            }
            if (living instanceof ServerPlayer player) {
                if (player != owner) {
                    InfectionManager.addInfection(player, 120, this.sourceForm);
                }
            }
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2));
        }
        BlockPos pos = this.blockPosition();
        BlockState puddle = ModBlocks.BLACK_LATEX_PUDDLE.defaultBlockState();
        if (level.getBlockState(pos).isAir() && puddle.canSurvive(level, pos)) {
            level.setBlockAndUpdate(pos, puddle);
        }
        level.playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.GOO_SPLAT, SoundSource.NEUTRAL, 1.0F, 1.0F);
        level.sendParticles(ParticleTypes.SQUID_INK, this.getX(), this.getY() + 0.2D, this.getZ(), 14, 0.5D, 0.3D, 0.5D, 0.02D);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 3) {
            ItemParticleOption particle = new ItemParticleOption(ParticleTypes.ITEM, this.getItem());
            for (int i = 0; i < 8; i++) {
                this.level().addParticle(particle, this.getX(), this.getY(), this.getZ(),
                        (this.random.nextDouble() - 0.5D) * 0.2D, this.random.nextDouble() * 0.2D, (this.random.nextDouble() - 0.5D) * 0.2D);
            }
        } else {
            super.handleEntityEvent(id);
        }
    }
}
