package fox.spiteful.forbidden.compat;

import fox.spiteful.forbidden.Forbidden;
import fox.spiteful.forbidden.content.item.CollarItem;
import fox.spiteful.forbidden.registry.FMItems;
import java.util.Optional;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.CuriosCapability;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

/**
 * Curios integration (Baubles in FM). Curios is a hard dependency of Thaumaturge, so it is always present. Slot
 * membership comes from the {@code curios:ring}/{@code curios:necklace} item tags; the capability below supplies the
 * tick behaviour.
 */
@EventBusSubscriber(modid = Forbidden.MODID)
public final class CuriosBridge {
    private CuriosBridge() {}

    @SubscribeEvent
    public static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerItem(CuriosCapability.ITEM, (stack, ctx) -> (ICurio) () -> stack, FMItems.RING_NUTRITION.get());
        event.registerItem(CuriosCapability.ITEM, (stack, ctx) -> new ICurio() {
            @Override
            public ItemStack getStack() {
                return stack;
            }

            @Override
            public void curioTick(SlotContext slotContext) {
                CollarItem.wornTick(stack, slotContext.entity());
            }
        }, FMItems.SUB_COLLAR.get());
    }

    /** First equipped curio of the item, or empty. */
    public static ItemStack findEquipped(LivingEntity entity, Item item) {
        return CuriosApi.getCuriosInventory(entity).flatMap(inv -> inv.findFirstCurio(item)).map(result -> result.stack()).orElse(ItemStack.EMPTY);
    }

    /** Puts the stack into the first empty necklace slot. Returns false when every necklace slot is taken. */
    public static boolean equipNecklace(LivingEntity entity, ItemStack stack) {
        Optional<ICurioStacksHandler> handler = CuriosApi.getCuriosInventory(entity).flatMap(inv -> inv.getStacksHandler("necklace"));
        if (handler.isEmpty()) {
            return false;
        }
        var stacks = handler.get().getStacks();
        for (int slot = 0; slot < stacks.getSlots(); slot++) {
            if (stacks.getStackInSlot(slot).isEmpty()) {
                stacks.setStackInSlot(slot, stack);
                return true;
            }
        }
        return false;
    }
}
