package fox.spiteful.forbidden.content.tool;

import com.leclowndu93150.thaumaturge.api.items.IWarpingGear;
import fox.spiteful.forbidden.FMConfig;
import fox.spiteful.forbidden.registry.FMEnchantments;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The four morphic ("chameleon") tools. All share phase switching ({@link MorphTools}); while enchanted with Voidtouched
 * they repair one point every 10 ticks and carry one warp, and the pickaxe and shovel dig 3x3 with Impact, as in FM.
 */
public final class MorphItems {
    private static final ThreadLocal<Boolean> IMPACT_RUNNING = ThreadLocal.withInitial(() -> false);

    private MorphItems() {}

    static int voidWarp(ItemStack stack, LivingEntity wearer) {
        return FMEnchantments.level(wearer.level(), stack, FMEnchantments.VOIDTOUCHED) > 0 ? 1 : 0;
    }

    /** FM: Voidtouched tools mend one durability every 10 ticks while carried. */
    public static void voidRepair(ItemStack stack, ServerLevel level, Entity owner) {
        if (FMConfig.enchantments() && stack.isDamaged() && owner.tickCount % 10 == 0 && FMEnchantments.level(level, stack, FMEnchantments.VOIDTOUCHED) > 0) {
            stack.setDamageValue(stack.getDamageValue() - 1);
        }
    }

    /**
     * FM Impact: after a block is mined, the other eight blocks of the 3x3 plane facing the player are harvested too, if
     * the tool is effective on them. FM remembered the clicked face; here the plane is perpendicular to the player's
     * dominant look direction, which picks the same face in practice.
     */
    public static void impact(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity owner) {
        if (!FMConfig.enchantments() || IMPACT_RUNNING.get() || !(owner instanceof ServerPlayer player) || !(level instanceof ServerLevel server)
                || !stack.isCorrectToolForDrops(state) || FMEnchantments.level(server, stack, FMEnchantments.IMPACT) <= 0) {
            return;
        }
        Direction.Axis axis = Direction.orderedByNearest(player)[0].getAxis();
        IMPACT_RUNNING.set(true);
        try {
            for (int a = -1; a <= 1; a++) {
                for (int b = -1; b <= 1; b++) {
                    if (a == 0 && b == 0) {
                        continue;
                    }
                    BlockPos target = switch (axis) {
                        case Y -> pos.offset(a, 0, b);
                        case Z -> pos.offset(a, b, 0);
                        case X -> pos.offset(0, b, a);
                    };
                    BlockState other = server.getBlockState(target);
                    if (stack.isEmpty() || other.isAir() || other.getDestroySpeed(server, target) < 0 || !stack.isCorrectToolForDrops(other)) {
                        continue;
                    }
                    player.gameMode.destroyBlock(target);
                }
            }
        } finally {
            IMPACT_RUNNING.set(false);
        }
    }

    /** Pickaxe and sword: plain {@link Item}s configured through {@code Properties#pickaxe/sword}. */
    public static final class Simple extends Item implements IWarpingGear {
        public Simple(Properties properties) {
            super(properties);
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            InteractionResult result = MorphTools.trySwitch(level, player, hand);
            return result == InteractionResult.PASS ? super.use(level, player, hand) : result;
        }

        @Override
        public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity owner) {
            boolean result = super.mineBlock(stack, level, state, pos, owner);
            impact(stack, level, state, pos, owner);
            return result;
        }

        @Override
        public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
            voidRepair(stack, level, owner);
        }

        @Override
        public int warp(ItemStack stack, LivingEntity wearer) {
            return voidWarp(stack, wearer);
        }
    }

    public static final class Shovel extends ShovelItem implements IWarpingGear {
        public Shovel(ToolMaterial material, float damage, float speed, Properties properties) {
            super(material, damage, speed, properties);
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            InteractionResult result = MorphTools.trySwitch(level, player, hand);
            return result == InteractionResult.PASS ? super.use(level, player, hand) : result;
        }

        @Override
        public InteractionResult useOn(UseOnContext context) {
            if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown()) {
                return MorphTools.trySwitch(context.getLevel(), context.getPlayer(), context.getHand());
            }
            return super.useOn(context);
        }

        @Override
        public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity owner) {
            boolean result = super.mineBlock(stack, level, state, pos, owner);
            impact(stack, level, state, pos, owner);
            return result;
        }

        @Override
        public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
            voidRepair(stack, level, owner);
        }

        @Override
        public int warp(ItemStack stack, LivingEntity wearer) {
            return voidWarp(stack, wearer);
        }
    }

    public static final class Axe extends AxeItem implements IWarpingGear {
        public Axe(ToolMaterial material, float damage, float speed, Properties properties) {
            super(material, damage, speed, properties);
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            InteractionResult result = MorphTools.trySwitch(level, player, hand);
            return result == InteractionResult.PASS ? super.use(level, player, hand) : result;
        }

        @Override
        public InteractionResult useOn(UseOnContext context) {
            if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown()) {
                return MorphTools.trySwitch(context.getLevel(), context.getPlayer(), context.getHand());
            }
            return super.useOn(context);
        }

        @Override
        public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
            voidRepair(stack, level, owner);
        }

        @Override
        public int warp(ItemStack stack, LivingEntity wearer) {
            return voidWarp(stack, wearer);
        }
    }
}
