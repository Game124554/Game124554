package net.scpfoundation.item;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsage;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.scpfoundation.registry.ModItems;

import java.util.Locale;

/** A cup dispensed by SCP-294. NBT "Drink" holds the drink id. */
public class Scp294CupItem extends Item {
    public static final String KEY = "Drink";

    public enum Drink {
        COFFEE("coffee", "coffee", "espresso", "latte", "cappuccino"),
        TEA("tea", "tea", "chai"),
        HOT_CHOCOLATE("hot_chocolate", "chocolate", "cocoa"),
        WATER("water", "water"),
        MILK("milk", "milk"),
        LAVA("lava", "lava", "magma", "fire"),
        COURAGE("courage", "courage", "strength", "power"),
        JOY("joy", "joy", "happiness", "love"),
        DESPAIR("despair", "despair", "sadness", "tears"),
        STARLIGHT("starlight", "starlight", "star", "night", "moon"),
        ANTIMATTER("antimatter", "antimatter", "explosion"),
        AIR("air", "air", "nothing", "void"),
        QUANTUM("quantum", "quantum", "teleport", "soup"),
        COLA("cola", "cola", "soda", "207");

        public final String id;
        private final String[] keywords;

        Drink(String id, String... keywords) {
            this.id = id;
            this.keywords = keywords;
        }

        public static Drink byId(String id) {
            for (Drink d : values()) if (d.id.equals(id)) return d;
            return WATER;
        }

        /** Matches a requested drink name ("a hot cup of coffee please") to a drink, or null. */
        public static Drink match(String request) {
            String r = request.toLowerCase(Locale.ROOT);
            for (Drink d : values()) {
                for (String k : d.keywords) if (r.contains(k)) return d;
            }
            return null;
        }

        public static Drink random(Random random) {
            Drink[] all = values();
            return all[random.nextInt(all.length)];
        }
    }

    public Scp294CupItem(Settings settings) {
        super(settings);
    }

    public static ItemStack create(Drink drink) {
        ItemStack stack = new ItemStack(ModItems.SCP_294_CUP);
        stack.getOrCreateNbt().putString(KEY, drink.id);
        return stack;
    }

    public static Drink drinkOf(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        return Drink.byId(nbt != null ? nbt.getString(KEY) : "water");
    }

    @Override
    public Text getName(ItemStack stack) {
        return Text.translatable("item.scpfoundation.scp_294_cup.named", Text.translatable("drink.scpfoundation." + drinkOf(stack).id));
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        return ItemUsage.consumeHeldItem(world, user, hand);
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.DRINK;
    }

    @Override
    public int getMaxUseTime(ItemStack stack) {
        return 32;
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        if (!world.isClient) {
            apply(drinkOf(stack), world, user);
        }
        if (!(user instanceof PlayerEntity player) || !player.getAbilities().creativeMode) {
            stack.decrement(1);
        }
        return stack;
    }

    private static void apply(Drink drink, World world, LivingEntity user) {
        switch (drink) {
            case COFFEE -> {
                user.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 1200, 0));
                user.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 1200, 0));
            }
            case TEA -> user.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 200, 0));
            case HOT_CHOCOLATE -> {
                user.addStatusEffect(new StatusEffectInstance(StatusEffects.SATURATION, 10, 1));
                user.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 1200, 0));
            }
            case WATER -> user.extinguish();
            case MILK -> user.clearStatusEffects();
            case LAVA -> {
                user.setOnFireFor(5);
                user.damage(world.getDamageSources().inFire(), 4.0f);
            }
            case COURAGE -> user.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 600, 1));
            case JOY -> {
                user.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 1200, 1));
                user.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 100, 1));
            }
            case DESPAIR -> {
                user.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 600, 1));
                user.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 600, 0));
            }
            case STARLIGHT -> {
                user.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 2400, 0));
                user.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 600, 0));
            }
            case ANTIMATTER -> world.createExplosion(null, user.getX(), user.getY(), user.getZ(), 2.0f, World.ExplosionSourceType.NONE);
            case AIR -> user.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, 60, 0));
            case QUANTUM -> {
                Random r = user.getRandom();
                for (int i = 0; i < 16; i++) {
                    double x = user.getX() + (r.nextDouble() - 0.5) * 32.0;
                    double y = user.getY() + (r.nextInt(16) - 8);
                    double z = user.getZ() + (r.nextDouble() - 0.5) * 32.0;
                    if (user.teleport(x, y, z, true)) break;
                }
            }
            case COLA -> {
                user.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 1200, 1));
                user.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 200, 0));
            }
        }
    }
}
