package turtleplayer.flagutilities;

import battlecode.common.RobotType;
import battlecode.common.Team;

import java.util.Objects;

/*
    implementation of FlagMessage for a flag signalling the first part of an ID
 */
public strictfp class IDMessagePartTwo implements FlagMessage{

    private final int robotIDPart;
    private final RobotType robotType;
    private final int scaledInfluence;
    private final Team team;

    private final static int SCALED_AMOUNT = 16;

    private final static int ID_PART_BIT_MASK = 0x000FFFFF;

    public IDMessagePartTwo(int flagCode){
        this.robotIDPart = FlagUtilities.getPartOfFlag(flagCode,10,0);
        this.team = Team.values()[FlagUtilities.getPartOfFlag(flagCode,12,11)];
        this.robotType = RobotType.values()[FlagUtilities.getPartOfFlag(flagCode,14,13)];
        this.scaledInfluence = FlagUtilities.getPartOfFlag(flagCode,19,15);
    }

    public IDMessagePartTwo(int robotID, RobotType robotType, int influence, Team team){
        this.robotIDPart = FlagUtilities.getPartOfFlag(robotID,30,20);
        this.team = team;
        this.robotType = robotType;
        this.scaledInfluence = influence/SCALED_AMOUNT;
    }

    private final static int PREFIX_BIT_MASK = 0x00F00000;
    private final static int PREFIX_CORRECT = 0x00A00000;
    public static boolean hasCorrectPrefix(int flagCode){
        return (flagCode & PREFIX_BIT_MASK) == PREFIX_CORRECT;
    }

    public int getRobotIDPart() {
        return robotIDPart;
    }

    public RobotType getRobotType() {
        return robotType;
    }

    public int getInfluence() {
        return scaledInfluence * SCALED_AMOUNT;
    }

    public Team getTeam() {
        return team;
    }

    public int getFlagCode(){
        return FlagUtilities.getFlag(
                new int[]{
                        this.robotIDPart,
                        this.team.ordinal(),
                        this.robotType.ordinal(),
                        this.scaledInfluence,
                        0x0A
                }, new int[]{
                        11, 2, 2, 5, 4
                }
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IDMessagePartTwo that = (IDMessagePartTwo) o;
        return robotIDPart == that.robotIDPart && scaledInfluence == that.scaledInfluence && robotType == that.robotType && team == that.team;
    }

    @Override
    public int hashCode() {
        return Objects.hash(robotIDPart, robotType, scaledInfluence, team);
    }

    @Override
    public String toString() {
        return "IDMessagePartTwo{" +
                "robotIDPart=" + robotIDPart +
                ", robotType=" + robotType +
                ", scaledInfluence=" + scaledInfluence +
                ", team=" + team +
                '}';
    }
}
