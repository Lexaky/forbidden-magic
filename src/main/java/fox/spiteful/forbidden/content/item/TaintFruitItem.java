package fox.spiteful.forbidden.content.item;

import com.leclowndu93150.thaumaturge.api.warp.WarpHelper;
import com.leclowndu93150.thaumaturge.api.warp.WarpType;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Tainted fruit (FM {@code ItemFruitTainted}): 4 hunger / 0.8 saturation, then one sticky warp, Flux Taint and Hunger
 * for 30 s, and a 40% chance of Thaumarhia. Effects come from Thaumaturge by id; a missing effect is simply skipped.
 */
public final class TaintFruitItem extends Item {
    private static final int DURATION = 600;

    public TaintFruitItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide() && entity instanceof ServerPlayer player) {
            WarpHelper.addWarp(player, 1, WarpType.NORMAL);
            effect("flux_taint").ifPresent(effect -> player.addEffect(new MobEffectInstance(effect, DURATION, 0, false, true)));
            player.addEffect(new MobEffectInstance(MobEffects.HUNGER, DURATION, 0, false, true));
            if (level.getRandom().nextFloat() < 0.4F) {
                player.sendSystemMessage(Component.translatable("message.forbidden.taint_fruit").withStyle(ChatFormatting.DARK_PURPLE));
                effect("thaumarhia").ifPresent(effect -> player.addEffect(new MobEffectInstance(effect, DURATION, 0, true, true)));
            }
        }
        return super.finishUsingItem(stack, level, entity);
    }

    private static Optional<Holder.Reference<MobEffect>> effect(String path) {
        return BuiltInRegistries.MOB_EFFECT.get(Identifier.fromNamespaceAndPath("thaumaturge", path));
    }
}
