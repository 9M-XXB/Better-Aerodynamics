package rfb.betteraero;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rfb.betteraero.AtmosphereManager;

public class BetterAerodynamics implements ModInitializer {
    public static final String MOD_ID = "betteraerodynamics";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Better Aerodynamics initialized!");

        // Run once/second (20 ticks)
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            long time = server.getOverworld().getTime();
            if ((time % 20) != 0) return;

            for (ServerWorld world : server.getWorlds()) {
                for (PlayerEntity player : world.getPlayers()) {
                    if (player.isCreative() || player.isSpectator()) continue;

                    float dps = AtmosphereManager.computeAtmosphereDamagePerSecond(world, player);
                    if (dps > 0f) {
                        player.damage(lowPressureDamage(world), dps);

                    }
                }
            }
        });
    }

    private DamageSource lowPressureDamage(ServerWorld world) {
    RegistryKey<DamageType> key = RegistryKey.of(
            RegistryKeys.DAMAGE_TYPE,
            Identifier.of(MOD_ID, "low_pressure")   // <-- use Identifier.of
    );

    RegistryEntry<DamageType> entry = world.getRegistryManager()
            .getOrThrow(RegistryKeys.DAMAGE_TYPE)  // <-- getOrThrow(...), not get(...)
            .entryOf(key);                         // <-- entryOf(...)
    return new DamageSource(entry);
}
}
