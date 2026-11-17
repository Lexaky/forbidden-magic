package fox.spiteful.forbidden.registry;

import com.leclowndu93150.thaumaturge.content.wands.ItemWandRod;
import fox.spiteful.forbidden.Forbidden;
import fox.spiteful.forbidden.content.item.BlackInkItem;
import fox.spiteful.forbidden.content.item.TaintCoalItem;
import fox.spiteful.forbidden.content.item.TaintFruitItem;
import fox.spiteful.forbidden.content.tool.MorphItems;
import fox.spiteful.forbidden.content.tool.RidingCropItem;
import fox.spiteful.forbidden.content.tool.TaintShovelItem;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Items. FM's metadata items are flattened: {@code NetherShard:0..6} become one item per vice, {@code FMResource:0/1}
 * become {@code emerald_nugget}/{@code black_ink}, {@code WandCores:N} become {@code wand_rod_*}.
 */
public final class FMItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Forbidden.MODID);

    // Deadly shards
    public static final DeferredItem<Item> WRATH_SHARD = ITEMS.registerSimpleItem("wrath_shard");
    public static final DeferredItem<Item> ENVY_SHARD = ITEMS.registerSimpleItem("envy_shard");
    public static final DeferredItem<Item> TAINT_SHARD = ITEMS.registerSimpleItem("taint_shard");
    public static final DeferredItem<Item> PRIDE_SHARD = ITEMS.registerSimpleItem("pride_shard", props -> props.rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> LUST_SHARD = ITEMS.registerSimpleItem("lust_shard");
    public static final DeferredItem<Item> SLOTH_SHARD = ITEMS.registerSimpleItem("sloth_shard");
    public static final DeferredItem<Item> GREED_SHARD = ITEMS.registerSimpleItem("greed_shard");
    public static final DeferredItem<Item> GLUTTONY_SHARD = ITEMS.registerSimpleItem("gluttony_shard",
            props -> props.food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.1F).build()));

    // Resources
    public static final DeferredItem<Item> EMERALD_NUGGET = ITEMS.registerSimpleItem("emerald_nugget");
    public static final DeferredItem<BlackInkItem> BLACK_INK = ITEMS.registerItem("black_ink", BlackInkItem::new);
    public static final DeferredItem<TaintFruitItem> TAINT_FRUIT = ITEMS.registerItem("taint_fruit", TaintFruitItem::new,
            props -> props.food(new FoodProperties.Builder().nutrition(4).saturationModifier(0.8F).build()));
    public static final DeferredItem<TaintCoalItem> TAINT_COAL = ITEMS.registerItem("taint_coal", TaintCoalItem::new);

    // Tools
    public static final DeferredItem<Item> SKULL_AXE = ITEMS.registerItem("skull_axe", Item::new,
            props -> props.sword(FMMaterials.ELEMENTAL, 3.0F, -2.4F).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> DIABOLIST_FORK = ITEMS.registerItem("diabolist_fork", Item::new,
            props -> props.sword(FMMaterials.THAUMIUM, 3.0F, -2.4F).repairable(net.minecraft.world.item.Items.QUARTZ));
    public static final DeferredItem<TaintShovelItem> TAINT_SHOVEL = ITEMS.registerItem("taint_shovel",
            props -> new TaintShovelItem(FMMaterials.ELEMENTAL, 1.5F, -3.0F, props.rarity(Rarity.RARE)));
    public static final DeferredItem<Item> TAINT_PICKAXE = ITEMS.registerItem("taint_pickaxe", Item::new,
            props -> props.pickaxe(FMMaterials.ELEMENTAL_HIGH, 1.0F, -2.8F).rarity(Rarity.RARE));
    public static final DeferredItem<MorphItems.Simple> MORPH_PICKAXE = ITEMS.registerItem("morph_pickaxe", MorphItems.Simple::new,
            props -> props.pickaxe(FMMaterials.ELEMENTAL_HIGH, 1.0F, -2.8F).rarity(Rarity.EPIC).repairable(FMTags.REPAIRS_MORPHIC));
    public static final DeferredItem<MorphItems.Simple> MORPH_SWORD = ITEMS.registerItem("morph_sword", MorphItems.Simple::new,
            props -> props.sword(FMMaterials.ELEMENTAL_HIGH, 3.0F, -2.4F).rarity(Rarity.EPIC).repairable(FMTags.REPAIRS_MORPHIC));
    public static final DeferredItem<MorphItems.Shovel> MORPH_SHOVEL = ITEMS.registerItem("morph_shovel",
            props -> new MorphItems.Shovel(FMMaterials.ELEMENTAL_HIGH, 1.5F, -3.0F, props.rarity(Rarity.EPIC).repairable(FMTags.REPAIRS_MORPHIC)));
    public static final DeferredItem<MorphItems.Axe> MORPH_AXE = ITEMS.registerItem("morph_axe",
            props -> new MorphItems.Axe(FMMaterials.ELEMENTAL_HIGH, 5.0F, -3.0F, props.rarity(Rarity.EPIC).repairable(FMTags.REPAIRS_MORPHIC)));
    public static final DeferredItem<RidingCropItem> RIDING_CROP = ITEMS.registerItem("riding_crop", RidingCropItem::new,
            props -> props.sword(FMMaterials.CROP, 3.0F, -2.4F));

    public static final DeferredItem<fox.spiteful.forbidden.content.item.MobCrystalItem> MOB_CRYSTAL = ITEMS.registerItem("mob_crystal",
            fox.spiteful.forbidden.content.item.MobCrystalItem::new, props -> props.stacksTo(1));

    // Scribing tools
    public static final DeferredItem<fox.spiteful.forbidden.content.item.ScribingToolsItems.Crystalwell> CRYSTALWELL = ITEMS.registerItem("crystalwell",
            fox.spiteful.forbidden.content.item.ScribingToolsItems.Crystalwell::new, props -> props.stacksTo(1).durability(100));
    public static final DeferredItem<fox.spiteful.forbidden.content.item.ScribingToolsItems.Primewell> PRIMEWELL = ITEMS.registerItem("primewell",
            fox.spiteful.forbidden.content.item.ScribingToolsItems.Primewell::new, props -> props.stacksTo(1).durability(100).rarity(Rarity.EPIC));

    // Baubles (Curios)
    public static final DeferredItem<Item> RING_NUTRITION = ITEMS.registerSimpleItem("ring_nutrition", props -> props.stacksTo(1).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<fox.spiteful.forbidden.content.item.CollarItem> SUB_COLLAR = ITEMS.registerItem("sub_collar",
            fox.spiteful.forbidden.content.item.CollarItem::new, props -> props.stacksTo(1).rarity(Rarity.RARE));

    // Wand rods (Thaumaturge generates the wand/sceptre assembly recipes for every ItemWandRod it finds)
    public static final DeferredItem<ItemWandRod> WAND_ROD_TAINTED = ITEMS.registerItem("wand_rod_tainted",
            props -> new ItemWandRod(props.rarity(Rarity.UNCOMMON), FMWandParts.ROD_TAINTED));
    public static final DeferredItem<ItemWandRod> WAND_ROD_INFERNAL = ITEMS.registerItem("wand_rod_infernal",
            props -> new ItemWandRod(props.rarity(Rarity.UNCOMMON), FMWandParts.ROD_INFERNAL));
    public static final DeferredItem<ItemWandRod> WAND_ROD_PROFANE = ITEMS.registerItem("wand_rod_profane",
            props -> new ItemWandRod(props.rarity(Rarity.UNCOMMON), FMWandParts.ROD_PROFANE));

    private FMItems() {}

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }
}
