package v3a;

import battlecode.common.*;

import java.util.Random;
import static v3a.Flag.*;
import static v3a.Pathing.*;
import static v3a.PoliticianLogic.*;
import static v3a.EnlightenmentCenterLogic.*;
import static v3a.SlandererLogic.*;
import static v3a.MuckrakerLogic.*;

public strictfp class RobotPlayer {
    static RobotController rc;
    static Team ally;
    static Team enemy;

    static int turnCount = 0;       //each EC needs to keep track of this since their creation might happen at anytime
    static String phase = "Early";
    static int ecID = 0;
    static Random rng = new Random();

    //EC state variables
    static int[] unitIDs = new int[40];        //keep track of IDs of units that will broadcast info; each turn, EC on processes batches of 20
    static int[] slandIDs = new int[30];        //keep track of slanderer IDs

    static MapLocation targetNeutralLoc = new MapLocation(-1, -1);
    static int targetNeutralCost = 999999;             //influence + 0.5 * distance^2, prioritize lower cost ECs
    static int bombCooldown = -1;
    static int slandCooldown = -1;
    static int slandScore = 0;              //+1 for every nongenerating sland, +3 for every generating sland

    static MapLocation[] enemyECLoc = {new MapLocation(-1, -1), new MapLocation(-1, -1), new MapLocation(-1, -1), new MapLocation(-1, -1), new MapLocation(-1, -1), new MapLocation(-1, -1)};       // store normalized coordinates of enemy EC
    static MapLocation[] neutralECLoc = {new MapLocation(-1, -1), new MapLocation(-1, -1), new MapLocation(-1, -1), new MapLocation(-1, -1), new MapLocation(-1, -1), new MapLocation(-1, -1)};      // store normalized coordinates of enemy EC
    static MapLocation[] allyECLoc = {new MapLocation(-1, -1), new MapLocation(-1, -1), new MapLocation(-1, -1), new MapLocation(-1, -1), new MapLocation(-1, -1), new MapLocation(-1, -1)};       // store normalized coordinates of enemy EC

    static RobotType toSpawn = RobotType.MUCKRAKER;            //spawn unit parameters
    static String spawnType = "None";
    static MapLocation spawnTarget = new MapLocation(-1, -1);
    static int spawnInfluence = 1;


    //Politician state variables
    static MapLocation homeLoc = new MapLocation(-1, -1);         //this is the location (absolute coords) of the EC that spawned the unit
    static int generalDir = -1;       //the general direction robot should move in, int from 0 to 7
    static int stuckCounter = 0;        //if this begins to build up, switch directions
    static MapLocation targetLoc = new MapLocation(-1,-1);
    static String type = "None";
    static boolean wandering = false;
    static int countdown = -1;          //lets a troop get as close as possible to target location b4 exploding
                                        //troop will only detonate if countdown <= 0 (countdown will be initialized to 3)

    //Muckraker state vars
    static int curTarget = -1;          //can also use this for STALK politicians

    static int getECID()
    {
        for (RobotInfo ri : rc.senseNearbyRobots(-1, ally))
            if(ri.type == RobotType.ENLIGHTENMENT_CENTER) {
                homeLoc = ri.getLocation();
                return ri.ID;
            }
        return 0; //could not find EC
    }
    static int getECFlag() throws GameActionException 
    {
        if(rc.canGetFlag(ecID))
        {
            return rc.getFlag(ecID);
        }
        return 0;
    }

    static void executeInstr(int instr) throws GameActionException
    {
        Flag.OPCODE op = opcode(instr);
        switch(op)
        {
            case MOVE: targetLoc = new MapLocation(instrX(instr), instrY(instr)); break;
            case SCOUT: generalDir = instrData(instr); type = "Scout"; break;
            case TROOP: 
                type = "Troop";
                generalDir = instrData(instr);
                break;
            case ENEMYEC: processEnemyEC(instrX(instr), instrY(instr), instrData(instr)); break;
            case NEUTRALEC: processNeutralEC(instrX(instr), instrY(instr), instrData(instr)); break;
            case ALLYEC: processAllyEC(instrX(instr), instrY(instr), instrData(instr)); break;
            case BOMB:
                type = "Bomb";
                targetLoc = new MapLocation(instrX(instr), instrY(instr));
                countdown = 3;
                break;
            case SLAND: processSland(instr % (1 << 20));
            default: break;
        }
    }


    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {

        // This is the RobotController object. You use it to perform actions from this robot,
        // and to get information on its current status.
        RobotPlayer.rc = rc;
        ally = rc.getTeam();
        enemy = rc.getTeam().opponent();


        while (true) {
            // Try/catch blocks stop unhandled exceptions, which cause your robot to freeze
            try {
                // Here, we've separated the controls into a different method for each RobotType.
                // You may rewrite this into your own control structure if you wish.
                switch (rc.getType()) {
                    case ENLIGHTENMENT_CENTER: runEnlightenmentCenter(); break;
                    case POLITICIAN:           runPolitician();          break;
                    case SLANDERER:            runSlanderer();           break;
                    case MUCKRAKER:            runMuckraker();           break;
                }

                turnCount += 1;

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
            }
        }
    }

    /**
     * x and y are mod 128 coords of EC
     * data is the EC's influence / 20
     */
    static void processEnemyEC(int x, int y, int data) throws GameActionException {
        MapLocation loc = new MapLocation(x, y);

        if (loc.equals(targetNeutralLoc)) {
            //reset target neutral
            targetNeutralLoc = new MapLocation(-1, -1);
            targetNeutralCost = 999999;
        }

        //System.out.println("Found enemy EC at " + x + " " + y + " with influence " + data * 20);

        //remove the loc if it already exists
        for (int i = 0; i < enemyECLoc.length; i++) {           //first remove the location from enemyECLoc if it exists there already
            if (loc.equals(enemyECLoc[i])) {
                enemyECLoc[i] = new MapLocation(-1, -1);
                break;
            }
        }
        for (int i = 0; i < neutralECLoc.length; i++) {         //remove from neutralECLoc
            if (loc.equals(neutralECLoc[i])) {
                neutralECLoc[i] = new MapLocation(-1, -1);
                break;
            }
        }
        for (int i = 0; i < allyECLoc.length; i++) {         //remove from allyECLoc
            if (loc.equals(allyECLoc[i])) {
                allyECLoc[i] = new MapLocation(-1, -1);
                break;
            }
        }

        //then add it to the correct location
        for (int i = 0; i < enemyECLoc.length; i++) {           //add the location to enemyECLoc
            if (enemyECLoc[i].x == -1 && enemyECLoc[i].y == -1) {
                enemyECLoc[i] = loc;
                break;
            }
        }
    }

    /**
     * x and y are mod 128 coords of EC
     * data is the EC's influence / 20
     */
    static void processNeutralEC(int x, int y, int data) throws GameActionException {
        MapLocation loc = new MapLocation(x, y);
        MapLocation absLoc = getAbsCoords(loc);
        MapLocation selfLoc = rc.getLocation();

        int cost = (int) (0.5 * selfLoc.distanceSquaredTo(absLoc) + data * 20);
        if (cost < targetNeutralCost) {
            targetNeutralLoc = loc;
            targetNeutralCost = cost;
        }

        //System.out.println("Found neutral EC at " + x + " " + y + " with influence " + data * 20);

        //remove the loc if it already exists
        for (int i = 0; i < enemyECLoc.length; i++) {           //first remove the location from enemyECLoc if it exists there already
            if (loc.equals(enemyECLoc[i])) {
                enemyECLoc[i] = new MapLocation(-1, -1);
                break;
            }
        }
        for (int i = 0; i < neutralECLoc.length; i++) {         //remove from neutralECLoc
            if (loc.equals(neutralECLoc[i])) {
                neutralECLoc[i] = new MapLocation(-1, -1);
                break;
            }
        }
        for (int i = 0; i < allyECLoc.length; i++) {         //remove from allyECLoc
            if (loc.equals(allyECLoc[i])) {
                allyECLoc[i] = new MapLocation(-1, -1);
                break;
            }
        }

        //then add it to the correct location
        for (int i = 0; i < neutralECLoc.length; i++) {           //add the location to neutralECLoc
            if (neutralECLoc[i].x == -1 && neutralECLoc[i].y == -1) {
                neutralECLoc[i] = loc;
                break;
            }
        }
    }

    /**
     * x and y are mod 128 coords of EC
     * data is the EC's influence / 20
     */
    static void processAllyEC(int x, int y, int data) throws GameActionException {
        MapLocation loc = new MapLocation(x, y);

        if (loc.equals(targetNeutralLoc)) {
            //reset target neutral
            targetNeutralLoc = new MapLocation(-1, -1);
            targetNeutralCost = 999999;
        }

        //System.out.println("Found ally EC at " + x + " " + y + " with influence " + data * 20);

        //remove the loc if it already exists
        for (int i = 0; i < enemyECLoc.length; i++) {           //first remove the location from enemyECLoc if it exists there already
            if (loc.equals(enemyECLoc[i])) {
                enemyECLoc[i] = new MapLocation(-1, -1);
                break;
            }
        }
        for (int i = 0; i < neutralECLoc.length; i++) {         //remove from neutralECLoc
            if (loc.equals(neutralECLoc[i])) {
                neutralECLoc[i] = new MapLocation(-1, -1);
                break;
            }
        }
        for (int i = 0; i < allyECLoc.length; i++) {         //remove from allyECLoc
            if (loc.equals(allyECLoc[i])) {
                allyECLoc[i] = new MapLocation(-1, -1);
                break;
            }
        }

        //then add it to the correct location
        for (int i = 0; i < allyECLoc.length; i++) {           //add the location to allyECLoc
            if (allyECLoc[i].x == -1 && allyECLoc[i].y == -1) {
                allyECLoc[i] = loc;
                break;
            }
        }
    }

    /**
     * Calculate slandScore
     */
    static void processSland(int roundNum) throws GameActionException {
        if (rc.getRoundNum() - roundNum < 50)
            slandScore += 3;
        else if (rc.getRoundNum() - roundNum < 300) 
            slandScore += 1;
    }


    /**
     *  Robot will scan for EC and send location info via flag
     *
     */
    static void scanForEC() throws GameActionException {
        int sensorRadius = rc.getType().sensorRadiusSquared;
        MapLocation selfLoc = rc.getLocation();

        //return intelligence about closest EC
        RobotInfo[] nearbyRobots = rc.senseNearbyRobots(rc.getLocation(), sensorRadius, null);             //look for ECs
        MapLocation ECLoc = new MapLocation(-1, -1);
        Team ECTeam = Team.NEUTRAL;
        int ECInfl = -1;
        for (RobotInfo rinfo : nearbyRobots) {
            if (rinfo.type == RobotType.ENLIGHTENMENT_CENTER) {
                if (ECLoc.equals(new MapLocation(-1, -1)) || selfLoc.distanceSquaredTo(rinfo.getLocation()) < selfLoc.distanceSquaredTo(ECLoc)) {
                    //if we find a closer EC
                    ECLoc = rinfo.getLocation();
                    ECTeam = rinfo.team;
                    ECInfl = rinfo.getInfluence() / 20;
                    ECInfl = Math.min(ECInfl, 63);
                }
            }
        }
        if (ECLoc.equals(new MapLocation(-1, -1)))          //we ain't found jack
            return;
        if (ECTeam == rc.getTeam().opponent()) {
            //discovered an enemy EC
            //System.out.println("Found enemy EC at " + ECLoc.x + " " + ECLoc.y);
            if (rc.canSetFlag(encodeInstruction(OPCODE.ENEMYEC, ECLoc.x % 128, ECLoc.y % 128, ECInfl))) {//op, xcoord, ycoord, data 
                rc.setFlag(encodeInstruction(OPCODE.ENEMYEC, ECLoc.x % 128, ECLoc.y % 128, ECInfl));
            }
        }
        else if (ECTeam == Team.NEUTRAL) {
            //System.out.println("Found neutral EC at " + ECLoc.x + " " + ECLoc.y);
            if (rc.canSetFlag(encodeInstruction(OPCODE.NEUTRALEC, ECLoc.x % 128, ECLoc.y % 128, ECInfl))) {//op, xcoord, ycoord, data 
                rc.setFlag(encodeInstruction(OPCODE.NEUTRALEC, ECLoc.x % 128, ECLoc.y % 128, ECInfl));
            }
        }
        else if (ECTeam == rc.getTeam()) {
            //System.out.println("Found ally EC at " + ECLoc.x + " " + ECLoc.y);
            if (rc.canSetFlag(encodeInstruction(OPCODE.ALLYEC, ECLoc.x % 128, ECLoc.y % 128, ECInfl))) {//op, xcoord, ycoord, data 
                rc.setFlag(encodeInstruction(OPCODE.ALLYEC, ECLoc.x % 128, ECLoc.y % 128, ECInfl));
            }
        }
    }


}
