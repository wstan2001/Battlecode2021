package turtleplayer.flagutilities;

import battlecode.common.RobotType;

import java.util.Objects;

import static turtleplayer.Utilities.LOG_BASE;
import static turtleplayer.Utilities.bound;

/*
    implementation of FlagMessage for a flag signalling the first part of an ID
 */
public strictfp class IDMessage implements FlagMessage{

    public final int processedRobotID;
    public final RobotType robotType;
    public final int scaledConviction;


    public IDMessage(int flagCode){
        this.processedRobotID = FlagUtilities.getPartOfFlag(flagCode,13,0);
        this.robotType = RobotType.values()[FlagUtilities.getPartOfFlag(flagCode,15,14)];
        this.scaledConviction = FlagUtilities.getPartOfFlag(flagCode,19,16);
    }

    public IDMessage(int robotID, RobotType robotType, int conviction){
        this.processedRobotID = (robotID- 10000) & 0x03FFF;
        this.robotType = robotType;
        this.scaledConviction = bound((int)(Math.log((double)conviction)/LOG_BASE),0,15);
    }

    public int getFlagCode(){
        return FlagUtilities.getFlag(
                new int[]{
                        this.processedRobotID,
                        this.robotType.ordinal(),
                        this.scaledConviction,
                        0x0B
                }, new int[]{
                        14, 2, 3, 4
                }
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IDMessage idMessage = (IDMessage) o;
        return processedRobotID == idMessage.processedRobotID && scaledConviction == idMessage.scaledConviction && robotType == idMessage.robotType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(processedRobotID, robotType, scaledConviction);
    }

    @Override
    public String toString() {
        return "IDMessage{" +
                "robotIDPart=" + processedRobotID +
                ", robotType=" + robotType +
                ", scaledConviction=" + scaledConviction +
                '}';
    }
}
