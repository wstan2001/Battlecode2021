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

    static final int[] dirHelper = {0, 1, -1, 2, -2, 3, -3, 4};         //help with pathing

    static int turnCount;
    static Random rng = new Random();

    //EC state variables
    static ArrayList<Integer> botIDs = new ArrayList<Integer>();        //keep track of scout IDs
    static int xBound0 = -1;       //mod 64 coordinate of left edge
    static int xBound1 = -1;        //mod 64 coordinate of right edge
    static int yBound0 = -1;        //mod 64 coordinate of lower edge
    static int yBound1 = -1;        //mod 64 coordinate of upper edge

    //Politician state variables
    static boolean foundXBound = false;
    static boolean foundYBound = false;
    static int generalDir = -1;       //the general direction robot should move in, int from 0 to 7

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

        if (rc.getRoundNum() % 5 == 0 && botIDs.size() < 10) {
            int randDirection = rng.nextInt(8);
            rc.setFlag(randDirection);
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

        /*if (xBound0 != -1) 
            System.out.println("xBound0: " + xBound0);
        if (yBound0 != -1) 
            System.out.println("yBound0: " + yBound0);
        if (xBound1 != -1) 
            System.out.println("xBound1: " + xBound1);
        if (yBound1 != -1) 
            System.out.println("yBound1: " + yBound1);*/
        
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

        if (generalDir == -1) {    //robot is uninitialized
            RobotInfo[] nearbyAllies = rc.senseNearbyRobots(2, ally);
            for (RobotInfo rinfo : nearbyAllies) {
                if (rinfo.type == RobotType.ENLIGHTENMENT_CENTER) {
                    generalDir = rc.getFlag(rinfo.getID());
                    if (generalDir < 0 || generalDir > 7) {
                        System.out.println("Something went wrong! Scout read wrong message from wrong EC!");
                        generalDir = Math.abs(generalDir);
                        generalDir %= 7;
                    }
                    break;
                }
            }
        }

        //System.out.println("General Direction: " + directions[generalDir]);

        //check if done exploring
        if (foundXBound && foundYBound) {
            //robot is done exploring
            return;
        }

        //resume coding from here!

        if (!foundYBound) {
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
                foundYBound = true;
                rc.setFlag(rc.getFlag(rc.getID()) + ybound_mod64 + (1 << 6) + (1 << 7));
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
                foundYBound = true;
                rc.setFlag(rc.getFlag(rc.getID()) + ybound_mod64 + (0 << 6) + (1 << 7));
            }
        }

        if (!foundXBound) {
            //sense right x boundary
            int breakpoint = 0;
            for (int i = 1; i <= 5; i++) {
                if (!rc.onTheMap(rc.getLocation().translate(i, 0))) {
                    breakpoint = i-1;
                    break;
                }
            }
            if (breakpoint != 0) {
                //we found right x boundary
                int xbound_mod64 = (rc.getLocation().x + breakpoint) % 64;
                foundXBound = true;
                rc.setFlag(rc.getFlag(rc.getID()) + xbound_mod64 * (1 << 8) + (1 << 14) + (1 << 15));
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
                foundXBound = true;
                rc.setFlag(rc.getFlag(rc.getID()) + xbound_mod64 * (1 << 8) + (0 << 14) + (1 << 15));
            }
        }


        //figure out where to move next
        Direction heading = Direction.CENTER;
        MapLocation curloc = rc.getLocation();
        double minPenalty = 999;
        int[] temp = {0, 1, -1};        //move roughly according to general direction
        for (int i : temp) {
            Direction h = directions[Math.abs(generalDir + i) % 7];
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

    static void runSlanderer() throws GameActionException {
        if (tryMove(randomDirection()))
            System.out.println("I moved!");
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
        return spawnableRobot[(int) (Math.random() * spawnableRobot.length)];
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

