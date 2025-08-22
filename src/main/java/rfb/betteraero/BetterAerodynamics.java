package rfb.betteraero;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import main.java.rfb.betteraero.AtmosphereManager;

public class BetterAerodynamics implements ModInitializer {
	public static final String MOD_ID = "betteraerodynamics";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
    public void onInitialize() {
        LOGGER.info("Better Aerodynamics initialized!");

        // Example: run atmosphere calculations each tick
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerWorld world : server.getWorlds()) {
                for (PlayerEntity player : world.getPlayers()) {
                    double density = AtmosphereManager.getAirDensity(world, player);
                    double pressure = AtmosphereManager.getPressure(world, player);
                    double temperature = AtmosphereManager.getTemperature(world, player);

                    // Just demo output for now:
                    player.sendMessage(Text.of(
                        String.format("ρ=%.2f, P=%.1f kPa, T=%.1f °C",
                                      density, pressure, temperature)
                    ), true);
                }
            }
        });
    }
}