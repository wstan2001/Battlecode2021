package boundfinder;
import battlecode.common.*;
import java.util.ArrayList;
import java.util.Random;
import java.lang.Math;

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
    

    static int turnCount;
    static Random rng = new Random();

    //EC state variables
    static ArrayList<Integer> botIDs = new ArrayList<Integer>();        //keep track of scout IDs
    static int xBound0 = -1;       //mod 64 coordinate of left edge
    static int xBound1 = -1;        //mod 64 coordinate of right edge
    static int yBound0 = -1;        //mod 64 coordinate of lower edge
    static int yBound1 = -1;        //mod 64 coordinate of upper edge
    static int mapSize = -1;        //will be either 32 or 64

    //Politician state variables
    static boolean foundXBound = false;
    static boolean foundYBound = false;
    static int generalDir = -1;       //the general direction robot should move in, int from 0 to 7
    static MapLocation targetLoc = new MapLocation(-1,-1);

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

        while (true) {
            turnCount += 1;
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
        RobotType toBuild = RobotType.POLITICIAN;
        Direction buildDir;
        int influence = 1;

        //System.out.println("Conviction: " + rc.getConviction());
        //System.out.println("Influence: " + rc.getInfluence());
        

        if (rc.getRoundNum() % 5 == 0 && rc.getRoundNum() < 100 && botIDs.size() < 10) {
            int randDirection = rng.nextInt(8);
            rc.setFlag((1 << 16) + randDirection);
            for (int i : dirHelper) {
                buildDir = directions[Math.abs(randDirection + i) % 7];
                if (rc.canBuildRobot(toBuild, buildDir, influence)) {
                    rc.buildRobot(toBuild, buildDir, influence);
                    RobotInfo rinfo = rc.senseRobotAtLocation(rc.getLocation().add(buildDir));
                    botIDs.add(rinfo.getID());
                    break;
                }
            }
        }
        else if (rc.getRoundNum() % 5 == 0 && rc.getRoundNum() >= 100 && xBound0 != -1 && yBound0 != -1) {
            //try sending a bot to a target location (16, 16)
            rc.setFlag((2 << 16) + (16 << 6) + 16);
            for (Direction d : directions) {
                if (rc.canBuildRobot(toBuild, d, influence)) {
                    rc.buildRobot(toBuild, d, influence);
                    break;
                }
            }
        }
        else {
            //broadcast map coordinates if possible
            if (xBound0 != -1 && yBound0 != -1)
                rc.setFlag((1 << 20) + (xBound0 << 6) + yBound0);
        }

        botIDs.removeIf(id -> !rc.canGetFlag(id));          //purge dead scouts
        for (Integer id : botIDs) {
            int flag = rc.getFlag(id);
            if (yBound0 == -1 && ((flag >> 7) & 1) == 1  && ((flag >> 6) & 1) == 0) {
                yBound0 = flag % 64;
            }
            if (xBound0 == -1 && ((flag >> 15) & 1) == 1 && ((flag >> 14) & 1) == 0) {
                xBound0 = (flag >> 8) % 64;
            }
            if (yBound1 == -1 && ((flag >> 7) & 1) == 1 && ((flag >> 6) & 1) == 1) {
                yBound1 = flag % 64;
            }
            if (xBound1 == -1 && ((flag >> 15) & 1) == 1 && ((flag >> 14) & 1) == 1) {
                xBound1 = (flag >> 8) % 64;
            }
        }

        //deduce coordinates
        if (mapSize == -1) {
            if (yBound0 != -1 && yBound1 != -1) {
                int temp = Math.abs(yBound1 - yBound0 + 1) % 64;
                if (temp == 32) 
                        mapSize = 32;
                    else if (temp == 0) 
                        mapSize = 64;
                    else 
                        System.out.println("Something went wrong calculating map size.");
            }
            else if (xBound0 != -1 && xBound1 != -1) {
                int temp = Math.abs(xBound1 - xBound0 + 1) % 64;
                if (temp == 32) 
                        mapSize = 32;
                    else if (temp == 0) 
                        mapSize = 64;
                    else 
                        System.out.println("Something went wrong calculating map size.");
            }
        }
        else {
            if (yBound0 == -1 && yBound1 != -1) 
                yBound0 = (Math.abs(yBound1 - mapSize + 1)) % 64;
            if (yBound1 == -1 && yBound0 != -1)
                yBound1 = (Math.abs(yBound0 + mapSize - 1)) % 64;
            if (xBound0 == -1 && xBound1 != -1) 
                xBound0 = (Math.abs(xBound1 - mapSize + 1)) % 64;
            if (xBound1 == -1 && xBound0 != -1)
                xBound1 = (Math.abs(xBound0 + mapSize - 1)) % 64;
            
        }

        /*if (xBound0 != -1) 
            System.out.println("xBound0: " + xBound0);
        if (yBound0 != -1) 
            System.out.println("yBound0: " + yBound0);
        if (xBound1 != -1) 
            System.out.println("xBound1: " + xBound1);
        if (yBound1 != -1) 
            System.out.println("yBound1: " + yBound1);
        if (mapSize != -1) 
            System.out.println("mapSize: " + mapSize);
        System.out.println("shrek");*/
        
    }

    static void runPolitician() throws GameActionException {
        Team ally = rc.getTeam();
        Team enemy = rc.getTeam().opponent();
        int actionRadius = rc.getType().actionRadiusSquared;
        final int senseRadius = 25;
        final double baseCooldown = 1.0;

        /*
        POLITICIAN FLAG CODE:

        Treat the flag as a 24 bit binary number, want to transmit boundary location

        00000000 | 00000000 | 00000000
        ^ bit 23                     ^ bit 0

        Bits[5:0] = mod 64 of y coord boundary of map
        Bit 6 = 1 if top boundary, 0 if bottom boundary
        Bit 7 = 1 iff bot has finished exploration of y boundary

        Bits[13:8] = mod 64 of x coord boundary of map
        Bit 14 = 1 if right boundary, 0 if left boundary
        Bit 15 = 1 iff bot has finished exploration of x boundary
        */

        if (generalDir == -1 || targetLoc.x == -1 || targetLoc.y == -1) {    //robot is uninitialized
            RobotInfo[] nearbyAllies = rc.senseNearbyRobots(2, ally);
            for (RobotInfo rinfo : nearbyAllies) {
                if (rinfo.type == RobotType.ENLIGHTENMENT_CENTER) {
                    int ECflag = rc.getFlag(rinfo.getID());
                    if ((ECflag >> 16) % 8 == 1) {
                        //set up a boundary finder
                        generalDir = ECflag % 8;
                        if (generalDir < 0 || generalDir > 7) {
                            System.out.println("Something went wrong! Scout read wrong message from wrong EC!");
                            generalDir = Math.abs(generalDir);
                            generalDir %= 7;
                        }
                        break;
                    }
                    else if ((ECflag >> 16) % 8 == 2) {
                        //set up a targeter
                        targetLoc = new MapLocation((ECflag >> 6) % 64, ECflag % 64);
                    }
                }
            }
        }

        if (generalDir != -1) {
            //this scout is a boundary finder
            scoutBounds();
        }
        else if (targetLoc.x != -1 && targetLoc.y != -1) {
            //this scout wants to move towards a target
            scoutTarget();
        }
        
        
    }

    static void scoutBounds() throws GameActionException {
        //System.out.println("General Direction: " + directions[generalDir]);

        //check if done exploring
        if (foundXBound && foundYBound) {
            //robot is done exploring
            //System.out.println("Done exploring!")
            //make it go kaboom?
            //return;
        }

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
            int ybound_mod64 = (rc.getLocation().y + breakpoint) % 64;
            if (!foundYBound) {
                rc.setFlag(rc.getFlag(rc.getID()) + ybound_mod64 + (1 << 6) + (1 << 7));
                foundYBound = true;
            }
            //make bots going north change direction
            if (generalDir == 0 || generalDir == 1) {
                generalDir = 2;
                return;
            }
            else if (generalDir == 7) {
                generalDir = 6;
                return;
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
            int ybound_mod64 = (rc.getLocation().y + breakpoint * -1) % 64;
            if (!foundYBound) {
                //signal with flag
                rc.setFlag(rc.getFlag(rc.getID()) + ybound_mod64 + (0 << 6) + (1 << 7));
                foundYBound = true;
            }
            //make bot going south change direction
            if (generalDir == 4 || generalDir == 5) {
                generalDir = 6;
                return;
            }
            else if (generalDir == 3) {
                generalDir = 2;
                return;
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
            int xbound_mod64 = (rc.getLocation().x + breakpoint) % 64;
            if (!foundXBound) {
                rc.setFlag(rc.getFlag(rc.getID()) + xbound_mod64 * (1 << 8) + (1 << 14) + (1 << 15));
                foundXBound = true;
            }
            //make bot going east change direction
            if (generalDir == 2 || generalDir == 3) {
                generalDir = 4;
                return;
            }
            else if (generalDir == 1) {
                generalDir = 0;
                return;
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
            int xbound_mod64 = (rc.getLocation().x + breakpoint * -1) % 64;
            if (!foundXBound) {
                rc.setFlag(rc.getFlag(rc.getID()) + xbound_mod64 * (1 << 8) + (0 << 14) + (1 << 15));
                foundXBound = true;
            }
            //make bot going west change direction
            if (generalDir == 6 || generalDir == 7) {
                generalDir = 0;
                return;
            }
            else if (generalDir == 5) {
                generalDir = 4;
                return;
            }
        }


        //figure out where to move next
        moveDir(directions[generalDir]);
    }

    static void scoutTarget() throws GameActionException {
        Team ally = rc.getTeam();

        if (xBound0 == -1 || yBound0 == -1) {
            //we need to wait for map coordinate offsets before moving
            RobotInfo[] nearbyAllies = rc.senseNearbyRobots(2, ally);
            for (RobotInfo rinfo : nearbyAllies) {
                if (rinfo.type == RobotType.ENLIGHTENMENT_CENTER) {
                    int ECflag = rc.getFlag(rinfo.getID());
                    if ((ECflag >> 20) % 8 == 1) {
                        //set up a boundary coordinates
                        xBound0 = (ECflag >> 6) % 64;
                        yBound0 = ECflag % 64;
                        break;
                    }
                }
            }
        }
        else {
            moveToLoc(targetLoc);
        }
    }

    static void runSlanderer() throws GameActionException {
        
    }

    static void runMuckraker() throws GameActionException {
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
        return spawnableRobot[(int) (Math.random() * spawnableRobot.length)];
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
        
        double minPenalty = 999;
        int[] temp = {0, 1, -1};        //move roughly according to general direction
        for (int i : temp) {
            Direction h = directions[Math.abs(dirIdx + i) % 7];
            if (rc.canSenseLocation(curloc.add(h))) {
                double adjPenalty = baseCooldown / rc.sensePassability(curloc.add(h));
                if (rc.canMove(h) && adjPenalty < minPenalty - 1) {
                    heading = h;
                    minPenalty = adjPenalty;
                }
            }
        }
        if (heading != Direction.CENTER && rc.canMove(heading)) {
            rc.move(heading);
        }
    }
}

