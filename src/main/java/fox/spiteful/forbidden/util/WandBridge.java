package fox.spiteful.forbidden.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.wands.WandAccess;
import com.leclowndu93150.thaumaturge.api.wands.WandRod;
import com.leclowndu93150.thaumaturge.api.wands.WandVis;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import java.util.Optional;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;

/**
 * Narrow bridge to a Thaumaturge wand stack.
 *
 * <p>Reading vis goes through the public {@link WandAccess} API. Thaumaturge's API only offers a copying setter, while
 * rod update hooks must change the stack they were handed, so writes target the {@code thaumaturge:wand_vis} data
 * component (its value type, {@link WandVis}, is part of the API). The wand's parts live in the
 * {@code thaumaturge:wand_parts} component, whose Java type is internal; it is only touched through the component's own
 * codec, so this addon never links against Thaumaturge implementation classes for it.
 */
public final class WandBridge {
    private static final Identifier VIS_COMPONENT = Identifier.fromNamespaceAndPath("thaumaturge", "wand_vis");
    private static final Identifier PARTS_COMPONENT = Identifier.fromNamespaceAndPath("thaumaturge", "wand_parts");
    private static final int CENTIVIS_PER_VIS = 100;
    private static final int SCEPTRE_CENTIVIS_PER_VIS = 150;

    private WandBridge() {}

    public static int vis(ItemStack wand, ResourceKey<IAspect> aspect) {
        return WandAccess.getVis(wand, aspect);
    }

    /** Sets the stored centivis of one aspect on this exact stack. */
    @SuppressWarnings("unchecked")
    public static void setVis(ItemStack wand, ResourceKey<IAspect> aspect, int centivis) {
        DataComponentType<WandVis> type = (DataComponentType<WandVis>) BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(VIS_COMPONENT);
        if (type == null) {
            return;
        }
        wand.set(type, WandAccess.getAllVis(wand).with(aspect, Math.max(0, centivis)));
    }

    /** Adds centivis, clamped to the wand's capacity. Returns the amount actually added. */
    public static int addVis(ItemStack wand, ResourceKey<IAspect> aspect, int centivis) {
        int current = vis(wand, aspect);
        int capacity = capacityCentivis(wand);
        int added = Math.max(0, Math.min(centivis, capacity - current));
        if (added > 0) {
            setVis(wand, aspect, current + added);
        }
        return added;
    }

    public static int capacityCentivis(ItemStack wand) {
        Optional<JsonObject> parts = parts(wand);
        if (parts.isEmpty() || !parts.get().has("rod")) {
            return 0;
        }
        WandRod rod = rods().getValue(Identifier.parse(parts.get().get("rod").getAsString()));
        if (rod == null) {
            return 0;
        }
        boolean sceptre = parts.get().has("sceptre") && parts.get().get("sceptre").getAsBoolean();
        return rod.capacity() * (sceptre ? SCEPTRE_CENTIVIS_PER_VIS : CENTIVIS_PER_VIS);
    }

    public static Optional<Identifier> rodId(ItemStack wand) {
        return parts(wand).filter(json -> json.has("rod")).map(json -> Identifier.parse(json.get("rod").getAsString()));
    }

    public static Optional<Identifier> capId(ItemStack wand) {
        return parts(wand).filter(json -> json.has("cap")).map(json -> Identifier.parse(json.get("cap").getAsString()));
    }

    public static boolean hasRod(ItemStack wand, Identifier rod) {
        return rodId(wand).map(rod::equals).orElse(false);
    }

    /** Swaps the rod of a wand (used when a profane contract runs out). */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static boolean replaceRod(ItemStack wand, Identifier newRod) {
        DataComponentType type = BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(PARTS_COMPONENT);
        Optional<JsonObject> parts = parts(wand);
        if (type == null || parts.isEmpty() || type.codec() == null || !rods().containsKey(newRod)) {
            return false;
        }
        JsonObject edited = parts.get().deepCopy();
        edited.add("rod", new JsonPrimitive(newRod.toString()));
        Optional<?> decoded = ((Codec) type.codec()).parse(JsonOps.INSTANCE, edited).result();
        if (decoded.isEmpty()) {
            return false;
        }
        wand.set(type, decoded.get());
        return true;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Optional<JsonObject> parts(ItemStack wand) {
        DataComponentType type = BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(PARTS_COMPONENT);
        if (type == null || type.codec() == null) {
            return Optional.empty();
        }
        Object value = wand.get(type);
        if (value == null) {
            return Optional.empty();
        }
        Optional<JsonElement> json = ((Codec) type.codec()).encodeStart(JsonOps.INSTANCE, value).result();
        return json.filter(JsonElement::isJsonObject).map(JsonElement::getAsJsonObject);
    }

    @SuppressWarnings("unchecked")
    public static Registry<WandRod> rods() {
        return (Registry<WandRod>) BuiltInRegistries.REGISTRY.getValue(WandRod.REGISTRY_KEY.identifier());
    }
}
