package fox.spiteful.forbidden.content.item;

import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.api.wands.WandVis;
import fox.spiteful.forbidden.compat.CuriosBridge;
import fox.spiteful.forbidden.event.FMCombatEvents;
import fox.spiteful.forbidden.registry.FMDataComponents;
import fox.spiteful.forbidden.util.WandBridge;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * Collar of Pain (FM {@code ItemSubCollar}, a TC4 vis amulet). It stores up to 250 vis of each primal and fills itself
 * from the wearer's pain: every point of damage taken adds three random vis (more when the blow comes from a riding
 * crop). While worn in the necklace slot it feeds that vis into the wand held in the main hand, like TC4's vis amulet.
 *
 * <p>Thaumaturge's own vis amulets draw from the aura instead of storing vis, so the store is this addon's own
 * {@code forbidden:stored_vis} component (Thaumaturge's public {@link WandVis} type). Right-clicking another player
 * places the collar on them if their necklace slot is empty, recording the owner, as in FM.
 */
public final class CollarItem extends Item {
    public static final int CAPACITY = 25000;
    /** Centivis per primal moved into the held wand every {@link #PULSE_TICKS} ticks. */
    public static final int PULSE = 25;
    public static final int PULSE_TICKS = 5;

    public CollarItem(Properties properties) {
        super(properties);
    }

    public static WandVis stored(ItemStack collar) {
        return collar.getOrDefault(FMDataComponents.STORED_VIS.get(), WandVis.EMPTY);
    }

    /** Adds {@code doses} vis, one random primal at a time (FM: {@code addVis(amulet, primal, 1, true)} per dose). */
    public static void feed(ItemStack collar, int doses, RandomSource random) {
        WandVis vis = stored(collar);
        for (int i = 0; i < doses; i++) {
            ResourceKey<IAspect> primal = TTAspects.PRIMALS.get(random.nextInt(TTAspects.PRIMALS.size()));
            vis = vis.with(primal, Math.min(CAPACITY, vis.amount(primal) + 100));
        }
        collar.set(FMDataComponents.STORED_VIS.get(), vis);
    }

    /** Moves stored vis into the wearer's held wand. */
    public static void wornTick(ItemStack collar, LivingEntity wearer) {
        if (wearer.level().isClientSide() || wearer.tickCount % PULSE_TICKS != 0) {
            return;
        }
        ItemStack wand = wearer.getMainHandItem();
        if (!wand.is(FMCombatEvents.WANDS)) {
            return;
        }
        WandVis vis = stored(collar);
        boolean changed = false;
        for (ResourceKey<IAspect> primal : TTAspects.PRIMALS) {
            int moved = WandBridge.addVis(wand, primal, Math.min(PULSE, vis.amount(primal)));
            if (moved > 0) {
                vis = vis.with(primal, vis.amount(primal) - moved);
                changed = true;
            }
        }
        if (changed) {
            collar.set(FMDataComponents.STORED_VIS.get(), vis);
        }
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof Player sub)) {
            return InteractionResult.PASS;
        }
        if (player.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        ItemStack collar = stack.copyWithCount(1);
        collar.set(FMDataComponents.OWNER.get(), player.getName().getString());
        if (CuriosBridge.equipNecklace(sub, collar)) {
            stack.shrink(1);
            sub.sendSystemMessage(Component.translatable("message.forbidden.collar.placescollar", player.getName()));
            player.sendSystemMessage(Component.translatable("message.forbidden.collar.youplacecollar", sub.getName()));
        } else {
            player.sendSystemMessage(Component.translatable("message.forbidden.collar.alreadywearing", sub.getName()));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable("tooltip.forbidden.capacity", CAPACITY / 100).withStyle(ChatFormatting.GOLD));
        WandVis vis = stored(stack);
        for (ResourceKey<IAspect> primal : TTAspects.PRIMALS) {
            int amount = vis.amount(primal);
            if (amount > 0) {
                builder.accept(Component.translatable("aspect.thaumaturge." + primal.identifier().getPath()).append(String.format(" x %.2f", amount / 100.0F)));
            }
        }
        String owner = stack.get(FMDataComponents.OWNER.get());
        if (owner != null) {
            builder.accept(Component.translatable("tooltip.forbidden.collar.owner", owner).withStyle(ChatFormatting.DARK_PURPLE));
        }
    }
}
