package fox.spiteful.forbidden.content.wand;

import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.api.wands.IWandRodOnUpdate;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Infernal rod (FM {@code InfernalWandUpdate}): every 100 ticks Ignis recharges up to 20% of capacity and, in the
 * Nether, the other primals up to 10%. The holder is extinguished and cured of Wither. Immunity to fire and wither
 * damage while holding the wand is handled in {@link fox.spiteful.forbidden.event.FMCombatEvents}.
 */
public final class InfernalRodUpdate implements IWandRodOnUpdate {
    @Override
    public void onUpdate(ItemStack wand, Player player) {
        if (player.level().isClientSide()) {
            return;
        }
        if (player.tickCount % 100 == 0) {
            if (player.level().dimension() == Level.NETHER) {
                for (ResourceKey<IAspect> primal : RodUpdates.PRIMALS) {
                    if (primal != TTAspects.IGNIS) {
                        RodUpdates.topUpBelow(wand, primal, 10);
                    }
                }
            }
            RodUpdates.topUpBelow(wand, TTAspects.IGNIS, 5);
        }
        if (player.isOnFire()) {
            player.clearFire();
        }
        if (player.hasEffect(MobEffects.WITHER)) {
            player.removeEffect(MobEffects.WITHER);
        }
    }
}
