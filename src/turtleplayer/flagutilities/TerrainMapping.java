package turtleplayer.flagutilities;

import battlecode.common.Direction;

import java.util.Arrays;
import java.util.Objects;

import static turtleplayer.Utilities.bound;

/*
    implementation of FlagMessage for a flag signalling the robot's status
 */
public strictfp class TerrainMapping implements FlagMessage{
    public final int[] estimatedImpassabilityBits;

    /*
     * Fill with last reported location
     */
    public TerrainMapping(double[] passabilities){
        estimatedImpassabilityBits = new int[]{
                bound((int) (((1.0 / passabilities[0]) - 1.0) * 32.0 / 9.0),0,31),
                bound((int) (((1.0 / passabilities[1]) - 1.0) * 32.0 / 9.0),0,31),
                bound((int) (((1.0 / passabilities[2]) - 1.0) * 32.0 / 9.0),0,31),
                bound((int) (((1.0 / passabilities[3]) - 1.0) * 32.0 / 9.0),0,31)
        };
    }

    public TerrainMapping(int flagCode){
        estimatedImpassabilityBits = new int[4];
        estimatedImpassabilityBits[0] = FlagUtilities.getPartOfFlag(flagCode,0,4);
        estimatedImpassabilityBits[1] = FlagUtilities.getPartOfFlag(flagCode,5,9);
        estimatedImpassabilityBits[2] = FlagUtilities.getPartOfFlag(flagCode,10,14);
        estimatedImpassabilityBits[3] = FlagUtilities.getPartOfFlag(flagCode,15,19);
    }

    public int getFlagCode(){
        return FlagUtilities.getFlag(
                new int[]{
                        estimatedImpassabilityBits[0],
                        estimatedImpassabilityBits[1],
                        estimatedImpassabilityBits[2],
                        estimatedImpassabilityBits[3],
                        0x08
                }, new int[]{
                        5,5,5,5,4
                }
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TerrainMapping that = (TerrainMapping) o;
        return Arrays.equals(estimatedImpassabilityBits, that.estimatedImpassabilityBits);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(estimatedImpassabilityBits);
    }

    @Override
    public String toString() {
        return "TerrainMapping{" +
                "estimatedImpassabilityBits=" + Arrays.toString(estimatedImpassabilityBits) +
                '}';
    }
}
