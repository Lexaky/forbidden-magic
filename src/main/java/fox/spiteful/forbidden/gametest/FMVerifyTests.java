package fox.spiteful.forbidden.gametest;

import fox.spiteful.forbidden.Forbidden;
import fox.spiteful.forbidden.event.FMDropEvents;
import fox.spiteful.forbidden.event.FMHarvestEvents;
import fox.spiteful.forbidden.registry.FMBlocks;
import fox.spiteful.forbidden.registry.FMEnchantments;
import fox.spiteful.forbidden.registry.FMItems;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

/**
 * Verification of FM's chance-based rules. Each rule is exercised through the real event handler many times and the hit
 * rate is compared with FM's formula (tolerance: five standard deviations), so a wrong formula fails while luck does not.
 */
final class FMVerifyTests {
    private FMVerifyTests() {}

    static void register(FMGameTests.Registrar r) {
        registerDrops(r);
        FMVerifyMechanics.register(r);
    }

    // ------------------------------------------------------------------------------------------------------------------
    // Mob drops (FMEventHandler#onLivingDrops)
    // ------------------------------------------------------------------------------------------------------------------

    private static void registerDrops(FMGameTests.Registrar r) {
        r.add("verify/drops_wrath_shard_rate_in_nether", helper -> {
            ServerLevel nether = nether(helper);
            Player killer = helper.makeMockPlayer(GameType.SURVIVAL);
            // Bare hand: wrath = 2 + weapon damage (0) -> 3/61.
            int hits = count(4000, () -> drops(nether, EntityType.ZOMBIE, nether.damageSources().playerAttack(killer), true).stream()
                    .anyMatch(s -> s.is(FMItems.WRATH_SHARD.get())));
            expect(helper, hits, 4000, 3.0 / 61, "wrath shard");
            helper.succeed();
        });

        r.add("verify/drops_no_shards_outside_nether", helper -> {
            Player killer = helper.makeMockPlayer(GameType.SURVIVAL);
            int hits = count(1000, () -> drops(helper.getLevel(), EntityType.ZOMBIE, helper.getLevel().damageSources().playerAttack(killer), true).stream()
                    .anyMatch(s -> s.is(FMItems.WRATH_SHARD.get()) || s.is(FMItems.SLOTH_SHARD.get())));
            check(helper, hits == 0, "shards dropped in the overworld " + hits + " times");
            helper.succeed();
        });

        r.add("verify/drops_sloth_shards_from_unattended_deaths", helper -> {
            ServerLevel nether = nether(helper);
            int[] total = new int[1];
            int hits = count(3000, () -> {
                List<ItemStack> d = drops(nether, EntityType.ZOMBIE, nether.damageSources().generic(), false);
                d.stream().filter(s -> s.is(FMItems.SLOTH_SHARD.get())).forEach(s -> total[0] += s.getCount());
                return d.stream().anyMatch(s -> s.is(FMItems.SLOTH_SHARD.get()));
            });
            expect(helper, hits, 3000, 4.0 / 30, "sloth shard");
            double mean = (double) total[0] / hits;
            check(helper, mean > 1.7 && mean < 2.3, "sloth shards per drop should average 2 (1-3), got " + mean);
            helper.succeed();
        });

        r.add("verify/drops_pride_from_bosses_and_greed_from_looting", helper -> {
            ServerLevel nether = nether(helper);
            Player killer = helper.makeMockPlayer(GameType.SURVIVAL);
            List<ItemStack> wither = drops(nether, EntityType.WITHER, nether.damageSources().playerAttack(killer), true);
            int pride = wither.stream().filter(s -> s.is(FMItems.PRIDE_SHARD.get())).mapToInt(ItemStack::getCount).sum();
            check(helper, pride >= 2, "a boss should always drop at least 2 pride shards, got " + pride);
            killer.setItemSlot(EquipmentSlot.MAINHAND, enchanted(helper, Items.IRON_SWORD, Enchantments.LOOTING, 3));
            int greed = count(3000, () -> drops(nether, EntityType.ZOMBIE, nether.damageSources().playerAttack(killer), true).stream()
                    .anyMatch(s -> s.is(FMItems.GREED_SHARD.get())));
            expect(helper, greed, 3000, 4.0 / 20, "greed shard with Looting III");
            helper.succeed();
        });

        r.add("verify/drops_envy_from_piglins_and_spawn_rate", helper -> {
            ServerLevel nether = nether(helper);
            Player killer = helper.makeMockPlayer(GameType.SURVIVAL);
            Mob piglin = EntityType.ZOMBIFIED_PIGLIN.create(nether, EntitySpawnReason.COMMAND);
            piglin.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(FMItems.ENVY_SHARD.get()));
            LivingDropsEvent event = new LivingDropsEvent(piglin, nether.damageSources().playerAttack(killer), new ArrayList<>(), true);
            FMDropEvents.onDrops(event);
            check(helper, event.getDrops().stream().anyMatch(e -> e.getItem().is(FMItems.ENVY_SHARD.get())), "piglin holding an envy shard must drop it");
            int armed = count(7000, () -> {
                Mob fresh = EntityType.ZOMBIFIED_PIGLIN.create(nether, EntitySpawnReason.NATURAL);
                FMDropEvents.onSpawn(new FinalizeSpawnEvent(fresh, nether, 0, 64, 0, nether.getCurrentDifficultyAt(BlockPos.ZERO), EntitySpawnReason.NATURAL, null, null));
                return fresh.getItemBySlot(EquipmentSlot.MAINHAND).is(FMItems.ENVY_SHARD.get());
            });
            expect(helper, armed, 7000, 1.0 / 175, "piglins spawning with an envy shard");
            helper.succeed();
        });

