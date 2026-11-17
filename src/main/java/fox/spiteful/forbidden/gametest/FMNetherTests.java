package fox.spiteful.forbidden.gametest;

import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.api.capability.KnowledgeAccess;
import com.leclowndu93150.thaumaturge.api.casters.CasterTriggerRegistry;
import com.leclowndu93150.thaumaturge.api.spell.part.SpellPart;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import fox.spiteful.forbidden.Forbidden;
import fox.spiteful.forbidden.content.wand.HellfireTrigger;
import fox.spiteful.forbidden.registry.FMBlocks;
import fox.spiteful.forbidden.registry.FMItems;
import fox.spiteful.forbidden.registry.FMVillagers;
import fox.spiteful.forbidden.registry.FMWandParts;
import fox.spiteful.forbidden.util.WandBridge;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

/** Hellfire trigger, Heretic villager and Blink upgrade spell parts (milestone M11). */
final class FMNetherTests {
    private FMNetherTests() {}

    static void register(BiConsumer<String, Consumer<GameTestHelper>> add) {
        add.accept("nether/spell_parts_and_trades_loaded", helper -> {
            var access = helper.getLevel().registryAccess();
            var parts = access.lookupOrThrow(SpellPart.REGISTRY_KEY);
            for (String part : List.of("hellfire", "pandemonium")) {
                check(helper, parts.get(ResourceKey.create(SpellPart.REGISTRY_KEY, Forbidden.id(part))).isPresent(), "spell part " + part + " missing");
            }
            var tradeSets = access.lookupOrThrow(Registries.TRADE_SET);
            for (int level = 1; level <= 3; level++) {
                check(helper, tradeSets.get(ResourceKey.create(Registries.TRADE_SET, Forbidden.id("heretic/level_" + level))).isPresent(), "trade set level " + level);
            }
            check(helper, access.lookupOrThrow(Registries.VILLAGER_TRADE).get(ResourceKey.create(Registries.VILLAGER_TRADE, Forbidden.id("heretic/profane_wand"))).isPresent(),
                    "profane wand trade missing");
            var lower = FMBlocks.UMBRAL_BUSH.get().defaultBlockState();
            check(helper, PoiTypes.forState(lower).filter(h -> h.is(FMVillagers.HERETIC_POI_KEY)).isPresent(), "umbral bush should be the heretic job site");
            helper.succeed();
        });

        add.accept("nether/heretic_offers_fm_trades", helper -> {
            ServerLevel level = helper.getLevel();
            Villager villager = EntityType.VILLAGER.create(level, EntitySpawnReason.COMMAND);
            villager.snapTo(helper.absolutePos(new BlockPos(2, 2, 2)).getCenter());
            level.addFreshEntity(villager);
            villager.setVillagerData(villager.getVillagerData().withProfession(FMVillagers.HERETIC.getDelegate()).withLevel(1));
            var offers = villager.getOffers();
            check(helper, offers.size() == 2, "a level 1 heretic should offer 2 trades, has " + offers.size());
            for (MerchantOffer offer : offers) {
                boolean fm = offer.getResult().getItem().builtInRegistryHolder().key().identifier().getNamespace().equals(Forbidden.MODID)
                        || offer.getBaseCostA().getItem().builtInRegistryHolder().key().identifier().getNamespace().equals(Forbidden.MODID);
                check(helper, fm, "unexpected heretic trade " + offer.getBaseCostA() + " -> " + offer.getResult());
            }
            villager.discard();
            helper.succeed();
        });

        add.accept("nether/profane_wand_trade_builds_a_wand", helper -> {
            ServerLevel level = helper.getLevel();
            Villager villager = EntityType.VILLAGER.create(level, EntitySpawnReason.COMMAND);
            villager.snapTo(helper.absolutePos(new BlockPos(2, 2, 2)).getCenter());
            level.addFreshEntity(villager);
            villager.setVillagerData(villager.getVillagerData().withProfession(FMVillagers.HERETIC.getDelegate()).withLevel(3));
            // A fresh villager only rolls the trades of its current level: level 3 is the profane wand.
            var offers = villager.getOffers();
            ItemStack wand = offers.isEmpty() ? ItemStack.EMPTY : offers.getFirst().getResult();
            check(helper, offers.size() == 1 && offers.getFirst().getCostB().is(net.minecraft.world.item.Items.STICK), "profane wand trade should also want a stick");
            check(helper, WandBridge.hasRod(wand, FMWandParts.ROD_PROFANE.getId()), "traded wand should have the profane rod: " + wand);
            check(helper, WandBridge.vis(wand, TTAspects.IGNIS) == 5000, "traded wand should hold 50 vis of each primal");
            villager.discard();
            helper.succeed();
        });

        add.accept("nether/hellfire_trigger_lights_netherrack", helper -> {
            ServerLevel level = helper.getLevel();
            BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 2));
            level.setBlockAndUpdate(pos, Blocks.NETHERRACK.defaultBlockState());
            level.setBlockAndUpdate(pos.above(), Blocks.AIR.defaultBlockState());
            var player = FakePlayerFactory.getMinecraft(level);
            ItemStack wand = wand();
            WandBridge.setVis(wand, TTAspects.IGNIS, 100);
            KnowledgeAccess.of(player).removeResearch(HellfireTrigger.RESEARCH);
            check(helper, !CasterTriggerRegistry.performTrigger(level, wand, player, pos, Direction.UP, level.getBlockState(pos)), "must need the Hellfire research");
            KnowledgeAccess.of(player).addResearch(HellfireTrigger.RESEARCH);
            check(helper, !CasterTriggerRegistry.performTrigger(level, wand, player, pos, Direction.UP, level.getBlockState(pos)), "a started but unfinished research must not count");
            // The public API can only start research; finishing happens through play. Test-only use of Thaumaturge's implementation.
            ((com.leclowndu93150.thaumaturge.content.research.PlayerKnowledge) KnowledgeAccess.of(player)).markComplete(HellfireTrigger.RESEARCH);
            check(helper, !CasterTriggerRegistry.performTrigger(level, wand, player, pos, Direction.NORTH, level.getBlockState(pos)), "only the top face lights");
            check(helper, CasterTriggerRegistry.performTrigger(level, wand, player, pos, Direction.UP, level.getBlockState(pos)), "trigger did not fire");
            check(helper, level.getBlockState(pos.above()).is(Blocks.FIRE), "no fire above the netherrack");
            check(helper, WandBridge.vis(wand, TTAspects.IGNIS) == 100 - HellfireTrigger.COST, "hellfire should cost 2 centivis of ignis");
            KnowledgeAccess.of(player).removeResearch(HellfireTrigger.RESEARCH);
            level.setBlockAndUpdate(pos.above(), Blocks.AIR.defaultBlockState());
            helper.succeed();
        });
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static ItemStack wand() {
        ItemStack wand = new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("thaumaturge", "wand")));
        DataComponentType type = BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(Identifier.fromNamespaceAndPath("thaumaturge", "wand_parts"));
        JsonObject json = new JsonObject();
        json.addProperty("cap", "thaumaturge:iron");
        json.addProperty("rod", "thaumaturge:greatwood");
        wand.set(type, ((Codec) type.codec()).parse(JsonOps.INSTANCE, json).getOrThrow());
        return wand;
    }

    private static void check(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            throw helper.assertionException(Component.literal(message));
        }
    }
}
