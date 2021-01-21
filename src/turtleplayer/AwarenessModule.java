package turtleplayer;

import battlecode.common.*;
import org.jetbrains.annotations.Nullable;
import turtleplayer.flagutilities.*;

public strictfp class AwarenessModule {

    private final static int NUM_ROBOTS_IN_ARRAY = 20;
    private final static int NUM_MAX_ECS = 10;

    public IncompleteRobotInfo[] politicianMaybeSlandererRobotInfo = new IncompleteRobotInfo[NUM_ROBOTS_IN_ARRAY];
    public IncompleteRobotInfo[][] incompleteRobotInfo = new IncompleteRobotInfo[4][NUM_ROBOTS_IN_ARRAY];
    public int[] numIncompleteRobotInfo = new int[4];
    public int numPoliticianMaybeSlandererRobotInfos = 0;

    public double[][] mapKnowledge = new double[64][64];

    public int[] friendECIDs = new int[NUM_MAX_ECS];

    @Nullable
    public MoveCommand lastMoveCommand;

    public void removeLastMoveCommand(){
        lastMoveCommand = null;
    }

    public final static Direction[] CARDINAL_DIRECTIONS =
            new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
    public final int[] coordinateBound = new int[4];

    AwarenessModule(){
        for (int i = 0; i < 4; i++) {
            coordinateBound[i] = -1;
        }
        for (int i = 0; i < NUM_MAX_ECS; i++) {
            friendECIDs[i] = -1;
        }
    }

    public void update(RobotController rc, boolean turnHasPassed, int roundNum, RobotType type,
                MapLocation location) throws GameActionException{
        RobotInfo[] robotInfos = rc.senseNearbyRobots();
        System.out.println("UPDATE1: "+Clock.getBytecodeNum());
        IncompleteRobotInfo[] sensedRobotInfos = updateSensedRobots(robotInfos, type, roundNum);
        System.out.println("UPDATE2: "+Clock.getBytecodeNum());
        updateBounds(type, location, rc);
        System.out.println("UPDATE3: "+Clock.getBytecodeNum());
        updateSensedTerrain(type, location, rc);
        System.out.println("UPDATE4: "+Clock.getBytecodeNum());
        if(turnHasPassed) {
            for (IncompleteRobotInfo sender : sensedRobotInfos) {
                if(sender != null) {
                    if (sender.processedRobotID != -1 && rc.canGetFlag(sender.processedRobotID+10000) && sender.team == Utilities.ALLY_TEAM) {
                        FlagMessage flagMessage = FlagUtilities.decodeFlag(rc.getFlag(sender.processedRobotID+10000));
                        if (flagMessage instanceof IDMessage) {
                            IDMessage idMessage = (IDMessage) flagMessage;
                        } else {
                            processFlagMessage(flagMessage, sender.processedRobotID, roundNum, sender, rc, type, location);
                        }
                    }
                }
            }
        }
    }

//    private static final double POSSIBILITY_THRESHOLD = 0.9;

    private IncompleteRobotInfo processNewIncompleteRobotInfo(IncompleteRobotInfo robotInfo){
        return robotInfo;
//        if(robotInfo.couldBeSlanderer){
//            double bestConfidence = POSSIBILITY_THRESHOLD;
//            int bestConfidenceRobotInfoIndex = -1;
//            int bestIndx = -1;
//            int max = numPoliticianMaybeSlandererRobotInfos;
//            for (int i = 0; i < max; i++) {
//                double confidence = politicianMaybeSlandererRobotInfo[i].isRobotConfidence(robotInfo);
//                if (confidence > bestConfidence) {
//                    bestConfidence = confidence;
//                    bestConfidenceRobotInfoIndex = i;
//                    bestIndx = -1;
//                }
//            }
//            int indx = RobotType.SLANDERER.ordinal();
//            max = numIncompleteRobotInfo[indx];
//            for (int i = 0; i < max; i++) {
//                double confidence = incompleteRobotInfo[indx][i].isRobotConfidence(robotInfo);
//                if (confidence > bestConfidence) {
//                    bestConfidence = confidence;
//                    bestConfidenceRobotInfoIndex = i;
//                    bestIndx = indx;
//                }
//            }
//            indx = RobotType.POLITICIAN.ordinal();
//            max = numIncompleteRobotInfo[indx];
//            for (int i = 0; i < max; i++) {
//                double confidence = incompleteRobotInfo[indx][i].isRobotConfidence(robotInfo);
//                if (confidence > bestConfidence) {
//                    bestConfidence = confidence;
//                    bestConfidenceRobotInfoIndex = i;
//                    bestIndx = indx;
//                }
//            }
//            if (bestConfidenceRobotInfoIndex > -1) {
//                if(bestIndx > -1){
//                    incompleteRobotInfo[bestIndx][bestConfidenceRobotInfoIndex].update(robotInfo);
//                    return incompleteRobotInfo[bestIndx][bestConfidenceRobotInfoIndex];
//                }else {
//                    politicianMaybeSlandererRobotInfo[bestConfidenceRobotInfoIndex].update(robotInfo);
//                    return politicianMaybeSlandererRobotInfo[bestConfidenceRobotInfoIndex];
//                }
//            } else if (numPoliticianMaybeSlandererRobotInfos < 20) {
//                politicianMaybeSlandererRobotInfo[numPoliticianMaybeSlandererRobotInfos++] = robotInfo;
//                return robotInfo;
//            }
//            return robotInfo;
//        }else {
//            int indx = robotInfo.robotType.ordinal();
//            double bestConfidence = POSSIBILITY_THRESHOLD;
//            int bestConfidenceRobotInfoIndex = -1;
//            int max = numIncompleteRobotInfo[indx];
//            for (int i = 0; i < max; i++) {
//                double confidence = incompleteRobotInfo[indx][i].isRobotConfidence(robotInfo);
//                if (confidence > bestConfidence) {
//                    bestConfidence = confidence;
//                    bestConfidenceRobotInfoIndex = i;
//                }
//            }
//            if (bestConfidenceRobotInfoIndex > -1) {
//                incompleteRobotInfo[indx][bestConfidenceRobotInfoIndex].update(robotInfo);
//                return incompleteRobotInfo[indx][bestConfidenceRobotInfoIndex];
//            } else if (max < 20) {
//                incompleteRobotInfo[indx][max] = robotInfo;
//                numIncompleteRobotInfo[indx] = max + 1;
//                return robotInfo;
//            }
//            return robotInfo;
//        }
    }

    private void processFlagMessage(FlagMessage flagMessage, int robotID, int roundNum,
                            IncompleteRobotInfo sender, RobotController rc, RobotType type, MapLocation mapLocation){
        if(flagMessage instanceof UnitPresent){
            IncompleteRobotInfo robotInfo = new IncompleteRobotInfo(
                    (UnitPresent) flagMessage, sender, roundNum, mapLocation);
            processNewIncompleteRobotInfo(robotInfo);
        }else if(flagMessage instanceof MoveCommand){
            lastMoveCommand = (MoveCommand) flagMessage;
        }else if(flagMessage instanceof SelfStatus){
            sender.update((SelfStatus) flagMessage, mapLocation);
//        }else if(flagMessage instanceof TerrainMapping){
//            if(sender.lastReportedMapLocation != null && type == RobotType.ENLIGHTENMENT_CENTER) {
//                TerrainMapping terrainMapping = (TerrainMapping) flagMessage;
//                MapLocation lastLocation = sender.lastReportedMapLocation;
//                int xmod = (lastLocation.x % 64) & 0xFFFFFFFE;
//                int ymod = (lastLocation.y % 64) & 0xFFFFFFFE;
//                if(mapKnowledge[xmod][ymod] < 0.5)
//                    mapKnowledge[xmod][ymod] = ((terrainMapping.estimatedImpassabilityBits[0]+0.5) * 9.0/32.0)+1.0;
//                if(mapKnowledge[xmod+1][ymod] < 0.5)
//                    mapKnowledge[xmod+1][ymod] = ((terrainMapping.estimatedImpassabilityBits[1]+0.5) * 9.0/32.0)+1.0;
//                if(mapKnowledge[xmod][ymod+1] < 0.5)
//                    mapKnowledge[xmod][ymod+1] = ((terrainMapping.estimatedImpassabilityBits[2]+0.5) * 9.0/32.0)+1.0;
//                if(mapKnowledge[xmod+1][ymod+1] < 0.5)
//                    mapKnowledge[xmod+1][ymod+1] = ((terrainMapping.estimatedImpassabilityBits[3]+0.5) * 9.0/32.0)+1.0;
//            }
        }
    }

    /*
     * return ids of all non-EC robots
     */
    private IncompleteRobotInfo[] updateSensedRobots(RobotInfo[] robotInfos, RobotType type, int roundNum){
        IncompleteRobotInfo[] closestRobots = new IncompleteRobotInfo[NUM_ROBOTS_IN_ARRAY];
        int counter = 0;
        for(RobotInfo robotInfo : robotInfos){
            IncompleteRobotInfo incompleteRobotInfo = new IncompleteRobotInfo(robotInfo,type.canTrueSense(),roundNum);
            IncompleteRobotInfo closeRobot = processNewIncompleteRobotInfo(incompleteRobotInfo);
            if(counter < NUM_ROBOTS_IN_ARRAY){
                closestRobots[counter++] = closeRobot;
            }
        }
        return closestRobots;
    }

    private void updateBounds(RobotType type, MapLocation robotMapLocation, RobotController rc){
        int detectionRange = (int)(Math.floor(Math.sqrt(type.detectionRadiusSquared))+0.5);
        for(int diri = 0; diri < CARDINAL_DIRECTIONS.length; diri++){
            Direction direction = CARDINAL_DIRECTIONS[diri];
            if(coordinateBound[diri] == -1) {
                MapLocation mapLocation = robotMapLocation.add(direction);
                for (int i = 1; i <= detectionRange; i++, mapLocation = mapLocation.add(direction)) {
                    if(!rc.canDetectLocation(mapLocation)){
                        MapLocation prevLocation = mapLocation.subtract(direction);
                        int bound = (direction.dx == 0) ? prevLocation.y : prevLocation.x;
                        coordinateBound[diri] = bound;
                        break;
                    }
                }
            }
        }
    }

    private void updateSensedTerrain(RobotType type, MapLocation location, RobotController rc) throws GameActionException{
        RelativeLocation[] allSensedLocations = Utilities.getRelativeLocations();
        int limit = Utilities.getEndingLimit(type.sensorRadiusSquared);
        for (int i = 0; i < limit; i++) {
            System.out.println("UST1: "+ Clock.getBytecodeNum());
            MapLocation mapLocation = allSensedLocations[i].applyTo(location);
            System.out.println("UST2: "+ Clock.getBytecodeNum());
            if(rc.canSenseLocation(mapLocation)){
                int xmod = mapLocation.x % 64;
                int ymod = mapLocation.y % 64;
                double passability = rc.sensePassability(mapLocation);
                mapKnowledge[xmod][ymod] = 1/passability;
            }
            System.out.println("UST5: "+ Clock.getBytecodeNum());
        }
    }
}
