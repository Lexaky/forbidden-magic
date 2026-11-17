package fox.spiteful.forbidden.event;

import fox.spiteful.forbidden.FMConfig;
import fox.spiteful.forbidden.Forbidden;
import fox.spiteful.forbidden.registry.FMEnchantments;
import fox.spiteful.forbidden.registry.FMItems;
import fox.spiteful.forbidden.registry.FMTags;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

/** Mining and eating rules from FM's {@code FMEventHandler}. */
@EventBusSubscriber(modid = Forbidden.MODID)
public final class FMHarvestEvents {
    private static final TagKey<Item> VIS_CRYSTALS = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("thaumaturge", "vis_crystals"));

    private FMHarvestEvents() {}

    /**
     * Pickaxe of Distortion (FM {@code onPlayerBreaking}): instant blocks are unbreakable, soft blocks crawl at 0.1, and
     * the harder the block the faster it goes (hardness/2 below 20, 5 + hardness above).
     */
    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (!event.getEntity().getMainHandItem().is(FMItems.TAINT_PICKAXE.get()) || event.getPosition().isEmpty()) {
            return;
        }
        float hardness = event.getState().getDestroySpeed(event.getEntity().level(), event.getPosition().get());
        event.setNewSpeed(taintPickSpeed(hardness));
    }

    public static float taintPickSpeed(float hardness) {
        if (hardness == 0.0F) {
            return 0.0F;
        } else if (hardness < 5.0F) {
            return 0.1F;
        } else if (hardness < 20.0F) {
            return hardness / 2.0F;
        }
        return 5.0F + hardness;
    }

    @SubscribeEvent
    public static void onBlockDrops(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof Player player)) {
            return;
        }
        ServerLevel level = event.getLevel();
        RandomSource rand = level.getRandom();
        ItemStack tool = event.getTool();
        BlockState state = event.getState();
        BlockPos pos = event.getPos();
        List<ItemEntity> drops = event.getDrops();
        int fortune = FMDropEvents.enchant(level, tool, Enchantments.FORTUNE);

        // Shovel of the Purifier: taint shards out of taint blocks that drop nothing.
        if (tool.is(FMItems.TAINT_SHOVEL.get()) && state.is(FMTags.TAINT_BLOCKS) && drops.isEmpty()) {
            int chance = 1 + 2 * fortune + (FMDropEvents.enchant(level, tool, Enchantments.SILK_TOUCH) > 0 ? 6 : 0);
            if (rand.nextInt(31) <= chance) {
                drops.add(entity(level, pos, new ItemStack(FMItems.TAINT_SHARD.get())));
            }
        }

        if (FMConfig.enchantments()) {
            // Consuming: destroy garbage drops.
            if (FMEnchantments.level(level, tool, FMEnchantments.CONSUMING) > 0) {
                drops.removeIf(drop -> drop.getItem().is(FMTags.CONSUMING_GARBAGE));
            }
            // Corrupting: one time in three, vis crystals turn into random sin shards.
            if (FMEnchantments.level(level, tool, FMEnchantments.CORRUPTING) > 0 && rand.nextInt(3) == 1) {
                List<ItemEntity> corrupted = new ArrayList<>();
                drops.removeIf(drop -> {
                    if (!drop.getItem().is(VIS_CRYSTALS)) {
                        return false;
                    }
                    for (int i = 0; i < drop.getItem().getCount(); i++) {
                        corrupted.add(entity(level, pos, sinShard(rand)));
                    }
                    return true;
                });
                drops.addAll(corrupted);
            }
        }

        // Fiery Core (FM cluster): without Fortune, each ore drop may turn into a Thaumaturge ore cluster.
        int fieryCore = FMConfig.enchantments() ? FMEnchantments.level(level, tool, FMEnchantments.FIERY_CORE) : 0;
        if (fieryCore > 0 && fortune == 0) {
            float chance = 0.2F + fieryCore * 0.075F;
            boolean ore = state.is(net.neoforged.neoforge.common.Tags.Blocks.ORES);
            boolean upgraded = false;
            for (ItemEntity drop : drops) {
                ItemStack cluster = clusterFor(drop.getItem(), ore);
                if (!cluster.isEmpty() && rand.nextFloat() < chance) {
                    drop.setItem(cluster.copyWithCount(drop.getItem().getCount()));
                    upgraded = true;
                }
            }
            if (upgraded) {
                level.playSound(null, pos, net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP, net.minecraft.sounds.SoundSource.BLOCKS, 0.2F, 0.7F + rand.nextFloat() * 0.2F);
            }
        }

        // Fortune on Nether quartz ore: greed shards.
        if (level.dimension() == Level.NETHER && state.is(Blocks.NETHER_QUARTZ_ORE) && fortune > 0 && rand.nextInt(60) <= fortune) {
            drops.add(entity(level, pos, new ItemStack(FMItems.GREED_SHARD.get())));
        }
    }

    private static final java.util.List<String> CLUSTER_METALS = java.util.List.of("iron", "copper", "gold", "lead", "silver", "tin", "cinnabar");

    /**
     * TC4 {@code Utils.findSpecialMiningResult}: raw ore becomes Thaumaturge's matching cluster ({@code c:raw_materials/X} to
     * {@code thaumaturge:cluster_X}); quartz mined from an ore block becomes a quartz cluster. Empty when there is none.
     */
    public static ItemStack clusterFor(ItemStack drop, boolean fromOre) {
        for (String metal : CLUSTER_METALS) {
            if (drop.is(TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "raw_materials/" + metal)))) {
                return cluster(metal);
            }
        }
        return fromOre && drop.is(net.minecraft.world.item.Items.QUARTZ) ? cluster("quartz") : ItemStack.EMPTY;
    }

    private static ItemStack cluster(String name) {
        return net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(Identifier.fromNamespaceAndPath("thaumaturge", "cluster_" + name))
                .map(ItemStack::new).orElse(ItemStack.EMPTY);
    }

    /** FM {@code getSinShard}: one of the seven sins, re-rolling lust when it is disabled. */
    public static ItemStack sinShard(RandomSource rand) {
        while (true) {
            switch (rand.nextInt(7)) {
                case 0: return new ItemStack(FMItems.WRATH_SHARD.get());
                case 1: return new ItemStack(FMItems.ENVY_SHARD.get());
                case 2: return new ItemStack(FMItems.GLUTTONY_SHARD.get());
                case 3: return new ItemStack(FMItems.PRIDE_SHARD.get());
                case 4:
                    if (FMConfig.lustEnabled()) {
                        return new ItemStack(FMItems.LUST_SHARD.get());
                    }
                    break;
                case 5: return new ItemStack(FMItems.SLOTH_SHARD.get());
                default: return new ItemStack(FMItems.GREED_SHARD.get());
            }
        }
    }

    /** Food eaten in the Nether: 20% chance to cough up a gluttony shard. */
    @SubscribeEvent
    public static void onEat(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) {
            return;
        }
        ItemStack eaten = event.getItem();
        if (!eaten.has(net.minecraft.core.component.DataComponents.FOOD) || eaten.is(FMItems.GLUTTONY_SHARD.get())) {
            return;
        }
        // Ring of Nutrition: every meal is worth 2 more hunger and 8 more saturation (TC4/1.7 addStats(2, 2.0F)).
        if (!fox.spiteful.forbidden.compat.CuriosBridge.findEquipped(player, FMItems.RING_NUTRITION.get()).isEmpty()) {
            player.getFoodData().eat(2, 2.0F);
        }
        if (player.level().dimension() == Level.NETHER && player.getRandom().nextInt(10) < 2) {
            ItemEntity drop = player.spawnAtLocation((ServerLevel) player.level(), new ItemStack(FMItems.GLUTTONY_SHARD.get()), 1.0F);
            if (drop != null) {
                RandomSource rand = player.getRandom();
                drop.setDeltaMovement(drop.getDeltaMovement().add((rand.nextFloat() - rand.nextFloat()) * 0.1F, rand.nextFloat() * 0.05F, (rand.nextFloat() - rand.nextFloat()) * 0.1F));
            }
        }
    }

    private static ItemEntity entity(Level level, BlockPos pos, ItemStack stack) {
        ItemEntity entity = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
        entity.setDefaultPickUpDelay();
        return entity;
    }
}
