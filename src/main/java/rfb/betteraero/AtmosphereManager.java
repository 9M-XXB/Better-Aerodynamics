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
    //Y=320 equals the height of Mt.Everest, which is 8848m or 29031ft, convert linearly
    public static double AltitudeConversion(double y_level){
        double output = y_level / 320 * 29031;
        return output;
    }

    //get theta, ratio between temp and t0
    public static double getTheta(ServerWorld world, PlayerEntity player){
        double h = AltitudeConversion(player.getBlockY());
        if (h <= 36089){
            return 1-6.87535*1e-6*h;
        }
        else if (h > 36089 && h <= 65617) {
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
        else if (h > 36089 && h <= 65617) {
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
        double delta = getTheta(world, player);
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
}
