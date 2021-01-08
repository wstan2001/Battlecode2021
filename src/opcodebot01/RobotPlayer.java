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
    	MOVE, SCOUT, NEEDECID, SENDINGECID, INVALID;
    } 
    static int turnCount;
    static int ecID = 0;
    static Deque<Integer> instructionQueue = new ArrayDeque<>(); 
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
    static int opcodeToInt(OPCODE op)
    {
    	switch(op)
    	{
    	    case MOVE: return 0;
    	    case SCOUT: return 1;
    	    case NEEDECID: return 2;
    	    case SENDINGECID: return 3;
    	}
    	return -1;
    }
    static OPCODE intToOpcode(int op)
    {
    	switch(op)
    	{
    	    case 0: return OPCODE.MOVE;
    	    case 1: return OPCODE.SCOUT;
    	    case 2: return OPCODE.NEEDECID;
    	    case 3: return OPCODE.SENDINGECID;
    	}
    	return OPCODE.INVALID;
    }
    static int encodeInstruction(OPCODE op, int x, int y, int data)
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
    	//todo
    	return Direction.WEST;
    }
    static void runEnlightenmentCenter() throws GameActionException {
        RobotType toBuild = randomSpawnableRobotType();
        int influence = rc.getInfluence()/5;
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
        if(turnCount % 2 == 0)
        {
            if(rc.canSetFlag(encodeInstruction(OPCODE.MOVE, 1, 0, 0))) //op, xcoord, ycoord, data
                rc.setFlag(encodeInstruction(OPCODE.MOVE, 1, 0, 0));
        }
        else
            if(rc.canSetFlag(encodeInstruction(OPCODE.SCOUT, 1, 0, 0))) //op, xcoord, ycoord, data
                rc.setFlag(encodeInstruction(OPCODE.SCOUT, 1, 0, 0));
    }
    static void runPolitician() throws GameActionException {
        if(ecID == 0)
            for (RobotInfo ri : rc.senseNearbyRobots(-1, rc.getTeam()))
                if(ri.type == RobotType.ENLIGHTENMENT_CENTER)
                    ecID = ri.ID;
        System.out.println("ECID: " + ecID);
        if(ecID != 0)    
            if(rc.canGetFlag(ecID))
            {
                int flag = rc.getFlag(ecID);
                System.out.println("Flag found: " + flag);
                if(flag != 0)
                    instructionQueue.addLast(flag);
            }
	
        Team enemy = rc.getTeam().opponent();
        int actionRadius = rc.getType().actionRadiusSquared;
        RobotInfo[] attackable = rc.senseNearbyRobots(actionRadius, enemy);
        if (attackable.length != 0 && rc.canEmpower(actionRadius)) {
            System.out.println("empowering...");
            rc.empower(actionRadius);
            System.out.println("empowered");
            return;
        }
        if(ecID == 0)
            tryMove(randomDirection());
        if(instructionQueue.isEmpty() || !rc.isReady())
            return;    
        int instr = instructionQueue.removeFirst();        
        System.out.println("received instruction: " + instr);
        OPCODE op = opcode(instr);
        switch(op)
        {
            case MOVE: tryMove(pathFind(instrX(instr), instrY(instr))); break;
            case SCOUT: tryMove(randomDirection()); break;
            default: break;
        }
        //tryMove(pathFind(instrX(instr), instrY(instr)));
    }

    static void runSlanderer() throws GameActionException {
         if(ecID == 0)
            for (RobotInfo ri : rc.senseNearbyRobots(-1, rc.getTeam()))
                if(ri.type == RobotType.ENLIGHTENMENT_CENTER)
                    ecID = ri.ID;
       if (tryMove(randomDirection()))
            System.out.println("I moved!");
    }

    static void runMuckraker() throws GameActionException {
         if(ecID == 0)
            for (RobotInfo ri : rc.senseNearbyRobots(-1, rc.getTeam()))
                if(ri.type == RobotType.ENLIGHTENMENT_CENTER)
                    ecID = ri.ID;
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
        if (tryMove(randomDirection()))
            System.out.println("I moved!");
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
