package v3b;

import battlecode.common.*;

import static v3b.Bidding.bid;
import static v3b.Flag.encodeInstruction;
import static v3b.Flag.opcode;
import static v3b.Pathing.*;
import static v3b.RobotPlayer.*;

public class EnlightenmentCenterLogic {


    static void runEnlightenmentCenter() throws GameActionException {
        toSpawn = RobotType.MUCKRAKER;            //reset spawn unit parameters
        spawnType = "Random";
        spawnTarget = new MapLocation(-1, -1);
        spawnInfluence = 1;
        boolean canSpawnSlanderer = true;
        Direction spawnDir;
        int[] goodSlandererInfluences = new int[]{21, 41, 63, 85, 107, 130, 154, 178, 203, 228, 255, 282, 310, 339, 368,
                                                    399, 431, 463, 497, 532, 568, 605, 643, 683, 724};
        // boolean hasSpentInfluence = rc.getInfluence() < expectedNewInfluenceIfNoBid;

        //System.out.println("Conviction: " + rc.getConviction());
        //System.out.println("Influence: " + rc.getInfluence());

        int sensorRadius = rc.getType().sensorRadiusSquared;
        for (RobotInfo robot : rc.senseNearbyRobots(sensorRadius, enemy)) {
            if (robot.type == RobotType.MUCKRAKER || robot.type == RobotType.ENLIGHTENMENT_CENTER) {
                //if there are muckrakers nearby, don't build slanderers!
                canSpawnSlanderer = false;
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
        if (rc.getRoundNum() < 15 && slandCooldown < 0) {      //don't want to spam slands TOO rapidly
            int slandInfl = (int) (0.95 * rc.getInfluence());
            if (slandInfl < 21 || !canSpawnSlanderer) {
                //build muck instead
                spawnRobot(RobotType.MUCKRAKER, "Random", new MapLocation(-1, -1), 1);
            }
            else
            {
                for (int inf : goodSlandererInfluences)
                    if (inf < slandInfl)
                        spawnInfluence = Math.max(spawnInfluence, inf);
                spawnInfluence = Math.max(21, spawnInfluence);
                spawnRobot(RobotType.SLANDERER, "Random", new MapLocation(-1, -1), spawnInfluence);
                slandCooldown = 4;
            }
        }
        /*
        //Bomb self strat now obsolete
        else if (rc.getEmpowerFactor(rc.getTeam(), 15) > 4.2 && bombCooldown < 0) {      //need to check multiplier 15 turns in future bc of 10 turn unit spawn penalty
            MapLocation selfLoc = new MapLocation(rc.getLocation().x % 128, rc.getLocation().y % 128);
            //System.out.println("Bombing self!");
            spawnRobot(RobotType.POLITICIAN, "Bomb", selfLoc, (int) (rc.getInfluence() * 2.0 / 3));
            bombCooldown = 5;
        }*/
        else if (enemyCount > 4 && defenderCount < 2) {
            spawnRobot(RobotType.POLITICIAN, "Troop", new MapLocation(-1, -1), Math.max(17, rc.getInfluence() / 10));
        }
        else if (canSpawnSlanderer && slandCooldown < 0) {
            int slandInfl = Math.max((int) (3 * Math.pow(rc.getInfluence(), 0.75)), (int) (rc.getInfluence() * 2.0/3));
            if (slandInfl < 21) {
                //build muck instead
                spawnRobot(RobotType.MUCKRAKER, "Random", new MapLocation(-1, -1), 1);
            }
            else
            {
                for (int inf : goodSlandererInfluences)
                    if (inf < slandInfl)
                        spawnInfluence = Math.max(spawnInfluence, inf);
                spawnInfluence = Math.max(21, spawnInfluence);
            }
            spawnRobot(RobotType.SLANDERER, "Random", new MapLocation(-1, -1), spawnInfluence);
            slandCooldown = 10;
        }
        else if (rng.nextDouble() < Math.pow(rc.getInfluence(), 0.25) / 10.0) {
            //randomized spawn code
            //note we're not going to spawn slanderers in this state
            //System.out.println("Poli spawn prob: " + (0.35 + slandScore / 40.0));
            toSpawn = (rng.nextDouble() < Math.min(0.75, 0.35 + slandScore / 40.0)) ? RobotType.POLITICIAN : RobotType.MUCKRAKER;

            if (toSpawn == RobotType.POLITICIAN) {
                if (rng.nextDouble() < 0.5 && bombCooldown < 0 && targetECLoc.x != -1 && targetECLoc.y != -1 
                    && rc.getInfluence() > targetECInfl + 30) {
                    spawnType = "Bomb";
                    spawnTarget = targetECLoc;
                    spawnInfluence = targetECInfl + 30;
                    bombCooldown = 10;              //wait before spawning more bombs
                }
                else {
                    spawnType = "Troop";
                    spawnInfluence = rc.getInfluence() / 10;
                    if (spawnInfluence < 17)
                        spawnInfluence = 0;
                }
            }
            else if (toSpawn == RobotType.MUCKRAKER) {
                int numFoundEnemy = 0;
                for (int i = 0; i < enemyECLoc.length; i++) {
                    if (enemyECLoc[i].x != -1 && enemyECLoc[i].y != -1)
                        numFoundEnemy += 1;
                }
                if (numFoundEnemy > 0 && rng.nextDouble() < 0.7) {
                    //send muck to enemy EC
                    spawnType = "Targeting";
                    spawnInfluence = 1;
                    int temp = rng.nextInt(numFoundEnemy);
                    for (int i = 0; i < enemyECLoc.length; i++) {
                        if (enemyECLoc[i].x != -1 && enemyECLoc[i].y != -1) {
                            if (temp == 0) {
                                //send muckracker to this location
                                spawnTarget = enemyECLoc[i];
                                break;
                            }
                            else
                                temp -= 1;
                        }
                    }
                }
                else {
                    spawnType = "Random";
                    spawnInfluence = 1;
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

            //calculate centroid of enemy ECs for spawn direction purposes
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
                //System.out.println("Num enemy: " + numEnemy);
                //System.out.println("Centroid: " + centroid);
                //System.out.println("Optimal sland spawn is " + oppositeDir(rc.getLocation().directionTo(centroid)));
                //System.out.println("Optimal troop spawn is " + rc.getLocation().directionTo(centroid));
            }


            int randDirection = rng.nextInt(8);             //we will use this if we give robot a random direction
            if (toSpawn == RobotType.POLITICIAN) {
                if (spawnType.equals("Bomb")) {
                    //try to attack random neutral EC
                    if(rc.canSetFlag(encodeInstruction(Flag.OPCODE.BOMB, spawnTarget.x, spawnTarget.y, 0))) { //op, xcoord, ycoord, of location to seek
                        rc.setFlag(encodeInstruction(Flag.OPCODE.BOMB, spawnTarget.x, spawnTarget.y, 0));
                        MapLocation absLoc = getAbsCoords(spawnTarget);
                        System.out.println("Sending bomb to " + targetECTeam + " EC at " + absLoc.x + " " + absLoc.y);
                    }
                }
                else {
                    if(rc.canSetFlag(encodeInstruction(Flag.OPCODE.TROOP, 0, 0, randDirection))) //op, xcoord, ycoord, direction to patrol
                        rc.setFlag(encodeInstruction(Flag.OPCODE.TROOP, 0, 0, randDirection));
                }
            }
            else if (toSpawn == RobotType.SLANDERER) {
                //don't need to broadcast flag codes to sland
                //instead we try to find optimal spawn location
                if (numEnemy != 0) {
                    //if we do find enemies, spawn the Sland away from the centroid
                    MapLocation selfLoc = rc.getLocation();
                    randDirection = getDirIdx(oppositeDir(selfLoc.directionTo(centroid)));
                    randDirection = Math.max(randDirection, 0);           //in the rare case that offset somehow is Direction.CENTER
                }
            }
            else if (toSpawn == RobotType.MUCKRAKER) {
                if (spawnType.equals("Targeting")) {
                    if(rc.canSetFlag(encodeInstruction(Flag.OPCODE.MOVE, spawnTarget.x, spawnTarget.y, 0))) //op, xcoord, ycoord of location to go to
                        rc.setFlag(encodeInstruction(Flag.OPCODE.MOVE, spawnTarget.x, spawnTarget.y, 0));
                }
                else {
                    if(rc.canSetFlag(encodeInstruction(Flag.OPCODE.SCOUT, 0, 0, randDirection))) //op, xcoord, ycoord, direction to travel
                        rc.setFlag(encodeInstruction(Flag.OPCODE.SCOUT, 0, 0, randDirection));
                }
            }


            for (int i = 0; i < directions.length; i++) {
                Direction dir = directions[(dirHelper[i] + randDirection + 8) % 8];
                if (rc.canBuildRobot(toSpawn, dir, spawnInfluence)) {
                    rc.buildRobot(toSpawn, dir, spawnInfluence);
                    if (toSpawn == RobotType.POLITICIAN || toSpawn == RobotType.MUCKRAKER) {      //we can make more than 100 troops but only keep track of this many
                        RobotInfo rinfo = rc.senseRobotAtLocation(rc.getLocation().add(dir));
                        for (int j = 0; j < unitIDs.length; j++) {
                            if (unitIDs[j] == 0) {
                                unitIDs[j] = rinfo.getID();
                                break;
                            }
                        }
                    }
                    else if (toSpawn == RobotType.SLANDERER) {
                        RobotInfo rinfo = rc.senseRobotAtLocation(rc.getLocation().add(dir));
                        for (int j = 0; j < slandIDs.length; j++) {
                            if (slandIDs[j] == 0) {
                                slandIDs[j] = rinfo.getID();
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

        slandScore = 0;
        for (int i = 0; i < slandIDs.length; i++) {
            int id = slandIDs[i];
            if (rc.canGetFlag(id)) {
                Flag.OPCODE opc = opcode(rc.getFlag(id));
                if (opc == Flag.OPCODE.SLAND)
                    executeInstr(rc.getFlag(id));           //decode sland info
            }
            else
                slandIDs[i] = 0;
        }

        //System.out.println("SlandScore: " + slandScore);

        bid();

        bombCooldown -= 1;
        slandCooldown -= 1;

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
     * Initialize a robot with given parameters
     */
    static void spawnRobot(RobotType rtype, String utype, MapLocation target, int infl) {
        toSpawn = rtype;
        spawnType = utype;
        spawnTarget = target;
        spawnInfluence = infl;
    }

}
