package turtleplayer.flagutilities;

import battlecode.common.RobotType;

import java.util.Objects;

/*
    implementation of FlagMessage for a flag signalling the first part of an ID
 */
public strictfp class IDMessagePartOne implements FlagMessage{

    private final int robotIDPart;

    private final static int ID_PART_BIT_MASK = 0x000FFFFF;
    public IDMessagePartOne(int robotIDOrFlagCode){
        this.robotIDPart = robotIDOrFlagCode & ID_PART_BIT_MASK;
    }

    private final static int PREFIX_BIT_MASK = 0x00F00000;
    private final static int PREFIX_CORRECT = 0x00C00000;
    public static boolean hasCorrectPrefix(int flagCode){
        return (flagCode & PREFIX_BIT_MASK) == PREFIX_CORRECT;
    }

    public int getRobotIDPart() {
        return robotIDPart;
    }

    public int getFlagCode(){
        return FlagUtilities.getFlag(
                new int[]{
                        this.robotIDPart,
                        0x0C
                }, new int[]{
                        20, 4
                }
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IDMessagePartOne that = (IDMessagePartOne) o;
        return robotIDPart == that.robotIDPart;
    }

    @Override
    public int hashCode() {
        return Objects.hash(robotIDPart);
    }

    @Override
    public String toString() {
        return "IDMessagePartOne{" +
                "robotIDPart=" + robotIDPart +
                '}';
    }
}
