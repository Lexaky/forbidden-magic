package fox.spiteful.forbidden.content.spell;

import com.leclowndu93150.thaumaturge.api.spell.behavior.AbstractEffectBehavior;
import com.leclowndu93150.thaumaturge.api.spell.behavior.SpellBehaviorType;
import com.leclowndu93150.thaumaturge.api.spell.cast.CastContext;
import com.leclowndu93150.thaumaturge.api.spell.cast.SpellTarget;
import com.mojang.serialization.MapCodec;
import fox.spiteful.forbidden.registry.FMSpellBehaviors;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * FM's Blink focus upgrades as Thaumaturge spell effects.
 *
 * <p>In FM they were upgrades of one focus: Hellfire burned everything around the arrival point, Pandemonium dragged the
 * monsters there back to where the caster stood. Thaumaturge builds spells from parts, so each becomes an effect that
 * works on whatever the spell hits. Placed after Blink, Hellfire reproduces FM; Pandemonium must run before Blink, while
 * the caster is still at the departure point. Both act on a 3-block radius around a point target, as FM did, or on the
 * entity hit.
 */
public final class BlinkUpgradeEffects {
    static final double RADIUS = 3.0;

    private BlinkUpgradeEffects() {}

    static List<LivingEntity> around(CastContext ctx, SpellTarget target) {
        if (target.entity().isPresent() && target.entity().get() instanceof LivingEntity living) {
            return List.of(living);
        }
        Vec3 at = target.position();
        return new ArrayList<>(ctx.level().getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(RADIUS), e -> e.isAlive() && e != ctx.caster()));
    }

    /** Hellfire: 3 fire damage and 3 seconds of burning, +3 of each per point of power above 1 (FM: per potency rank). */
    public static final class Hellfire extends AbstractEffectBehavior {
        public static final MapCodec<Hellfire> CODEC = MapCodec.unit(Hellfire::new);

        @Override
        public SpellBehaviorType<?> type() {
            return FMSpellBehaviors.HELLFIRE.get();
        }

        @Override
        protected boolean widens() {
            return false;
        }

        @Override
        protected void apply(CastContext ctx, SpellTarget target, float power, int index) {
            float potency = Math.max(0.0F, power - 1.0F);
            ServerLevel level = ctx.level();
            for (LivingEntity entity : around(ctx, target)) {
                if (entity == ctx.caster()) {
                    continue;
                }
                level.sendParticles(ParticleTypes.FLAME, entity.getX(), entity.getY() + entity.getBbHeight() / 2.0, entity.getZ(), 8, entity.getBbWidth() / 2.0,
                        entity.getBbHeight() / 2.0, entity.getBbWidth() / 2.0, 0.01);
                entity.hurtServer(level, level.damageSources().fireball(null, ctx.caster()), 3.0F + 3.0F * potency);
                entity.igniteForSeconds(3.0F + 3.0F * potency);
            }
        }
    }

    /** Pandemonium: monsters near the target are pulled to the caster. */
    public static final class Pandemonium extends AbstractEffectBehavior {
        public static final MapCodec<Pandemonium> CODEC = MapCodec.unit(Pandemonium::new);

        @Override
        public SpellBehaviorType<?> type() {
            return FMSpellBehaviors.PANDEMONIUM.get();
        }

        @Override
        protected boolean widens() {
            return false;
        }

        @Override
        protected void apply(CastContext ctx, SpellTarget target, float power, int index) {
            LivingEntity caster = ctx.caster();
            if (caster == null) {
                return;
            }
            ServerLevel level = ctx.level();
            for (LivingEntity entity : around(ctx, target)) {
                if (entity instanceof Enemy) {
                    level.sendParticles(ParticleTypes.SMOKE, entity.getX(), entity.getY() + entity.getBbHeight() / 2.0, entity.getZ(), 8, 0.3, 0.3, 0.3, 0.01);
                    entity.teleportTo(caster.getX(), caster.getY(), caster.getZ());
                }
            }
        }
    }
}
