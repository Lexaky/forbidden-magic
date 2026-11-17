package fox.spiteful.forbidden.content.item;

import com.leclowndu93150.thaumaturge.api.aspect.Aspects;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.api.research.pool.AspectPoolAccess;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * FM's scribing tools. Both are ordinary scribing tools for Thaumaturge through the {@code thaumaturge:scribing_tools}
 * tag, which is all its research table and notes look at.
 */
public final class ScribingToolsItems {
    private ScribingToolsItems() {}

    /**
     * Crystal Scribing Tools (FM {@code ItemCrystalwell}): 100 uses of ink. Once dry it glows; using it then shatters the
     * crystal into 4-7 points of every primal in the player's aspect pool and leaves an empty set of Thaumaturge scribing
     * tools behind.
     */
    public static final class Crystalwell extends Item {
        private static final Identifier EMPTY_TOOLS = Identifier.fromNamespaceAndPath("thaumaturge", "scribing_tools");

        public Crystalwell(Properties properties) {
            super(properties);
        }

        public static boolean isDry(ItemStack stack) {
            return stack.getDamageValue() >= stack.getMaxDamage();
        }

        @Override
        public boolean isFoil(ItemStack stack) {
            return isDry(stack);
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (!isDry(stack)) {
                return InteractionResult.PASS;
            }
            if (player instanceof ServerPlayer serverPlayer) {
                shatter(serverPlayer);
                level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 1.0F, 1.0F);
                player.setItemInHand(hand, emptyTools());
            }
            player.swing(hand);
            return InteractionResult.SUCCESS;
        }

        /** Grants 4-7 points of each primal to the player's aspect pool. */
        public static void shatter(ServerPlayer player) {
            for (ResourceKey<IAspect> primal : TTAspects.PRIMALS) {
                Holder<IAspect> holder = Aspects.resolve(player.level(), primal);
                if (holder != null) {
                    AspectPoolAccess.grant(player, holder, 4 + player.getRandom().nextInt(4));
                }
            }
        }

        public static ItemStack emptyTools() {
            ItemStack tools = new ItemStack(BuiltInRegistries.ITEM.getValue(EMPTY_TOOLS));
            tools.setDamageValue(tools.getMaxDamage());
            return tools;
        }
    }

    /** Primal Scribing Tools (FM {@code ItemPrimewell}): never runs dry. */
    public static final class Primewell extends Item {
        public Primewell(Properties properties) {
            super(properties);
        }

        @Override
        public void setDamage(ItemStack stack, int damage) {
            super.setDamage(stack, 0);
        }
    }
}
