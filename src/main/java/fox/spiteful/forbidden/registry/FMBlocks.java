package fox.spiteful.forbidden.registry;

import fox.spiteful.forbidden.Forbidden;
import fox.spiteful.forbidden.content.block.ArcaneCakeBlock;
import fox.spiteful.forbidden.content.block.UmbralBushBlock;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TintedParticleLeavesBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Blocks (FM {@code ForbiddenBlocks}). Metadata variants are flattened into separate blocks. */
public final class FMBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Forbidden.MODID);

    public static final ResourceKey<ConfiguredFeature<?, ?>> TAINTED_TREE = ResourceKey.create(Registries.CONFIGURED_FEATURE, Forbidden.id("tainted_tree"));
    public static final TreeGrower TAINTED = new TreeGrower("forbidden:tainted", Optional.empty(), Optional.of(TAINTED_TREE), Optional.empty());

    public static final DeferredBlock<ArcaneCakeBlock> ARCANE_CAKE = BLOCKS.registerBlock("arcane_cake", ArcaneCakeBlock::new,
            props -> props.forceSolidOn().strength(0.5F).sound(SoundType.WOOL).randomTicks().pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<FlowerBlock> UMBRAL_ROSE = BLOCKS.registerBlock("umbral_rose", props -> new FlowerBlock(MobEffects.BLINDNESS, 8.0F, props),
            props -> props.mapColor(MapColor.COLOR_BLACK).noCollision().instabreak().sound(SoundType.GRASS).offsetType(BlockBehaviour.OffsetType.XZ)
                    .pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<UmbralBushBlock> UMBRAL_BUSH = BLOCKS.registerBlock("umbral_bush", UmbralBushBlock::new,
            props -> props.mapColor(MapColor.COLOR_BLACK).noCollision().instabreak().sound(SoundType.GRASS).randomTicks().offsetType(BlockBehaviour.OffsetType.XZ)
                    .ignitedByLava().pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<Block> NETHER_STAR_BLOCK = BLOCKS.registerBlock("nether_star_block", Block::new,
            props -> props.mapColor(MapColor.QUARTZ).strength(5.0F, 1200.0F).sound(SoundType.METAL).lightLevel(state -> 7).requiresCorrectToolForDrops());
    public static final DeferredBlock<RotatedPillarBlock> TAINT_LOG = BLOCKS.registerBlock("taint_log", RotatedPillarBlock::new,
            props -> props.mapColor(MapColor.COLOR_PURPLE).strength(2.0F).sound(SoundType.WOOD).ignitedByLava());
    public static final DeferredBlock<Block> TAINT_PLANKS = BLOCKS.registerBlock("taint_planks", Block::new,
            props -> props.mapColor(MapColor.COLOR_PURPLE).strength(2.0F, 3.0F).sound(SoundType.WOOD).ignitedByLava());
    public static final DeferredBlock<TintedParticleLeavesBlock> TAINT_LEAVES = BLOCKS.registerBlock("taint_leaves", props -> new TintedParticleLeavesBlock(0.01F, props),
            props -> props.mapColor(MapColor.COLOR_PURPLE).strength(0.2F).randomTicks().sound(SoundType.SLIME_BLOCK).noOcclusion().ignitedByLava()
                    .isValidSpawn((s, l, p, e) -> false).isSuffocating((s, l, p) -> false).isViewBlocking((s, l, p) -> false).pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<SaplingBlock> TAINT_SAPLING = BLOCKS.registerBlock("taint_sapling", props -> new SaplingBlock(TAINTED, props),
            props -> props.mapColor(MapColor.COLOR_PURPLE).noCollision().randomTicks().instabreak().sound(SoundType.SLIME_BLOCK).pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<Block> TAINT_STONE = BLOCKS.registerBlock("taint_stone", Block::new,
            props -> props.mapColor(MapColor.COLOR_PURPLE).strength(2.0F, 10.0F).sound(SoundType.STONE).requiresCorrectToolForDrops());
    public static final DeferredBlock<Block> TAINT_STONE_BRICKS = BLOCKS.registerBlock("taint_stone_bricks", Block::new,
            props -> props.mapColor(MapColor.COLOR_PURPLE).strength(2.0F, 10.0F).sound(SoundType.STONE).requiresCorrectToolForDrops());

    public static final DeferredBlock<fox.spiteful.forbidden.content.block.WrathCageBlock> WRATH_CAGE = BLOCKS.registerBlock("wrath_cage",
            fox.spiteful.forbidden.content.block.WrathCageBlock::new,
            props -> props.mapColor(MapColor.METAL).strength(5.0F, 2000.0F).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops());

    public static final DeferredItem<BlockItem> WRATH_CAGE_ITEM = FMItems.ITEMS.registerSimpleBlockItem(WRATH_CAGE, props -> props.rarity(Rarity.RARE));
    public static final DeferredItem<BlockItem> ARCANE_CAKE_ITEM = FMItems.ITEMS.registerSimpleBlockItem(ARCANE_CAKE, props -> props.stacksTo(1));
    public static final DeferredItem<BlockItem> UMBRAL_ROSE_ITEM = FMItems.ITEMS.registerSimpleBlockItem(UMBRAL_ROSE);
    public static final DeferredItem<BlockItem> UMBRAL_BUSH_ITEM = FMItems.ITEMS.registerItem("umbral_bush",
            props -> new net.minecraft.world.item.DoubleHighBlockItem(UMBRAL_BUSH.get(), props.useBlockDescriptionPrefix()));
    public static final DeferredItem<BlockItem> NETHER_STAR_BLOCK_ITEM = FMItems.ITEMS.registerSimpleBlockItem(NETHER_STAR_BLOCK, props -> props.rarity(Rarity.UNCOMMON));
    public static final DeferredItem<BlockItem> TAINT_LOG_ITEM = FMItems.ITEMS.registerSimpleBlockItem(TAINT_LOG);
    public static final DeferredItem<BlockItem> TAINT_PLANKS_ITEM = FMItems.ITEMS.registerSimpleBlockItem(TAINT_PLANKS);
    public static final DeferredItem<BlockItem> TAINT_LEAVES_ITEM = FMItems.ITEMS.registerSimpleBlockItem(TAINT_LEAVES);
    public static final DeferredItem<BlockItem> TAINT_SAPLING_ITEM = FMItems.ITEMS.registerSimpleBlockItem(TAINT_SAPLING);
    public static final DeferredItem<BlockItem> TAINT_STONE_ITEM = FMItems.ITEMS.registerSimpleBlockItem(TAINT_STONE);
    public static final DeferredItem<BlockItem> TAINT_STONE_BRICKS_ITEM = FMItems.ITEMS.registerSimpleBlockItem(TAINT_STONE_BRICKS);

    private FMBlocks() {}

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
    }
}
