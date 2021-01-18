package turtleplayer;

import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;
import org.jetbrains.annotations.Nullable;
import turtleplayer.flagutilities.IDMessagePartOne;
import turtleplayer.flagutilities.IDMessagePartTwo;
import turtleplayer.flagutilities.SelfStatus;
import turtleplayer.flagutilities.UnitPresent;

public class IncompleteRobotInfo{

    public boolean couldBeSlanderer;
    public RobotType robotType;
    public final Team team;

    @Nullable
    public Integer robotId;
    @Nullable
    public MapLocation lastSeenMapLocation;
    @Nullable
    public Integer lastSeenRoundNumber;
    @Nullable
    public Integer lastSeenConviction;

    IncompleteRobotInfo(IDMessagePartOne idMessagePartOne, IDMessagePartTwo idMessagePartTwo, int roundNumber){
        this(idMessagePartTwo.getTeam(),false,idMessagePartTwo.getRobotType(),
                (idMessagePartOne.getRobotIDPart() | (idMessagePartTwo.getRobotIDPart() << 20)),
                null, roundNumber, idMessagePartTwo.getInfluence() );
    }

    IncompleteRobotInfo(UnitPresent unitPresent, IncompleteRobotInfo sender, int roundNumber){
        this(unitPresent.getTeam(), unitPresent.isTrueSense(), unitPresent.getRobotType(), null,
                sender.lastSeenMapLocation != null
                        ? unitPresent.getRelativeLocation().applyTo(sender.lastSeenMapLocation)
                        : null
                , roundNumber, unitPresent.getInfluence());
    }

    IncompleteRobotInfo(RobotInfo robotInfo, boolean trueSense, int roundNumber){
        this(robotInfo.getTeam(), trueSense, robotInfo.getType(), robotInfo.getID(), robotInfo.getLocation(),
                roundNumber,robotInfo.getConviction());
    }

    IncompleteRobotInfo(Team team, boolean trueSense, RobotType robotType, @Nullable Integer robotId,
                        @Nullable MapLocation mapLocation, int roundNumber, @Nullable Integer conviction){
        this.team = team;
        this.couldBeSlanderer = (!trueSense) && (robotType == RobotType.POLITICIAN);
        this.robotType = robotType;
        this.robotId = robotId;
        this.lastSeenMapLocation = mapLocation;
        this.lastSeenRoundNumber = roundNumber;
        this.lastSeenConviction = conviction;
    }

    private static boolean couldBeSameType(IncompleteRobotInfo r1, IncompleteRobotInfo r2){
        if(r1.robotType == r2.robotType){
            return true;
        }
        if(r1.robotType == RobotType.SLANDERER && r2.robotType == RobotType.POLITICIAN && r2.couldBeSlanderer){
            return true;
        }else if(r2.robotType == RobotType.SLANDERER && r1.robotType == RobotType.POLITICIAN && r1.couldBeSlanderer){
            return true;
        }
        return false;
    }

    private static boolean couldBeIDMatch(IncompleteRobotInfo r1, IncompleteRobotInfo r2){
        if(r1.robotId != null && r2.robotId != null){
            return r1.robotId.equals(r2.robotId);
        }else{
            return true;
        }
    }

    public double isRobotConfidence(IncompleteRobotInfo robotInfo){
        if(!couldBeSameType(this, robotInfo) || !couldBeIDMatch(this, robotInfo)){
            return 0.0;
        }else if(robotId != null && robotInfo.robotId != null && robotId.intValue() == robotInfo.robotId.intValue()) {
            return 1.0;
        }else{
            assert lastSeenRoundNumber != null;
            assert robotInfo.lastSeenRoundNumber != null;
            assert lastSeenMapLocation != null;
            assert robotInfo.lastSeenMapLocation != null;
            int earlierRoundNum = lastSeenRoundNumber;
            int laterRoundNum = robotInfo.lastSeenRoundNumber;
            MapLocation earlierMapLocation = lastSeenMapLocation;
            MapLocation laterMapLocation = robotInfo.lastSeenMapLocation;
            int roundNumDifference = laterRoundNum - earlierRoundNum;
            assert roundNumDifference >= 0;
            int distance = Math.max(Math.abs(earlierMapLocation.x-laterMapLocation.x),
                    Math.abs(earlierMapLocation.y-laterMapLocation.y));
            if(roundNumDifference > distance){
                return 0.925 - 0.05*((double)distance/roundNumDifference);
            }else{
                return 0;
            }
        }
    }

    public void update(IncompleteRobotInfo robotInfo){
        if(couldBeSlanderer && robotInfo.robotType == RobotType.SLANDERER){
            this.couldBeSlanderer = false;
            this.robotType = RobotType.SLANDERER;
        }
        if(robotId == null && robotInfo.robotId != null){
            robotId = robotInfo.robotId;
        }
        assert robotInfo.lastSeenRoundNumber != null;
        assert lastSeenRoundNumber != null;
        int otherRoundNumber = robotInfo.lastSeenRoundNumber;
        if(otherRoundNumber > lastSeenRoundNumber){
            lastSeenRoundNumber = otherRoundNumber;
            if(robotInfo.lastSeenMapLocation != null) {
                lastSeenMapLocation = robotInfo.lastSeenMapLocation;
            }
            if(robotInfo.lastSeenConviction != null){
                lastSeenConviction = robotInfo.lastSeenConviction;
            }
        }
    }

    public void update(SelfStatus selfStatus){
        if(lastSeenMapLocation != null) {
            this.lastSeenMapLocation = selfStatus.getRelativeLocation().applyTo(lastSeenMapLocation);
        }
        this.lastSeenConviction = selfStatus.getConviction();
    }

}
