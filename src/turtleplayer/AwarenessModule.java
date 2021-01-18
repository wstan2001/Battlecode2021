package turtleplayer;

import battlecode.common.*;
import org.jetbrains.annotations.Nullable;
import turtleplayer.flagutilities.*;

public strictfp class AwarenessModule {

    IncompleteRobotInfo[] politicianMaybeSlandererRobotInfos = new IncompleteRobotInfo[20];
    IncompleteRobotInfo[][] incompleteRobotInfos = new IncompleteRobotInfo[4][20];
    int[] numIncomepleteRobotInfos = new int[4];
    int numPoliticianMaybeSlandererRobotInfos = 0;

    PassabilityKnowledge[][] mapKnowledge = new PassabilityKnowledge[64][64];

    IDMessagePartOne[] firstIDMessageParts = new IDMessagePartOne[20];
    int[] firstIDMessagePartSenderRobotIDs = new int[20];
    int numfirstIDMessages = 0;

    int[] friendECIDs = new int[10];

    @Nullable
    public MoveCommand lastMoveCommand;

    public void removeLastMoveCommand(){
        lastMoveCommand = null;
    }

    private final static Direction[] CARDINAL_DIRECTIONS =
            new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
    private final int[] coordinateBound = new int[4];

    AwarenessModule(){
        for (int i = 0; i < 4; i++) {
            coordinateBound[i] = -1;
            numIncomepleteRobotInfos[i] = 0;
        }
        for (int i = 0; i < 10; i++) {
            friendECIDs[i] = -1;
        }
    }

    public void update(RobotController rc, boolean turnHasPassed, int roundNum, RobotType type,
                MapLocation location, Team team) throws GameActionException{
        RobotInfo[] robotInfos = rc.senseNearbyRobots();
        System.out.println("UPDATE1: "+Clock.getBytecodeNum());
        IncompleteRobotInfo[] sensedRobotInfos = updateSensedRobots(robotInfos, type, roundNum);
        System.out.println("UPDATE2: "+Clock.getBytecodeNum());
        updateBounds(type, location, rc);
        System.out.println("UPDATE3: "+Clock.getBytecodeNum());
        updateSensedTerrain(type, location, rc);
        System.out.println("UPDATE4: "+Clock.getBytecodeNum());
        if(turnHasPassed) {
            int newNumFirstIDMessages = 0;
            IDMessagePartOne[] newFirstIDMessageParts = new IDMessagePartOne[20];
            int[] newFirstIDMessagePartSenderRobotIDs = new int[20];
            for (IncompleteRobotInfo sender : sensedRobotInfos) {
                if(sender != null) {
                    if (sender.robotId != null && rc.canGetFlag(sender.robotId) && sender.team == team) {
                        FlagMessage flagMessage = FlagUtilities.decodeFlag(rc.getFlag(sender.robotId));
                        if (flagMessage instanceof IDMessagePartOne) {
                            newFirstIDMessageParts[newNumFirstIDMessages] = (IDMessagePartOne) flagMessage;
                            newFirstIDMessagePartSenderRobotIDs[newNumFirstIDMessages++] = sender.robotId;
                        } else if (flagMessage instanceof IDMessagePartTwo) {
                            for (int i = 0; i < numfirstIDMessages; i++) {
                                if (firstIDMessagePartSenderRobotIDs[i] == sender.robotId) {
                                    processNewIncompleteRobotInfo(new IncompleteRobotInfo(
                                            firstIDMessageParts[i], (IDMessagePartTwo) flagMessage, roundNum));
                                    break;
                                }
                            }
                        } else {
                            processFlagMessage(flagMessage, sender.robotId, roundNum, sender, rc);
                        }
                    }
                }
            }
            firstIDMessagePartSenderRobotIDs = newFirstIDMessagePartSenderRobotIDs;
            firstIDMessageParts = newFirstIDMessageParts;
        }
    }

    private static final double POSSIBILITY_THRESHOLD = 0.9;
    private void processNewIncompleteRobotInfo(IncompleteRobotInfo robotInfo){
        if(robotInfo.couldBeSlanderer){
            double bestConfidence = POSSIBILITY_THRESHOLD;
            int bestConfidenceRobotInfoIndex = -1;
            int bestIndx = -1;
            int max = numPoliticianMaybeSlandererRobotInfos;
            for (int i = 0; i < max; i++) {
                double confidence = politicianMaybeSlandererRobotInfos[i].isRobotConfidence(robotInfo);
                if (confidence > bestConfidence) {
                    bestConfidence = confidence;
                    bestConfidenceRobotInfoIndex = i;
                    bestIndx = -1;
                }
            }
            int indx = RobotType.SLANDERER.ordinal();
            max = numIncomepleteRobotInfos[indx];
            for (int i = 0; i < max; i++) {
                double confidence = incompleteRobotInfos[indx][i].isRobotConfidence(robotInfo);
                if (confidence > bestConfidence) {
                    bestConfidence = confidence;
                    bestConfidenceRobotInfoIndex = i;
                    bestIndx = indx;
                }
            }
            indx = RobotType.POLITICIAN.ordinal();
            max = numIncomepleteRobotInfos[indx];
            for (int i = 0; i < max; i++) {
                double confidence = incompleteRobotInfos[indx][i].isRobotConfidence(robotInfo);
                if (confidence > bestConfidence) {
                    bestConfidence = confidence;
                    bestConfidenceRobotInfoIndex = i;
                    bestIndx = indx;
                }
            }
            if (bestConfidenceRobotInfoIndex > -1) {
                if(bestIndx > -1){
                    incompleteRobotInfos[bestIndx][bestConfidenceRobotInfoIndex].update(robotInfo);
                }else {
                    politicianMaybeSlandererRobotInfos[bestConfidenceRobotInfoIndex].update(robotInfo);
                }
            } else if (numPoliticianMaybeSlandererRobotInfos < 20) {
                politicianMaybeSlandererRobotInfos[numPoliticianMaybeSlandererRobotInfos++] = robotInfo;
            }
        }else {
            int indx = robotInfo.robotType.ordinal();
            double bestConfidence = POSSIBILITY_THRESHOLD;
            int bestConfidenceRobotInfoIndex = -1;
            int max = numIncomepleteRobotInfos[indx];
            for (int i = 0; i < max; i++) {
                double confidence = incompleteRobotInfos[indx][i].isRobotConfidence(robotInfo);
                if (confidence > bestConfidence) {
                    bestConfidence = confidence;
                    bestConfidenceRobotInfoIndex = i;
                }
            }
            if (bestConfidenceRobotInfoIndex > -1) {
                incompleteRobotInfos[indx][bestConfidenceRobotInfoIndex].update(robotInfo);
            } else if (max < 20) {
                incompleteRobotInfos[indx][max] = robotInfo;
                numIncomepleteRobotInfos[indx] = max + 1;
            }
        }
    }

    private void processFlagMessage(FlagMessage flagMessage, int robotID, int roundNum,
                            IncompleteRobotInfo sender, RobotController rc){
        if(flagMessage instanceof UnitPresent){
            IncompleteRobotInfo robotInfo = new IncompleteRobotInfo(
                    (UnitPresent) flagMessage, sender, roundNum);
            processNewIncompleteRobotInfo(robotInfo);
        }else if(flagMessage instanceof MoveCommand){
            lastMoveCommand = (MoveCommand) flagMessage;
        }else if(flagMessage instanceof SelfStatus){
            if(!rc.canSenseRobot(robotID)){
                sender.update((SelfStatus) flagMessage);
            }
        }else if(flagMessage instanceof TerrainMapping){
            if(sender.lastSeenMapLocation != null) {
                TerrainMapping terrainMapping = (TerrainMapping) flagMessage;
                MapLocation lastLocation = sender.lastSeenMapLocation;
                boolean[] turnBits = terrainMapping.locationTurnsBit;
                int bitNumber = terrainMapping.bitNumber;
                RelativeLocation[] relativeLocations = terrainMapping.getRelativeLocations();
                for (int i = 0; i < 15; i++) {
                    MapLocation newLocation = relativeLocations[i].applyTo(lastLocation);
                    int xmod = newLocation.x % 64;
                    int ymod = newLocation.y % 64;
                    if(mapKnowledge[xmod][ymod] == null){
                        mapKnowledge[xmod][ymod] = new PassabilityKnowledge();
                    }
                    mapKnowledge[xmod][ymod].flagKnowledge[bitNumber] = turnBits[i];
                }
            }
        }
    }

    /*
     * return ids of all non-EC robots
     */
    private IncompleteRobotInfo[] updateSensedRobots(RobotInfo[] robotInfos, RobotType type, int roundNum){
        IncompleteRobotInfo[] closestRobots = new IncompleteRobotInfo[20];
        int counter = 0;
        for(RobotInfo robotInfo : robotInfos){
            int robotId = robotInfo.getID();
            IncompleteRobotInfo incompleteRobotInfo = new IncompleteRobotInfo(robotInfo,type.canTrueSense(),roundNum);
            if(counter < 20){
                closestRobots[counter++] = incompleteRobotInfo;
            }
            processNewIncompleteRobotInfo(incompleteRobotInfo);
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
            RelativeLocation relativeLocation = allSensedLocations[i];
            MapLocation mapLocation = relativeLocation.applyTo(location);
            System.out.println("UST2: "+ Clock.getBytecodeNum());
            if(rc.canSenseLocation(mapLocation)){
                int xmod = mapLocation.x % 64;
                int ymod = mapLocation.y % 64;
                double passability = rc.sensePassability(mapLocation);
                if(mapKnowledge[xmod][ymod] == null){
                    mapKnowledge[xmod][ymod] = new PassabilityKnowledge(passability);
                }else {
                    mapKnowledge[xmod][ymod].sensedKnowledge = rc.sensePassability(mapLocation);
                }
            }
            System.out.println("UST5: "+ Clock.getBytecodeNum());
        }
    }
}
