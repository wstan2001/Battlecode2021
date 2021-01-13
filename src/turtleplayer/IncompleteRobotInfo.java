package turtleplayer;

import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;
import com.sun.istack.internal.Nullable;
import turtleplayer.flagutilities.IDMessagePartOne;
import turtleplayer.flagutilities.IDMessagePartTwo;
import turtleplayer.flagutilities.SelfStatus;
import turtleplayer.flagutilities.UnitPresent;

import java.util.Optional;

public class IncompleteRobotInfo{

    private boolean couldBeSlanderer;
    private RobotType robotType;
    private final Team team;

    @Nullable
    private Integer robotId;
    @Nullable
    private MapLocation lastSeenMapLocation;
    @Nullable
    private Integer lastSeenRoundNumber;
    @Nullable
    private Integer lastSeenConviction;

    IncompleteRobotInfo(IDMessagePartOne idMessagePartOne, IDMessagePartTwo idMessagePartTwo, int roundNumber){
        this(idMessagePartTwo.getTeam(),false,idMessagePartTwo.getRobotType(),
                (idMessagePartOne.getRobotIDPart() | (idMessagePartTwo.getRobotIDPart() << 20)),
                null, roundNumber, idMessagePartTwo.getInfluence() );
    }

    IncompleteRobotInfo(UnitPresent unitPresent, IncompleteRobotInfo sender, int roundNumber){
        this(unitPresent.getTeam(), unitPresent.isTrueSense(), unitPresent.getRobotType(), null,
                sender.getLastSeenMapLocation().isPresent()
                        ? unitPresent.getRelativeLocation().applyTo(sender.getLastSeenMapLocation().get())
                        : null
                , roundNumber, unitPresent.getInfluence());
    }

    IncompleteRobotInfo(RobotInfo robotInfo, boolean trueSense, int roundNumber){
        this(robotInfo.getTeam(), trueSense, robotInfo.getType(), robotInfo.getID(), robotInfo.getLocation(),
                roundNumber,robotInfo.getConviction());
    }

    IncompleteRobotInfo(Team team, boolean trueSense, RobotType robotType, Integer robotId,
                        MapLocation mapLocation, int roundNumber, Integer conviction){
        this.team = team;
        this.couldBeSlanderer = (!trueSense) && (robotType == RobotType.POLITICIAN);
        this.robotType = robotType;
        this.robotId = robotId;
        this.lastSeenMapLocation = mapLocation;
        this.lastSeenRoundNumber = roundNumber;
        this.lastSeenConviction = conviction;
    }

    public Optional<Integer> getRobotId(){
        return Optional.ofNullable(robotId);
    }
    public Optional<MapLocation> getLastSeenMapLocation(){
        return Optional.ofNullable(lastSeenMapLocation);
    }
    public Optional<Integer> getLastSeenRoundNumber(){
        return Optional.ofNullable(lastSeenRoundNumber);
    }
    public Optional<Integer> getLastSeenConviction(){
        return Optional.ofNullable(lastSeenConviction);
    }
    public RobotType getRobotType(){
        return robotType;
    }
    public boolean getCouldBeSlanderer(){
        return couldBeSlanderer;
    }
    public Team getTeam() {
        return team;
    }

    public void setLastSeenConviction(Integer lastSeenConviction) {
        this.lastSeenConviction = lastSeenConviction;
    }
    public void setLastSeenMapLocation(MapLocation lastSeenMapLocation) {
        this.lastSeenMapLocation = lastSeenMapLocation;
    }
    public void setLastSeenRoundNumber(Integer lastSeenRoundNumber) {
        this.lastSeenRoundNumber = lastSeenRoundNumber;
    }
    public void setRobotId(Integer robotId) {
        this.robotId = robotId;
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
        if(r1.getRobotId().isPresent() && r2.getRobotId().isPresent()){
            return r1.getRobotId().get().equals(r2.getRobotId().get());
        }else{
            return true;
        }
    }

    public double isRobotConfidence(IncompleteRobotInfo robotInfo){
        if(!couldBeSameType(this, robotInfo) || !couldBeIDMatch(this, robotInfo)){
            return 0.0;
        }else{
            assert getLastSeenRoundNumber().isPresent();
            assert robotInfo.getLastSeenRoundNumber().isPresent();
            assert getLastSeenMapLocation().isPresent();
            assert robotInfo.getLastSeenMapLocation().isPresent();
            int earlierRoundNum = getLastSeenRoundNumber().get();
            int laterRoundNum = robotInfo.getLastSeenRoundNumber().get();
            MapLocation earlierMapLocation = getLastSeenMapLocation().get();
            MapLocation laterMapLocation = robotInfo.getLastSeenMapLocation().get();
            int roundNumDifference = laterRoundNum - earlierRoundNum;
            assert roundNumDifference >= 0;
            int distance = Math.max(Math.abs(earlierMapLocation.x-laterMapLocation.x),
                    Math.abs(earlierMapLocation.y-laterMapLocation.y));
            return Math.min(1.0, ((double) roundNumDifference)/distance);
        }
    }

    public void update(IncompleteRobotInfo robotInfo){
        if(couldBeSlanderer && robotInfo.robotType == RobotType.SLANDERER){
            this.couldBeSlanderer = false;
            this.robotType = RobotType.SLANDERER;
        }
        if(!getRobotId().isPresent() && robotInfo.getRobotId().isPresent()){
            robotId = robotInfo.getRobotId().get();
        }
        assert robotInfo.getLastSeenRoundNumber().isPresent();
        assert getLastSeenRoundNumber().isPresent();
        int otherRoundNumber = robotInfo.getLastSeenRoundNumber().get();
        if(otherRoundNumber > getLastSeenRoundNumber().get()){
            lastSeenRoundNumber = otherRoundNumber;
            if(robotInfo.getLastSeenMapLocation().isPresent()) {
                lastSeenMapLocation = robotInfo.getLastSeenMapLocation().get();
            }
            if(robotInfo.getLastSeenConviction().isPresent()){
                lastSeenConviction = robotInfo.getLastSeenConviction().get();
            }
        }
    }

    public void update(SelfStatus selfStatus){
        if(getLastSeenMapLocation().isPresent()) {
            this.lastSeenMapLocation = selfStatus.getRelativeLocation().applyTo(getLastSeenMapLocation().get());
        }
        this.lastSeenConviction = selfStatus.getConviction();
    }

}
