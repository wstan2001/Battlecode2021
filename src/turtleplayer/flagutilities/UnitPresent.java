package turtleplayer.flagutilities;

import battlecode.common.RobotType;
import battlecode.common.Team;

import java.util.Objects;

/*
    implementation of FlagMessage for a flag signalling that there is an enemy nearby
 */
public strictfp class UnitPresent implements FlagMessage{

    private final RelativeLocation relativeLocation;
    private final int influence;
    private final RobotType robotType;
    private final boolean trueSense;
    private final Team team;

    public UnitPresent(RelativeLocation relativeLocation, int influence, RobotType robotType, boolean trueSense, Team team){
        this.relativeLocation = relativeLocation;
        this.influence = influence;
        this.robotType = robotType;
        this.trueSense = trueSense;
        this.team = team;
    }

    private final static int PREFIX_BIT_MASK = 0x00C00000;
    private final static int PREFIX_CORRECT = 0x00C00000;
    public static boolean hasCorrectPrefix(int flagCode){
        return (flagCode & PREFIX_BIT_MASK) == PREFIX_CORRECT;
    }

    public UnitPresent(int flagCode){
        this.relativeLocation = new RelativeLocation(
                FlagUtilities.getPartOfFlag(flagCode,3,0)-8,
                FlagUtilities.getPartOfFlag(flagCode,7,4)-8);
        this.trueSense = FlagUtilities.getPartOfFlag(flagCode, 8,8) == 1;
        this.robotType = RobotType.values()[FlagUtilities.getPartOfFlag(flagCode,10,9)];
        this.influence = FlagUtilities.getPartOfFlag(flagCode,19,11);
        this.team = Team.values()[FlagUtilities.getPartOfFlag(flagCode,21,20)];
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

    public Team getTeam() {
        return team;
    }

    public int getFlagCode(){
        return FlagUtilities.getFlag(
                new int[]{
                        this.relativeLocation.getX()+8,
                        this.relativeLocation.getY()+8,
                        this.trueSense? 1:0,
                        this.robotType.ordinal(),
                        this.influence,
                        this.team.ordinal(),
                        0x03
                }, new int[]{
                        4, 4, 1, 2, 9, 2, 2
                }
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UnitPresent that = (UnitPresent) o;
        return influence == that.influence && trueSense == that.trueSense && Objects.equals(relativeLocation, that.relativeLocation) && robotType == that.robotType && team == that.team;
    }

    @Override
    public int hashCode() {
        return Objects.hash(relativeLocation, influence, robotType, trueSense, team);
    }

    @Override
    public String toString() {
        return "UnitPresent{" +
                "relativeLocation=" + relativeLocation +
                ", influence=" + influence +
                ", robotType=" + robotType +
                ", trueSense=" + trueSense +
                ", team=" + team +
                '}';
    }
}
