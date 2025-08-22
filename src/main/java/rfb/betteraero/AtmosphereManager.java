package main.java.rfb.betteraero;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.entity.player.PlayerEntity;

public class AtmosphereManager {
    //constants in air property calculation, unfortunately all imperial cause I'm an AE student
    public static double R0 = 1716.554;
    public static double T0 = 518.67;
    public static double P0 = 2116.22;
    public static double rho0 = 0.0023769;
    public static double gamma = 1.4;

    //taking damage threshold
    private static final double min_pressure = 1200.0;
    private static final double min_temp = 440;
    private static final double min_density = 0.0012;

    // Scaling factors
    private static final double PRESSURE_DMG_PER  = 0.002;   // HP/s per psi below threshold
    private static final double TEMP_DMG_PER     = 0.01;   // HP/s per °R below threshold
    private static final double DENS_DMG_PER  = 900.0;  // HP/s per (slug/ft³) below threshold

    private static final float MAX_DPS = 4.0f;

    //Y=320 equals the height of Mt.Everest, which is 8848m or 29031ft, convert linearly
    public static double altitudeFeet(ServerWorld world, PlayerEntity player) {
        int sea = world.getSeaLevel();             // usually ~62
        double y  = player.getY();                 // double precision Y
        double top = 320.0;

        double span = Math.max(1.0, top - sea);    // avoid divide-by-zero
        double normalized = (y - sea) / span;      // <=1 at Y=top
        double h = normalized * 29031.0;           // ft
        // Clamp to a reasonable range of this simple model
        return MathHelper.clamp(h, -2000.0, 100000.0);
    }

    //get theta, ratio between temp and t0
    public static double getTheta(ServerWorld world, PlayerEntity player){
        double h = AltitudeConversion(player.getBlockY());
        if (h <= 36089){
            return 1-6.87535*1e-6*h;
        }
        else if (h <= 65617) {
            return 0.75187;
        }
        else {
            return 0.68246+1.05778*1e-6*h;
        }
    }
    //get delta, ratio between pressure and p0
    public static double getDelta(ServerWorld world, PlayerEntity player){
        double h = AltitudeConversion(player.getBlockY());
        double theta = getTheta(world, player);
        if (h <= 36089){
            return Math.pow(theta, 5.2561);
        }
        else if (h <= 65617) {
            return 0.22336*Math.exp((36089-h)/(20806.7));
        }
        else {
            return 3.17176*1e-6*Math.pow(theta, -34.1632);
        }
    }
    //get sigma, ratio between density and rho0
    public static double getSigma(ServerWorld world, PlayerEntity player){
        double h = AltitudeConversion(player.getBlockY());
        double theta = getTheta(world, player);
        double delta = getDelta(world, player);
        return delta/theta;
    }


    public static double getAirDensity(ServerWorld world, PlayerEntity player) {
        double sigma = getSigma(world, player);
        return sigma*rho0;
    }

    public static double getPressure(ServerWorld world, PlayerEntity player) {
        double delta = getDelta(world, player);
        return delta*P0;
    }

    public static double getTemperature(ServerWorld world, PlayerEntity player) {
        double theta = getTheta(world, player);
        return theta*T0;
    }

    public static float computeAtmosphereDamagePerSecond(ServerWorld world, PlayerEntity player) {
        double p   = getPressureLbfPerFt2(world, player);
        double tr  = getTemperatureR(world, player);
        double rho = getDensitySlugPerFt3(world, player);

        double dmg = 0.0;

        if (p < min_pressure) {
            dmg += (min_pressure - p) * PRESSURE_DMG_PER;
        }
        if (tr < min_temp) {
            dmg += (min_temp - tr) * TEMP_DMG_PER;
        }
        if (rho < min_density) {
            dmg += (min_density - rho) * DENS_DMG_PER;
        }

        if (dmg <= 0.0) return 0f;
        return (float) Math.min(dmg, MAX_DPS);
    }
}
