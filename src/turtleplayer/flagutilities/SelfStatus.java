package turtleplayer.flagutilities;

import battlecode.common.MapLocation;

import java.util.Objects;

import static turtleplayer.Utilities.LOG_BASE;
import static turtleplayer.Utilities.bound;

/*
    implementation of FlagMessage for a flag signalling the robot's status
 */
public strictfp class SelfStatus implements FlagMessage{

    public final EncodedMapLocation encodedMapLocation;
    public final int scaledConviction;

    private static final int SCALE_FACTOR = 16;

    public SelfStatus(MapLocation mapLocation, int conviction){
        this.encodedMapLocation = new EncodedMapLocation(mapLocation);
        this.scaledConviction = bound((int)(Math.log((double)conviction)/LOG_BASE),0,15);
    }

    public SelfStatus(int flagCode){
        this.encodedMapLocation = new EncodedMapLocation(
                FlagUtilities.getPartOfFlag(flagCode,6,0),
                FlagUtilities.getPartOfFlag(flagCode,13,7));
        this.scaledConviction = FlagUtilities.getPartOfFlag(flagCode,19,14);
    }

    public int getFlagCode(){
        return FlagUtilities.getFlag(
                new int[]{
                        this.encodedMapLocation.x,
                        this.encodedMapLocation.y,
                        this.scaledConviction,
                        0x09
                }, new int[]{
                        7,7,6,4
                }
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SelfStatus that = (SelfStatus) o;
        return scaledConviction == that.scaledConviction && Objects.equals(encodedMapLocation, that.encodedMapLocation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(encodedMapLocation, scaledConviction);
    }

    @Override
    public String
    toString() {
        return "SelfStatus{" +
                "encodedMapLocation=" + encodedMapLocation +
                ", scaledConviction=" + scaledConviction +
                '}';
    }
}
