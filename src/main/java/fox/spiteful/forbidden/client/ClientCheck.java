package fox.spiteful.forbidden.client;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import fox.spiteful.forbidden.Forbidden;
import fox.spiteful.forbidden.content.item.MobCrystalItem;
import fox.spiteful.forbidden.content.tool.MorphTools;
import fox.spiteful.forbidden.registry.FMItems;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Development-only visual check, enabled with {@code -Dforbidden.clientcheck=true} (the {@code clientCheck} run). Once in a
 * world it builds a showcase, frames it and saves screenshots to {@code screenshots/fmcheck_*.png}, then quits. Nothing
 * here runs in a normal game.
 */
@net.neoforged.fml.common.EventBusSubscriber(modid = Forbidden.MODID, value = net.neoforged.api.distmarker.Dist.CLIENT)
public final class ClientCheck {
    private static int ticks;
    private static int step;
    private static int wait;

    private ClientCheck() {}

    @net.neoforged.bus.api.SubscribeEvent
    public static void enable(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
        if (Boolean.getBoolean("forbidden.clientcheck")) {
            NeoForge.EVENT_BUS.addListener(ClientCheck::onTick);
        }
    }

    private static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.getSingleplayerServer() == null) {
            return;
        }
        if (++ticks < 100 || --wait > 0) {
            return;
        }
        MinecraftServer server = mc.getSingleplayerServer();
        switch (step++) {
            case 0 -> server.execute(() -> setUp(server, server.getPlayerList().getPlayer(mc.player.getUUID())));
            case 1 -> pause(200);
            case 2 -> view(server, mc, "0.5 201 3.5 180 12");
            case 3 -> shot(mc, "blocks");
            case 4 -> view(server, mc, "0.5 201 -11.5 0 12");
            case 5 -> shot(mc, "logs_and_cage");
            case 6 -> {
                clean(mc);
                mc.setScreen(new ItemGrid());
                pause(20);
            }
            case 7 -> shot(mc, "items");
            case 8 -> {
                mc.setScreen(null);
                hold(server, mc, 0);
            }
            case 9 -> shot(mc, "hand_infernal_wand");
            case 10 -> hold(server, mc, 1);
            case 11 -> shot(mc, "hand_tainted_wand");
            case 12 -> hold(server, mc, 2);
            case 13 -> shot(mc, "hand_profane_wand");
            case 14 -> hold(server, mc, 3);
            case 15 -> shot(mc, "hand_morph_pickaxe_phase1");
            case 16 -> view(server, mc, "0.5 201 10.5 180 5");
            case 17 -> shot(mc, "heretic");
            case 18 -> {
                clean(mc);
                openBrowser(mc);
                pause(40);
            }
            case 19 -> shot(mc, "thaumonomicon");
            case 20 -> {
                openEntry(mc, "skull_axe");
                pause(40);
            }
            case 21 -> shot(mc, "entry_skull_axe");
            case 22 -> {
                openEntry(mc, "wrath_cage");
                pause(40);
            }
            case 23 -> shot(mc, "entry_wrath_cage");
            case 24 -> {
                openEntry(mc, "skull_axe");
                pause(40);
            }
            case 25 -> click(mc, 591, 148);
            case 26 -> shot(mc, "recipe_skull_axe");
            case 27 -> {
                openEntry(mc, "wrath_cage");
                pause(40);
            }
            case 28 -> click(mc, 591, 148);
            case 29 -> shot(mc, "recipe_wrath_cage");
            case 30 -> click(mc, 591, 172);
            case 31 -> shot(mc, "recipe_mob_crystal");
            case 32 -> {
                Forbidden.LOGGER.info("[clientcheck] done");
                mc.stop();
            }
            default -> {}
        }
    }

    private static void pause(int t) {
        wait = t;
    }

    private static void run(MinecraftServer server, ServerPlayer player, String command) {
        server.getCommands().performPrefixedCommand(player.createCommandSourceStack().withPermission(net.minecraft.server.permissions.PermissionSet.ALL_PERMISSIONS), command);
    }

    private static void setUp(MinecraftServer server, ServerPlayer player) {
        for (String c : List.of("gamerule sendCommandFeedback false", "gamemode creative", "time set noon", "weather clear", "gamerule doDaylightCycle false", "gamerule doMobSpawning false",
                "fill -12 199 -16 12 199 16 minecraft:stone", "fill -12 200 -16 12 200 16 minecraft:grass_block", "fill -12 201 -16 12 220 16 minecraft:air",
                // Row 1 (looked at from z=3.5): small and special blocks
                "setblock -5 201 -5 forbidden:arcane_cake", "setblock -4 201 -5 forbidden:arcane_cake[eaten=6]", "setblock -3 201 -5 forbidden:umbral_rose",
                "setblock -2 201 -5 forbidden:umbral_bush[half=lower]", "setblock -2 202 -5 forbidden:umbral_bush[half=upper]",
                "setblock -1 201 -5 forbidden:nether_star_block", "setblock 0 201 -5 forbidden:taint_log", "setblock 1 201 -5 forbidden:taint_planks",
                "setblock 2 201 -5 forbidden:taint_leaves[persistent=true]", "setblock 3 201 -5 forbidden:taint_sapling", "setblock 4 201 -5 forbidden:taint_stone",
                "setblock 5 201 -5 forbidden:taint_stone_bricks",
                // Row 2 (looked at from z=-11.5): a log wall for the random bark, horizontal logs and the wrath cage
                "fill -3 201 -8 1 203 -8 forbidden:taint_log", "fill 2 201 -8 4 201 -8 forbidden:taint_log[axis=x]", "setblock 3 202 -8 forbidden:wrath_cage",
                // The heretic, standing still where the camera looks at z=10.5
                "summon minecraft:villager 0.5 201 7.5 {NoAI:1b,Rotation:[0f,0f],VillagerData:{profession:\"forbidden:heretic\",level:3,type:\"minecraft:plains\"}}")) {
            run(server, player, c);
        }
        // Unlock every research so the Thaumonomicon shows the whole tree.
        var entries = server.registryAccess().lookupOrThrow(com.leclowndu93150.thaumaturge.api.research.IResearchEntry.REGISTRY_KEY);
        entries.listElementIds().forEach(key -> com.leclowndu93150.thaumaturge.content.research.ResearchManager.complete(player, key.identifier()));
        player.getInventory().setItem(0, wand("forbidden:infernal", "thaumaturge:gold"));
        player.getInventory().setItem(1, wand("forbidden:tainted", "thaumaturge:thaumium"));
        player.getInventory().setItem(2, wand("forbidden:profane", "thaumaturge:iron"));
        ItemStack morph = new ItemStack(FMItems.MORPH_PICKAXE.get());
        MorphTools.switchPhase(morph);
        player.getInventory().setItem(3, morph);
    }

    private static void clean(Minecraft mc) {
        mc.gui.getChat().clearMessages(false);
        mc.getToastManager().clear();
    }

    private static void view(MinecraftServer server, Minecraft mc, String where) {
        clean(mc);
        server.execute(() -> run(server, server.getPlayerList().getPlayer(mc.player.getUUID()), "tp @s " + where));
        pause(40);
    }

    private static void hold(MinecraftServer server, Minecraft mc, int slot) {
        server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayer(mc.player.getUUID());
            run(server, player, "tp @s 0.5 201 3.5 180 25");
        });
        mc.player.getInventory().setSelectedSlot(slot);
        mc.options.hideGui = false;
        clean(mc);
        pause(40);
    }

    private static void shot(Minecraft mc, String name) {
        boolean scene = !name.startsWith("hand") && mc.screen == null;
        mc.options.hideGui = scene;
        Screenshot.grab(mc.gameDirectory, "fmcheck_" + name + ".png", mc.getMainRenderTarget(), 1, msg -> Forbidden.LOGGER.info("[clientcheck] {}: {}", name, msg.getString()));
        pause(10);
    }

    private static void openBrowser(Minecraft mc) {
        try {
            var field = com.leclowndu93150.thaumaturge.client.screen.research.ThaumonomiconBrowserScreen.class.getDeclaredField("rememberedCategory");
            field.setAccessible(true);
            field.set(null, Forbidden.id(Forbidden.MODID));
        } catch (ReflectiveOperationException e) {
            Forbidden.LOGGER.error("[clientcheck] could not preselect the category", e);
        }
        mc.setScreen(new com.leclowndu93150.thaumaturge.client.screen.research.ThaumonomiconBrowserScreen());
    }

    private static void openEntry(Minecraft mc, String entry) {
        var key = net.minecraft.resources.ResourceKey.create(com.leclowndu93150.thaumaturge.api.research.IResearchEntry.REGISTRY_KEY, Forbidden.id(entry));
        var holder = mc.level.registryAccess().lookupOrThrow(com.leclowndu93150.thaumaturge.api.research.IResearchEntry.REGISTRY_KEY).getOrThrow(key);
        mc.setScreen(new com.leclowndu93150.thaumaturge.client.screen.research.EntryDetailScreen(holder, Forbidden.id(entry), null));
    }

    /** Left-clicks the current screen at GUI coordinates (a recipe bookmark of the research book). */
    private static void click(Minecraft mc, double x, double y) {
        mc.screen.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(x, y, new net.minecraft.client.input.MouseButtonInfo(0, 0)), false);
        pause(40);
    }

    /** Opens an entry and turns {@code turns} spreads forward with the arrow key, as a player would. */
    private static void page(Minecraft mc, String entry, int turns) {
        openEntry(mc, entry);
        for (int i = 0; i < turns; i++) {
            mc.screen.keyPressed(new net.minecraft.client.input.KeyEvent(org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT, 0, 0));
        }
        pause(40);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static ItemStack wand(String rod, String cap) {
        ItemStack wand = new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("thaumaturge", "wand")));
        DataComponentType type = BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(Identifier.fromNamespaceAndPath("thaumaturge", "wand_parts"));
        JsonObject json = new JsonObject();
        json.addProperty("cap", cap);
        json.addProperty("rod", rod);
        wand.set(type, ((Codec) type.codec()).parse(JsonOps.INSTANCE, json).getOrThrow());
        return wand;
    }

    /** All FM items, the three Chameleon phases, an imprinted crystal and wands with FM rods, twice their normal size. */
    static final class ItemGrid extends Screen {
        private final List<ItemStack> stacks = new ArrayList<>();

        ItemGrid() {
            super(Component.literal("Forbidden Magic items"));
            FMItems.ITEMS.getEntries().forEach(item -> stacks.add(new ItemStack(item.get())));
            for (int phase = 1; phase < 3; phase++) {
                ItemStack pick = new ItemStack(FMItems.MORPH_PICKAXE.get());
                MorphTools.applyTint(pick, phase);
                stacks.add(pick);
            }
            stacks.add(MobCrystalItem.imprinted(FMItems.MOB_CRYSTAL.get(), EntityType.PIG));
            stacks.add(wand("forbidden:infernal", "thaumaturge:gold"));
            stacks.add(wand("forbidden:tainted", "thaumaturge:thaumium"));
            stacks.add(wand("forbidden:profane", "thaumaturge:iron"));
            stacks.add(wand("forbidden:profaned", "thaumaturge:iron"));
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
            g.fill(0, 0, width, height, 0xFF202028);
            int columns = 10;
            int cellW = width / columns;
            int cellH = 46;
            for (int i = 0; i < stacks.size(); i++) {
                int x = (i % columns) * cellW;
                int y = (i / columns) * cellH + 4;
                g.pose().pushMatrix();
                g.pose().translate(x + cellW / 2.0F - 16, y);
                g.pose().scale(2.0F, 2.0F);
                g.item(stacks.get(i), 0, 0);
                g.pose().popMatrix();
                String name = stacks.get(i).getHoverName().getString();
                if (font.width(name) > cellW - 4) {
                    name = font.plainSubstrByWidth(name, cellW - 8) + "…";
                }
                g.text(font, name, x + (cellW - font.width(name)) / 2, y + 34, 0xFFFFFFFF, true);
            }
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }
    }

    @SuppressWarnings("unused")
    private static File dir(Minecraft mc) {
        return new File(mc.gameDirectory, "screenshots");
    }
}
