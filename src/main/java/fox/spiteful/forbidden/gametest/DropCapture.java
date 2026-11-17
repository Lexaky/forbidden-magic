package fox.spiteful.forbidden.gametest;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/**
 * Records every item entity a piece of code tries to add to a level and cancels it, so drops in chunks the test
 * harness has not loaded (the Nether) can be counted without littering the world.
 */
final class DropCapture {
    private final ServerLevel level;
    private final List<ItemStack> captured = new ArrayList<>();

    private DropCapture(ServerLevel level) {
        this.level = level;
    }

    static List<ItemStack> during(ServerLevel level, Runnable action) {
        DropCapture capture = new DropCapture(level);
        NeoForge.EVENT_BUS.register(capture);
        try {
            action.run();
        } finally {
            NeoForge.EVENT_BUS.unregister(capture);
        }
        return capture.captured;
    }

    @SubscribeEvent
    public void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel() == level && event.getEntity() instanceof ItemEntity item) {
            captured.add(item.getItem().copy());
            event.setCanceled(true);
        }
    }
}
