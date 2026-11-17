package fox.spiteful.forbidden.content.wand;

import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.taint.TaintApi;
import com.leclowndu93150.thaumaturge.api.wands.IWandRodOnUpdate;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Tainted rod. FM: every 100 ticks inside the Tainted Land biome, each primal is topped up by 1 vis while under 10%.
 * Thaumaturge has no taint biome (taint is a block-level infection), so "standing in tainted land" becomes
 * {@link TaintApi#isTainted} at or under the player's feet.
 */
public final class TaintedRodUpdate implements IWandRodOnUpdate {
    @Override
    public void onUpdate(ItemStack wand, Player player) {
        if (player.level().isClientSide() || player.tickCount % 100 != 0) {
            return;
        }
        if (!TaintApi.isTainted(player.level(), player.blockPosition()) && !TaintApi.isTainted(player.level(), player.blockPosition().below())) {
            return;
        }
        for (ResourceKey<IAspect> primal : RodUpdates.PRIMALS) {
            RodUpdates.topUpBelow(wand, primal, 10);
        }
    }
}
