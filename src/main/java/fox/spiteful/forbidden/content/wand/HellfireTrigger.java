package fox.spiteful.forbidden.content.wand;

import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.api.capability.KnowledgeAccess;
import com.leclowndu93150.thaumaturge.api.casters.CasterTriggerRegistry;
import com.leclowndu93150.thaumaturge.api.casters.ICasterTriggerManager;
import fox.spiteful.forbidden.Forbidden;
import fox.spiteful.forbidden.event.FMCombatEvents;
import fox.spiteful.forbidden.util.WandBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/**
 * Hellfire (FM {@code WandOverlord}): after the Hellfire research, right-clicking the top of obsidian or netherrack with
 * a focus-less wand lights an eternal fire on it for 2 centivis of Ignis.
 */
public final class HellfireTrigger implements ICasterTriggerManager {
    public static final int EVENT = 1;
    public static final int COST = 2;
    public static final Identifier RESEARCH = Forbidden.id("hellfire");
    private static final Identifier SOCKETED_FOCUS = Identifier.fromNamespaceAndPath("thaumaturge", "socketed_focus");

    public static void register() {
        HellfireTrigger trigger = new HellfireTrigger();
        CasterTriggerRegistry.registerCasterBlockTrigger(trigger, EVENT, Blocks.OBSIDIAN.defaultBlockState(), Forbidden.MODID);
        CasterTriggerRegistry.registerCasterBlockTrigger(trigger, EVENT, Blocks.NETHERRACK.defaultBlockState(), Forbidden.MODID);
    }

    @Override
    public boolean performTrigger(Level level, ItemStack wand, Player player, BlockPos pos, Direction side, int event) {
        if (event != EVENT || side != Direction.UP || level.isClientSide() || !wand.is(FMCombatEvents.WANDS)) {
            return false;
        }
        var focusType = BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(SOCKETED_FOCUS);
        if (focusType != null && wand.has(focusType)) {
            return false;
        }
        if (!KnowledgeAccess.of(player).isResearchComplete(RESEARCH) || !level.isEmptyBlock(pos.above())) {
            return false;
        }
        int ignis = WandBridge.vis(wand, TTAspects.IGNIS);
        if (ignis < COST && !player.getAbilities().instabuild) {
            return false;
        }
        if (!player.getAbilities().instabuild) {
            WandBridge.setVis(wand, TTAspects.IGNIS, ignis - COST);
        }
        level.playSound(null, pos.above(), SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, level.getRandom().nextFloat() * 0.4F + 0.8F);
        level.setBlockAndUpdate(pos.above(), Blocks.FIRE.defaultBlockState());
        player.swing(player.getUsedItemHand());
        return true;
    }
}
