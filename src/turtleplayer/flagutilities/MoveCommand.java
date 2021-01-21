package turtleplayer.flagutilities;

import battlecode.common.MapLocation;

import java.util.Objects;

/*
    implementation of FlagMessage for a flag signalling the robot's status
 */
public strictfp class MoveCommand implements FlagMessage{

    public final EncodedMapLocation encodedMapLocation;
    public final int robotIDPart;

    public MoveCommand(MapLocation mapLocation, int robotID){
        this.encodedMapLocation = new EncodedMapLocation(mapLocation);
        this.robotIDPart = FlagUtilities.getPartOfFlag(robotID,5,0);
    }

    public MoveCommand(int flagCode){
        this.encodedMapLocation = new EncodedMapLocation(
                FlagUtilities.getPartOfFlag(flagCode,6,0),
                FlagUtilities.getPartOfFlag(flagCode,13,7));
        this.robotIDPart = FlagUtilities.getPartOfFlag(flagCode,19,14);
    }

    public int getFlagCode(){
        return FlagUtilities.getFlag(
                new int[]{
                        this.encodedMapLocation.x,
                        this.encodedMapLocation.y,
                        this.robotIDPart,
                        0x0A
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
        return robotIDPart == that.robotIDPart && Objects.equals(encodedMapLocation, that.encodedMapLocation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(encodedMapLocation, robotIDPart);
    }

    @Override
    public String toString() {
        return "MoveCommand{" +
                "encodedMapLocation=" + encodedMapLocation +
                ", robotIDPart=" + robotIDPart +
                '}';
    }
}
