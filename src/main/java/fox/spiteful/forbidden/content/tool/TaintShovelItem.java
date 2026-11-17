package fox.spiteful.forbidden.content.tool;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Shovel of the Purifier (FM {@code ItemTaintShovel}). Right-click purges flux goo in an 11x9x11 box around the clicked
 * block, costing one durability per block (max 15). Fast digging of taint blocks comes from Thaumaturge's own
 * {@code thaumaturge:taint_tools} tag; the extra taint shard drop lives in {@link fox.spiteful.forbidden.event.FMHarvestEvents}.
 *
 * <p>FM also cleared flux gas. Thaumaturge has no flux gas block, only the flux goo fluid.
 */
public final class TaintShovelItem extends ShovelItem {
    private static final Identifier FLUX_GOO = Identifier.fromNamespaceAndPath("thaumaturge", "flux_goo");

    public TaintShovelItem(ToolMaterial material, float damage, float speed, Properties properties) {
        super(material, damage, speed, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        BlockPos origin = context.getClickedPos();
        Block goo = BuiltInRegistries.BLOCK.getValue(FLUX_GOO);
        if (goo == Blocks.AIR) {
            return super.useOn(context);
        }
        int purified = 0;
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-5, -4, -5), origin.offset(5, 4, 5))) {
            BlockState state = level.getBlockState(pos);
            if (state.is(goo)) {
                purified++;
                if (!level.isClientSide()) {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                    ((ServerLevel) level).sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 3, 0.3, 0.3, 0.3, 0.01);
                }
            }
        }
        if (purified == 0) {
            return super.useOn(context);
        }
        if (!level.isClientSide() && player != null) {
            context.getItemInHand().hurtAndBreak(Math.min(purified, 15), player, context.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
            level.playSound(null, origin, SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.4F, 0.4F + level.getRandom().nextFloat() * 0.2F);
        }
        return InteractionResult.SUCCESS;
    }
}
