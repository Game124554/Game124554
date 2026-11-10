package net.latexdrones.item;

import net.latexdrones.entity.LatexDroneEntity;
import net.latexdrones.infection.InfectionData;
import net.latexdrones.infection.InfectionManager;
import net.latexdrones.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.Level;

/** The four melee weapons: stun baton, tesla prod, bone saw and latex blade. */
public class MeleeWeaponItem extends SwordItem {
    public enum Kind {
        STUN_BATON, TESLA_PROD, BONE_SAW, LATEX_BLADE
    }

    private final Kind kind;

    public MeleeWeaponItem(Kind kind, Tier tier, int attackDamage, float attackSpeed, Item.Properties properties) {
        super(tier, attackDamage, attackSpeed, properties);
        this.kind = kind;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        Level level = attacker.level();
        if (level instanceof ServerLevel server) {
            switch (this.kind) {
                case STUN_BATON:
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 4));
                    target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 1));
                    zap(server, target);
                    break;
                case TESLA_PROD:
                    zap(server, target);
                    for (Mob other : server.getEntitiesOfClass(Mob.class, target.getBoundingBox().inflate(3.5D),
                            e -> e != attacker && e != target && e.isAlive())) {
                        other.hurt(server.damageSources().lightningBolt(), 4.0F);
                        other.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 2));
                        server.sendParticles(ParticleTypes.ELECTRIC_SPARK, other.getX(), other.getY() + other.getBbHeight() * 0.5D,
                                other.getZ(), 8, 0.3D, 0.4D, 0.3D, 0.05D);
                    }
                    break;
                case BONE_SAW:
                    target.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 0));
                    break;
                case LATEX_BLADE:
                    if (target instanceof ServerPlayer victim) {
                        String form = attacker instanceof Player player && InfectionData.isTransformed(player)
                                ? InfectionData.server(player.getUUID()).form : "drone_unit";
                        InfectionManager.addInfection(victim, 120, form);
                    } else if (!(target instanceof LatexDroneEntity)) {
                        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1));
                    }
                    break;
                default:
                    break;
            }
        }
        return super.hurtEnemy(stack, target, attacker);
    }

    private static void zap(ServerLevel level, LivingEntity target) {
        level.playSound(null, target.getX(), target.getY(), target.getZ(), ModSounds.ZAP, SoundSource.PLAYERS, 0.8F, 1.0F);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                12, 0.3D, 0.4D, 0.3D, 0.05D);
    }
}
