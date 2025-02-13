package v1;
import battlecode.common.*;

import java.awt.*;
import java.util.ArrayList;
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
    	NEEDECID, //units will broadcast this if they don't have an EC ID [2(4) | random data(20)]
        SENDECID, //EC will broadcast this when it broadcasts its ID [3(4) | id (20)]
    	SENDBOUNDARIES, //scouts will broadcast this when it finds a boundary [4(4) | x-coord(6) | y-coord(6) | see google doc for details on last 8 bits]
    	SENDCOORDINATES, // units sending coordinates of important locations [5(4) | x_coord (6) | y_coord (6) | data (8)] data will depend on information it sends
        TROOP,  //general infantry politician
        DEFENDER,  //defensive politician to protect slanderers
    	INVALID;
    	//example: let's say we want to tell a unit to move to (15, 16). 15 = 001111 and 16 = 010000, the move opcode is 0000, and the data is 8 random bits. So a valid opcode for this would be
    	//0000 | 001111 | 010000 | 10101010 = 245913.
    	//a scout opcode would be something like 0001 | 10101010101010101010, since we don't care about the last 20 bits.
    } 

    public enum COORDINFO {
        ENEMYEC, //scout has discovered enemy EC
        NEUTRALEC, //scout has discovered neutral EC
        ALLYEC, //scout has discovered ally EC
        INVALID
    }

    static int turnCount = 0;       //each EC needs to keep track of this since their creation might happen at anytime
    static String phase = "Scouting";
    static int ecID = 0;
    static Random rng = new Random();

    //EC state variables
    static ArrayList<Integer> scoutIDs = new ArrayList<Integer>();        //keep track of scout IDs
    static ArrayList<Integer> troopIDs = new ArrayList<Integer>();        //non scout robot IDs... may want to make this more specific later
    static int xBound0 = -1;       //mod 64 coordinate of left edge
    static int xBound1 = -1;        //mod 64 coordinate of right edge
    static int yBound0 = -1;        //mod 64 coordinate of lower edge
    static int yBound1 = -1;        //mod 64 coordinate of upper edge
    static int xSize = -1;        //xBound1 - xBound0, will be btwn 32 and 64 inc
    static int ySize = -1;        //yBound1 - yBound0, will be btwn 32 and 64 inc

    static List<MapLocation> enemyECLoc = new ArrayList<MapLocation>();             // store normalized coordinates of enemy EC
    static List<MapLocation> neutralECLoc = new ArrayList<MapLocation>();            //store normalized coordinates of neutral EC
    static List<MapLocation> allyECLoc = new ArrayList<MapLocation>();              // store normalized coordinates of ally EC

    static List<Integer> allyECIDs = new ArrayList<Integer>();           //store allied EC IDs for communication


    //Politician state variables
    static boolean foundXBound = false;
    static boolean foundYBound = false;
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
    	    case NEEDECID: return 2;
    	    case SENDECID: return 3;
    	    case SENDBOUNDARIES: return 4;
    	    case SENDCOORDINATES: return 5;
            case TROOP: return 6;                   //IMPORTANT! When initializing a troop that goes to a target location, make sure its last 8 bits of extra data is > 7
            case DEFENDER: return 7;
    	}
    	return -1;
    }
    static OPCODE intToOpcode(int op) //need to be changed when new opcode added
    {
    	switch(op)
    	{
    	    case 0: return OPCODE.MOVE;
    	    case 1: return OPCODE.SCOUT;
    	    case 2: return OPCODE.NEEDECID;
    	    case 3: return OPCODE.SENDECID;
    	    case 4: return OPCODE.SENDBOUNDARIES;
    	    case 5: return OPCODE.SENDCOORDINATES;
            case 6: return OPCODE.TROOP;
            case 7: return OPCODE.DEFENDER;
    	}
    	return OPCODE.INVALID;
    }
    static int coordinfoToInt(COORDINFO cinfo) {
        switch (cinfo) {
            case ENEMYEC: return 0;
            case NEUTRALEC: return 1;
            case ALLYEC: return 2;
        }
        return -1;
    }
    static COORDINFO intToCoordinfo(int x) {
        switch (x) {
            case 0: return COORDINFO.ENEMYEC;
            case 1: return COORDINFO.NEUTRALEC;
            case 2: return COORDINFO.ALLYEC;
        }
        return COORDINFO.INVALID;
    }
    static int encodeInstruction(OPCODE op, int x, int y, int data) //MOVE, SCOUT, NEEDECID, SENDCOORDINATES
    {
    	if(opcodeToInt(op) == -1)
    	    System.out.println("INVALID OPCODE RECEIVED");
        return (opcodeToInt(op) << 20) + (x << 14) + (y << 8) + data;
    }
    static OPCODE opcode(int instr)
    {
    	if(intToOpcode((instr >> 20) % 16) == OPCODE.INVALID)
    	    System.out.println("INVALID INSTRUCTION RECEIVED");
        return intToOpcode((instr >> 20) % 16);
    }
    static int instrX(int instr)
    {
        return (instr >> 14) % 64;
    }
    static int instrY(int instr)
    {
        return (instr >> 8) % 64;
    }
    static int instrData(int instr)
    {
        return instr % 256;
    }
    static int getECID() throws GameActionException 
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
            int flag = rc.getFlag(ecID);
            return flag;
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
            case SENDECID: addAllyECID(instr); break;
            case SENDBOUNDARIES: processBoundary(instrX(instr), instrY(instr), instrData(instr)); break;
            case SENDCOORDINATES: processCoords(instrX(instr), instrY(instr), instrData(instr)); break;
            case TROOP: 
                type = "Troop";
                if (instrData(instr) > 7)       //this troop has a target location
                    targetLoc = new MapLocation(instrX(instr), instrY(instr));
                else 
                    generalDir = instrData(instr);
                break;
            case DEFENDER: type = "Defender"; break;
            default: break;
        }
        return;
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
        RobotType toBuild;
        boolean canBuildSlanderer = true;
        Direction buildDir;
        int influence = 1;

        //System.out.println("Conviction: " + rc.getConviction());
        //System.out.println("Influence: " + rc.getInfluence());
        
        
        int sensorRadius = rc.getType().sensorRadiusSquared;
        for (RobotInfo robot : rc.senseNearbyRobots(sensorRadius, enemy)) {
            if (robot.type == RobotType.MUCKRAKER) {
                //if there are muckrakers nearby, don't build slanderers!
                canBuildSlanderer = false; 
            }
        }
        for (RobotInfo robot : rc.senseNearbyRobots(sensorRadius, ally)) {
            if (opcode(rc.getFlag(robot.getID())) == OPCODE.SENDECID) {
                //get ID of ally EC
                executeInstr(rc.getFlag(robot.getID()));
            }
        }

        if (phase == "Scouting") {
            if (turnCount % 2 == 0 && turnCount % 8 != 0 && scoutIDs.size() < 40) {
                toBuild = RobotType.POLITICIAN;
                int randDirection = rng.nextInt(8);
                if(rc.canSetFlag(encodeInstruction(OPCODE.SCOUT, 0, 0, randDirection))) //op, xcoord, ycoord, direction to scout in
                    rc.setFlag(encodeInstruction(OPCODE.SCOUT, 0, 0, randDirection));
                for (int i : dirHelper) {
                    buildDir = directions[(randDirection + i + 8) % 8];
                    if (rc.canBuildRobot(toBuild, buildDir, influence)) {
                        rc.buildRobot(toBuild, buildDir, influence);
                        RobotInfo rinfo = rc.senseRobotAtLocation(rc.getLocation().add(buildDir));
                        scoutIDs.add(rinfo.getID());
                        break;
                    }
                }
            }
            else if (turnCount % 8 == 0 && rc.getInfluence() >= 100 && canBuildSlanderer) {
                toBuild = RobotType.SLANDERER;
                influence = 21;
                
                for (Direction dir : directions) {
                    if (rc.canBuildRobot(toBuild, dir, influence)) {
                        rc.buildRobot(toBuild, dir, influence);
                    }
                }
            }

            //try to bypass scouting phase if possible
            if (xBound0 == -1 || yBound0 == -1) {
                for (int ECID : allyECIDs) {
                    if (rc.canGetFlag(ECID)) {
                        int allyFlag = rc.getFlag(ECID);
                        if (opcode(allyFlag) == OPCODE.SENDBOUNDARIES) {
                            System.out.println("Reading boundaries from allied EC!");
                            executeInstr(allyFlag);
                            System.out.println(xBound0 + " " + yBound0);
                        }
                    }
                }
            }
            /*if ((xBound0 != -1 && yBound0 != -1) || turnCount > 100)
                phase = "Default";*/
            if (turnCount > 100)
                phase = "Default";          //it might be better to extend scouting phase a bit, since we want to find ECs as well as boundaries
        }
        else if (phase == "Default") {
            if (turnCount % 4 == 0) {
                //  build random robot
                toBuild = randomSpawnableRobotType();
                if(!canBuildSlanderer && toBuild == RobotType.SLANDERER)
                    toBuild = Math.random() < 0.25 ? RobotType.POLITICIAN : RobotType.MUCKRAKER;
                    
                if (toBuild == RobotType.POLITICIAN) {
                    influence = Math.max(20, (int) Math.pow(rc.getInfluence(), 2.0/3));
                    int poliType = rng.nextInt(1);
                    //change the above later. Right now we don't make defenders
                    if (poliType == 0) {
                        //build a troop
                        /*if(rc.canSetFlag(encodeInstruction(OPCODE.TROOP, 62, 62, 9))) //op, xcoord, ycoord, of location to seek
                            rc.setFlag(encodeInstruction(OPCODE.TROOP, 62, 62, 9));*/
                        int randDirection = rng.nextInt(8);
                        if(rc.canSetFlag(encodeInstruction(OPCODE.TROOP, 0, 0, randDirection))) //op, xcoord, ycoord, direction to patrol
                            rc.setFlag(encodeInstruction(OPCODE.TROOP, 0, 0, randDirection));
                    }
                    else if (poliType == 1) {
                        //build a defender
                        if(rc.canSetFlag(encodeInstruction(OPCODE.DEFENDER, 0, 0, 0))) //op, xcoord, ycoord, direction to patrol
                            rc.setFlag(encodeInstruction(OPCODE.DEFENDER, 0, 0, 0));
                    }
                }
                else if (toBuild == RobotType.SLANDERER) {
                    influence = Math.max(21, (int) Math.pow(rc.getInfluence(), 2.0/3));
                }
                else if (toBuild == RobotType.MUCKRAKER) {
                    influence = 1;
                    int temp = rng.nextInt(2);

                    if (temp == 0 && xBound0 != -1 && yBound0 != -1 && enemyECLoc.size() > 0) {
                        //go to enemyEC
                        temp = rng.nextInt(enemyECLoc.size());
                        MapLocation loc = enemyECLoc.get(temp);
                        if(rc.canSetFlag(encodeInstruction(OPCODE.MOVE, loc.x, loc.y, 0))) //op, xcoord, ycoord of location to go to
                            rc.setFlag(encodeInstruction(OPCODE.MOVE, loc.x, loc.y, 0));
                    }
                    else {
                        //move in random direction
                        int randDirection = rng.nextInt(8);
                        if(rc.canSetFlag(encodeInstruction(OPCODE.SCOUT, 0, 0, randDirection))) //op, xcoord, ycoord, direction to travel
                            rc.setFlag(encodeInstruction(OPCODE.SCOUT, 0, 0, randDirection));
                    }
                }

                for (Direction dir : directions) {
                    if (rc.canBuildRobot(toBuild, dir, influence)) {
                        rc.buildRobot(toBuild, dir, influence);
                        if (opcode(rc.getFlag(rc.getID())) == OPCODE.TROOP && troopIDs.size() < 100) {      //we can make more than 100 troops but only keep track of this many
                            RobotInfo rinfo = rc.senseRobotAtLocation(rc.getLocation().add(dir));
                            troopIDs.add(rinfo.getID());
                        }
                        break;
                    }
                }
            }
            else {
                //broadcast map boundaries if possible
                //don't interfere with bot creation commands
                if (turnCount % 4 > 1 && xBound0 != -1 && yBound0 != -1) {
                    if(rc.canSetFlag(encodeInstruction(OPCODE.SENDBOUNDARIES, xBound0, yBound0, (1 << 3) + (1 << 1)))) //send bot left corner coords
                        rc.setFlag(encodeInstruction(OPCODE.SENDBOUNDARIES, xBound0, yBound0, (1 << 3) + (1 << 1)));
                }
            }
        }


        scoutIDs.removeIf(id -> !rc.canGetFlag(id));          //purge dead scouts
        for (Integer id : scoutIDs) {
            if (rc.canGetFlag(id)) {
                OPCODE opc = opcode(rc.getFlag(id));
                if (opc == OPCODE.SENDECID || opc == OPCODE.SENDBOUNDARIES || opc == OPCODE.SENDCOORDINATES)
                    executeInstr(rc.getFlag(id));           //decode scout info
            }
        }

        //if (rc.getID() == 13029)
        //    System.out.println("b4: " + Clock.getBytecodesLeft());
        troopIDs.removeIf(id -> !rc.canGetFlag(id));          //purge dead troops
        //if (rc.getID() == 13029)
        //    System.out.println("after: " + Clock.getBytecodesLeft());
        for (Integer id : troopIDs) {
            if (rc.canGetFlag(id)) {
                OPCODE opc = opcode(rc.getFlag(id));
                if (opc == OPCODE.SENDCOORDINATES || opc == OPCODE.SENDECID)
                    executeInstr(rc.getFlag(id));           //decode troop info
            }
            else
                System.out.println("Weird bug while reading troop flag " + id);
        }

        allyECIDs.removeIf(id -> !rc.canGetFlag(id));         //purge dead allied EC



        double BID_INFLUENCE_RANDOM_UB = 0.02;
        double BID_INFLUENCE_INCOME_LB = 0.1;
        double BID_INFLUENCE_INCOME_UB = 0.5 + rc.getRoundNum()/9000.f;
		int influenceLeft = rc.getInfluence();
        int passiveInfluenceIncome = (int) (Math.ceil(Math.sqrt(rc.getRoundNum())*0.2)+0.5);
		int bidAmount = (int) (influenceLeft * Math.random() * BID_INFLUENCE_RANDOM_UB + passiveInfluenceIncome * Math.random() * (BID_INFLUENCE_INCOME_UB-BID_INFLUENCE_INCOME_LB)+BID_INFLUENCE_INCOME_LB);
		//int bidAmount = (int) (influenceLeft * (rng.nextGaussian() / 50.0 + 0.05));
        bidAmount = Math.min(Math.max(bidAmount,0), (int) (influenceLeft * 0.25));
        if(rc.canBid(bidAmount))
		    rc.bid(bidAmount);
	    
	    //System.out.println("Turn: " + rc.getRoundNum());
        //System.out.println("Current bid: " + bidAmount);
        //System.out.println("Total Votes: " + rc.getTeamVotes());

        turnCount += 1;

        System.out.println("Enemy EC: " + enemyECLoc.size());
        System.out.println("Neutral EC: " + neutralECLoc.size());
        System.out.println("Ally EC: " + allyECLoc.size());

        /*if (xBound0 != -1) 
            System.out.println("xBound0: " + xBound0);
        if (yBound0 != -1) 
            System.out.println("yBound0: " + yBound0);
        if (xBound1 != -1) 
            System.out.println("xBound1: " + xBound1);
        if (yBound1 != -1) 
            System.out.println("yBound1: " + yBound1);*/
        
    }

    static void addAllyECID(int instr) throws GameActionException {
        if (opcode(instr) != OPCODE.SENDECID) {
            System.out.println("Cannot decode ally EC ID messge");
            return;
        }
        int ECID = instr % (1 << 20);
        if (allyECIDs.indexOf(ECID) == -1) {
            allyECIDs.add(ECID);
        }
    }

    static void processBoundary(int x, int y, int data) throws GameActionException {
        if (yBound0 == -1 && ((data >> 1) % 2) == 1 && data % 2 == 0) {
            //System.out.println("discovered bot y " + y);
            yBound0 = y;
        }
        if (xBound0 == -1 && ((data >> 3) % 2) == 1 && ((data >> 2) % 2) == 0) {
            //System.out.println("discovered left x " + x);
            xBound0 = x;
        }
        if (yBound1 == -1 && ((data >> 1) % 2) == 1 && data % 2 == 1) {
            //System.out.println("discovered top y " + y);
            yBound1 = y;
        }
        if (xBound1 == -1 && ((data >> 3) % 2) == 1 && ((data >> 2) % 2) == 1) {
            //System.out.println("discovered right x " + x);
            xBound1 = x;
        }

        //deduce gridSize
        if (ySize == -1 && yBound0 != -1 && yBound1 != -1) {
            int temp = (yBound1 - yBound0 + 1 + 64) % 64;
            if (temp == 0) 
                temp = 64;
            ySize =temp;
        }
        if (xSize == -1 && xBound0 != -1 && xBound1 != -1) {
            int temp = (xBound1 - xBound0 + 1 + 64) % 64;
            if (temp == 0) 
                temp = 64;
            xSize = temp;
        }
    }

    static void processCoords(int x, int y, int data) throws GameActionException {
        //remember that the coordinates we're receiving are not normalized, so if we don't
        //have the boundary coordinates, this command is useless
        if (yBound0 == -1 || xBound0 == -1) 
            return;

        //normalize coordinates
        y = (y - yBound0 + 64) % 64;
        x = (x - xBound0 + 64) % 64;
        switch (intToCoordinfo(data)) {
            case ENEMYEC:
                MapLocation enemyLoc = new MapLocation(x, y);
                if (enemyECLoc.indexOf(enemyLoc) == -1) {               //don't add dupes
                    enemyECLoc.add(enemyLoc);
                }
                neutralECLoc.removeIf(loc -> loc.equals(enemyLoc));         //update other info
                allyECLoc.removeIf(loc -> loc.equals(enemyLoc));         //update other info
                break;
            case NEUTRALEC:
                MapLocation neutralLoc = new MapLocation(x, y);
                if (neutralECLoc.indexOf(neutralLoc) == -1) {               //don't add dupes
                    neutralECLoc.add(neutralLoc);
                }
                enemyECLoc.removeIf(loc -> loc.equals(neutralLoc));         //update other info
                allyECLoc.removeIf(loc -> loc.equals(neutralLoc));         //update other info
                break;
            case ALLYEC:
                MapLocation allyLoc = new MapLocation(x, y);
                if (allyECLoc.indexOf(allyLoc) == -1) {               //don't add dupes
                    allyECLoc.add(allyLoc);
                }
                enemyECLoc.removeIf(loc -> loc.equals(allyLoc));         //update other info
                neutralECLoc.removeIf(loc -> loc.equals(allyLoc));         //update other info
                break;
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
        
        if (type == "Scout") {
            scoutBounds();
        }
        else if (type == "Troop") {
            runTroop();
        }
        else if (type == "Defender") {
            if (rc.getFlag(rc.getID()) == 0)            //display ID of its EC
                rc.setFlag(encodeInstruction(OPCODE.SENDECID, 0, 0, ecID));

            defendSland();
        }
        else {
            //for som reason this politician doesn't have a type, maybe bc it was ex-Slanderer. Make it a troop
            type = "Troop";
            generalDir = rng.nextInt(8);
        }        
    }

    /**
     * Scouts for boundaries as well as EC locations
     *
     */
    static void scoutBounds() throws GameActionException {
        //System.out.println("General Direction: " + directions[generalDir]);

        //check if done exploring
        if (foundXBound && foundYBound) {
            //robot is done exploring
            //System.out.println("Done exploring!")
            //make it go kaboom?
            //return
        
        }

        int newFlag = 0;
        int ybound_mod64 = 0;      //y coord
        int topbotY = 0;           //bit to signify if top or bot boundary found
        int sendY = 0;             //bit to signify if we send y or not
        int xbound_mod64 = 0;      //x coord
        int leftrightX = 0;        //bit to signify if left or right boundary found
        int sendX = 0;              //bit to signify if we send x or not

        

        //sense top y boundary
        int breakpoint = 0;
        for (int i = 1; i <= 5; i++) {
            if (!rc.onTheMap(rc.getLocation().translate(0, i))) {
                breakpoint = i-1;
                break;
            }
        }
        if (breakpoint != 0) {
            //we found top y boundary
            ybound_mod64 = (rc.getLocation().y + breakpoint) % 64;
            if (!foundYBound) {
                sendY = 1;          //we want to broadcast
                topbotY = 1;
                foundYBound = true;
            }
        }

        //sense bot y boundary
        breakpoint = 0;
        for (int i = 1; i <= 5; i++) {
            if (!rc.onTheMap(rc.getLocation().translate(0, i * -1))) {
                breakpoint = i-1;
                break;
            }
        }
        if (breakpoint != 0) {
            //we found bot y boundary
            ybound_mod64 = (rc.getLocation().y + breakpoint * -1) % 64;
            if (!foundYBound) {
                //signal with flag
                sendY = 1;
                topbotY = 0;
                foundYBound = true;
            }
        }

        //sense right x boundary
        breakpoint = 0;
        for (int i = 1; i <= 5; i++) {
            if (!rc.onTheMap(rc.getLocation().translate(i, 0))) {
                breakpoint = i-1;
                break;
            }
        }
        if (breakpoint != 0) {
            //we found right x boundary
            xbound_mod64 = (rc.getLocation().x + breakpoint) % 64;
            if (!foundXBound) {
                sendX = 1;
                leftrightX = 1;
                foundXBound = true;
            }
        }

        //sense left x boundary
        breakpoint = 0;
        for (int i = 1; i <= 5; i++) {
            if (!rc.onTheMap(rc.getLocation().translate(i * -1, 0))) {
                breakpoint = i-1;
                break;
            }
        }
        if (breakpoint != 0) {
            //we found left x boundary
            xbound_mod64 = (rc.getLocation().x + breakpoint * -1) % 64;
            if (!foundXBound) {
                sendX = 1;
                leftrightX = 0;
                foundXBound = true;
            }
        }

        if (sendX == 1 || sendY == 1) {
            //we want to broadcast these coordinates
            int boundData = (sendX << 3) + (leftrightX << 2) + (sendY << 1) + topbotY;
            if(rc.canSetFlag(encodeInstruction(OPCODE.SENDBOUNDARIES, xbound_mod64, ybound_mod64, boundData))) //op, xcoord, ycoord, data
                rc.setFlag(encodeInstruction(OPCODE.SENDBOUNDARIES, xbound_mod64, ybound_mod64, boundData));
        }
        else {
            scanForEC();
        }

        //figure out where to move next
        moveDir(directions[generalDir]);
    }

    static void runTroop() throws GameActionException {
        int actionRadius = rc.getType().actionRadiusSquared;
        Team enemy = rc.getTeam().opponent();
        final int senseRadius = 25;
        final double baseCooldown = 1.0;

        if (!wandering && rng.nextDouble() < 0.05) {
            //any troop has a 5% chance of beginning to move in random directions
            wandering = true;
        } 
        if (rc.getInfluence() > 10) {
            RobotInfo[] attackable = rc.senseNearbyRobots(actionRadius, enemy);
            RobotInfo[] neutralEC = rc.senseNearbyRobots(actionRadius, Team.NEUTRAL);
            if (neutralEC.length > 0) {
                System.out.println("Found neutral EC!");
            }
            if (attackable.length + neutralEC.length > 0 && rc.canEmpower(actionRadius)) {
                rc.empower(actionRadius);                
            }
        }
        if (wandering == true) 
            moveDir(directions[rng.nextInt(8)]);
        else if (generalDir != -1) 
            moveDir(directions[generalDir]);
        else if (targetLoc.x != -1 && targetLoc.y != -1) 
            moveToLoc(targetLoc);
        else 
            generalDir = rng.nextInt(8);

        scanForEC();
        if (rng.nextInt(2) == 0)            //with probability 1/2, broadcast ID of own EC
            rc.setFlag(encodeInstruction(OPCODE.SENDECID, 0, 0, ecID));     //set flag to ECID to signal to other ally ECs
    }

    static void runSlanderer() throws GameActionException {
        int actionRadius = rc.getType().actionRadiusSquared;
        Team ally = rc.getTeam();
        Team enemy = ally.opponent();

        if(ecID == 0) {
            ecID = getECID();
            rc.setFlag(encodeInstruction(OPCODE.SENDECID, 0, 0, ecID));
        }
        
        // senses nearby allies and tries to maintain a fixed distance from them
        // Using the distance 3 for now, may change eventually
        List<RobotInfo> nearbyAllies = new ArrayList<>();
        RobotInfo[] nearbyRobots = rc.senseNearbyRobots();
        for (RobotInfo robot : nearbyRobots) {
            if (robot.team == ally) {
                nearbyAllies.add(robot);
            }
        }
        if (nearbyAllies.size() > 0) {
            moveDir(maintainDistance(nearbyAllies, 3));
        } else {
            // dont move
        }
    }

    static void defendSland() throws GameActionException {
        Team ally = rc.getTeam();
        Team enemy = rc.getTeam().opponent();
        int actionRadius = rc.getType().actionRadiusSquared;

        RobotInfo[] friendly = rc.senseNearbyRobots(actionRadius, ally);
        RobotInfo[] enemylocs = rc.senseNearbyRobots(actionRadius, enemy);
        ArrayList<MapLocation> slandererCoords = new ArrayList<>();
        for (RobotInfo ri : friendly)
        {
            if(ri.type == RobotType.SLANDERER)
                slandererCoords.add(ri.location);
        }
        ArrayList<MapLocation> enemyMuckCoords = new ArrayList<>();
        for (RobotInfo ri : enemylocs)
        {
            if(ri.type == RobotType.MUCKRAKER)
                enemyMuckCoords.add(ri.location);
        }
        int minDist = 9999999;
        for (MapLocation slanderer : slandererCoords)
            for (MapLocation muckraker : enemyMuckCoords)
                minDist = Math.min(minDist, slanderer.distanceSquaredTo(muckraker));
        if (minDist < 20 && rc.canEmpower(actionRadius))
        {
            rc.empower(actionRadius);
            return;
        }
        
        List<RobotInfo> nearbyAllies = new ArrayList<>();
        RobotInfo[] nearbyRobots = rc.senseNearbyRobots();
        for (RobotInfo robot : nearbyRobots) {
            if (robot.team == ally) {
                nearbyAllies.add(robot);
            }
        }
        if (nearbyAllies.size() > 0) {
            moveDir(maintainDistance(nearbyAllies, 5));
        } else {
            // dont move
        }
    }
    
    // finds distance b/t two locs
    static double absoluteDist(MapLocation loc1, MapLocation loc2) {
        return Math.sqrt(Math.pow(loc1.x - loc2.x, 2) + Math.pow(loc1.y - loc2.y, 2));
    }
    
    // computes penalty for a certain loc given nearbyAllies and dist
    static double computePenalty(MapLocation loc, List<RobotInfo> nearbyAllies, double dist) {
        double penalty = 0;
        for (RobotInfo robot : nearbyAllies) {
            penalty += Math.pow((absoluteDist(loc, robot.getLocation()) - dist), 2);
        }
        return penalty;
    }
    
    /**
    * Helper function to compute a direction for a robot to move to maintain a certain distance from a list of robots
    * The best direction is computed by weighting penalties between directions with the sum of the squares of their possible distances from all the robots
    **/
    static Direction maintainDistance(List<RobotInfo> nearbyAllies, double dist) {
        
        // finds the direction which gives minimum penalty
        double minPenalty = 999999999;
        Direction heading = Direction.CENTER;
        for (Direction d : Direction.values()) {
            double penalty = computePenalty(rc.getLocation().add(d), nearbyAllies, dist);
            if (penalty < minPenalty) {
                minPenalty = penalty;
                heading = d;
            }
        }
        return heading;
    }
    
    static void runMuckraker() throws GameActionException {
        Team enemy = rc.getTeam().opponent();
        int sensorRadius = rc.getType().sensorRadiusSquared;
        MapLocation curLoc = rc.getLocation();

        if(ecID == 0) {
            ecID = getECID();
            int flag = getECFlag();
            if (flag != 0) {
                executeInstr(flag);
            }
            rc.setFlag(encodeInstruction(OPCODE.SENDECID, 0, 0, ecID));
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
            if (generalDir != -1) 
                moveDir(directions[generalDir]);
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
     *  Robot will scan for EC and send this info via flag
     *
     */
    static void scanForEC() throws GameActionException {
        int sensorRadius = rc.getType().sensorRadiusSquared;

        RobotInfo[] nearbyRobots = rc.senseNearbyRobots(rc.getLocation(), sensorRadius, null);             //look for ECs
        for (RobotInfo rinfo : nearbyRobots) {
            if (rinfo.type == RobotType.ENLIGHTENMENT_CENTER) {
                if (rinfo.team == rc.getTeam().opponent()) {
                    //discovered an enemy EC
                    if (rc.canSetFlag(encodeInstruction(OPCODE.SENDCOORDINATES, rinfo.getLocation().x % 64, 
                            rinfo.getLocation().y % 64, coordinfoToInt(COORDINFO.ENEMYEC)))) {//op, xcoord, ycoord, data 
                        rc.setFlag(encodeInstruction(OPCODE.SENDCOORDINATES, rinfo.getLocation().x % 64, 
                            rinfo.getLocation().y % 64, coordinfoToInt(COORDINFO.ENEMYEC)));
                    }
                }
                else if (rinfo.team == Team.NEUTRAL) {
                    if (rc.canSetFlag(encodeInstruction(OPCODE.SENDCOORDINATES, rinfo.getLocation().x % 64, 
                            rinfo.getLocation().y % 64, coordinfoToInt(COORDINFO.NEUTRALEC)))) {//op, xcoord, ycoord, data 
                        rc.setFlag(encodeInstruction(OPCODE.SENDCOORDINATES, rinfo.getLocation().x % 64, 
                            rinfo.getLocation().y % 64, coordinfoToInt(COORDINFO.NEUTRALEC)));
                    }
                }
                else if (rinfo.team == rc.getTeam()) {
                    if (rc.canSetFlag(encodeInstruction(OPCODE.SENDCOORDINATES, rinfo.getLocation().x % 64, 
                            rinfo.getLocation().y % 64, coordinfoToInt(COORDINFO.ALLYEC)))) {//op, xcoord, ycoord, data 
                        rc.setFlag(encodeInstruction(OPCODE.SENDCOORDINATES, rinfo.getLocation().x % 64, 
                            rinfo.getLocation().y % 64, coordinfoToInt(COORDINFO.ALLYEC)));
                    }
                }
                //by adding this break statement, we only broadcast the closest found EC
                break;
            }
        }
    }

    /**
     * Returns a random spawnable RobotType
     *
     * @return a random RobotType
     */
    static RobotType randomSpawnableRobotType() {
        double prob = Math.random();
        if(prob < 0.2){
            return RobotType.SLANDERER;
        }else if(prob < 0.7){
            return RobotType.MUCKRAKER;
        }else{
            return RobotType.POLITICIAN;
        }
    }

    /**
     * Attempts to move one step toward a given location
     *
     * @param loc The intended location in NORMALIZED COORDINATES
     * @return true if map coordinates are known
     * @throws GameActionException
     */
    static boolean moveToLoc(MapLocation loc) throws GameActionException {
        if (xBound0 == -1 || yBound0 == -1) {
            //can't move without knowing normalized coordinates
            //we need to wait for map coordinate offsets before moving
            int flag = getECFlag();
            if (flag != 0) {
                executeInstr(flag);
            }
            return false;
        }
        MapLocation absLoc = rc.getLocation();          //get absolute coordinates
        MapLocation normLoc = new MapLocation((absLoc.x - xBound0) % 64, (absLoc.y - yBound0) % 64);

        Direction dirToLoc = normLoc.directionTo(loc);
        moveDir(dirToLoc);

        return true;
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

        //don't get too close to boundaries
        if (!rc.onTheMap(curloc.translate(0, 3))) {
            while (generalDir == 0 || generalDir == 1 || generalDir == 7) {
                generalDir += 1;
                generalDir %= 8;
            }
        }
        if (!rc.onTheMap(curloc.translate(3, 0))) {
            while (generalDir == 1 || generalDir == 2 || generalDir == 3) {
                generalDir += 1;
                generalDir %= 8;
            }
        }
        if (!rc.onTheMap(curloc.translate(0, -3))) {
            while (generalDir == 3 || generalDir == 4 || generalDir == 5) {
                generalDir += 1;
                generalDir %= 8;
            }
        }
        if (!rc.onTheMap(curloc.translate(-3, 0))) {
            while (generalDir == 5 || generalDir == 6 || generalDir == 7) {
                generalDir += 1;
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
}

