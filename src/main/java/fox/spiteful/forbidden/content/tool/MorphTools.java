package fox.spiteful.forbidden.content.tool;

import fox.spiteful.forbidden.registry.FMDataComponents;
import fox.spiteful.forbidden.registry.FMDataComponents.MorphSlot;
import fox.spiteful.forbidden.registry.FMDataComponents.MorphStore;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;

/**
 * Phase switching shared by the four morphic tools (FM {@code ItemMorph*#onItemRightClick}).
 *
 * <p>A morphic tool keeps three independent sets of enchantments and custom names. Sneak + use stores the current set in
 * the slot of the current phase, advances the phase (0 → 1 → 2 → 0) and loads the next slot, at the cost of 5
 * durability. The tool refuses to switch when 5 or fewer uses remain. The overlay colour of each phase (red, blue,
 * yellow) is written to {@code minecraft:custom_model_data} so the item model can tint it without client code.
 */
public final class MorphTools {
    public static final int[] PHASE_COLORS = {0x980000, 0x0010CC, 0xE5DA00};
    private static final int SWITCH_COST = 5;

    private MorphTools() {}

    public static int phase(ItemStack stack) {
        return stack.getOrDefault(FMDataComponents.MORPH_PHASE.get(), 0);
    }

    public static InteractionResult trySwitch(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown() || stack.getMaxDamage() - stack.getDamageValue() <= SWITCH_COST) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            switchPhase(stack);
            stack.hurtAndBreak(SWITCH_COST, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 0.3F, 0.6F + level.getRandom().nextFloat() * 0.2F);
        }
        player.swing(hand);
        return InteractionResult.SUCCESS;
    }

    /** Stores the active phase, advances, and restores the next one. Visible for game tests. */
    public static void switchPhase(ItemStack stack) {
        int phase = phase(stack);
        MorphStore store = stack.getOrDefault(FMDataComponents.MORPH_STORE.get(), MorphStore.EMPTY);
        ItemEnchantments current = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        Optional<Component> name = Optional.ofNullable(stack.get(DataComponents.CUSTOM_NAME));
        store = store.with(phase, new MorphSlot(current, name));

        int next = (phase + 1) % 3;
        MorphSlot loaded = store.get(next);
        stack.set(DataComponents.ENCHANTMENTS, loaded.enchantments());
        loaded.name().ifPresentOrElse(n -> stack.set(DataComponents.CUSTOM_NAME, n), () -> stack.remove(DataComponents.CUSTOM_NAME));
        stack.set(FMDataComponents.MORPH_STORE.get(), store);
        stack.set(FMDataComponents.MORPH_PHASE.get(), next);
        applyTint(stack, next);
    }

    public static void applyTint(ItemStack stack, int phase) {
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(), List.of(), List.of(PHASE_COLORS[phase])));
    }
}
