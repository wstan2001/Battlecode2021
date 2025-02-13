package v3a;

import battlecode.common.*;

import static v3a.Flag.encodeInstruction;
import static v3a.Flag.opcode;
import static v3a.Pathing.*;
import static v3a.RobotPlayer.*;

public class PoliticianLogic {

    static void runPolitician() throws GameActionException {
        if(ecID == 0) {
            ecID = getECID();
            int flag = getECFlag();
            if (flag != 0) {
                executeInstr(flag);
            }
        }

        if (type.equals( "Troop") ){
            runTroop();
        }
        else if (type.equals("Bomb")) {
            runBomb();
        }
        else if (type.equals("Stalk")) {
            runStalk();
        }
        else {
            //for som reason this politician doesn't have a type, maybe bc it was ex-Slanderer. Make it a troop
            type = "Troop";
            generalDir = rng.nextInt(8);
        }
    }


    private static int radiusToIndex(int radiusSquared){
        switch (radiusSquared){
            case 1: return 0;
            case 2: return 1;
            case 4: return 2;
            case 5: return 3;
            case 8: return 4;
            case 9: return 5;
            default: System.out.println("ERROR: BAD RADIUS:" + radiusSquared); return 0;
        }
    }

    private static final int[] possibleSmallerRadii = new int[]{1,2,4,5,8,9};


