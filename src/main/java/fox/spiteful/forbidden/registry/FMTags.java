package fox.spiteful.forbidden.registry;

import fox.spiteful.forbidden.Forbidden;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public final class FMTags {
    /** Repair material of the morphic tools (FM: envy shard). */
    public static final TagKey<Item> REPAIRS_MORPHIC = item("repairs/morphic");
    /** Drops the Consuming enchantment destroys (FM config "Garbage Blocks": dirt, sand, gravel, cobblestone, netherrack). */
    public static final TagKey<Item> CONSUMING_GARBAGE = item("consuming_garbage");
    /** Thaumaturge blocks that count as taint for FM tools (TC4 "taint material"). */
    public static final TagKey<Block> TAINT_BLOCKS = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("thaumaturge", "taint_blocks"));

    private FMTags() {}

    private static TagKey<Item> item(String path) {
        return TagKey.create(Registries.ITEM, Forbidden.id(path));
    }
}
