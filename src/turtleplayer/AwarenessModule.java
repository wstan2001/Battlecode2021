package turtleplayer;

import battlecode.common.*;
import com.sun.istack.internal.Nullable;
import turtleplayer.flagutilities.*;

import java.util.*;
import java.util.function.Function;

public strictfp class AwarenessModule {

    private final Set<IncompleteRobotInfo> unknownIDRobotInfo; // (all are guaranteed to be friendly)
    private final Map<Integer, IncompleteRobotInfo> knownIDRobotInfo;
    private final Map<MapLocation, PassabilityKnowledge> mapKnowledge;
    private final Map<Integer, IDMessagePartOne> firstIDMessageParts;

    private MoveCommand lastMoveCommand;

    private Optional<MoveCommand> getLastMoveCommand(){
        return Optional.ofNullable(lastMoveCommand);
    }

    public void removeLastMoveCommand(){
        lastMoveCommand = null;
    }

    private final static Direction[] CARDINAL_DIRECTIONS =
            new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
    private final Map<Direction, Integer> coordinateBound;

    public Optional<Integer> getBound(Direction direction){
        return Optional.ofNullable(coordinateBound.get(direction));
    }
    private void setBound(Direction direction, int bound){
        coordinateBound.put(direction, bound);
    }

    AwarenessModule(){
        unknownIDRobotInfo = new HashSet<>();
        knownIDRobotInfo = new HashMap<>();
        mapKnowledge = new HashMap<>();
        firstIDMessageParts = new HashMap<>();
        coordinateBound = new HashMap<>();
        for (Direction direction : CARDINAL_DIRECTIONS) {
            coordinateBound.put(direction,null);
        }
    }

    public void update(RobotController rc, boolean turnHasPassed, int roundNum, RobotType type,
                MapLocation location, Team team) throws GameActionException{
        RobotInfo[] robotInfos = rc.senseNearbyRobots();
        Set<Integer> sensedRobotIDs = updateSensedRobots(robotInfos, type, roundNum);
        updateBounds(type, location, rc);
        updateSensedTerrain(type, location, rc);
        if(turnHasPassed) {
            for (Integer robotID : knownIDRobotInfo.keySet()) {
                IncompleteRobotInfo flagRobotInfo = knownIDRobotInfo.get(robotID);
                if (rc.canGetFlag(robotID) && flagRobotInfo.getTeam().equals(team)) {
                    processFlagMessage(FlagUtilities.decodeFlag(rc.getFlag(robotID)), robotID, roundNum, flagRobotInfo, rc, sensedRobotIDs);
                }
            }
        }
    }

    private static final double POSSIBILITY_THRESHOLD = 0.9;
    private void processNewIncompleteRobotInfo(IncompleteRobotInfo robotInfo, Set<Integer> sensedRobotIDs){
        double bestConfidence = POSSIBILITY_THRESHOLD;
        IncompleteRobotInfo bestConfidenceRobotInfo = null;
        for(IncompleteRobotInfo incompleteRobotInfo : unknownIDRobotInfo){
            double newConfidence = incompleteRobotInfo.isRobotConfidence(robotInfo);
            if(newConfidence > bestConfidence){
                bestConfidence = newConfidence;
                bestConfidenceRobotInfo = incompleteRobotInfo;
            }
        }
        for(Integer robotId : knownIDRobotInfo.keySet() ){
            if(!sensedRobotIDs.contains(robotId)) {
                IncompleteRobotInfo incompleteRobotInfo = knownIDRobotInfo.get(robotId);
                double newConfidence = incompleteRobotInfo.isRobotConfidence(robotInfo);
                if (newConfidence > bestConfidence) {
                    bestConfidence = newConfidence;
                    bestConfidenceRobotInfo = incompleteRobotInfo;
                }
            }
        }
        if(bestConfidenceRobotInfo != null){
            bestConfidenceRobotInfo.update(robotInfo);
        }else{
            unknownIDRobotInfo.add(robotInfo);
        }
    }

    private void processFlagMessage(FlagMessage flagMessage, int robotID, int roundNum,
                            IncompleteRobotInfo sender, RobotController rc, Set<Integer> sensedRobotIDs){
        if(flagMessage instanceof IDMessagePartOne){
            firstIDMessageParts.put(robotID, (IDMessagePartOne) flagMessage);
        }else if(flagMessage instanceof  IDMessagePartTwo){
            if(firstIDMessageParts.containsKey(robotID)){
                IncompleteRobotInfo robotInfo = new IncompleteRobotInfo(
                        firstIDMessageParts.get(robotID),(IDMessagePartTwo) flagMessage, roundNum);
                processNewIncompleteRobotInfo(robotInfo, sensedRobotIDs);
            }
        }else if(flagMessage instanceof UnitPresent){
            IncompleteRobotInfo robotInfo = new IncompleteRobotInfo(
                    (UnitPresent) flagMessage, sender, roundNum);
            processNewIncompleteRobotInfo(robotInfo, sensedRobotIDs);
        }else if(flagMessage instanceof MoveCommand){
            lastMoveCommand = (MoveCommand) flagMessage;
        }else if(flagMessage instanceof SelfStatus){
            if(!rc.canSenseRobot(robotID)){
                sender.update((SelfStatus) flagMessage);
            }
        }else if(flagMessage instanceof TerrainMapping){
            if(sender.getLastSeenMapLocation().isPresent()) {
                TerrainMapping terrainMapping = (TerrainMapping) flagMessage;
                MapLocation lastLocation = sender.getLastSeenMapLocation().get();
                Map<RelativeLocation, Boolean> turnBits = terrainMapping.getTurnBits();
                for (RelativeLocation relativeLocation : turnBits.keySet()) {
                    MapLocation newLocation = relativeLocation.applyTo(lastLocation);
                    if(!mapKnowledge.containsKey(newLocation)) {
                        mapKnowledge.put(newLocation, new PassabilityKnowledge());
                    }
                    mapKnowledge.get(newLocation).updateWithFlag(
                            terrainMapping.getBitNumber(), turnBits.get(relativeLocation));
                }
            }
        }
    }

    private Set<Integer> updateSensedRobots(RobotInfo[] robotInfos, RobotType type, int roundNum){
        Set<Integer> sensedRobotIDs = new HashSet<>();
        for(RobotInfo robotInfo : robotInfos){
            int robotId = robotInfo.getID();
            sensedRobotIDs.add(robotId);
            IncompleteRobotInfo incompleteRobotInfo = new IncompleteRobotInfo(robotInfo,type.canTrueSense(),roundNum);
            if(knownIDRobotInfo.containsKey(robotId)){
                knownIDRobotInfo.get(robotId).update(incompleteRobotInfo);
            }else{
                double bestConfidence = POSSIBILITY_THRESHOLD;
                IncompleteRobotInfo bestConfidenceRobotInfo = null;
                for(IncompleteRobotInfo otherRobotInfo : unknownIDRobotInfo){
                    double newConfidence = otherRobotInfo.isRobotConfidence(incompleteRobotInfo);
                    if(newConfidence > bestConfidence){
                        bestConfidence = newConfidence;
                        bestConfidenceRobotInfo = otherRobotInfo;
                    }
                }
                if(bestConfidenceRobotInfo != null){
                    unknownIDRobotInfo.remove(bestConfidenceRobotInfo);
                    bestConfidenceRobotInfo.update(incompleteRobotInfo);
                    knownIDRobotInfo.put(robotId,bestConfidenceRobotInfo);
                }else {
                    knownIDRobotInfo.put(robotInfo.ID,
                            new IncompleteRobotInfo(robotInfo, type.canTrueSense(), roundNum));
                }
            }
        }
        return sensedRobotIDs;
    }

    private void updateBounds(RobotType type, MapLocation robotMapLocation, RobotController rc){
        int detectionRange = (int)(Math.floor(Math.sqrt(type.detectionRadiusSquared))+0.5);
        for(Direction direction : CARDINAL_DIRECTIONS){
            if(!getBound(direction).isPresent()) {
                MapLocation mapLocation = robotMapLocation.add(direction);
                for (int i = 1; i <= detectionRange; i++, mapLocation = mapLocation.add(direction)) {
                    if(!rc.canDetectLocation(mapLocation)){
                        MapLocation prevLocation = mapLocation.subtract(direction);
                        int bound = (direction.dx == 0) ? prevLocation.y : prevLocation.x;
                        setBound(direction, bound);
                        break;
                    }
                }
            }
        }
    }

    private void updateSensedTerrain(RobotType type, MapLocation location, RobotController rc) throws GameActionException{
        for(RelativeLocation relativeLocation:
                Utilities.getRelativeLocationsWithin(type.sensorRadiusSquared)){
            MapLocation mapLocation = relativeLocation.applyTo(location);
            if(rc.canSenseLocation(mapLocation)){
                if(!mapKnowledge.containsKey(mapLocation)){
                    mapKnowledge.put(mapLocation, new PassabilityKnowledge());
                }
                mapKnowledge.get(mapLocation).updateWithSensed(rc.sensePassability(mapLocation));
            }
        }
    }
}
