package turtleplayer;

import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;
import org.jetbrains.annotations.Nullable;
import turtleplayer.flagutilities.*;

public class IncompleteRobotInfo{

    public boolean couldBeSlanderer;
    public RobotType robotType;
    public final Team team;

    public int processedRobotID;
    public int lastSeenRoundNumber;
    public int lastSeenConviction;
    public int lastSeenScaledConviction;

    @Nullable
    public MapLocation lastSeenMapLocation;

    IncompleteRobotInfo(IDMessage idMessage, int roundNumber){
        this(Utilities.ALLY_TEAM, true, idMessage.robotType, idMessage.processedRobotID, null,
                roundNumber, -1, idMessage.scaledConviction);
    }

    IncompleteRobotInfo(UnitPresent unitPresent, IncompleteRobotInfo sender, int roundNumber, MapLocation mapLocation){
        this(unitPresent.team, unitPresent.trueSense, unitPresent.robotType, -1,
                unitPresent.encodedMapLocation.applyTo(mapLocation),
                roundNumber, -1, unitPresent.scaledConviction);
    }

    IncompleteRobotInfo(RobotInfo robotInfo, boolean trueSense, int roundNumber){
        this(robotInfo.getTeam(), trueSense, robotInfo.getType(), robotInfo.getID(), robotInfo.getLocation(),
                roundNumber,robotInfo.getConviction(), -1);
    }

    private IncompleteRobotInfo(Team team, boolean trueSense, RobotType robotType, int processedRobotID,
                        @Nullable MapLocation mapLocation, int roundNumber, int conviction, int scaledConviction){
        this.team = team;
        this.couldBeSlanderer = (!trueSense) && (robotType == RobotType.POLITICIAN);
        this.robotType = robotType;
        this.processedRobotID = processedRobotID;
        this.lastSeenScaledConviction = scaledConviction;
        this.lastSeenMapLocation = mapLocation;
        this.lastSeenRoundNumber = roundNumber;
        this.lastSeenConviction = conviction;
    }

    private static double sameTypeConfidence(IncompleteRobotInfo r1, IncompleteRobotInfo r2){
        if(r1.robotType == r2.robotType){
            return 1.0;
        }
        if(r1.robotType == RobotType.SLANDERER && r2.robotType == RobotType.POLITICIAN && r2.couldBeSlanderer){
            return 0.5;
        }else if(r2.robotType == RobotType.SLANDERER && r1.robotType == RobotType.POLITICIAN && r1.couldBeSlanderer){
            return 0.5;
        }
        return 0.0;
    }

    private static double sameLocationConfidence(IncompleteRobotInfo r1, IncompleteRobotInfo r2){
        int roundNumDifference = r1.lastSeenRoundNumber - r2.lastSeenRoundNumber;
        MapLocation mapLocation1 = r1.lastSeenMapLocation;
        MapLocation mapLocation2 = r2.lastSeenMapLocation;
        if(mapLocation1 == null || mapLocation2 == null){
            return 0.5;
        }
        int distance = Math.max(Math.abs(mapLocation1.x-mapLocation2.x),
                Math.abs(mapLocation1.y-mapLocation2.y));
        return (distance <= roundNumDifference) ? 1-Math.exp(-(double)roundNumDifference/distance) : 0;
    }

    public double isRobotConfidence(IncompleteRobotInfo robotInfo){
        if(processedRobotID != -1 && robotInfo.processedRobotID != -1 &&
                processedRobotID== robotInfo.processedRobotID) {
            return 1.0;
        }else{
            return sameTypeConfidence(this,robotInfo) * sameLocationConfidence(this,robotInfo);
        }
    }

    public void update(IncompleteRobotInfo robotInfo){
        if(couldBeSlanderer && robotInfo.robotType == RobotType.SLANDERER){
            this.couldBeSlanderer = false;
            this.robotType = RobotType.SLANDERER;
        }
        if(processedRobotID == -1 && robotInfo.processedRobotID != -1){
            processedRobotID = robotInfo.processedRobotID;
        }
        int otherRoundNumber = robotInfo.lastSeenRoundNumber;
        if(otherRoundNumber > lastSeenRoundNumber){
            lastSeenRoundNumber = otherRoundNumber;
            if(robotInfo.lastSeenMapLocation != null) {
                lastSeenMapLocation = robotInfo.lastSeenMapLocation;
            }
            if(robotInfo.lastSeenConviction != -1){
                lastSeenConviction = robotInfo.lastSeenConviction;
            }
            if(robotInfo.lastSeenScaledConviction != -1){
                lastSeenScaledConviction = -1;
            }
        }
    }

    public void update(SelfStatus selfStatus, MapLocation mapLocation){
        if(lastSeenMapLocation != null) {
            this.lastSeenMapLocation = selfStatus.encodedMapLocation.applyTo(lastSeenMapLocation);
        }
        this.lastSeenScaledConviction = selfStatus.scaledConviction;
    }

}
