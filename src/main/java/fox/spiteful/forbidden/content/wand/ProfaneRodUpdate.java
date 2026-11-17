package fox.spiteful.forbidden.content.wand;

import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.capability.IPlayerKnowledge;
import com.leclowndu93150.thaumaturge.api.capability.KnowledgeAccess;
import com.leclowndu93150.thaumaturge.api.wands.IWandRodOnUpdate;
import com.leclowndu93150.thaumaturge.api.warp.WarpHelper;
import com.leclowndu93150.thaumaturge.api.warp.WarpType;
import fox.spiteful.forbidden.Forbidden;
import fox.spiteful.forbidden.registry.FMDataComponents;
import fox.spiteful.forbidden.registry.FMWandParts;
import fox.spiteful.forbidden.util.WandBridge;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Profane rod (FM {@code ProfaneWandUpdate}): a demonic contract worth 25 000 centivis refills the wand for free, with a
 * chance of warp proportional to what was restored. When the contract is spent the rod becomes the inert "profaned" rod
 * and the holder takes one more warp. Holding such a wand the first time unlocks the hidden {@code forbidden:rod_profane}
 * research, as in FM.
 */
public final class ProfaneRodUpdate implements IWandRodOnUpdate {
    public static final int CONTRACT = 25000;

    @Override
    public void onUpdate(ItemStack wand, Player player) {
        if (!(player instanceof ServerPlayer serverPlayer) || player.tickCount % 20 != 0) {
            return;
        }
        if (!wand.has(FMDataComponents.CONTRACT.get())) {
            IPlayerKnowledge knowledge = KnowledgeAccess.of(player);
            if (knowledge.addResearch(Forbidden.id("rod_profane"))) {
                knowledge.sync(serverPlayer);
            }
            wand.set(FMDataComponents.CONTRACT.get(), CONTRACT);
        }
        Result result = honourContract(wand, player.getRandom());
        if (result.warp() > 0) {
            WarpHelper.addWarp(serverPlayer, result.warp(), WarpType.NORMAL);
        }
        if (result.expired()) {
            ((ServerLevel) player.level()).sendParticles(ParticleTypes.EXPLOSION, player.getX(), player.getY() + player.getBbHeight() / 2.0, player.getZ(), 1, 0, 0, 0, 0);
        }
    }

    /**
     * Pays out the contract into the wand. Returns the warp earned and whether the contract ran out (in which case the
     * rod has already been swapped for the profaned rod and one extra warp is included).
     */
    public static Result honourContract(ItemStack wand, RandomSource random) {
        int contract = wand.getOrDefault(FMDataComponents.CONTRACT.get(), CONTRACT);
        if (contract <= 0) {
            return new Result(0, false);
        }
        int warp = 0;
        for (ResourceKey<IAspect> primal : RodUpdates.PRIMALS) {
            if (contract <= 0) {
                break;
            }
            int deficit = WandBridge.capacityCentivis(wand) - WandBridge.vis(wand, primal);
            if (deficit > 0) {
                int restored = WandBridge.addVis(wand, primal, Math.min(contract, deficit));
                contract -= restored;
                if (random.nextInt(2501) < restored) {
                    warp++;
                }
            }
        }
        wand.set(FMDataComponents.CONTRACT.get(), Math.max(0, contract));
        if (contract <= 0) {
            WandBridge.replaceRod(wand, FMWandParts.ROD_PROFANED.getId());
            return new Result(warp + 1, true);
        }
        return new Result(warp, false);
    }

    public record Result(int warp, boolean expired) {}
}
