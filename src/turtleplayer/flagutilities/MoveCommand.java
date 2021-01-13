package turtleplayer.flagutilities;

import battlecode.common.RobotType;

import java.util.Objects;

/*
    implementation of FlagMessage for a flag signalling the robot's status
 */
public strictfp class MoveCommand implements FlagMessage{

    private final RelativeLocation relativeLocation;
    private final int robotIDPart;

    public MoveCommand(RelativeLocation relativeLocation, int robotID){
        this.relativeLocation = relativeLocation;
        this.robotIDPart = FlagUtilities.getPartOfFlag(robotID,5,0);
    }

    private final static int PREFIX_BIT_MASK = 0x00F00000;
    private final static int PREFIX_CORRECT = 0x00700000;

    public static boolean hasCorrectPrefix(int flagCode){
        return (flagCode & PREFIX_BIT_MASK) == PREFIX_CORRECT;
    }

    public MoveCommand(int flagCode){
        this.relativeLocation = new RelativeLocation(
                FlagUtilities.getPartOfFlag(flagCode,6,0)-64,
                FlagUtilities.getPartOfFlag(flagCode,13,7)-64);
        this.robotIDPart = FlagUtilities.getPartOfFlag(flagCode,19,14);
    }
    public RelativeLocation getRelativeLocation() {
        return relativeLocation;
    }

    public int getRobotIDPart() {
        return robotIDPart;
    }

    public int getFlagCode(){
        return FlagUtilities.getFlag(
                new int[]{
                        this.relativeLocation.getX()+64,
                        this.relativeLocation.getY()+64,
                        this.robotIDPart,
                        0x07
                }, new int[]{
                        7,7,6,4
                }
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MoveCommand that = (MoveCommand) o;
        return robotIDPart == that.robotIDPart && Objects.equals(relativeLocation, that.relativeLocation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(relativeLocation, robotIDPart);
    }

    @Override
    public String toString() {
        return "MoveCommand{" +
                "relativeLocation=" + relativeLocation +
                ", robotIDPart=" + robotIDPart +
                '}';
    }
}
