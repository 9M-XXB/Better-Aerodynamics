package rfb.betteraero;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.MathHelper;

public class AtmosphereManager {
    // --- ISA-like constants (imperial) ---
    public static final double R0    = 1716.554;   // ft·lbf/(slug·°R)
    public static final double T0    = 518.67;     // °R
    public static final double P0    = 2116.22;    // lbf/ft^2
    public static final double RHO0  = 0.0023769;  // slug/ft^3
    public static final double GAMMA = 1.4;

    private static final double H_TROPOPAUSE = 36089.0; // ft
    private static final double H_STRAT_20KM = 65617.0; // ft

    // Map Minecraft Y to altitude feet; clamp to model range
    public static double altitudeFeet(ServerWorld world, PlayerEntity player) {
        int sea = world.getSeaLevel();     // ~62
        double y = player.getY();
        double top = 320.0;

        double span = Math.max(1.0, top - sea);
        double normalized = (y - sea) / span;      // 0 at sea level, ~1 at Y=320
        double h = normalized * 29031.0;           // Everest ~ 29,031 ft at Y=320

        return MathHelper.clamp(h, -2000.0, 100000.0);
    }

    // θ = T/T0
    public static double getTheta(ServerWorld world, PlayerEntity player) {
        double h = altitudeFeet(world, player);
        if (h <= H_TROPOPAUSE) {
            return 1.0 - 6.87535e-6 * h;
        } else if (h <= H_STRAT_20KM) {
            return 0.75187;
        } else {
            return 0.68246 + 1.05778e-6 * h;
        }
    }

    // δ = P/P0
    public static double getDelta(ServerWorld world, PlayerEntity player) {
        double h = altitudeFeet(world, player);
        double theta = getTheta(world, player);
        if (h <= H_TROPOPAUSE) {
            return Math.pow(theta, 5.2561);
        } else if (h <= H_STRAT_20KM) {
            return 0.22336 * Math.exp((H_TROPOPAUSE - h) / 20806.7);
        } else {
            return 3.17176e-6 * Math.pow(theta, -34.1632);
        }
    }

    // σ = ρ/ρ0
    public static double getSigma(ServerWorld world, PlayerEntity player) {
        double theta = getTheta(world, player);
        double delta = getDelta(world, player); // (fixed)
        return delta / theta;
    }

    // Physical values (keep units: lbf/ft^2, °R, slug/ft^3)
    public static double getDensity(ServerWorld world, PlayerEntity player) {
        return getSigma(world, player) * RHO0;
    }

    public static double getPressure(ServerWorld world, PlayerEntity player) {
        return getDelta(world, player) * P0;
    }

    public static double getTemperature(ServerWorld world, PlayerEntity player) {
        return getTheta(world, player) * T0;
    }

    // ---------- Damage thresholds (imperial base units) ----------
    private static final double MIN_PRESSURE          = 1200.0;    // lbf/ft^2
    private static final double MIN_TEMPERATURE_R     = 440.0;     // °R
    private static final double MIN_DENSITY           = 0.0012;    // slug/ft^3

    private static final double PRESSURE_DMG_PER      = 0.002;     // HP/s per lbf/ft^2
    private static final double TEMP_DMG_PER          = 0.01;      // HP/s per °R
    private static final double DENS_DMG_PER          = 900.0;     // HP/s per slug/ft^3

    private static final float  MAX_DPS               = 4.0f;

    /** Compute damage per second based on thresholds. */
    public static float computeAtmosphereDamagePerSecond(ServerWorld world, PlayerEntity player) {
        double p   = getPressure(world, player);
        double tr  = getTemperature(world, player);
        double rho = getDensity(world, player);

        double dmg = 0.0;
        if (p < MIN_PRESSURE)       dmg += (MIN_PRESSURE - p) * PRESSURE_DMG_PER;
        if (tr < MIN_TEMPERATURE_R) dmg += (MIN_TEMPERATURE_R - tr) * TEMP_DMG_PER;
        if (rho < MIN_DENSITY)      dmg += (MIN_DENSITY - rho) * DENS_DMG_PER;

        if (dmg <= 0.0) return 0f;
        return (float) Math.min(dmg, MAX_DPS);
    }
}