    static void runTroop() throws GameActionException {
        int actionRadius = rc.getType().actionRadiusSquared;
        Team ally = rc.getTeam();
        Team enemy = rc.getTeam().opponent();
        final int senseRadius = 25;
        final double baseCooldown = 1.0;

        /*System.out.println("General Direction: " + generalDir);
        System.out.println("Wandering: " + wandering);
        System.out.println("Target: " + targetLoc.x + " " + targetLoc.y);*/


        rc.setFlag(0);              //don't send outdated info
        scanForEC();

        if (!wandering) {
            //any troop has a 1% chance of beginning to move in random directions
            //also, troops start to wander when ~10 units from base, need to defend against mucks
            if (rng.nextDouble() < 0.01 || (!homeLoc.equals(new MapLocation(-1, -1)) && rc.getLocation().distanceSquaredTo(homeLoc) >= 100))
                wandering = true;
        }


        //check if the troop should start to stalk a muck
        if (turnCount > 12) {            //wait a few turns first in case base defense is higher priority
            int[] trackedMucks = new int[40];
            int arrLen = 0;
            RobotInfo[] nearbyAlly = rc.senseNearbyRobots(senseRadius, ally);
            for (RobotInfo rinfo : nearbyAlly) {
                if (rc.canGetFlag(rinfo.getID())) {
                    int flag = rc.getFlag(rinfo.getID());
                    if (opcode(flag) == Flag.OPCODE.STALK) {
                        trackedMucks[arrLen] = flag % (1 << 20);
                        arrLen += 1;
                    }
                }
            }
            RobotInfo[] nearbyEnemy = rc.senseNearbyRobots(senseRadius, enemy);
            for (RobotInfo rinfo : nearbyEnemy) {
                //don't need to bind to muckraker that is far from ally EC
                //although some polis don't have a homeLoc (think ex-slands)
                if (rinfo.getType() == RobotType.MUCKRAKER && !homeLoc.equals(new MapLocation(-1, -1)) &&
                     rinfo.getLocation().distanceSquaredTo(homeLoc) <= 225) {
                    //check if it's already being tracked
                    boolean isTracked = false;
                    for (int i = 0; i < arrLen; i++) {
                        if (trackedMucks[i] == rinfo.getID() % (1 << 20)) {
                            isTracked = true;
                            break;
                        }
                    }
                    if (isTracked == false) {
                        type = "Stalk";
                        curTarget = rinfo.getID();
                        rc.setFlag(encodeInstruction(Flag.OPCODE.STALK, 0, 0, curTarget % (1 << 20)));
                        //System.out.println("Now stalking " + curTarget);
                        runStalk();
                        break;
                    }
                }
            }
        }

        if (rc.getInfluence() > 10 && countdown <= 0) {
            int sensorRadius = rc.getType().sensorRadiusSquared;
            int empowerInfluence = rc.getInfluence() - 10;
            boolean shouldEmpower = false;
            MapLocation currentLocation = rc.getLocation();
            RobotInfo[][] sortedByRadius = new RobotInfo[6][8];
            int[] sortedByRadiusLengths = new int[]{0,0,0,0,0,0};
            double[][] allyECSurroundedBonus = new double[6][8];
            RobotInfo[] actionRadiusNearbyRobots = rc.senseNearbyRobots(actionRadius);
            int[] allyECSurroundedValue = new int[10];
            int indx, length;
            MapLocation[] allyECLocations = new MapLocation[10];
            int numAllyECLocations = 0;
            for(RobotInfo robotInfo : actionRadiusNearbyRobots){
                if(robotInfo.type == RobotType.ENLIGHTENMENT_CENTER && robotInfo.team == ally){
                    allyECLocations[numAllyECLocations] = robotInfo.location;
                    numAllyECLocations++;
                    for(Direction direction: directions){
                        MapLocation adjacent = robotInfo.location.add(direction);
                        if(adjacent.distanceSquaredTo(currentLocation) <= sensorRadius){
                            if(!rc.canSenseLocation(adjacent)){
                                allyECSurroundedValue[numAllyECLocations] += 1;
                            }
                        }
                    }
                }
            }
            for(RobotInfo robotInfo : actionRadiusNearbyRobots){
                //System.out.println(robotInfo.location.x + ", "+ robotInfo.location.y + " : " + robotInfo.location.distanceSquaredTo(currentLocation));
                indx = radiusToIndex(robotInfo.location.distanceSquaredTo(currentLocation));
                length = sortedByRadiusLengths[indx];
                sortedByRadius[indx][length] = robotInfo;
                sortedByRadiusLengths[indx]++;
                for (int i = 0; i < numAllyECLocations; i++) {
                    int distanceSquared = allyECLocations[i].distanceSquaredTo(robotInfo.location);
                    if(distanceSquared <= 2){
                        allyECSurroundedValue[i] += 1;
                    }
                }
            }
            for (int i = 0; i < 6; i++) {
                length = sortedByRadiusLengths[i];
                for (int j = 0; j < length; j++) {
                    RobotInfo robotInfo = sortedByRadius[i][j];
                    if(robotInfo.type == RobotType.ENLIGHTENMENT_CENTER || robotInfo.team == enemy){
                        for (int k = 0; k < numAllyECLocations; k++) {
                            int distanceSquared = allyECLocations[k].distanceSquaredTo(robotInfo.location);
                            if(distanceSquared <= 2){
                                allyECSurroundedBonus[i][j] += Math.pow(allyECSurroundedValue[k],3)/27.0;
                            }
                        }
                    }
                }
            }
            int totalnum = 0;
            double convictionUseOnEach;
            double effectiveness;
            //double bestEffectiveness = (sortedByRadiusLengths[0] == 4 && sortedByRadiusLengths[1] == 4)?empowerInfluence*0.3 : empowerInfluence*0.8;
            double bestEffectiveness = empowerInfluence*0.5;
            int bestActionRadius = -1;
            for (int i = 0; i < 6; i++) {
                if(sortedByRadiusLengths[i] == 0){
                    continue;
                }
                totalnum += sortedByRadiusLengths[i];
                if(totalnum == 0){
                    continue;
                }
                convictionUseOnEach = totalnum > 0 ? ((double)empowerInfluence )/ totalnum : 0;
                effectiveness = 0;
                for (int j = 0; j <= i; j++) {
                    int arrayLength = sortedByRadiusLengths[j];
                    for (int k = 0; k < arrayLength; k++) {
                        RobotInfo robotInfo = sortedByRadius[j][k];
                        if(robotInfo.team == ally){
                            if(robotInfo.type == RobotType.ENLIGHTENMENT_CENTER) {
                                effectiveness += convictionUseOnEach*0.1;
                            }
                        }else if(robotInfo.team == enemy){
                            if (robotInfo.type == RobotType.ENLIGHTENMENT_CENTER) {
                                if (robotInfo.conviction < convictionUseOnEach) {
                                    effectiveness += empowerInfluence * 0.5;        //automatically empower
                                }
                                effectiveness += convictionUseOnEach * 0.8;
                            } else {
                                if (robotInfo.conviction < convictionUseOnEach) {
                                    //effectiveness += empowerInfluence * 0.1;            //might be overkill, but treat it as kill bonus
                                    effectiveness += robotInfo.conviction;
                                    //System.out.println("Ally EC Surrounded Bonus: " + robotInfo.ID+ ", "+ allyECSurroundedBonus[j][k]);
                                    effectiveness += allyECSurroundedBonus[j][k];
                                } else {
                                    effectiveness += convictionUseOnEach * 0.5;
                                }
                            }
                        }else{
                            effectiveness += convictionUseOnEach;
                            if(robotInfo.conviction < convictionUseOnEach){
                                effectiveness += empowerInfluence * 0.5;        //automatically empower
                            }
                        }
                    }
                }
                if(effectiveness > bestEffectiveness){
                    bestEffectiveness = effectiveness;
                    bestActionRadius = possibleSmallerRadii[i];
                }
            }
            if(bestActionRadius > -1 && rc.canEmpower(bestActionRadius)){
                rc.empower(bestActionRadius);
            }
        }
        if (generalDir != -1) {
            if (!homeLoc.equals(new MapLocation(-1, -1)) && rc.getLocation().distanceSquaredTo(homeLoc) <= 16) //too close to home
                moveSlander(oppositeDir(rc.getLocation().directionTo(homeLoc)));        //want wider range of movement, ignore generalDir
            else if (wandering)
                moveDir(directions[rng.nextInt(8)]);
            else
                moveDir(directions[generalDir]);
        }
        else
            generalDir = rng.nextInt(8);

    }




