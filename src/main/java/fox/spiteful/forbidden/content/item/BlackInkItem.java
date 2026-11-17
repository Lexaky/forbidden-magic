package fox.spiteful.forbidden.content.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Black ink (FM {@code FMResource:1}): a black dye that also dyes unsheared sheep black on right click. */
public final class BlackInkItem extends Item {
    public BlackInkItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (target instanceof Sheep sheep && sheep.isAlive() && !sheep.isSheared() && sheep.getColor() != DyeColor.BLACK) {
            if (!player.level().isClientSide()) {
                sheep.setColor(DyeColor.BLACK);
                stack.consume(1, player);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
