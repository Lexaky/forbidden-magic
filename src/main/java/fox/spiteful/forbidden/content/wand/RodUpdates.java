package fox.spiteful.forbidden.content.wand;

import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import fox.spiteful.forbidden.util.WandBridge;
import java.util.List;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;

/** Shared helpers for FM rod update hooks. Vis amounts are in centivis (100 = 1 vis). */
final class RodUpdates {
    static final int ONE_VIS = 100;
    static final List<ResourceKey<IAspect>> PRIMALS = TTAspects.PRIMALS;

    private RodUpdates() {}

    /** FM's recharge rule: +1 vis of the aspect while it is below {@code capacity / divisor}. */
    static void topUpBelow(ItemStack wand, ResourceKey<IAspect> aspect, int divisor) {
        int ceiling = WandBridge.capacityCentivis(wand) / divisor;
        if (WandBridge.vis(wand, aspect) < ceiling) {
            WandBridge.addVis(wand, aspect, ONE_VIS);
        }
    }
}