    static void runBomb() throws GameActionException {
        int actionRadius = rc.getType().actionRadiusSquared;
        Team enemy = rc.getTeam().opponent();
        final int senseRadius = 25;
        final double baseCooldown = 1.0;

        /*System.out.println("General Direction: " + generalDir);
        System.out.println("Wandering: " + wandering);
        System.out.println("Target: " + targetLoc.x + " " + targetLoc.y);*/


        rc.setFlag(0);              //don't send outdated info
        scanForEC();
    
        if (!targetLoc.equals(new MapLocation(-1, -1)) && rc.getLocation().distanceSquaredTo(getAbsCoords(targetLoc)) <= 9) {
            //update the countdown
            countdown -= 1;
        }
        if (rc.getInfluence() > 10 && countdown <= 0) {
            int sensorRadius = rc.getType().sensorRadiusSquared;
            int empowerInfluence = rc.getInfluence() - 10;
            boolean shouldEmpower = false;
            MapLocation currentLocation = rc.getLocation();
            RobotInfo[][] sortedByRadius = new RobotInfo[6][8];
            int[] sortedByRadiusLengths = new int[]{0,0,0,0,0,0};
            double[][] allyECSurroundedBonus = new double[6][8];
            RobotInfo[] actionRadiusNearbyRobots = rc.senseNearbyRobots(actionRadius);
            int[] allyECSurroundedValue = new int[10];
            int indx, length;
            for(RobotInfo robotInfo : actionRadiusNearbyRobots){
                //System.out.println(robotInfo.location.x + ", "+ robotInfo.location.y + " : " + robotInfo.location.distanceSquaredTo(currentLocation));
                indx = radiusToIndex(robotInfo.location.distanceSquaredTo(currentLocation));
                length = sortedByRadiusLengths[indx];
                sortedByRadius[indx][length] = robotInfo;
                sortedByRadiusLengths[indx]++;
            }
            int totalnum = 0;
            double convictionUseOnEach;
            double effectiveness;
            //double bestEffectiveness = (sortedByRadiusLengths[0] == 4 && sortedByRadiusLengths[1] == 4)?empowerInfluence*0.3 : empowerInfluence*0.8;
            double bestEffectiveness = empowerInfluence*0.5;
            int bestActionRadius = -1;
            for (int i = 0; i < 6; i++) {
                if(sortedByRadiusLengths[i] == 0){
                    continue;
                }
                totalnum += sortedByRadiusLengths[i];
                if(totalnum == 0){
                    continue;
                }
                convictionUseOnEach = totalnum > 0 ? ((double)empowerInfluence )/ totalnum : 0;
                effectiveness = 0;
                for (int j = 0; j <= i; j++) {
                    int arrayLength = sortedByRadiusLengths[j];
                    for (int k = 0; k < arrayLength; k++) {
                        RobotInfo robotInfo = sortedByRadius[j][k];
                        if(robotInfo.team == ally){
                            if(robotInfo.location.equals(getAbsCoords(targetLoc))){
                                //System.out.println("Found ally target!");
                                effectiveness += convictionUseOnEach*10;
                            }
                            if(robotInfo.type == RobotType.ENLIGHTENMENT_CENTER) {
                                effectiveness += convictionUseOnEach*0.05;
                            }
                        }else if(robotInfo.team == enemy){
                            if (robotInfo.type == RobotType.ENLIGHTENMENT_CENTER) {
                                if(robotInfo.location.equals(getAbsCoords(targetLoc))){
                                    //System.out.println("Found enemy target!");
                                    effectiveness += convictionUseOnEach*10;
                                }
                                if (robotInfo.conviction < convictionUseOnEach) {
                                    effectiveness += empowerInfluence * 0.5;        //automatically empower
                                }
                                effectiveness += convictionUseOnEach * 0.3;
                            } else {
                                if (robotInfo.conviction < convictionUseOnEach) {
                                    effectiveness += convictionUseOnEach * 0.4;            //might be overkill, but treat it as kill bonus
                                    //effectiveness += robotInfo.conviction*2;
                                } else {
                                    effectiveness += convictionUseOnEach*0.3;
                                }
                            }
                        }else{
                            if(robotInfo.location.equals(getAbsCoords(targetLoc))){
                                effectiveness += convictionUseOnEach*10;
                            }
                            effectiveness += convictionUseOnEach*2.0;
                            if(robotInfo.conviction < convictionUseOnEach){
                                effectiveness += empowerInfluence * 0.5;        //automatically empower
                            }
                        }
                    }
                }
                if(effectiveness > bestEffectiveness){
                    bestEffectiveness = effectiveness;
                    bestActionRadius = possibleSmallerRadii[i];
                }
            }
            if(bestActionRadius > -1 && rc.canEmpower(bestActionRadius)){
                rc.empower(bestActionRadius);
            }
        }
        //sometimes there will be extra troops sent to an already captured neutral EC
        //we don't want these troops to clog the EC
        MapLocation absLoc = getAbsCoords(targetLoc);
        if (rc.canSenseLocation(absLoc)) {
            RobotInfo rinfo = rc.senseRobotAtLocation(absLoc);
            if (rinfo != null && rinfo.getTeam() == rc.getTeam() && rinfo.getType() == RobotType.ENLIGHTENMENT_CENTER) {
                //don't crowd the ally EC
                //System.out.println("I'm gonna stop crowding!");
                type = "Troop";
                targetLoc = new MapLocation(-1, -1);
                generalDir = rng.nextInt(8);
                return;
            }
        }
        moveToLoc(targetLoc);

    }



