package fox.spiteful.forbidden.content.block;

import fox.spiteful.forbidden.registry.FMBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.TallFlowerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * Umbral Rose Bush (FM {@code BlockRoseBush}): a two-block bush that seeds Umbral Roses around itself. A random tick
 * spreads a rose 30% of the time; bone meal always tries to (instead of dropping a copy of the bush, as tall flowers do).
 */
public class UmbralBushBlock extends TallFlowerBlock {
    public UmbralBushBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(10) <= 2) {
            spreadFlower(level, pos, random);
        }
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        BlockPos base = state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
        spreadFlower(level, base, random);
    }

    /** FM's walk: up to eight random steps of at most one block, then a rose is planted where there is room. */
    public static boolean spreadFlower(ServerLevel level, BlockPos origin, RandomSource random) {
        BlockState rose = FMBlocks.UMBRAL_ROSE.get().defaultBlockState();
        BlockPos current = origin;
        BlockPos candidate = step(current, random);
        for (int i = 0; i < 8; i++) {
            if (level.isEmptyBlock(candidate) && rose.canSurvive(level, candidate)) {
                current = candidate;
            }
            candidate = step(current, random);
        }
        if (level.isEmptyBlock(candidate) && rose.canSurvive(level, candidate)) {
            level.setBlock(candidate, rose, Block.UPDATE_CLIENTS);
            return true;
        }
        return false;
    }

    private static BlockPos step(BlockPos pos, RandomSource random) {
        return pos.offset(random.nextInt(3) - 1, random.nextInt(2) - random.nextInt(2), random.nextInt(3) - 1);
    }
}
