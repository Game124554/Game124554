package net.scpfoundation.item;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Equipment;
import net.minecraft.item.Item;

/** The rotting face worn by players who have become SCP-049-2 (rendered on the head; vanishes on death). */
public class Scp0492MaskItem extends Item implements Equipment {
    public Scp0492MaskItem(Settings settings) {
        super(settings);
    }

    @Override
    public EquipmentSlot getSlotType() {
        return EquipmentSlot.HEAD;
    }
}
