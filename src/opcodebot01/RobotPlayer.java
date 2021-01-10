package opcodebot01;
import battlecode.common.*;
import java.util.Deque;
import java.util.ArrayDeque;
public strictfp class RobotPlayer {
    static RobotController rc;

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

    public enum OPCODE {
    	MOVE, //move to specified location   [0(4) | x-coord(6) | y-coord (6) | random data (8)]
    	SCOUT, //tells units to scout [1(4) | random data (20)]
    	NEEDECID, //units will broadcast this if they don't have an EC ID [2(4) | random data(20)]
    	SENDINGECID, //EC will broadcast this when it broadcasts its ID [3(4) | id (20)]
    	SCOUTBOUNDARIES, //scouts will broadcast this when it finds a boundary [4(4) | stanley help with format]
    	SENDINGCOORDINATES, // units sending coordinates of important locations [5(4) | x_coord (6) | y_coord (6) | data (8)] data will depend on information it sends
    	INVALID;
    	//example: let's say we want to tell a unit to move to (15, 16). 15 = 001111 and 16 = 010000, the move opcode is 0000, and the data is 8 random bits. So a valid opcode for this would be
    	//0000 | 001111 | 010000 | 10101010 = 245913.
    	//a scout opcode would be something like 0001 | 10101010101010101010, since we don't care about the last 20 bits.
    } 
    static int turnCount;
    static int ecID = 0;
    static Deque<Integer> instructionQueue = new ArrayDeque<>(); //stores instructions from EC and executes them sequentially
    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {

        // This is the RobotController object. You use it to perform actions from this robot,
        // and to get information on its current status.
        RobotPlayer.rc = rc;
        turnCount = 0;

        System.out.println("I'm a " + rc.getType() + " and I just got created!");
        while (true) {
            turnCount += 1;
            // Try/catch blocks stop unhandled exceptions, which cause your robot to freeze
            try {
                // Here, we've separated the controls into a different method for each RobotType.
                // You may rewrite this into your own control structure if you wish.
                System.out.println("I'm a " + rc.getType() + "! Location " + rc.getLocation());
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
    static int opcodeToInt(OPCODE op) //need to be changed when new opcode added
    {
    	switch(op)
    	{
    	    case MOVE: return 0;
    	    case SCOUT: return 1;
    	    case NEEDECID: return 2;
    	    case SENDINGECID: return 3;
    	    case SCOUTBOUNDARIES: return 4;
    	    case SENDINGCOORDINATES: return 5;
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
    	    case 3: return OPCODE.SENDINGECID;
    	    case 4: return OPCODE.SCOUTBOUNDARIES;
    	    case 5: return OPCODE.SENDINGCOORDINATES;
    	}
    	return OPCODE.INVALID;
    }
    static int encodeInstruction(OPCODE op, int x, int y, int data) //MOVE, SCOUT, NEEDECID, SENDINGCOORDINATES
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
    static Direction pathFind(int x, int y)
    {
    	//todo stanley 
    	return Direction.WEST;
    }
    static int getECID() throws GameActionException 
    {
        for (RobotInfo ri : rc.senseNearbyRobots(-1, rc.getTeam()))
            if(ri.type == RobotType.ENLIGHTENMENT_CENTER)
                return ri.ID;
        return 0; //could not find EC
    }
    static int getECFlag() throws GameActionException 
    {
        if(rc.canGetFlag(ecID))
        {
            int flag = rc.getFlag(ecID);
            System.out.println("Flag found: " + flag);
            return flag;
        }
        return 0;
    }
    static void executeInstr(int instr) throws GameActionException
    {
        OPCODE op = opcode(instr);
        switch(op)
        {
            case MOVE: tryMove(pathFind(instrX(instr), instrY(instr))); break;
            case SCOUT: tryMove(randomDirection()); break;
            default: break;
        }
        return;
    } 
    
    static void runEnlightenmentCenter() throws GameActionException {
        RobotType toBuild = randomSpawnableRobotType();
        int influence = rc.getInfluence()/5; //heuristic 
        if(influence > 20)
            for (Direction dir : directions) {
                if (rc.canBuildRobot(toBuild, dir, influence)) {
                    rc.buildRobot(toBuild, dir, influence);
                } else {
                    break;
                }
            }
            
        if(rc.getInfluence() > 300)
            if(rc.canBid(20))
                rc.bid(20);
        
        if(turnCount % 2 == 0) //example functions
        {
            if(rc.canSetFlag(encodeInstruction(OPCODE.MOVE, 1, 0, 0))) //op, xcoord, ycoord, data
                rc.setFlag(encodeInstruction(OPCODE.MOVE, 1, 0, 0));
        }
        else
            if(rc.canSetFlag(encodeInstruction(OPCODE.SCOUT, 1, 0, 0))) //op, xcoord, ycoord, data
                rc.setFlag(encodeInstruction(OPCODE.SCOUT, 1, 0, 0));
    }
    static void politicianSpeech() throws GameActionException {
        Team enemy = rc.getTeam().opponent();
        int actionRadius = rc.getType().actionRadiusSquared;
        RobotInfo[] attackable = rc.senseNearbyRobots(actionRadius, enemy);
        if (attackable.length != 0 && rc.canEmpower(actionRadius)) {
            System.out.println("empowering...");
            rc.empower(actionRadius);
            System.out.println("empowered");
            return;
        }
    }
    static void runPolitician() throws GameActionException {
        if(ecID == 0)
            ecID = getECID();
        System.out.println("ECID: " + ecID);
        
        if(ecID != 0 && getECFlag() != 0)
            instructionQueue.addLast(getECFlag());
	
	//always execute this
	politicianSpeech();
        
        if (!rc.isReady())
            return;
        
        //execute this when no instructions remaining from EC
        if(instructionQueue.isEmpty())
        {
            tryMove(randomDirection());
            return;
        }

        //otherwise, execute first instruction in queue
        int instr = instructionQueue.removeFirst();        
        System.out.println("received instruction: " + instr);
        executeInstr(instr);
        return;
    }

    static void runSlanderer() throws GameActionException {
         if(ecID == 0)
             ecID = getECID();
         System.out.println("ECID: " + ecID);
         if(ecID != 0 && getECFlag() != 0)
             instructionQueue.addLast(getECFlag());
         if (!rc.isReady())
            return;
        
         //execute this when no instructions remaining from EC
         if(instructionQueue.isEmpty())
         {
             tryMove(randomDirection());
             return;
         }

         //otherwise, execute first instruction in queue
         int instr = instructionQueue.removeFirst();        
         System.out.println("received instruction: " + instr);
         executeInstr(instr);
         return;         
    }

    static void exposeSlanderers() throws GameActionException 
    {
        Team enemy = rc.getTeam().opponent();
        int actionRadius = rc.getType().actionRadiusSquared;
        for (RobotInfo robot : rc.senseNearbyRobots(actionRadius, enemy)) {
            if (robot.type.canBeExposed()) {
                // It's a slanderer... go get them!
                if (rc.canExpose(robot.location)) {
                    System.out.println("e x p o s e d");
                    rc.expose(robot.location);
                    return;
                }
            }
        }
    }
    static void runMuckraker() throws GameActionException {
        if(ecID == 0)
             ecID = getECID();
         System.out.println("ECID: " + ecID);
         if(ecID != 0 && getECFlag() != 0)
             instructionQueue.addLast(getECFlag());
         if (!rc.isReady())
            return;
         
         //always run this
         exposeSlanderers();
         
         //execute this when no instructions remaining from EC
         if(instructionQueue.isEmpty())
         {
             tryMove(randomDirection());
             return;
         }

         //otherwise, execute first instruction in queue
         int instr = instructionQueue.removeFirst();        
         System.out.println("received instruction: " + instr);
         executeInstr(instr);
         return;
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
     * Returns a random spawnable RobotType
     *
     * @return a random RobotType
     */
    static RobotType randomSpawnableRobotType() {
    //fix later when we have info	
        double p = Math.random();
        if(p < 0.15)
            return RobotType.MUCKRAKER;
        if(rc.getInfluence() > 200)
            return RobotType.POLITICIAN;
        return RobotType.SLANDERER;
    }

    /**
     * Attempts to move in a given direction.
     *
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMove(Direction dir) throws GameActionException {
        System.out.println("I am trying to move " + dir + "; " + rc.isReady() + " " + rc.getCooldownTurns() + " " + rc.canMove(dir));
        if (rc.canMove(dir)) {
            rc.move(dir);
            return true;
        } else return false;
    }
}
