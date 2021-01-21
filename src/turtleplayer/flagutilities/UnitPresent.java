package turtleplayer.flagutilities;

import battlecode.common.MapLocation;
import battlecode.common.RobotType;
import battlecode.common.Team;

import java.util.Objects;

import static turtleplayer.Utilities.*;

/*
    implementation of FlagMessage for a flag signalling that there is an enemy nearby
 */
public strictfp class UnitPresent implements FlagMessage{

    public final EncodedMapLocation encodedMapLocation;
    public final int scaledConviction;
    public final RobotType robotType;
    public final boolean trueSense;
    public final Team team;
    private final int code;

    public UnitPresent(MapLocation mapLocation, int conviction, RobotType robotType, boolean trueSense, Team team){
        this.encodedMapLocation = new EncodedMapLocation(mapLocation);
        this.scaledConviction = bound((int)(Math.log((double)conviction)/LOG_BASE),0,15);
        this.robotType = robotType;
        this.trueSense = trueSense;
        this.team = team;
        if(robotType == RobotType.POLITICIAN && trueSense){
            code = team == Team.A ? 9 : 10;
        }else if (robotType == RobotType.ENLIGHTENMENT_CENTER){
            code = team.ordinal();
        }else{
            int baseCode = team == Team.A? 3 : 6;
            switch(robotType){
                case POLITICIAN: code = baseCode; break;
                case SLANDERER: code = baseCode+1; break;
                default: code = baseCode+2; break;
            }
        }
    }

    private final static int PREFIX_BIT_MASK = 0x00C00000;
    private final static int PREFIX_CORRECT = 0x00C00000;

    public UnitPresent(int flagCode){
        this.encodedMapLocation = new EncodedMapLocation(
                FlagUtilities.getPartOfFlag(flagCode,6,0),
                FlagUtilities.getPartOfFlag(flagCode,13,7));
        this.scaledConviction = FlagUtilities.getPartOfFlag(flagCode,17,14);
        code = FlagUtilities.getPartOfFlag(flagCode,21,18);
        if(code < 3){
            this.trueSense = false;
            this.robotType = RobotType.ENLIGHTENMENT_CENTER;
            this.team = Team.values()[code];
        }else if (code < 9){
            this.trueSense = false;
            switch(code % 3){
                case 0: this.robotType = RobotType.POLITICIAN; break;
                case 1: this.robotType = RobotType.SLANDERER; break;
                default: this.robotType = RobotType.MUCKRAKER; break;
            }
            if (code / 3 == 1) {
                this.team = Team.A;
            } else {
                this.team = Team.B;
            }
        }else{
            this.trueSense = true;
            this.robotType = RobotType.POLITICIAN;
            this.team = code == 9 ? Team.A : Team.B;
        }
    }

    public int getFlagCode(){
        return FlagUtilities.getFlag(
                new int[]{
                        this.encodedMapLocation.x,
                        this.encodedMapLocation.y,
                        this.scaledConviction,
                        this.code,
                        0x03
                }, new int[]{
                        7,7,4,4,2
                }
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UnitPresent that = (UnitPresent) o;
        return scaledConviction == that.scaledConviction
                && trueSense == that.trueSense
                && code == that.code
                && Objects.equals(encodedMapLocation, that.encodedMapLocation)
                && robotType == that.robotType
                && team == that.team;
    }

    @Override
    public int hashCode() {
        return Objects.hash(encodedMapLocation, scaledConviction, robotType, trueSense, team, code);
    }

    @Override
    public String toString() {
        return "UnitPresent{" +
                "encodedMapLocation=" + encodedMapLocation +
                ", scaledConviction=" + scaledConviction +
                ", robotType=" + robotType +
                ", trueSense=" + trueSense +
                ", team=" + team +
                ", code=" + code +
                '}';
    }
}
