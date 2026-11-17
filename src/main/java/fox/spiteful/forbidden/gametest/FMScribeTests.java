package fox.spiteful.forbidden.gametest;

import com.leclowndu93150.thaumaturge.api.aspect.Aspects;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.api.research.pool.AspectPoolAccess;
import fox.spiteful.forbidden.Forbidden;
import fox.spiteful.forbidden.content.item.ScribingToolsItems;
import fox.spiteful.forbidden.registry.FMItems;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

/** Crystal and Primal Scribing Tools checks for milestone M10. */
final class FMScribeTests {
    private static final TagKey<net.minecraft.world.item.Item> SCRIBING_TOOLS = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("thaumaturge", "scribing_tools"));

    private FMScribeTests() {}

    static void register(BiConsumer<String, Consumer<GameTestHelper>> add) {
        add.accept("scribe/data_loaded", helper -> {
            var recipes = helper.getLevel().getServer().getRecipeManager();
            for (String key : List.of("arcane/crystalwell", "arcane/primewell")) {
                check(helper, recipes.byKey(ResourceKey.create(Registries.RECIPE, Forbidden.id(key))).isPresent(), "recipe " + key + " missing");
            }
            check(helper, new ItemStack(FMItems.CRYSTALWELL.get()).is(SCRIBING_TOOLS) && new ItemStack(FMItems.PRIMEWELL.get()).is(SCRIBING_TOOLS),
                    "both must count as Thaumaturge scribing tools");
            helper.succeed();
        });

        add.accept("scribe/primewell_never_runs_dry", helper -> {
            ItemStack primewell = new ItemStack(FMItems.PRIMEWELL.get());
            primewell.setDamageValue(primewell.getDamageValue() + 1); // what Thaumaturge's research table does per note
            check(helper, primewell.getDamageValue() == 0, "primewell took damage: " + primewell.getDamageValue());
            check(helper, primewell.getDamageValue() < primewell.getMaxDamage(), "primewell must always have ink");
            helper.succeed();
        });

        add.accept("scribe/crystalwell_shatters_into_aspects", helper -> {
            var player = FakePlayerFactory.getMinecraft(helper.getLevel());
            Map<ResourceKey<IAspect>, Integer> before = new HashMap<>();
            for (ResourceKey<IAspect> primal : TTAspects.PRIMALS) {
                before.put(primal, AspectPoolAccess.amount(player, holder(helper, primal)));
            }
            ItemStack well = new ItemStack(FMItems.CRYSTALWELL.get());
            check(helper, !ScribingToolsItems.Crystalwell.isDry(well), "a fresh crystalwell has ink");
            well.setDamageValue(well.getMaxDamage());
            check(helper, ScribingToolsItems.Crystalwell.isDry(well), "a used crystalwell is dry");
            player.setItemInHand(InteractionHand.MAIN_HAND, well);
            well.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            for (ResourceKey<IAspect> primal : TTAspects.PRIMALS) {
                int gained = AspectPoolAccess.amount(player, holder(helper, primal)) - before.get(primal);
                check(helper, gained >= 4 && gained <= 7, primal.identifier() + " gained " + gained + ", expected 4-7");
            }
            ItemStack left = player.getMainHandItem();
            check(helper, left.is(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("thaumaturge", "scribing_tools")))
                    && left.getDamageValue() >= left.getMaxDamage(), "should leave empty Thaumaturge scribing tools, got " + left);
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            helper.succeed();
        });
    }

    private static Holder<IAspect> holder(GameTestHelper helper, ResourceKey<IAspect> key) {
        return Aspects.resolve(helper.getLevel(), key);
    }

    private static void check(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            throw helper.assertionException(Component.literal(message));
        }
    }
}
