package fox.spiteful.forbidden.gametest;

import fox.spiteful.forbidden.Forbidden;
import fox.spiteful.forbidden.content.block.ArcaneCakeBlock;
import fox.spiteful.forbidden.content.block.UmbralBushBlock;
import fox.spiteful.forbidden.registry.FMBlocks;
import fox.spiteful.forbidden.registry.FMItems;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Block behaviour checks for milestone M6. */
final class FMBlockTests {
    private static final List<String> RECIPES = List.of("infusion/arcane_cake", "crucible/umbral_bush", "crucible/taint_sapling", "black_ink", "taint_planks",
            "taint_coal", "arcane/taint_stone", "taint_stone_bricks", "nether_star_block", "nether_star_from_block");

    private FMBlockTests() {}

    static void register(BiConsumer<String, java.util.function.Consumer<GameTestHelper>> add) {
        add.accept("block/recipes_and_research_loaded", helper -> {
            var recipes = helper.getLevel().getServer().getRecipeManager();
            for (String key : RECIPES) {
                check(helper, recipes.byKey(ResourceKey.create(Registries.RECIPE, Forbidden.id(key))).isPresent(), "recipe forbidden:" + key + " missing");
            }
            var research = helper.getLevel().registryAccess().lookupOrThrow(com.leclowndu93150.thaumaturge.api.research.IResearchEntry.REGISTRY_KEY);
            for (String key : List.of("arcane_cake", "black_flower", "taint_tree", "taint_stone")) {
                check(helper, research.get(ResourceKey.create(com.leclowndu93150.thaumaturge.api.research.IResearchEntry.REGISTRY_KEY, Forbidden.id(key))).isPresent(),
                        "research forbidden:" + key + " missing");
            }
            check(helper, FMBlocks.NETHER_STAR_BLOCK.get().defaultBlockState().is(BlockTags.BEACON_BASE_BLOCKS), "star block must be a beacon base");
            check(helper, FMBlocks.TAINT_LOG.get().defaultBlockState().is(BlockTags.LOGS), "taint log must count as a log for leaf decay");
            helper.succeed();
        });

        add.accept("block/loot_tables", helper -> {
            ServerLevel level = helper.getLevel();
            BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
            for (Block block : List.of(FMBlocks.TAINT_LOG.get(), FMBlocks.TAINT_PLANKS.get(), FMBlocks.TAINT_STONE.get(), FMBlocks.TAINT_STONE_BRICKS.get(),
                    FMBlocks.NETHER_STAR_BLOCK.get(), FMBlocks.UMBRAL_ROSE.get(), FMBlocks.TAINT_SAPLING.get())) {
                List<ItemStack> drops = Block.getDrops(block.defaultBlockState(), level, pos, null);
                check(helper, drops.size() == 1 && drops.getFirst().is(block.asItem()), block + " should drop itself, got " + drops);
            }
            List<ItemStack> upper = Block.getDrops(FMBlocks.UMBRAL_BUSH.get().defaultBlockState().setValue(DoublePlantBlock.HALF,
                    net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER), level, pos, null);
            check(helper, upper.isEmpty(), "upper half of the bush must not drop");
            check(helper, Block.getDrops(FMBlocks.ARCANE_CAKE.get().defaultBlockState(), level, pos, null).isEmpty(), "the cake drops nothing, as in FM");
            helper.succeed();
        });

        add.accept("block/arcane_cake_twelve_slices", helper -> {
            BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
            ServerLevel level = helper.getLevel();
            level.setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(pos, FMBlocks.ARCANE_CAKE.get().defaultBlockState());
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            player.getFoodData().setFoodLevel(0);
            ArcaneCakeBlock.eat(level, pos, level.getBlockState(pos), player);
            check(helper, level.getBlockState(pos).getValue(ArcaneCakeBlock.EATEN) == 1, "one slice eaten");
            check(helper, player.getFoodData().getFoodLevel() == 2, "a slice restores 2 hunger, got " + player.getFoodData().getFoodLevel());
            // A random tick grows the slice back.
            level.getBlockState(pos).randomTick(level, pos, level.getRandom());
            check(helper, level.getBlockState(pos).getValue(ArcaneCakeBlock.EATEN) == 0, "random tick should regrow a slice");
            for (int i = 0; i < ArcaneCakeBlock.SLICES; i++) {
                player.getFoodData().setFoodLevel(0);
                ArcaneCakeBlock.eat(level, pos, level.getBlockState(pos), player);
            }
            check(helper, level.getBlockState(pos).isAir(), "cake should be gone after 12 slices");
            helper.succeed();
        });

        add.accept("block/umbral_bush_spreads_roses", helper -> {
            ServerLevel level = helper.getLevel();
            for (BlockPos p : BlockPos.betweenClosed(helper.absolutePos(new BlockPos(0, 1, 0)), helper.absolutePos(new BlockPos(4, 1, 4)))) {
                level.setBlockAndUpdate(p, Blocks.GRASS_BLOCK.defaultBlockState());
            }
            BlockPos bush = helper.absolutePos(new BlockPos(2, 2, 2));
            DoublePlantBlock.placeAt(level, FMBlocks.UMBRAL_BUSH.get().defaultBlockState(), bush, Block.UPDATE_ALL);
            boolean spread = false;
            for (int i = 0; i < 100 && !spread; i++) {
                spread = UmbralBushBlock.spreadFlower(level, bush, level.getRandom());
            }
            check(helper, spread, "the bush never seeded a rose in 100 tries");
            long roses = BlockPos.betweenClosedStream(helper.absolutePos(new BlockPos(0, 1, 0)), helper.absolutePos(new BlockPos(4, 4, 4)))
                    .filter(p -> level.getBlockState(p).is(FMBlocks.UMBRAL_ROSE.get())).count();
            check(helper, roses >= 1, "no umbral rose found next to the bush");
            helper.succeed();
        });

        add.accept("block/taint_sapling_grows_tree", helper -> {
            ServerLevel level = helper.getLevel();
            BlockPos ground = helper.absolutePos(new BlockPos(2, 1, 2));
            for (BlockPos p : BlockPos.betweenClosed(ground.offset(-2, 1, -2), ground.offset(2, 9, 2))) {
                level.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
            }
            level.setBlockAndUpdate(ground, Blocks.DIRT.defaultBlockState());
            check(helper, level.registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE).get(FMBlocks.TAINTED_TREE).isPresent(),
                    "configured feature forbidden:tainted_tree is not loaded");
            BlockPos sapling = ground.above();
            BlockState state = FMBlocks.TAINT_SAPLING.get().defaultBlockState();
            level.setBlockAndUpdate(sapling, state);
            boolean grown = FMBlocks.TAINTED.growTree(level, level.getChunkSource().getGenerator(), sapling, state, level.getRandom());
            check(helper, grown, "tainted tree failed to grow");
            check(helper, level.getBlockState(sapling).is(FMBlocks.TAINT_LOG.get()), "trunk should start where the sapling was");
            long leaves = BlockPos.betweenClosedStream(sapling.offset(-2, 2, -2), sapling.offset(2, 7, 2))
                    .filter(p -> level.getBlockState(p).is(FMBlocks.TAINT_LEAVES.get())).count();
            check(helper, leaves > 10, "tree has too few tainted leaves: " + leaves);
            helper.succeed();
        });

    }

    private static void check(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            throw helper.assertionException(net.minecraft.network.chat.Component.literal(message));
        }
    }
}
