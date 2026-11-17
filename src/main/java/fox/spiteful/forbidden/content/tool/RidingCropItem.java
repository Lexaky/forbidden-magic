package fox.spiteful.forbidden.content.tool;

import fox.spiteful.forbidden.FMConfig;
import fox.spiteful.forbidden.registry.FMItems;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.golem.AbstractGolem;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Riding crop (FM {@code ItemRidingCrop}). Hitting a horse or pig gives Speed VI; hitting a player or golem gives Speed
 * II, Haste II and Strength II. In the Nether each hit has a 1/15 chance to shake loose a lust shard. Right-click while
 * riding lashes the mount (1 damage, Speed VI).
 */
public final class RidingCropItem extends Item {
    public RidingCropItem(Properties properties) {
        super(properties);
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity victim, LivingEntity attacker) {
        if (victim instanceof AbstractHorse || victim instanceof Pig) {
            victim.addEffect(new MobEffectInstance(MobEffects.SPEED, 200, 5));
        } else if (victim instanceof Player || victim instanceof AbstractGolem) {
            victim.addEffect(new MobEffectInstance(MobEffects.SPEED, 200, 1));
            victim.addEffect(new MobEffectInstance(MobEffects.HASTE, 200, 1));
            victim.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 200, 1));
        }
        Level level = attacker.level();
        if (!level.isClientSide() && FMConfig.lustEnabled() && level.dimension() == Level.NETHER && level.getRandom().nextInt(15) == 1) {
            ItemEntity drop = victim.spawnAtLocation((net.minecraft.server.level.ServerLevel) level, new ItemStack(FMItems.LUST_SHARD.get()), 1.0F);
            if (drop != null) {
                drop.setDeltaMovement(drop.getDeltaMovement().add((level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.1F,
                        level.getRandom().nextFloat() * 0.05F, (level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.1F));
            }
        }
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity victim, LivingEntity attacker) {
        stack.hurtAndBreak(1, attacker, EquipmentSlot.MAINHAND);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        Entity vehicle = player.getVehicle();
        if (vehicle instanceof LivingEntity mount) {
            if (!level.isClientSide()) {
                player.getItemInHand(hand).hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
                mount.hurtServer((net.minecraft.server.level.ServerLevel) level, level.damageSources().generic(), 1.0F);
                mount.addEffect(new MobEffectInstance(MobEffects.SPEED, 200, 5));
            }
            player.swing(hand);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
