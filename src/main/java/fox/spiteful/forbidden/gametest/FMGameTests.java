package fox.spiteful.forbidden.gametest;

import com.google.gson.JsonObject;
import com.leclowndu93150.thaumaturge.api.aspect.AspectIndexAccess;
import com.leclowndu93150.thaumaturge.api.aspect.AspectList;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.api.research.IResearchEntry;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import fox.spiteful.forbidden.Forbidden;
import fox.spiteful.forbidden.content.tool.MorphTools;
import fox.spiteful.forbidden.content.wand.InfernalRodUpdate;
import fox.spiteful.forbidden.content.wand.ProfaneRodUpdate;
import fox.spiteful.forbidden.event.FMHarvestEvents;
import fox.spiteful.forbidden.registry.FMDataComponents;
import fox.spiteful.forbidden.registry.FMEnchantments;
import fox.spiteful.forbidden.registry.FMItems;
import fox.spiteful.forbidden.registry.FMWandParts;
import fox.spiteful.forbidden.util.WandBridge;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

/**
 * Headless checks run by {@code gradlew runGameTestServer}. They cover what can be verified without a client: that every
 * datapack entry loads through Thaumaturge's codecs and the logic of rods, morphic tools and the taint pickaxe.
 */
public final class FMGameTests {
    /** Empty test structure shipped by Thaumaturge (referenced by id, not copied). */
    private static final Identifier STRUCTURE = Identifier.fromNamespaceAndPath("thaumaturge", "empty_5x5x7");
    private static final List<String> RESEARCH = List.of("nether_shards", "rod_profane", "skull_axe", "morph_tools", "rod_infernal", "wrath",
            "capitalist", "corrupting", "impact", "voidtouched", "diabolist_fork", "taint_shovel", "taint_pickaxe", "rod_tainted", "consuming",
            "educational", "emerald_transmutation", "riding_crop");
    private static final List<String> RECIPES = List.of("infusion/taint_shovel", "infusion/taint_pickaxe", "infusion/wand_rod_tainted", "infusion/skull_axe",
            "infusion/morph_pickaxe", "infusion/morph_sword", "infusion/morph_shovel", "infusion/morph_axe", "infusion/wand_rod_infernal",
            "infusion/diabolist_fork", "infusion/wrath_book", "infusion/capitalist_book", "infusion/consuming_book", "infusion/educational_book",
            "infusion/corrupting_book", "infusion/impact_book", "infusion/voidtouched_book", "crucible/emerald_transmutation", "emerald_from_nuggets",
            "emerald_nugget", "riding_crop");
    private static final List<String> ASPECTS = List.of("infernus", "ira", "invidia", "gula", "superbia", "luxuria", "desidia");

    private FMGameTests() {}

