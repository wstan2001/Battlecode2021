package v3;
import battlecode.common.*;
import jdk.nashorn.internal.runtime.arrays.IntElements;

import java.util.List;
import java.util.Random;
import java.lang.Math;
import java.util.*;

public strictfp class RobotPlayer {
    static RobotController rc;
    static Team ally;
    static Team enemy;
    
    static final RobotType[] spawnableRobot = {
        RobotType.POLITICIAN,
        RobotType.SLANDERER,
        RobotType.MUCKRAKER,
    };

    static final Direction[] directions = {
        Direction.NORTH,
        Direction.NORTHEAST,
        Direction.EAST,
        Direction.SOUTHEAST,
        Direction.SOUTH,
        Direction.SOUTHWEST,
        Direction.WEST,
        Direction.NORTHWEST,
    };

    static int getDirIdx(Direction dir) {
        switch (dir) {
            case NORTH:
                return 0;
            case NORTHEAST:
                return 1;
            case EAST:
                return 2;
            case SOUTHEAST:
                return 3;
            case SOUTH:
                return 4;
            case SOUTHWEST:
                return 5;
            case WEST:
                return 6;
            case NORTHWEST:
                return 7;
        }
        return -1;
    }

    static final int[] dirHelper = {0, 1, -1, 2, -2, 3, -3, 4};         //help with pathing

    public enum OPCODE {
    	MOVE, //move to specified location   [0(4) | x-coord(6) | y-coord (6) | random data (8)]
    	SCOUT, //tells units to scout [1(4) | random data (20)]
        TROOP,  //general infantry politician
        ENEMYEC, // units sending coordinates of important locations [5(4) | x_coord (6) | y_coord (6) | data (8)] data will depend on information it sends
        NEUTRALEC,
        ALLYEC,
    	INVALID
    	//example: let's say we want to tell a unit to move to (15, 16). 15 = 001111 and 16 = 010000, the move opcode is 0000, and the data is 8 random bits. So a valid opcode for this would be
    	//0000 | 001111 | 010000 | 10101010 = 245913.
    	//a scout opcode would be something like 0001 | 10101010101010101010, since we don't care about the last 20 bits.
    } 

    static int turnCount = 0;       //each EC needs to keep track of this since their creation might happen at anytime
    static String phase = "Early";
    static int ecID = 0;
    static Random rng = new Random();

    //EC state variables
    static int[] unitIDs = new int[40];        //keep track of IDs of units that will broadcast info; each turn, EC on processes batches of 20

    static MapLocation targetNeutralLoc = new MapLocation(-1, -1);
    static int targetNeutralCost = 999999;             //influence + 0.5 * distance^2, prioritize lower cost ECs

    static MapLocation[] enemyECLoc = {new MapLocation(-1, -1), new MapLocation(-1, -1), new MapLocation(-1, -1), new MapLocation(-1, -1), new MapLocation(-1, -1), new MapLocation(-1, -1)};       // store normalized coordinates of enemy EC
    static MapLocation[] neutralECLoc = {new MapLocation(-1, -1), new MapLocation(-1, -1), new MapLocation(-1, -1), new MapLocation(-1, -1), new MapLocation(-1, -1), new MapLocation(-1, -1)};      // store normalized coordinates of enemy EC
    static MapLocation[] allyECLoc = {new MapLocation(-1, -1), new MapLocation(-1, -1), new MapLocation(-1, -1), new MapLocation(-1, -1), new MapLocation(-1, -1), new MapLocation(-1, -1)};       // store normalized coordinates of enemy EC

    //Politician state variables
    static int generalDir = -1;       //the general direction robot should move in, int from 0 to 7
    static int stuckCounter = 0;        //if this begins to build up, switch directions
    static MapLocation targetLoc = new MapLocation(-1,-1);
    static String type = "None";
    static boolean wandering = false;

    //Muckraker state vars
    static int curTarget = -1;

    static int opcodeToInt(OPCODE op) //need to be changed when new opcode added
    {
    	switch(op)
    	{
    	    case MOVE: return 0;
    	    case SCOUT: return 1;
            case TROOP: return 2;                   //IMPORTANT! When initializing a troop that goes to a target location, make sure its last 8 bits of extra data is > 7
            case ENEMYEC: return 3;                 //data should contain floor of (enemy EC's influence / 20)
            case NEUTRALEC: return 4;
            case ALLYEC: return 5;
    	}
    	return -1;
    }
    static OPCODE intToOpcode(int op) //need to be changed when new opcode added
    {
    	switch(op)
    	{
    	    case 0: return OPCODE.MOVE;
    	    case 1: return OPCODE.SCOUT;
            case 2: return OPCODE.TROOP;
            case 3: return OPCODE.ENEMYEC;
            case 4: return OPCODE.NEUTRALEC;
            case 5: return OPCODE.ALLYEC;
    	}
    	return OPCODE.INVALID;
    }
    static int encodeInstruction(OPCODE op, int x, int y, int data) //MOVE, SCOUT, NEEDECID, etc
    {
    	if(opcodeToInt(op) == -1)
    	    System.out.println("INVALID OPCODE RECEIVED");
        return (opcodeToInt(op) << 20) + (x << 13) + (y << 6) + data;
    }
    static OPCODE opcode(int instr)
    {
    	if(intToOpcode((instr >> 20) % 16) == OPCODE.INVALID)
    	    System.out.println("INVALID INSTRUCTION RECEIVED");
        return intToOpcode((instr >> 20) % 16);
    }
    static int instrX(int instr)
    {
        return (instr >> 13) % 128;
    }
    static int instrY(int instr)
    {
        return (instr >> 6) % 128;
    }
    static int instrData(int instr)
    {
        return instr % 64;
    }
    static int getECID()
    {
        for (RobotInfo ri : rc.senseNearbyRobots(-1, ally))
            if(ri.type == RobotType.ENLIGHTENMENT_CENTER)
                return ri.ID;
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
        OPCODE op = opcode(instr);
        switch(op)
        {
            case MOVE: targetLoc = new MapLocation(instrX(instr), instrY(instr)); break;
            case SCOUT: generalDir = instrData(instr); type = "Scout"; break;
            case TROOP: 
                type = "Troop";
                if (instrData(instr) > 7)       //this troop has a target location
                    targetLoc = new MapLocation(instrX(instr), instrY(instr));
                else 
                    generalDir = instrData(instr);
                break;
            case ENEMYEC: processEnemyEC(instrX(instr), instrY(instr), instrData(instr)); break;
            case NEUTRALEC: processNeutralEC(instrX(instr), instrY(instr), instrData(instr)); break;
            case ALLYEC: processAllyEC(instrX(instr), instrY(instr), instrData(instr)); break;
            default: break;
        }
    }

    static MapLocation getAbsCoords(MapLocation loc) throws GameActionException {
        //given mod 128 coordinates, return the absolute coordinates
        int selfX = rc.getLocation().x % 128;
        int selfY = rc.getLocation().y % 128;

        int dx = loc.x - selfX;
        int dy = loc.y - selfY;

        //figure out x coord first
        if (Math.abs(dx) >= 64) {
            if (dx < 0)
                dx += 128;
            else
                dx -= 128;
        }
        //then y coord
        if (Math.abs(dy) >= 64) {
            if (dy < 0)
                dy += 128;
            else
                dy -= 128;
        }

        if (Math.abs(dx) >= 64 || Math.abs(dy) >= 64) 
            System.out.println("Something went very wrong when decoding coordinates!");
        
        return new MapLocation(rc.getLocation().x + dx, rc.getLocation().y + dy);
    }

    static int[] biddingActionAmounts = new int[]{1, 2, 3, 4, 5, 6, 8, 10, 12, 14, 16, 18, 20} ;
    static int biddingAmountToAction (int biddingAmount){
        if(biddingAmount >7){
            return (biddingAmount/2 )+ 2;
        }else{
            return  biddingAmount-1;
        }
    }

//    static double rewardForWinningBid (int roundNum){
//        return 10.0;//*Math.exp(0.0015*roundNum);
//    }
//    static final double bidLosingPenalty = 0.1;
//    static double[] biddingActionRewards = new double[]{-1, -2, 7, 6, 5, 4, 2, 0, -2, -4, -6, -8, -10};
//    static int[] biddingActionOccurrences = new int[]{1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1};
//    static int expectedNewInfluenceIfNoBid = 150;
    static int previousNumVotes = 0;
    static int previousBidAmount = 0;
    static double aggression=2;
    static double avgLosingBid=5;
    static int numLosingBids = 1;
    static boolean previousRoundSkipped =false;

    static double getAggressionDecayRate(int roundNum, double proportionVotesNecessary){
        return 0.7 + (roundNum*0.1/1500.0) + Math.max(0.0,Math.min(proportionVotesNecessary-0.45,0.2))/2.0;
    }
    static double getAggressionIncreaseRate(int roundNum, double proportionVotesNecessary){
        return 2 + roundNum*0.5/1500 + Math.max(0.0,Math.min(proportionVotesNecessary-0.45,0.2))*4;
    }
    static int convToIntRandomly(double d){
        int lb = (int) d;
        double prob = d - lb;
        if(prob > rng.nextDouble()){
            return lb + 1;
        }else{
            return lb;
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

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
            }
        }
    }

    static void runEnlightenmentCenter() throws GameActionException {
        RobotType toBuild = RobotType.MUCKRAKER;
        boolean canBuildSlanderer = true;
        Direction buildDir;
        int influence = 1;
        int[] goodSlandererInfluences = new int[]{21, 41, 63, 85, 107, 130, 154, 178, 203, 228};
        // boolean hasSpentInfluence = rc.getInfluence() < expectedNewInfluenceIfNoBid;

        //System.out.println("Conviction: " + rc.getConviction());
        //System.out.println("Influence: " + rc.getInfluence());
        
        int sensorRadius = rc.getType().sensorRadiusSquared;
        for (RobotInfo robot : rc.senseNearbyRobots(sensorRadius, enemy)) {
            if (robot.type == RobotType.MUCKRAKER) {
                //if there are muckrakers nearby, don't build slanderers!
                canBuildSlanderer = false;
                break;
            }
        }

        if (phase.equals("Early")) {
            if (turnCount < 15) 
                toBuild = RobotType.SLANDERER;          //start w/ a couple slands
            else
                toBuild = earlySpawnRobot();

            if(!canBuildSlanderer && toBuild == RobotType.SLANDERER)
                toBuild = Math.random() < 0.25 ? RobotType.POLITICIAN : RobotType.MUCKRAKER;

            if (turnCount > 50)
                phase = "Mid";
        }
        else if (phase.equals( "Mid") ){
            toBuild = midSpawnRobot();
            if(!canBuildSlanderer && toBuild == RobotType.SLANDERER)
                toBuild = Math.random() < 0.25 ? RobotType.POLITICIAN : RobotType.MUCKRAKER;
        }

        if (rc.isReady() && rng.nextDouble() < Math.pow(rc.getInfluence(), 0.3) / 10.0) {                 
            //don't remove the above if! Else instructions will get all mixed up bc we spawn too frequently
            //focus on growth; we can afford to spawn more when our influence is higher
            int randDirection = rng.nextInt(8);             //we will use this if we give robot a random direction
            if (toBuild == RobotType.POLITICIAN) {
                influence = rc.getInfluence() / 10;
                if (influence < 13)
                    influence = 1;
                int poliType = rng.nextInt(2);
                if(rc.canSetFlag(encodeInstruction(OPCODE.TROOP, 0, 0, randDirection))) //op, xcoord, ycoord, direction to patrol
                    rc.setFlag(encodeInstruction(OPCODE.TROOP, 0, 0, randDirection));
                if (poliType == 0 && targetNeutralLoc.x != -1 && targetNeutralLoc.y != -1 && rc.getInfluence() > 90) {
                    //try to attack random neutral EC
                    if(rc.canSetFlag(encodeInstruction(OPCODE.TROOP, targetNeutralLoc.x, targetNeutralLoc.y, 9))) { //op, xcoord, ycoord, of location to seek
                        rc.setFlag(encodeInstruction(OPCODE.TROOP, targetNeutralLoc.x, targetNeutralLoc.y, 9));
                        influence = rc.getInfluence() / 3;
                        MapLocation absLoc = getAbsCoords(targetNeutralLoc);
                        System.out.println("Sending troop to neutral EC at " + absLoc.x + " " + absLoc.y);
                    }
                }
            }
            else if (toBuild == RobotType.SLANDERER) {
                int slandInfl = (int) (6 * Math.log(rc.getInfluence()));
                slandInfl /= 21;
                slandInfl *= 21;
                if (slandInfl < 21) {
                    //build muck instead
                    toBuild = RobotType.MUCKRAKER;
                    if(rc.canSetFlag(encodeInstruction(OPCODE.SCOUT, 0, 0, randDirection))) //op, xcoord, ycoord, direction to travel
                        rc.setFlag(encodeInstruction(OPCODE.SCOUT, 0, 0, randDirection));
                    influence = 1;
                }
                else
                {
                    for (int inf : goodSlandererInfluences)
                        if (inf < slandInfl)
                            influence = Math.max(influence, inf);
                    influence = Math.max(21, slandInfl);
                }
            }
            else if (toBuild == RobotType.MUCKRAKER) {
                influence = 1;
                //begin by assuming we move in random direction
                if(rc.canSetFlag(encodeInstruction(OPCODE.SCOUT, 0, 0, randDirection))) //op, xcoord, ycoord, direction to travel
                    rc.setFlag(encodeInstruction(OPCODE.SCOUT, 0, 0, randDirection));
                int temp = rng.nextInt(2);
                if (temp == 0) {
                    //go to enemyEC
                    int numFoundEnemy = 0;
                    for (int i = 0; i < enemyECLoc.length; i++) {
                        if (enemyECLoc[i].x != -1 && enemyECLoc[i].y != -1)
                            numFoundEnemy += 1;
                    }
                    if (numFoundEnemy > 0) {
                        temp = rng.nextInt(numFoundEnemy);
                        for (int i = 0; i < enemyECLoc.length; i++) {
                            if (enemyECLoc[i].x != -1 && enemyECLoc[i].y != -1) {
                                if (temp == 0) {
                                    //send muckracker to this location
                                    MapLocation loc = enemyECLoc[i];
                                    if(rc.canSetFlag(encodeInstruction(OPCODE.MOVE, loc.x, loc.y, 0))) //op, xcoord, ycoord of location to go to
                                        rc.setFlag(encodeInstruction(OPCODE.MOVE, loc.x, loc.y, 0));
                                    break;
                                }
                                else
                                    temp -= 1;
                            }
                        }
                    }
                }
            }

            if (toBuild == RobotType.SLANDERER) {         //try to find optimal slander spawn direction
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
                OPCODE opc = opcode(rc.getFlag(id));
                if (opc == OPCODE.ENEMYEC || opc == OPCODE.NEUTRALEC || opc == OPCODE.ALLYEC)
                    executeInstr(rc.getFlag(id));           //decode scout info
            }
            else
                unitIDs[i] = 0;
        }

        int numTeamVotes = rc.getTeamVotes();
        int roundNumber = rc.getRoundNum();
        int influenceLeft = rc.getInfluence();
        double proportionVotesNecessary = (751.0-numTeamVotes)/(1500.01-roundNumber);
        boolean hasWonBid = numTeamVotes > previousNumVotes;
        if(!previousRoundSkipped) {
            if (hasWonBid) {
                //System.out.println("Won, start with: "+ aggression);
                aggression *= getAggressionDecayRate(roundNumber, proportionVotesNecessary);
                //System.out.println("Won, end with: "+ aggression);
            } else {
                avgLosingBid = (avgLosingBid * ((double) numLosingBids) / (numLosingBids + 1.0)) + previousBidAmount / (numLosingBids + 1.0);
                numLosingBids++;
                //System.out.println("Lost, start with: "+ aggression);
                aggression += getAggressionIncreaseRate(roundNumber, proportionVotesNecessary);
                //System.out.println("Lost, end with: "+ aggression);
            }
        }
        // we want at 0.8+, bid every turn, at 0.5 bid frequently, at 0.3- bid sometimes, 0.1
        double minBidFrequency = ((double)roundNumber*roundNumber/0.8e7)+0.25;
        double biddingFrequency = minBidFrequency+Math.min(0.8,proportionVotesNecessary)*(1.0/0.8)*(1-minBidFrequency);
        double exceedingFactor = (Math.min(200,Math.max(100,aggression))/100)- 1;
        boolean shouldSkip = rng.nextDouble() <= exceedingFactor*exceedingFactor || rng.nextDouble() > biddingFrequency;
        int bidAmount;
        if(shouldSkip){
            previousRoundSkipped = true;
            bidAmount = 0;
        }else {
            if (influenceLeft < 100) { // slow down bro leave some for the others
                previousRoundSkipped = true;
                bidAmount = 0;
            } 
            else if (aggression > 0.3 * influenceLeft) {
                previousRoundSkipped = true;
                bidAmount = 0;
                aggression *= 0.8;
            }
            else {
                previousRoundSkipped =false;
                bidAmount = convToIntRandomly(aggression);
            }
        }
        previousNumVotes = numTeamVotes;

        previousBidAmount = bidAmount;
        if(rc.canBid(bidAmount) && numTeamVotes < 751) {
            rc.bid(bidAmount);
        }

        turnCount += 1;

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
        else {
            //for som reason this politician doesn't have a type, maybe bc it was ex-Slanderer. Make it a troop
            type = "Troop";
            generalDir = rng.nextInt(8);
        }        
    }

    static void runTroop() throws GameActionException {
        int actionRadius = rc.getType().actionRadiusSquared;
        Team enemy = rc.getTeam().opponent();
        final int senseRadius = 25;
        final double baseCooldown = 1.0;

        /*System.out.println("General Direction: " + generalDir);
        System.out.println("Wandering: " + wandering);
        System.out.println("Target: " + targetLoc.x + " " + targetLoc.y);*/


        rc.setFlag(0);              //don't send outdated info
        scanForEC();

        if (!wandering && rng.nextDouble() < 0.01) {
            //any troop has a 1% chance of beginning to move in random directions
            wandering = true;
        } 
        if (rc.getInfluence() > 10) {
            boolean shouldEmpower = false;
            RobotInfo[] attackable = rc.senseNearbyRobots(actionRadius, enemy);
            RobotInfo[] neutralEC = rc.senseNearbyRobots(actionRadius, Team.NEUTRAL);
            if (neutralEC.length > 0) {
                System.out.println("Found neutral EC!");
            }
            //it might be worth it for a troop who is targeting a neutral EC to not get "distracted" and empower along the way
            for (int i = 0; i < attackable.length; i++) {
                if (attackable[i].getType() == RobotType.ENLIGHTENMENT_CENTER)
                    shouldEmpower = true;
            }
            if (attackable.length > 3 || neutralEC.length > 0)
                shouldEmpower = true;
            if (shouldEmpower && rc.canEmpower(actionRadius)) {
                rc.empower(actionRadius);                
            }
        }
        if (generalDir != -1) { 
            if (wandering)
                moveDir(directions[rng.nextInt(8)]);
            else
                moveDir(directions[generalDir]);
        }
        else if (targetLoc.x != -1 && targetLoc.y != -1) {
            //sometimes there will be extra troops sent to an already captured neutral EC
            //we don't want these troops to clog the EC
            MapLocation absLoc = getAbsCoords(targetLoc);
            if (rc.canSenseLocation(absLoc)) {
                RobotInfo rinfo = rc.senseRobotAtLocation(absLoc);
                if (rinfo != null && rinfo.getTeam() == rc.getTeam() && rinfo.getType() == RobotType.ENLIGHTENMENT_CENTER) {
                    //don't crowd the ally EC
                    //System.out.println("I'm gonna stop crowding!");
                    targetLoc = new MapLocation(-1, -1);
                    generalDir = rng.nextInt(8);
                }
            }
            moveToLoc(targetLoc);
        }
        else 
            generalDir = rng.nextInt(8);

    }

    static void runSlanderer() throws GameActionException {
        int sensorRadius = rc.getType().sensorRadiusSquared;
        Team ally = rc.getTeam();
        Team enemy = ally.opponent();

        RobotInfo[] nearbyEnemy = rc.senseNearbyRobots(sensorRadius, enemy);
        MapLocation selfLoc = rc.getLocation();
        MapLocation closestMuck = new MapLocation(-1, -1);
        for (RobotInfo rinfo : nearbyEnemy) {
            if (rinfo.getType() == RobotType.MUCKRAKER) {
                if (closestMuck.equals(new MapLocation(-1, -1)) || selfLoc.distanceSquaredTo(rinfo.getLocation()) < selfLoc.distanceSquaredTo(closestMuck))
                    closestMuck = rinfo.getLocation();
            }
        }

        if (!closestMuck.equals(new MapLocation(-1, -1))) {
            //if there is a muckraker nearby
            Direction away = oppositeDir(selfLoc.directionTo(closestMuck));
            moveSlander(away);
        }
        else {
            Direction away = Direction.CENTER;
            RobotInfo[] nearbyAlly = rc.senseNearbyRobots(5, ally);
            for (RobotInfo rinfo : nearbyAlly) {
                if (rinfo.getType() == RobotType.ENLIGHTENMENT_CENTER) {
                    //don't clog own EC
                    away = oppositeDir(selfLoc.directionTo(rinfo.getLocation()));
                    break;
                }
            }
            if (away != Direction.CENTER)
                moveSlander(away);
            else if (rng.nextDouble() < 0.2)
                moveSlander(directions[rng.nextInt(8)]);
            //slanderers generally don't move unless too close to EC or muckraker nearby
        }
    }
    
    static void runMuckraker() throws GameActionException {
        Team enemy = rc.getTeam().opponent();
        int sensorRadius = rc.getType().sensorRadiusSquared;
        MapLocation curLoc = rc.getLocation();

        if (!wandering && rng.nextDouble() < 0.01) {
            //any muck has a 1% chance of beginning to move in random directions
            wandering = true;
        } 

        rc.setFlag(0);              //don't send outdated info
        scanForEC();

        if(ecID == 0) {
            ecID = getECID();
            int flag = getECFlag();
            if (flag != 0) {
                executeInstr(flag);
            }
        }

        for (RobotInfo robot : rc.senseNearbyRobots(sensorRadius, enemy)) {
            if (robot.type.canBeExposed()) {
                // It's a slanderer... go get them!
                if (rc.canExpose(robot.location)) {
                    rc.expose(robot.location);
                    return;
                }
                else {
                    //decide whether to make it a new target
                    if (!rc.canSenseRobot(curTarget) || 
                        curLoc.distanceSquaredTo(robot.getLocation()) < curLoc.distanceSquaredTo(rc.senseRobot(curTarget).getLocation())) {
                        curTarget = robot.getID();
                    }
                }
            }
        }

        //try to chase current target, if exists
        if (!chase(curTarget)) {
            curTarget = -1;
            //try to find a current target or move to location
            if (generalDir != -1) {
                if (wandering)
                    moveDir(directions[rng.nextInt(8)]);
                else
                    moveDir(directions[generalDir]);
            }
            else if (targetLoc.x != -1 && targetLoc.y != -1)
                moveToLoc(targetLoc);
        }
    }

    /**
     * Returns a random Direction.
     *
     * @return a random Direction
     */
    static Direction randomDirection() {
        return directions[(int) (Math.random() * directions.length)];
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

    /**
     * Returns a random spawnable RobotType in the early game
     *
     * @return a random RobotType
     */
    static RobotType earlySpawnRobot() {
        double prob = Math.random();
        if(prob < 0.3){
            return RobotType.SLANDERER;
        }else if(prob < 0.8){
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
        if(prob < 0.20){
            return RobotType.SLANDERER;
        }else if(prob < 0.65){
            return RobotType.MUCKRAKER;
        }else{
            return RobotType.POLITICIAN;
        }
    }


    /**
     * Attempts to move one step toward a given location
     *
     * @param loc The intended location in MOD 128 COORDINATES
     * @throws GameActionException
     */
    static void moveToLoc(MapLocation loc) throws GameActionException {
        MapLocation absLoc = getAbsCoords(loc);          //get absolute coordinates
        MapLocation selfLoc = rc.getLocation();

        Direction dirToLoc = selfLoc.directionTo(absLoc);
        moveDir(dirToLoc);
    }

    /**
     * Makes the robot try to take one step in "roughly" the given direction
     * For example if dir = N, going NE or NW is also acceptable
     * 
     * @param dir is general direction to go in
     * @throws GameActionException
     */
    static void moveDir(Direction dir) throws GameActionException {
        //this statement is important, DON'T DELETE! Else robot might think it's stuck when instead it is on cooldown
        if (!rc.isReady())
            return;

        double baseCooldown = 0.0;
        switch (rc.getType()) {
            case POLITICIAN:
                baseCooldown = 1.0;
                break;
            case SLANDERER:
                baseCooldown = 2.0;
                break;
            case MUCKRAKER:
                baseCooldown = 1.5;
                break;
            case ENLIGHTENMENT_CENTER:
                return;
        }
        int dirIdx = getDirIdx(dir);
        if (dirIdx == -1) {
            return;
        }

        Direction heading = Direction.CENTER;
        MapLocation curloc = rc.getLocation();
        
        double minPenalty = 99999;
        int[] temp = {0, 1, -1};        //move roughly according to general direction
        int ccw = 1;
        if (rng.nextInt(2) == 1)
            ccw = 7;                    //it should be -1, but negative mods are ugly in code

        //don't get too close to boundaries
        if (!rc.onTheMap(curloc.translate(0, 3))) {
            while (generalDir == 0 || generalDir == 1 || generalDir == 7) {
                generalDir += ccw;
                generalDir %= 8;
            }
        }
        if (!rc.onTheMap(curloc.translate(3, 0))) {
            while (generalDir == 1 || generalDir == 2 || generalDir == 3) {
                generalDir += ccw;
                generalDir %= 8;
            }
        }
        if (!rc.onTheMap(curloc.translate(0, -3))) {
            while (generalDir == 3 || generalDir == 4 || generalDir == 5) {
                generalDir += ccw;
                generalDir %= 8;
            }
        }
        if (!rc.onTheMap(curloc.translate(-3, 0))) {
            while (generalDir == 5 || generalDir == 6 || generalDir == 7) {
                generalDir += ccw;
                generalDir %= 8;
            }
        }

        for (int i : temp) {
            Direction h = directions[(dirIdx + i + 8) % 8];
            if (rc.canSenseLocation(curloc.add(h))) {
                double adjPenalty = baseCooldown / rc.sensePassability(curloc.add(h));
                if (rc.canMove(h) && adjPenalty < minPenalty - 1) {
                    heading = h;
                    minPenalty = adjPenalty;
                }
            }
        }
        if (heading != Direction.CENTER) {
            rc.move(heading);
            stuckCounter = 0;
        }
        else {
            stuckCounter += 1;
            if (stuckCounter > 5) {
                //if we've been stuck for a long time, try changing direction
                //avoids robot traffic
                if (generalDir != -1) {
                    generalDir += 1;
                    generalDir %= 8;
                    //System.out.println("Switching direction to " + directions[generalDir]);
                }
                stuckCounter = 0;
            }
        }
    }


    /**
     * Pretty much same as moveDir but offers wider range of movement (5 possibilities)
     * ignores the existence of generalDir and does not try to "unstick" slanderers
     * @param dir
     * @throws GameActionException
     */
    static void moveSlander(Direction dir) throws GameActionException {
        if (!rc.isReady())
            return;

        double baseCooldown = 2.0;          //slanderer
        int dirIdx = getDirIdx(dir);
        if (dirIdx == -1) {
            return;
        }

        Direction heading = Direction.CENTER;
        MapLocation curloc = rc.getLocation();
        
        double minPenalty = 999999;
        int[] temp = {0, 1, -1, 2, -2};        //move roughly according to general direction

        for (int i : temp) {
            Direction h = directions[(dirIdx + i + 8) % 8];
            if (rc.canSenseLocation(curloc.add(h))) {
                double adjPenalty = baseCooldown / rc.sensePassability(curloc.add(h));
                if (rc.canMove(h) && adjPenalty < minPenalty - Math.abs(i)) {       //prioritize moving in given direction
                    heading = h;
                    minPenalty = adjPenalty;
                }
            }
        }
        if (heading != Direction.CENTER) {
            rc.move(heading);
        }
    }

    /**
     * Makes robot pursue enemy robot with certain ID
     * 
     * @return true if the chase is ongoing
     *         false if the enemy is destroyed or escapes
     */
    static boolean chase(int enemyID) throws GameActionException {
        if (!rc.canSenseRobot(enemyID)) 
            return false;
        
        RobotInfo rinfo = rc.senseRobot(enemyID);
        moveDir(rc.getLocation().directionTo(rinfo.getLocation()));          //take a step towards enemy
        return true;
    }

    static Direction oppositeDir(Direction dir) throws GameActionException {
        if (getDirIdx(dir) == -1)
            return Direction.CENTER;
        else
            return directions[(getDirIdx(dir) + 4) % 8];
    }
}
