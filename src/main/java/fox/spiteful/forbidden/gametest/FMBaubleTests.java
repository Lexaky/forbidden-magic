package fox.spiteful.forbidden.gametest;

import com.google.gson.JsonObject;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import fox.spiteful.forbidden.Forbidden;
import fox.spiteful.forbidden.compat.CuriosBridge;
import fox.spiteful.forbidden.content.item.CollarItem;
import fox.spiteful.forbidden.event.FMCombatEvents;
import fox.spiteful.forbidden.event.FMHarvestEvents;
import fox.spiteful.forbidden.registry.FMItems;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import top.theillusivec4.curios.api.CuriosApi;

/** Ring of Nutrition and Collar of Pain checks for milestone M9. */
final class FMBaubleTests {
    private FMBaubleTests() {}

    static void register(BiConsumer<String, Consumer<GameTestHelper>> add) {
        add.accept("bauble/data_loaded", helper -> {
            var recipes = helper.getLevel().getServer().getRecipeManager();
            for (String key : List.of("arcane/ring_nutrition", "infusion/sub_collar")) {
                check(helper, recipes.byKey(ResourceKey.create(Registries.RECIPE, Forbidden.id(key))).isPresent(), "recipe " + key + " missing");
            }
            check(helper, new ItemStack(FMItems.RING_NUTRITION.get()).is(TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("curios", "ring"))), "ring tag");
            check(helper, new ItemStack(FMItems.SUB_COLLAR.get()).is(TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("curios", "necklace"))), "necklace tag");
            helper.succeed();
        });

        add.accept("bauble/collar_feeds_on_pain", helper -> {
            check(helper, FMCombatEvents.painDoses(4.5F, false) == 12, "3 doses per whole point of damage");
            check(helper, FMCombatEvents.painDoses(2.0F, true) == 9, "riding crop adds 3 doses");
            ItemStack collar = new ItemStack(FMItems.SUB_COLLAR.get());
            CollarItem.feed(collar, 30, helper.getLevel().getRandom());
            int total = TTAspects.PRIMALS.stream().mapToInt(a -> CollarItem.stored(collar).amount(a)).sum();
            check(helper, total == 3000, "30 doses should store 30 vis, stored " + total + " centivis");
            CollarItem.feed(collar, 2000, helper.getLevel().getRandom());
            check(helper, TTAspects.PRIMALS.stream().allMatch(a -> CollarItem.stored(collar).amount(a) <= CollarItem.CAPACITY), "capacity exceeded");
            helper.succeed();
        });

        add.accept("bauble/collar_charges_held_wand", helper -> {
            ItemStack collar = new ItemStack(FMItems.SUB_COLLAR.get());
            collar.set(fox.spiteful.forbidden.registry.FMDataComponents.STORED_VIS.get(),
                    com.leclowndu93150.thaumaturge.api.wands.WandVis.EMPTY.with(TTAspects.IGNIS, 100));
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            player.setItemInHand(InteractionHand.MAIN_HAND, wand());
            player.tickCount = CollarItem.PULSE_TICKS;
            CollarItem.wornTick(collar, player);
            int wandIgnis = fox.spiteful.forbidden.util.WandBridge.vis(player.getMainHandItem(), TTAspects.IGNIS);
            check(helper, wandIgnis == CollarItem.PULSE, "wand should receive one pulse of ignis, has " + wandIgnis);
            check(helper, CollarItem.stored(collar).amount(TTAspects.IGNIS) == 100 - CollarItem.PULSE, "collar should give up what it moved");
            helper.succeed();
        });

        add.accept("bauble/collar_goes_on_another_player", helper -> {
            Player sub = helper.makeMockPlayer(GameType.SURVIVAL);
            check(helper, CuriosApi.getCuriosInventory(sub).isPresent(), "mock player has no Curios inventory");
            initSlots(sub);
            check(helper, CuriosBridge.equipNecklace(sub, new ItemStack(FMItems.SUB_COLLAR.get())), "necklace slot refused the collar");
            check(helper, !CuriosBridge.findEquipped(sub, FMItems.SUB_COLLAR.get()).isEmpty(), "collar not found after equipping");
            helper.succeed();
        });

        add.accept("bauble/ring_of_nutrition_bonus", helper -> {
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            initSlots(player);
            var rings = CuriosApi.getCuriosInventory(player).flatMap(inv -> inv.getStacksHandler("ring"));
            check(helper, rings.isPresent(), "no ring slot on players, slots: " + CuriosApi.getCuriosInventory(player).map(i -> i.getCurios().keySet()).orElse(null));
            rings.get().getStacks().setStackInSlot(0, new ItemStack(FMItems.RING_NUTRITION.get()));
            player.getFoodData().setFoodLevel(0);
            player.getFoodData().setSaturation(0.0F);
            ItemStack bread = new ItemStack(Items.BREAD);
            FMHarvestEvents.onEat(new LivingEntityUseItemEvent.Finish(player, bread, 0, ItemStack.EMPTY));
            check(helper, player.getFoodData().getFoodLevel() == 2, "ring should add 2 hunger, food is " + player.getFoodData().getFoodLevel());
            check(helper, player.getFoodData().getSaturationLevel() > 1.9F, "ring should add saturation, has " + player.getFoodData().getSaturationLevel());
            helper.succeed();
        });
    }

    /** A mock player never "joins", so Curios has not built its slots yet; real players get them on login. */
    private static void initSlots(Player player) {
        CuriosApi.getCuriosInventory(player).ifPresent(inv -> {
            if (inv.getCurios().isEmpty()) {
                inv.reset();
            }
        });
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static ItemStack wand() {
        ItemStack wand = new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("thaumaturge", "wand")));
        DataComponentType type = BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(Identifier.fromNamespaceAndPath("thaumaturge", "wand_parts"));
        JsonObject json = new JsonObject();
        json.addProperty("cap", "thaumaturge:iron");
        json.addProperty("rod", "forbidden:infernal");
        wand.set(type, ((Codec) type.codec()).parse(JsonOps.INSTANCE, json).getOrThrow());
        return wand;
    }

    private static void check(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            throw helper.assertionException(Component.literal(message));
        }
    }
}