    public static void register(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> env = event.registerEnvironment(Forbidden.id("default"), new TestEnvironmentDefinition.AllOf(List.of()));
        Registrar r = new Registrar(event, env);
        FMBlockTests.register(r::add);
        FMCageTests.register(r::add);
        FMBaubleTests.register(r::add);
        FMScribeTests.register(r::add);
        FMNetherTests.register(r::add);
        FMVerifyTests.register(r);

        r.add("data/aspects_registered", helper -> {
            var lookup = helper.getLevel().registryAccess().lookupOrThrow(IAspect.REGISTRY_KEY);
            for (String tag : ASPECTS) {
                var holder = lookup.get(ResourceKey.create(IAspect.REGISTRY_KEY, Forbidden.id(tag)));
                check(helper, holder.isPresent(), "aspect forbidden:" + tag + " missing");
                check(helper, holder.get().value().components().size() == 2, "aspect forbidden:" + tag + " is not compound");
            }
            helper.succeed();
        });

        r.add("data/item_aspects", helper -> {
            AspectList shard = AspectIndexAccess.of(FMItems.WRATH_SHARD.get());
            check(helper, shard.amountOf(aspect("ira"), helper.getLevel().registryAccess()) == 4 && shard.amountOf(aspect("infernus"), helper.getLevel().registryAccess()) == 2, "wrath shard aspects: " + shard);
            AspectList netherrack = AspectIndexAccess.of(net.minecraft.world.item.Items.NETHERRACK);
            check(helper, netherrack.amountOf(aspect("infernus"), helper.getLevel().registryAccess()) == 2 && netherrack.amountOf(TTAspects.TERRA, helper.getLevel().registryAccess()) > 0, "netherrack aspects: " + netherrack);
            helper.succeed();
        });

        r.add("data/research_loaded", helper -> {
            var lookup = helper.getLevel().registryAccess().lookupOrThrow(IResearchEntry.REGISTRY_KEY);
            for (String key : RESEARCH) {
                check(helper, lookup.get(ResourceKey.create(IResearchEntry.REGISTRY_KEY, Forbidden.id(key))).isPresent(), "research forbidden:" + key + " missing");
            }
            helper.succeed();
        });

        r.add("data/recipes_loaded", helper -> {
            var recipes = helper.getLevel().getServer().getRecipeManager();
            for (String key : RECIPES) {
                check(helper, recipes.byKey(ResourceKey.create(Registries.RECIPE, Forbidden.id(key))).isPresent(), "recipe forbidden:" + key + " missing");
            }
            // Thaumaturge generates wand assembly recipes for addon rods.
            ResourceKey<Recipe<?>> assembly = ResourceKey.create(Registries.RECIPE,
                    Identifier.fromNamespaceAndPath("thaumaturge", "wand/assembly/thaumaturge_iron_forbidden_infernal"));
            check(helper, recipes.byKey(assembly).isPresent(), "no generated assembly recipe for the infernal rod");
            helper.succeed();
        });

        r.add("data/enchantments_loaded", helper -> {
            var lookup = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            for (ResourceKey<Enchantment> key : List.of(FMEnchantments.WRATH, FMEnchantments.CAPITALIST, FMEnchantments.CONSUMING, FMEnchantments.EDUCATIONAL,
                    FMEnchantments.CORRUPTING, FMEnchantments.IMPACT, FMEnchantments.VOIDTOUCHED)) {
                var holder = lookup.get(key);
                check(helper, holder.isPresent(), "enchantment " + key.identifier() + " missing");
                check(helper, !holder.get().is(EnchantmentTags.IN_ENCHANTING_TABLE), key.identifier() + " must not be offered by the enchanting table");
            }
            var capitalist = lookup.getOrThrow(FMEnchantments.CAPITALIST);
            var looting = lookup.getOrThrow(Enchantments.LOOTING);
            check(helper, !Enchantment.areCompatible(capitalist, looting), "capitalist must be exclusive with looting");
            helper.succeed();
        });

        r.add("wand/infernal_rod_recharges_ignis", helper -> {
            ItemStack wand = wand(helper, "forbidden:infernal");
            check(helper, WandBridge.capacityCentivis(wand) == 150 * 100, "infernal wand capacity " + WandBridge.capacityCentivis(wand));
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            player.tickCount = 100;
            new InfernalRodUpdate().onUpdate(wand, player);
            check(helper, WandBridge.vis(wand, TTAspects.IGNIS) == 100, "ignis after one update: " + WandBridge.vis(wand, TTAspects.IGNIS));
            // In the overworld the other primals stay empty.
            check(helper, WandBridge.vis(wand, TTAspects.AQUA) == 0, "aqua should not recharge outside the Nether");
            // The cap is 20% of capacity for ignis.
            WandBridge.setVis(wand, TTAspects.IGNIS, 3000);
            new InfernalRodUpdate().onUpdate(wand, player);
            check(helper, WandBridge.vis(wand, TTAspects.IGNIS) == 3000, "ignis must stop at 20%: " + WandBridge.vis(wand, TTAspects.IGNIS));
            helper.succeed();
        });

        r.add("wand/profane_contract_runs_out", helper -> {
            ItemStack wand = wand(helper, "forbidden:profane");
            wand.set(FMDataComponents.CONTRACT.get(), 250);
            ProfaneRodUpdate.Result result = ProfaneRodUpdate.honourContract(wand, helper.getLevel().getRandom());
            check(helper, result.expired() && result.warp() >= 1, "contract should expire with warp: " + result);
            int total = TTAspects.PRIMALS.stream().mapToInt(a -> WandBridge.vis(wand, a)).sum();
            check(helper, total == 250, "contract should have been converted to 250 centivis, got " + total);
            check(helper, wand.getOrDefault(FMDataComponents.CONTRACT.get(), -1) == 0, "contract not spent");
            check(helper, WandBridge.hasRod(wand, FMWandParts.ROD_PROFANED.getId()), "rod did not turn profaned: " + WandBridge.rodId(wand));
            helper.succeed();
        });

        r.add("tool/morph_phases_keep_enchantments", helper -> {
            ItemStack pick = new ItemStack(FMItems.MORPH_PICKAXE.get());
            var efficiency = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.EFFICIENCY);
            ItemEnchantments.Mutable ench = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
            ench.set(efficiency, 5);
            pick.set(DataComponents.ENCHANTMENTS, ench.toImmutable());
            pick.set(DataComponents.CUSTOM_NAME, Component.literal("Red"));

            MorphTools.switchPhase(pick);
            check(helper, MorphTools.phase(pick) == 1, "phase after one switch");
            check(helper, pick.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).isEmpty(), "phase 1 should start unenchanted");
            check(helper, !pick.has(DataComponents.CUSTOM_NAME), "phase 1 should start unnamed");
            check(helper, pick.get(DataComponents.CUSTOM_MODEL_DATA).colors().equals(List.of(MorphTools.PHASE_COLORS[1])), "phase 1 tint");

            MorphTools.switchPhase(pick);
            MorphTools.switchPhase(pick);
            check(helper, MorphTools.phase(pick) == 0, "phase should wrap to 0");
            check(helper, pick.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).getLevel(efficiency) == 5, "efficiency lost");
            check(helper, "Red".equals(pick.get(DataComponents.CUSTOM_NAME).getString()), "name lost");
            helper.succeed();
        });

        r.add("tool/voidtouched_repairs", helper -> {
            ItemStack axe = new ItemStack(FMItems.MORPH_AXE.get());
            axe.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(FMEnchantments.VOIDTOUCHED), 1);
            axe.setDamageValue(10);
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            player.tickCount = 20;
            fox.spiteful.forbidden.content.tool.MorphItems.voidRepair(axe, helper.getLevel(), player);
            check(helper, axe.getDamageValue() == 9, "voidtouched should mend one point, damage is " + axe.getDamageValue());
            check(helper, fox.spiteful.forbidden.content.tool.MorphItems.Axe.class.cast(axe.getItem()).warp(axe, player) == 1, "voidtouched should carry 1 warp");
            helper.succeed();
        });

        r.add("tool/impact_digs_3x3", helper -> {
            net.minecraft.server.level.ServerLevel level = helper.getLevel();
            net.minecraft.core.BlockPos center = helper.absolutePos(new net.minecraft.core.BlockPos(2, 3, 4));
            for (int a = -1; a <= 1; a++) {
                for (int b = -1; b <= 1; b++) {
                    level.setBlockAndUpdate(center.offset(a, b, 0), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
                }
            }
            ItemStack pick = new ItemStack(FMItems.MORPH_PICKAXE.get());
            pick.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(FMEnchantments.IMPACT), 1);
            var fake = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
            fake.setPos(center.getX() + 0.5, center.getY() - 1.0, center.getZ() - 2.5);
            fake.setYRot(0.0F);
            fake.setXRot(0.0F);
            fake.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, pick);
            fake.gameMode.destroyBlock(center);
            fake.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            for (int a = -1; a <= 1; a++) {
                for (int b = -1; b <= 1; b++) {
                    check(helper, level.getBlockState(center.offset(a, b, 0)).isAir(), "block at " + a + "," + b + " survived impact");
                }
            }
            check(helper, !level.getBlockState(center.offset(0, 0, 1)).is(net.minecraft.world.level.block.Blocks.STONE), "impact must stay in the facing plane");
            helper.succeed();
        });

        r.add("tool/fiery_core_makes_clusters", helper -> {
            var lookup = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            var fiery = lookup.get(FMEnchantments.FIERY_CORE);
            check(helper, fiery.isPresent(), "fiery core enchantment missing");
            check(helper, !Enchantment.areCompatible(fiery.get(), lookup.getOrThrow(Enchantments.FORTUNE)), "fiery core must exclude fortune");
            check(helper, helper.getLevel().getServer().getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, Forbidden.id("infusion/fiery_core_book"))).isPresent(),
                    "fiery core book recipe missing");
            ItemStack iron = FMHarvestEvents.clusterFor(new ItemStack(net.minecraft.world.item.Items.RAW_IRON), true);
            check(helper, iron.is(BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("thaumaturge", "cluster_iron"))), "raw iron should become an iron cluster");
            check(helper, !FMHarvestEvents.clusterFor(new ItemStack(net.minecraft.world.item.Items.QUARTZ), true).isEmpty(), "ore quartz should become a cluster");
            check(helper, FMHarvestEvents.clusterFor(new ItemStack(net.minecraft.world.item.Items.QUARTZ), false).isEmpty(), "quartz from non-ore blocks stays quartz");
            check(helper, FMHarvestEvents.clusterFor(new ItemStack(net.minecraft.world.item.Items.COBBLESTONE), true).isEmpty(), "cobblestone has no cluster");
            helper.succeed();
        });

        r.add("tool/taint_pickaxe_speed_curve", helper -> {
            check(helper, FMHarvestEvents.taintPickSpeed(0.0F) == 0.0F, "instant blocks must be unbreakable");
            check(helper, FMHarvestEvents.taintPickSpeed(1.5F) == 0.1F, "soft blocks crawl");
            check(helper, FMHarvestEvents.taintPickSpeed(10.0F) == 5.0F, "medium blocks: hardness/2");
            check(helper, FMHarvestEvents.taintPickSpeed(50.0F) == 55.0F, "obsidian-class: 5 + hardness");
            helper.succeed();
        });
    }

    private static ResourceKey<IAspect> aspect(String tag) {
        return ResourceKey.create(IAspect.REGISTRY_KEY, Forbidden.id(tag));
    }

    /** Builds a Thaumaturge wand with an iron cap and the given rod through the parts component's codec. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static ItemStack wand(GameTestHelper helper, String rod) {
        ItemStack wand = new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("thaumaturge", "wand")));
        DataComponentType type = BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(Identifier.fromNamespaceAndPath("thaumaturge", "wand_parts"));
        JsonObject json = new JsonObject();
        json.addProperty("cap", "thaumaturge:iron");
        json.addProperty("rod", rod);
        Object parts = ((Codec) type.codec()).parse(JsonOps.INSTANCE, json).getOrThrow();
        wand.set(type, parts);
        return wand;
    }

    private static void check(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            throw helper.assertionException(Component.literal(message));
        }
    }

    record Registrar(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> env) {
        void add(String name, Consumer<GameTestHelper> body) {
            add(name, 40, body);
        }

        /** Runs in its own batch, away from the job sites and mobs other tests place. */
        void addIsolated(String name, int maxTicks, Consumer<GameTestHelper> body) {
            Holder<TestEnvironmentDefinition<?>> alone = event.registerEnvironment(Forbidden.id("isolated_" + name.replace('/', '_')),
                    new TestEnvironmentDefinition.AllOf(List.of()));
            TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(alone, STRUCTURE, maxTicks, 0, true, Rotation.NONE);
            event.registerTest(Forbidden.id(name.replace('/', '_')), new InlineTest(data, body));
        }

        void add(String name, int maxTicks, Consumer<GameTestHelper> body) {
            TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(env, STRUCTURE, maxTicks, 0, true, Rotation.NONE);
            event.registerTest(Forbidden.id(name.replace('/', '_')), new InlineTest(data, body));
        }
    }

    private static final class InlineTest extends GameTestInstance {
        private static final com.mojang.serialization.MapCodec<InlineTest> CODEC = com.mojang.serialization.MapCodec.unit((InlineTest) null);
        private final Consumer<GameTestHelper> body;

        InlineTest(TestData<Holder<TestEnvironmentDefinition<?>>> data, Consumer<GameTestHelper> body) {
            super(data);
            this.body = body;
        }

        @Override
        public void run(GameTestHelper helper) {
            body.accept(helper);
        }

        @Override
        public com.mojang.serialization.MapCodec<? extends GameTestInstance> codec() {
            return CODEC;
        }

        @Override
        protected MutableComponent typeDescription() {
            return Component.literal("forbidden:inline");
        }
    }
}
