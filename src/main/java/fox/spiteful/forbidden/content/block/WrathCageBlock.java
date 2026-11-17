package fox.spiteful.forbidden.content.block;

import com.mojang.serialization.MapCodec;
import fox.spiteful.forbidden.FMConfig;
import fox.spiteful.forbidden.content.item.MobCrystalItem;
import fox.spiteful.forbidden.registry.FMBlockEntities;
import fox.spiteful.forbidden.registry.FMItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * Wrath Cage block (FM {@code BlockWrathCage}). Using an imprinted crystal on it installs that mob and hands back the
 * previous crystal; using the Diabolist Fork cycles which essentia the cage asks for (mob, Ira, Desidia).
 */
public class WrathCageBlock extends BaseEntityBlock {
    public static final MapCodec<WrathCageBlock> CODEC = simpleCodec(WrathCageBlock::new);
    private static final String[] MODE_KEYS = {"message.forbidden.wrath_cage.mode.mob", "message.forbidden.wrath_cage.mode.ira", "message.forbidden.wrath_cage.mode.desidia"};

    public WrathCageBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WrathCageBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, FMBlockEntities.WRATH_CAGE.get(), WrathCageBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof WrathCageBlockEntity cage)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (stack.is(FMItems.MOB_CRYSTAL.get())) {
            var type = MobCrystalItem.mob(stack);
            if (type.isEmpty()) {
                return InteractionResult.TRY_WITH_EMPTY_HAND;
            }
            if (!level.isClientSide()) {
                ItemStack previous = cage.swapCrystal(type.get());
                stack.shrink(1);
                if (!previous.isEmpty() && !player.getInventory().add(previous)) {
                    player.drop(previous, false);
                }
            }
            return InteractionResult.SUCCESS;
        }
        if (stack.is(FMItems.DIABOLIST_FORK.get()) && FMConfig.wrathCost() > 0) {
            if (!level.isClientSide()) {
                int mode = cage.cycleMode();
                player.sendOverlayMessage(Component.translatable(MODE_KEYS[mode]));
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }
}