        r.add("verify/drops_skull_axe_heads", helper -> {
            ServerLevel level = helper.getLevel();
            Player killer = helper.makeMockPlayer(GameType.SURVIVAL);
            killer.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(FMItems.SKULL_AXE.get()));
            DamageSource hit = level.damageSources().playerAttack(killer);
            expect(helper, count(3000, () -> has(drops(level, EntityType.SKELETON, hit, true), Items.SKELETON_SKULL)), 3000, 4.0 / 26, "skeleton skull");
            expect(helper, count(3000, () -> has(drops(level, EntityType.WITHER_SKELETON, hit, true), Items.WITHER_SKELETON_SKULL)), 3000, 4.0 / 26, "wither skull");
            expect(helper, count(3000, () -> has(drops(level, EntityType.ZOMBIE, hit, true), Items.ZOMBIE_HEAD)), 3000, 3.0 / 26, "zombie head");
            expect(helper, count(3000, () -> has(drops(level, EntityType.CREEPER, hit, true), Items.CREEPER_HEAD)), 3000, 3.0 / 26, "creeper head");
            Player victim = helper.makeMockPlayer(GameType.SURVIVAL);
            ItemStack head = ItemStack.EMPTY;
            for (int i = 0; i < 200 && head.isEmpty(); i++) {
                LivingDropsEvent event = new LivingDropsEvent(victim, hit, new ArrayList<>(), true);
                FMDropEvents.onDrops(event);
                head = event.getDrops().stream().map(ItemEntity::getItem).filter(s -> s.is(Items.PLAYER_HEAD)).findFirst().orElse(ItemStack.EMPTY);
            }
            check(helper, head.has(DataComponents.PROFILE), "player heads must carry the victim's profile");
            killer.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
            check(helper, count(500, () -> has(drops(level, EntityType.SKELETON, level.damageSources().playerAttack(killer), true), Items.SKELETON_SKULL)) == 0,
                    "heads only come from the Skulltaker");
            helper.succeed();
        });

        r.add("verify/drops_silverfish_and_capitalist", helper -> {
            ServerLevel level = helper.getLevel();
            Player killer = helper.makeMockPlayer(GameType.SURVIVAL);
            DamageSource hit = level.damageSources().playerAttack(killer);
            // The test area is not a mountain biome: 2/70.
            expect(helper, count(4000, () -> has(drops(level, EntityType.SILVERFISH, hit, true), FMItems.EMERALD_NUGGET.get())), 4000, 2.0 / 70,
                    "silverfish emerald nugget");
            killer.setItemSlot(EquipmentSlot.MAINHAND, enchanted(helper, Items.IRON_SWORD, FMEnchantments.CAPITALIST, 1));
            check(helper, has(drops(level, EntityType.VILLAGER, hit, true), Items.EMERALD), "capitalist kills on villagers always drop an emerald");
            expect(helper, count(4000, () -> has(drops(level, EntityType.ZOMBIE, hit, true), FMItems.EMERALD_NUGGET.get())), 4000, 3.0 / 35,
                    "capitalist emerald nugget from monsters");
            helper.succeed();
        });

        r.add("verify/educational_triples_experience", helper -> {
            Player killer = helper.makeMockPlayer(GameType.SURVIVAL);
            killer.setItemSlot(EquipmentSlot.MAINHAND, enchanted(helper, Items.IRON_SWORD, FMEnchantments.EDUCATIONAL, 2));
            LivingEntity zombie = EntityType.ZOMBIE.create(helper.getLevel(), EntitySpawnReason.COMMAND);
            LivingExperienceDropEvent event = new LivingExperienceDropEvent(zombie, killer, 10);
            FMDropEvents.onExperience(event);
            check(helper, event.getDroppedExperience() == 10 + 3 * 10 * 2, "educational II: expected 70 xp, got " + event.getDroppedExperience());
            killer.setItemSlot(EquipmentSlot.MAINHAND, enchanted(helper, killer.getMainHandItem().getItem(), Enchantments.LOOTING, 1, killer.getMainHandItem()));
            LivingExperienceDropEvent looted = new LivingExperienceDropEvent(zombie, killer, 10);
            FMDropEvents.onExperience(looted);
            check(helper, looted.getDroppedExperience() == 10, "educational must not work together with looting");
            helper.succeed();
        });

        r.add("verify/eating_in_nether_coughs_gluttony", helper -> {
            ServerLevel nether = nether(helper);
            nether.getChunk(0, 0);
            var eater = FakePlayerFactory.getMinecraft(nether);
            eater.setPos(0.5, 64, 0.5);
            List<ItemStack> coughed = DropCapture.during(nether, () -> {
                for (int i = 0; i < 1000; i++) {
                    FMHarvestEvents.onEat(new LivingEntityUseItemEvent.Finish(eater, new ItemStack(Items.BREAD), 0, ItemStack.EMPTY));
                }
            });
            expect(helper, (int) coughed.stream().filter(s -> s.is(FMItems.GLUTTONY_SHARD.get())).count(), 1000, 0.2, "gluttony shard per meal");
            List<ItemStack> none = DropCapture.during(nether, () -> {
                for (int i = 0; i < 300; i++) {
                    FMHarvestEvents.onEat(new LivingEntityUseItemEvent.Finish(eater, new ItemStack(FMItems.GLUTTONY_SHARD.get()), 0, ItemStack.EMPTY));
                }
            });
            check(helper, none.isEmpty(), "eating a gluttony shard must not produce another");
            helper.succeed();
        });

        r.add("verify/harvest_enchantments_and_shards", helper -> {
            ServerLevel level = helper.getLevel();
            ServerLevel nether = nether(helper);
            Player miner = helper.makeMockPlayer(GameType.SURVIVAL);
            BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
            // Consuming: garbage vanishes, valuables stay.
            ItemStack consuming = enchanted(helper, Items.IRON_PICKAXE, FMEnchantments.CONSUMING, 1);
            List<ItemStack> left = harvest(level, pos, Blocks.STONE.defaultBlockState(), miner, consuming, new ItemStack(Items.COBBLESTONE), new ItemStack(Items.DIAMOND));
            check(helper, left.size() == 1 && left.getFirst().is(Items.DIAMOND), "consuming should eat cobblestone and keep the diamond: " + left);
            // Corrupting: one time in three, vis crystals become sin shards.
            ItemStack corrupting = enchanted(helper, Items.IRON_PICKAXE, FMEnchantments.CORRUPTING, 1);
            net.minecraft.world.item.Item crystal = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("thaumaturge", "essentia_crystal"));
            check(helper, new ItemStack(crystal).is(net.minecraft.tags.TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("thaumaturge", "vis_crystals"))),
                    "essentia crystals should be vis crystals");
            int corrupted = count(3000, () -> harvest(level, pos, Blocks.STONE.defaultBlockState(), miner, corrupting, new ItemStack(crystal)).stream()
                    .noneMatch(s -> s.is(crystal)));
            expect(helper, corrupted, 3000, 1.0 / 3, "corrupted vis crystal");
            // Fortune on nether quartz ore in the Nether: greed shard 4/60 with Fortune III.
            ItemStack fortune = enchanted(helper, Items.IRON_PICKAXE, Enchantments.FORTUNE, 3);
            int greed = count(4000, () -> has(harvest(nether, BlockPos.ZERO, Blocks.NETHER_QUARTZ_ORE.defaultBlockState(), miner, fortune, new ItemStack(Items.QUARTZ)),
                    FMItems.GREED_SHARD.get()));
            expect(helper, greed, 4000, 4.0 / 60, "greed shard from quartz");
            // Shovel of the Purifier on a Thaumaturge taint block that drops nothing: 2/31 without fortune.
            BlockState taint = BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("thaumaturge", "taint_fibre")).defaultBlockState();
            check(helper, taint.is(fox.spiteful.forbidden.registry.FMTags.TAINT_BLOCKS), "taint_fibre should be a taint block");
            int shards = count(4000, () -> has(harvest(level, pos, taint, miner, new ItemStack(FMItems.TAINT_SHOVEL.get())), FMItems.TAINT_SHARD.get()));
            expect(helper, shards, 4000, 2.0 / 31, "taint shard from the purifier");
            // Fiery Core IV: half of the raw ore becomes clusters.
            ItemStack fiery = enchanted(helper, FMItems.MORPH_PICKAXE.get(), FMEnchantments.FIERY_CORE, 4);
            int clusters = count(3000, () -> harvest(level, pos, Blocks.IRON_ORE.defaultBlockState(), miner, fiery, new ItemStack(Items.RAW_IRON)).stream()
                    .noneMatch(s -> s.is(Items.RAW_IRON)));
            expect(helper, clusters, 3000, 0.5, "fiery core cluster");
            helper.succeed();
        });

        r.add("verify/taint_leaves_drops", helper -> {
            ServerLevel level = helper.getLevel();
            BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
            BlockState leaves = FMBlocks.TAINT_LEAVES.get().defaultBlockState();
            int saplings = 0;
            int fruit = 0;
            int n = 6000;
            for (int i = 0; i < n; i++) {
                List<ItemStack> d = net.minecraft.world.level.block.Block.getDrops(leaves, level, pos, null);
                if (has(d, FMBlocks.TAINT_SAPLING_ITEM.get())) {
                    saplings++;
                }
                if (has(d, FMItems.TAINT_FRUIT.get())) {
                    fruit++;
                }
            }
            expect(helper, saplings, n, 1.0 / 60, "taint sapling from leaves");
            expect(helper, fruit, n, 0.05, "taint fruit from leaves");
            List<ItemStack> sheared = net.minecraft.world.level.block.Block.getDrops(leaves, level, pos, null, null, new ItemStack(Items.SHEARS));
            check(helper, sheared.size() == 1 && sheared.getFirst().is(FMBlocks.TAINT_LEAVES_ITEM.get()), "shears should drop the leaves: " + sheared);
            helper.succeed();
        });
    }

    // ------------------------------------------------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------------------------------------------------

    static ServerLevel nether(GameTestHelper helper) {
        return helper.getLevel().getServer().getLevel(Level.NETHER);
    }

    static List<ItemStack> drops(ServerLevel level, EntityType<? extends LivingEntity> type, DamageSource source, boolean recentlyHit) {
        LivingEntity victim = type.create(level, EntitySpawnReason.COMMAND);
        LivingDropsEvent event = new LivingDropsEvent(victim, source, new ArrayList<>(), recentlyHit);
        FMDropEvents.onDrops(event);
        return event.getDrops().stream().map(ItemEntity::getItem).toList();
    }

    static List<ItemStack> harvest(ServerLevel level, BlockPos pos, BlockState state, Player breaker, ItemStack tool, ItemStack... loot) {
        List<ItemEntity> drops = new ArrayList<>();
        for (ItemStack stack : loot) {
            drops.add(new ItemEntity(level, pos.getX(), pos.getY(), pos.getZ(), stack.copy()));
        }
        FMHarvestEvents.onBlockDrops(new BlockDropsEvent(level, pos, state, null, drops, breaker, tool));
        return drops.stream().map(ItemEntity::getItem).toList();
    }

    static boolean has(List<ItemStack> stacks, net.minecraft.world.item.Item item) {
        return stacks.stream().anyMatch(s -> s.is(item));
    }

    static int count(int trials, BooleanSupplier trial) {
        int hits = 0;
        for (int i = 0; i < trials; i++) {
            if (trial.getAsBoolean()) {
                hits++;
            }
        }
        return hits;
    }

    /** Fails when the observed count is more than five standard deviations from n * p. */
    static void expect(GameTestHelper helper, int hits, int n, double p, String what) {
        double mean = n * p;
        double sigma = Math.sqrt(n * p * (1 - p));
        check(helper, Math.abs(hits - mean) <= 5 * sigma + 1, what + ": " + hits + "/" + n + " hits, expected about " + Math.round(mean));
    }

    static ItemStack enchanted(GameTestHelper helper, net.minecraft.world.item.Item item, ResourceKey<Enchantment> key, int level) {
        return enchanted(helper, item, key, level, new ItemStack(item));
    }

    static ItemStack enchanted(GameTestHelper helper, net.minecraft.world.item.Item item, ResourceKey<Enchantment> key, int level, ItemStack base) {
        ItemStack stack = base.copy();
        stack.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key), level);
        return stack;
    }

    static void check(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            throw helper.assertionException(Component.literal(message));
        }
    }

}
