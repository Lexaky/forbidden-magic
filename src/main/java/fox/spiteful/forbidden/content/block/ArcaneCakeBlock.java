package fox.spiteful.forbidden.content.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Thaumic Cake (FM {@code BlockArcaneCake}): twelve slices, each restoring 2 hunger with a 1.0 saturation modifier, and a
 * random tick that grows one eaten slice back. The block drops nothing when broken, as in FM.
 */
public class ArcaneCakeBlock extends Block {
    public static final MapCodec<ArcaneCakeBlock> CODEC = simpleCodec(ArcaneCakeBlock::new);
    public static final int SLICES = 12;
    public static final IntegerProperty EATEN = IntegerProperty.create("eaten", 0, SLICES - 1);
    private static final VoxelShape[] SHAPES = new VoxelShape[SLICES];

    static {
        for (int eaten = 0; eaten < SLICES; eaten++) {
            SHAPES[eaten] = Block.box(1.0 + 14.0 * eaten / SLICES, 0.0, 1.0, 15.0, 8.0, 15.0);
        }
    }

    public ArcaneCakeBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(EATEN, 0));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(EATEN)];
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        return eat(level, pos, state, player);
    }

    @Override
    protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
        // FM also ate a slice when the cake was left-clicked.
        eat(level, pos, state, player);
    }

    public static InteractionResult eat(LevelAccessor level, BlockPos pos, BlockState state, Player player) {
        if (!player.canEat(false)) {
            return InteractionResult.PASS;
        }
        player.getFoodData().eat(2, 1.0F);
        level.gameEvent(player, GameEvent.EAT, pos);
        int eaten = state.getValue(EATEN) + 1;
        if (eaten >= SLICES) {
            level.removeBlock(pos, false);
            level.gameEvent(player, GameEvent.BLOCK_DESTROY, pos);
        } else {
            level.setBlock(pos, state.setValue(EATEN, eaten), Block.UPDATE_ALL);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return state.getValue(EATEN) > 0;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int eaten = state.getValue(EATEN);
        if (eaten > 0) {
            level.setBlock(pos, state.setValue(EATEN, eaten - 1), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction, BlockPos neighbourPos,
            BlockState neighbourState, RandomSource random) {
        return direction == Direction.DOWN && !state.canSurvive(level, pos) ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).isSolid();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(EATEN);
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }
}
