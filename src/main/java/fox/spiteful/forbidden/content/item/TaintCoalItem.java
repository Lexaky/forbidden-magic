package fox.spiteful.forbidden.content.item;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.FuelValues;
import org.jspecify.annotations.Nullable;

/** Tainted charcoal (FM {@code ItemTaintCoal}): burns for a random 1-2400 ticks, re-rolled every time it is queried. */
public final class TaintCoalItem extends Item {
    private static final RandomSource RANDOM = RandomSource.create();

    public TaintCoalItem(Properties properties) {
        super(properties);
    }

    @Override
    public int getBurnTime(ItemStack stack, @Nullable RecipeType<?> recipeType, FuelValues fuelValues) {
        return 1 + RANDOM.nextInt(2400);
    }
}