    static void runStalk() throws GameActionException {
        int actionRadius = rc.getType().actionRadiusSquared;
        Team enemy = rc.getTeam().opponent();
        Team ally = rc.getTeam();
        final int senseRadius = 25;
        final double baseCooldown = 1.0;

        //check if there are other stalkers nearby
        boolean otherStalkers = false;
        RobotInfo[] nearbyAlly = rc.senseNearbyRobots(senseRadius, ally);
        for (RobotInfo rinfo : nearbyAlly) {
            int flag = rc.getFlag(rinfo.getID());
            if (opcode(flag) == Flag.OPCODE.STALK && flag % (1 << 20) == curTarget) {
                otherStalkers = true;
                break;
            }
        }

        if (rc.canSenseRobot(curTarget) && rc.getConviction() > 12 && otherStalkers == false) {
            rc.setFlag(encodeInstruction(Flag.OPCODE.STALK, 0, 0, curTarget % (1 << 20)));
            MapLocation muckLoc = rc.senseRobot(curTarget).getLocation();
            int distToMuck = rc.getLocation().distanceSquaredTo(muckLoc);

            nearbyAlly = rc.senseNearbyRobots(senseRadius, ally);
            for (RobotInfo rinfo : nearbyAlly) {
                if (opcode(rc.getFlag(rinfo.getID())) == Flag.OPCODE.SLAND || rinfo.getType() == RobotType.ENLIGHTENMENT_CENTER) {
                    //enemy muckraker needs to die
                    if (distToMuck < actionRadius && rc.canEmpower(distToMuck))
                        rc.empower(distToMuck);
                }
            }

            //if enemy gets too close to home
            if (homeLoc.distanceSquaredTo(muckLoc) < 64) {
                //enemy muckraker needs to die
                if (distToMuck < actionRadius && rc.canEmpower(distToMuck))
                    rc.empower(distToMuck);
            }

            moveDir(rc.getLocation().directionTo(muckLoc));
        }
        else {
            //maybe another politician killed the target
            type = "Troop";
            curTarget = -1;
            rc.setFlag(0);
        }
    }
}
