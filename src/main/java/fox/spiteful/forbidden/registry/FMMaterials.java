package fox.spiteful.forbidden.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;

/**
 * Tool materials. Forbidden Magic built almost everything on Thaumcraft's elemental material and the fork on thaumium;
 * the values below mirror Thaumaturge's own elemental/thaumium tiers so FM tools sit at the same power level.
 */
public final class FMMaterials {
    public static final TagKey<Item> INGOTS_THAUMIUM = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "ingots/thaumium"));

    /** TC4 toolMatElemental: diamond tier, 1500 uses. */
    public static final ToolMaterial ELEMENTAL = new ToolMaterial(BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 1500, 9.0F, 3.0F, 18, INGOTS_THAUMIUM);
    /** Harvest level 4/5 tools in FM (morphic tools, taint pickaxe) — one step above diamond. */
    public static final ToolMaterial ELEMENTAL_HIGH = new ToolMaterial(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 1500, 9.0F, 3.0F, 18, INGOTS_THAUMIUM);
    /** TC4 toolMatThaumium. */
    public static final ToolMaterial THAUMIUM = new ToolMaterial(BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 500, 7.0F, 2.5F, 22, INGOTS_THAUMIUM);
    /** Riding crop: wooden material repaired with leather. */
    public static final ToolMaterial CROP = new ToolMaterial(BlockTags.INCORRECT_FOR_WOODEN_TOOL, 59, 2.0F, 0.0F, 15, net.neoforged.neoforge.common.Tags.Items.LEATHERS);

    private FMMaterials() {}
}
