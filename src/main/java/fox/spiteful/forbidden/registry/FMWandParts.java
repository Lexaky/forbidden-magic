package fox.spiteful.forbidden.registry;

import com.leclowndu93150.thaumaturge.api.wands.WandRod;
import fox.spiteful.forbidden.Forbidden;
import fox.spiteful.forbidden.content.wand.InfernalRodUpdate;
import fox.spiteful.forbidden.content.wand.ProfaneRodUpdate;
import fox.spiteful.forbidden.content.wand.TaintedRodUpdate;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Wand rods, registered straight into Thaumaturge's {@code thaumaturge:wand_rod} registry. Capacity and craft cost are
 * Forbidden Magic's original numbers.
 */
public final class FMWandParts {
    public static final DeferredRegister<WandRod> RODS = DeferredRegister.create(WandRod.REGISTRY_KEY, Forbidden.MODID);

    public static final DeferredHolder<WandRod, WandRod> ROD_TAINTED = RODS.register("tainted", () -> rod(150, 12, "tainted", new TaintedRodUpdate(), "rod_tainted"));
    public static final DeferredHolder<WandRod, WandRod> ROD_INFERNAL = RODS.register("infernal", () -> rod(150, 12, "infernal", new InfernalRodUpdate(), "rod_infernal"));
    public static final DeferredHolder<WandRod, WandRod> ROD_PROFANE = RODS.register("profane", () -> rod(50, 12, "profane", new ProfaneRodUpdate(), "rod_profane"));
    /** What a profane rod turns into once its contract is spent. Has no item, so no assembly recipe exists for it. */
    public static final DeferredHolder<WandRod, WandRod> ROD_PROFANED = RODS.register("profaned", () -> rod(50, 1000, "profaned", null, "rod_profane"));

    private FMWandParts() {}

    private static WandRod rod(int capacity, int craftCost, String texture, com.leclowndu93150.thaumaturge.api.wands.IWandRodOnUpdate update, String research) {
        Identifier tex = Forbidden.id("textures/models/wand_rod_" + texture + ".png");
        return new WandRod(capacity, craftCost, tex, update, false, false, false, Forbidden.id(research));
    }

    public static void register(IEventBus modBus) {
        RODS.register(modBus);
    }
}
