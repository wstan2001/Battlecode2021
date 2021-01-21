package v3arefactor;

import battlecode.common.*;

import static v3arefactor.Bidding.bid;
import static v3arefactor.Flag.encodeInstruction;
import static v3arefactor.Flag.opcode;
import static v3arefactor.Pathing.*;
import static v3arefactor.RobotPlayer.*;

public class EnlightenmentCenterLogic {


    static void runEnlightenmentCenter() throws GameActionException {
        RobotType toBuild = RobotType.MUCKRAKER;
        String unitType = "None";
        MapLocation unitTarget = new MapLocation(-1, -1);
        boolean canBuildSlanderer = true;
        Direction buildDir;
        int influence = 1;
        int[] goodSlandererInfluences = new int[]{21, 41, 63, 85, 107, 130, 154, 178, 203, 228};
        // boolean hasSpentInfluence = rc.getInfluence() < expectedNewInfluenceIfNoBid;

        //System.out.println("Conviction: " + rc.getConviction());
        //System.out.println("Influence: " + rc.getInfluence());

        int sensorRadius = rc.getType().sensorRadiusSquared;
        for (RobotInfo robot : rc.senseNearbyRobots(sensorRadius, enemy)) {
            if (robot.type == RobotType.MUCKRAKER || robot.type == RobotType.ENLIGHTENMENT_CENTER) {
                //if there are muckrakers nearby, don't build slanderers!
                canBuildSlanderer = false;
                break;
            }
        }

        RobotInfo[] closeEnemy = rc.senseNearbyRobots(9, enemy);
        int enemyCount = 0;
        for (RobotInfo rinfo : closeEnemy) {
            enemyCount += 1;
        }

        RobotInfo[] closeDefender = rc.senseNearbyRobots(9, ally);
        int defenderCount = 0;
        for (RobotInfo rinfo : closeDefender) {
            if (rinfo.getType() == RobotType.POLITICIAN && rinfo.getConviction() >= 17) {
                defenderCount += 1;
            }
        }

        //go through a few conditional checks b4 spawning randomly
        //decide upon spawn types and parameters here
        boolean shouldSpawn = true;
        if (rc.getRoundNum() < 15) {
            toBuild = RobotType.SLANDERER;
            int slandInfl = (int) (0.95 * rc.getInfluence());
            if (slandInfl < 21 || !canBuildSlanderer) {
                //build muck instead
                toBuild = RobotType.MUCKRAKER;
                unitType = "Random";
                influence = 1;
            }
            else
            {
                for (int inf : goodSlandererInfluences)
                    if (inf < slandInfl)
                        influence = Math.max(influence, inf);
                influence = Math.max(21, influence);
            }
        }
        else if (enemyCount > 4 && defenderCount < 2) {
            toBuild = RobotType.POLITICIAN;
            unitType = "Troop";
            influence = Math.max(17, rc.getInfluence() / 10);
        }
        else if (rng.nextDouble() < Math.pow(rc.getInfluence(), 0.2) / 10.0) {
            //randomized spawn code
            if (phase.equals("Early")) {
                toBuild = earlySpawnRobot();

                if(!canBuildSlanderer && toBuild == RobotType.SLANDERER)
                    toBuild = Math.random() < 0.4 ? RobotType.POLITICIAN : RobotType.MUCKRAKER;

                if (turnCount > 50)
                    phase = "Mid";
            }
            else if (phase.equals( "Mid") ){
                toBuild = midSpawnRobot();
                if(!canBuildSlanderer && toBuild == RobotType.SLANDERER)
                    toBuild = Math.random() < 0.6 ? RobotType.POLITICIAN : RobotType.MUCKRAKER;
            }

            if (toBuild == RobotType.POLITICIAN) {
                if (rng.nextDouble() < 0.5 && bombCooldown < 0 && targetNeutralLoc.x != -1 && targetNeutralLoc.y != -1 && rc.getInfluence() > 140) {
                    unitType = "Bomb";
                    unitTarget = targetNeutralLoc;
                    influence = rc.getInfluence() / 3;
                    bombCooldown = 10;              //wait before spawning more bombs
                }
                else {
                    unitType = "Troop";
                    influence = rc.getInfluence() / 10;
                    if (influence < 17)
                        influence = 0;
                }
            }
            else if (toBuild == RobotType.MUCKRAKER) {
                int numFoundEnemy = 0;
                for (int i = 0; i < enemyECLoc.length; i++) {
                    if (enemyECLoc[i].x != -1 && enemyECLoc[i].y != -1)
                        numFoundEnemy += 1;
                }
                if (numFoundEnemy > 0 && rng.nextDouble() < 0.7) {
                    //send muck to enemy EC
                    unitType = "Targeting";
                    influence = 1;
                    int temp = rng.nextInt(numFoundEnemy);
                    for (int i = 0; i < enemyECLoc.length; i++) {
                        if (enemyECLoc[i].x != -1 && enemyECLoc[i].y != -1) {
                            if (temp == 0) {
                                //send muckracker to this location
                                unitTarget = enemyECLoc[i];
                                break;
                            }
                            else
                                temp -= 1;
                        }
                    }
                }
                else {
                    unitType = "Random";
                    influence = 1;
                }
            }
            else if (toBuild == RobotType.SLANDERER) {
                int slandInfl = (int) (3 * Math.pow(rc.getInfluence(), 2.0/3));
                if (slandInfl < 21) {
                    //build muck instead
                    toBuild = RobotType.MUCKRAKER;
                    unitType = "Random";
                    influence = 1;
                }
                else
                {
                    for (int inf : goodSlandererInfluences)
                        if (inf < slandInfl)
                            influence = Math.max(influence, inf);
                    influence = Math.max(21, influence);
                }
            }
        }
        else {
            shouldSpawn = false;
        }


        //execute spawning code here
        if (rc.isReady() && shouldSpawn) {
            //don't remove the above if! Else instructions will get all mixed up bc we spawn too frequently
            //focus on growth; we can afford to spawn more when our influence is higher
            int randDirection = rng.nextInt(8);             //we will use this if we give robot a random direction
            if (toBuild == RobotType.POLITICIAN) {
                if (unitType.equals("Bomb")) {
                    //try to attack random neutral EC
                    if(rc.canSetFlag(encodeInstruction(Flag.OPCODE.BOMB, unitTarget.x, unitTarget.y, 0))) { //op, xcoord, ycoord, of location to seek
                        rc.setFlag(encodeInstruction(Flag.OPCODE.BOMB, unitTarget.x, unitTarget.y, 0));
                        MapLocation absLoc = getAbsCoords(unitTarget);
                        System.out.println("Sending bomb to neutral EC at " + absLoc.x + " " + absLoc.y);
                    }
                }
                else {
                    if(rc.canSetFlag(encodeInstruction(Flag.OPCODE.TROOP, 0, 0, randDirection))) //op, xcoord, ycoord, direction to patrol
                        rc.setFlag(encodeInstruction(Flag.OPCODE.TROOP, 0, 0, randDirection));
                }
            }
            else if (toBuild == RobotType.SLANDERER) {
                //don't need to broadcast flag codes to sland
                //instead we try to find optimal spawn location
                MapLocation centroid = new MapLocation(0, 0);         //find centroid of enemy ECs
                int numEnemy = 0;
                for (MapLocation loc : enemyECLoc) {
                    if (loc.x != -1 && loc.y != -1) {
                        numEnemy += 1;
                        MapLocation absLoc = getAbsCoords(loc);
                        centroid = centroid.translate(absLoc.x, absLoc.y);
                    }
                }
                if (numEnemy != 0) {
                    centroid = new MapLocation(centroid.x / numEnemy, centroid.y / numEnemy);
                    //if we do find enemies, spawn the Sland away from the centroid
                    MapLocation selfLoc = rc.getLocation();
                    //System.out.println("Centroid: " + centroid);
                    //System.out.println("Optimal sland spawn is " + oppositeDir(selfLoc.directionTo(centroid)));
                    randDirection = getDirIdx(oppositeDir(selfLoc.directionTo(centroid)));
                    randDirection = Math.max(randDirection, 0);           //in the rare case that offset somehow is Direction.CENTER
                }
            }
            else if (toBuild == RobotType.MUCKRAKER) {
                if (unitType.equals("Targeting")) {
                    if(rc.canSetFlag(encodeInstruction(Flag.OPCODE.MOVE, unitTarget.x, unitTarget.y, 0))) //op, xcoord, ycoord of location to go to
                        rc.setFlag(encodeInstruction(Flag.OPCODE.MOVE, unitTarget.x, unitTarget.y, 0));
                }
                else {
                    if(rc.canSetFlag(encodeInstruction(Flag.OPCODE.SCOUT, 0, 0, randDirection))) //op, xcoord, ycoord, direction to travel
                        rc.setFlag(encodeInstruction(Flag.OPCODE.SCOUT, 0, 0, randDirection));
                }
            }


            for (int i = 0; i < directions.length; i++) {
                Direction dir = directions[(dirHelper[i] + randDirection + 8) % 8];
                if (rc.canBuildRobot(toBuild, dir, influence)) {
                    rc.buildRobot(toBuild, dir, influence);
                    if (toBuild == RobotType.POLITICIAN || toBuild == RobotType.MUCKRAKER) {      //we can make more than 100 troops but only keep track of this many
                        RobotInfo rinfo = rc.senseRobotAtLocation(rc.getLocation().add(dir));
                        for (int j = 0; j < unitIDs.length; j++) {
                            if (unitIDs[j] == 0) {
                                unitIDs[j] = rinfo.getID();
                                break;
                            }
                        }
                    }
                    break;
                }
            }
        }

        for (int i = 0; i < unitIDs.length; i++) {
            int id = unitIDs[i];
            if (rc.canGetFlag(id)) {
                Flag.OPCODE opc = opcode(rc.getFlag(id));
                if (opc == Flag.OPCODE.ENEMYEC || opc == Flag.OPCODE.NEUTRALEC || opc == Flag.OPCODE.ALLYEC)
                    executeInstr(rc.getFlag(id));           //decode scout info
                else if (opc == Flag.OPCODE.STALK)           //can stop tracking this unit
                    unitIDs[i] = 0;
            }
            else
                unitIDs[i] = 0;
        }

        bid();

        bombCooldown -= 1;

        /*for (int i = 0; i < enemyECLoc.length; i++) {
            if (enemyECLoc[i].x != -1 && enemyECLoc[i].y != -1) {
                MapLocation absLoc = getAbsCoords(enemyECLoc[i]);
                System.out.println("Enemy EC at (" + absLoc.x + "," + absLoc.y + ")");
            }
        }
        for (int i = 0; i < neutralECLoc.length; i++) {
            if (neutralECLoc[i].x != -1 && neutralECLoc[i].y != -1) {
                MapLocation absLoc = getAbsCoords(neutralECLoc[i]);
                System.out.println("Neutral EC at (" + absLoc.x + "," + absLoc.y + ")");
            }
        }
        for (int i = 0; i < allyECLoc.length; i++) {
            if (allyECLoc[i].x != -1 && allyECLoc[i].y != -1) {
                MapLocation absLoc = getAbsCoords(allyECLoc[i]);
                System.out.println("Ally EC at (" + absLoc.x + "," + absLoc.y + ")");
            }
        }*/
    }


    /**
     * Returns a random spawnable RobotType in the early game
     *
     * @return a random RobotType
     */
    static RobotType earlySpawnRobot() {
        double prob = Math.random();
        if(prob < 0.4){
            return RobotType.SLANDERER;
        }else if(prob < 0.7){
            return RobotType.MUCKRAKER;
        }else{
            return RobotType.POLITICIAN;
        }
    }

    /**
     * Returns a random spawnable RobotType in the mid game
     *
     * @return a random RobotType
     */
    static RobotType midSpawnRobot() {
        double prob = Math.random();
        if(prob < 0.3){
            return RobotType.SLANDERER;
        }else if(prob < 0.5){
            return RobotType.MUCKRAKER;
        }else{
            return RobotType.POLITICIAN;
        }
    }
}
