package turtleplayer.flagutilities;

import battlecode.common.RobotType;

import java.util.Objects;

/*
    implementation of FlagMessage for a flag signalling that there is a friendly nearby
 */
public strictfp class FriendlyPresent implements FlagMessage{

    private final RelativeLocation relativeLocation;
    private final int influence;
    private final RobotType robotType;
    private final boolean trueSense;

    public FriendlyPresent(RelativeLocation relativeLocation, int influence, RobotType robotType, boolean trueSense){
        this.relativeLocation = relativeLocation;
        this.influence = influence;
        this.robotType = robotType;
        this.trueSense = trueSense;
    }

    private final static int PREFIX_BIT_MASK = 0x00E00000;
    private final static int PREFIX_CORRECT = 0x00E00000;
    public static boolean hasCorrectPrefix(int flagCode){
        return (flagCode & PREFIX_BIT_MASK) == PREFIX_CORRECT;
    }

    public FriendlyPresent(int flagCode){
        this.relativeLocation = new RelativeLocation(
                FlagUtilities.getPartOfFlag(flagCode,3,0)-8,
                FlagUtilities.getPartOfFlag(flagCode,7,4)-8);
        this.trueSense = FlagUtilities.getPartOfFlag(flagCode, 8,8) == 1;
        this.robotType = RobotType.values()[FlagUtilities.getPartOfFlag(flagCode,10,9)];
        this.influence = FlagUtilities.getPartOfFlag(flagCode,20,11);
    }

    public int getInfluence() {
        return influence;
    }
    public RelativeLocation getRelativeLocation() {
        return relativeLocation;
    }
    public RobotType getRobotType() {
        return robotType;
    }
    public boolean isTrueSense() {
        return trueSense;
    }

    public int getFlagCode(){
        return FlagUtilities.getFlag(
                new int[]{
                        this.relativeLocation.getX()+8,
                        this.relativeLocation.getY()+8,
                        this.trueSense? 1:0,
                        this.robotType.ordinal(),
                        this.influence,
                        0x07
                }, new int[]{
                        4, 4, 1, 2, 10, 3
                }
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FriendlyPresent that = (FriendlyPresent) o;
        return influence == that.influence && trueSense == that.trueSense && relativeLocation.equals(that.relativeLocation) && robotType == that.robotType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(relativeLocation, influence, robotType, trueSense);
    }

    @Override
    public String toString() {
        return "FriendlyPresent{" +
                "relativeLocation=" + relativeLocation +
                ", influence=" + influence +
                ", robotType=" + robotType +
                ", trueSense=" + trueSense +
                '}';
    }
}
