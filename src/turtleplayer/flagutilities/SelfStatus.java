package turtleplayer.flagutilities;

import battlecode.common.RobotType;

import java.util.Objects;

/*
    implementation of FlagMessage for a flag signalling the robot's status
 */
public strictfp class SelfStatus implements FlagMessage{

    private final RelativeLocation relativeLocation;
    private final int scaledConviction;

    private static final int SCALE_FACTOR = 16;

    public SelfStatus(RelativeLocation relativeLocation, int conviction){
        this.relativeLocation = relativeLocation;
        this.scaledConviction = conviction / SCALE_FACTOR;
    }

    private final static int PREFIX_BIT_MASK = 0x00F00000;
    private final static int PREFIX_CORRECT = 0x00900000;

    public static boolean hasCorrectPrefix(int flagCode){
        return (flagCode & PREFIX_BIT_MASK) == PREFIX_CORRECT;
    }

    public SelfStatus(int flagCode){
        this.relativeLocation = new RelativeLocation(
                FlagUtilities.getPartOfFlag(flagCode,6,0)-64,
                FlagUtilities.getPartOfFlag(flagCode,13,7)-64);
        this.scaledConviction = FlagUtilities.getPartOfFlag(flagCode,19,14);
    }
    public RelativeLocation getRelativeLocation() {
        return relativeLocation;
    }
    public int getConviction(){
        return scaledConviction*SCALE_FACTOR;
    }

    public int getFlagCode(){
        return FlagUtilities.getFlag(
                new int[]{
                        this.relativeLocation.getX()+64,
                        this.relativeLocation.getY()+64,
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
        return scaledConviction == that.scaledConviction && Objects.equals(relativeLocation, that.relativeLocation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(relativeLocation, scaledConviction);
    }

    @Override
    public String toString() {
        return "SelfStatus{" +
                "relativeLocation=" + relativeLocation +
                ", scaledConviction=" + scaledConviction +
                '}';
    }
}
