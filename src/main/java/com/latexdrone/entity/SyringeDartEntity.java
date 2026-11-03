package com.latexdrone.entity;

import com.latexdrone.infection.InfectionManager;
import com.latexdrone.registry.ModEntities;
import com.latexdrone.registry.ModItems;
import com.latexdrone.registry.ModSounds;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvent;
import net.minecraft.world.World;

/** Syringe dart fired by the Syringe Gun and by scientists. Injects latex serum + hypnotic. */
public class SyringeDartEntity extends PersistentProjectileEntity {

    public SyringeDartEntity(EntityType<? extends SyringeDartEntity> type, World world) {
        super(type, world);
        this.setDamage(1.5);
    }

    public SyringeDartEntity(World world, LivingEntity owner) {
        super(ModEntities.SYRINGE_DART, owner, world);
        this.setDamage(1.5);
        this.pickupType = PickupPermission.DISALLOWED;
    }

    @Override
    protected ItemStack asItemStack() {
        return new ItemStack(ModItems.SYRINGE_DART);
    }

    @Override
    protected void onHit(LivingEntity target) {
        super.onHit(target);
        if (!this.getWorld().isClient && !InfectionManager.isLatex(target)) {
            EvilScientistEntity.inject(target, 140, 8.0f);
        }
    }

    @Override
    protected SoundEvent getHitSound() {
        return ModSounds.SYRINGE_INJECT;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.inGround && this.inGroundTime > 200) {
            this.discard();
        }
    }
}
