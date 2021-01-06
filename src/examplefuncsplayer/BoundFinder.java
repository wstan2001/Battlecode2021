package examplefuncsplayer;
import battlecode.common.*;
import java.util.ArrayList;

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

    static int turnCount;

    //EC state variables
    static ArrayList<Integer> botIDs = new ArrayList<Integer>();        //keep track of scout IDs
    static int xBound0 = -1;       //mod 64 coordinate of left edge
    static int xBound1 = -1;        //mod 64 coordinate of right edge
    static int yBound0 = -1;        //mod 64 coordinate of lower edge
    static int yBound1 = -1;        //mod 64 coordinate of upper edge

    //Politician state variables
    static boolean foundXBound = false;
    static boolean foundYBound = false;
    static int sign = 0;       //-1 means exploring southwest, +1 means exploring northeast

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

    static void runEnlightenmentCenter() throws GameActionException {
        RobotType toBuild = RobotType.POLITICIAN;
        Direction buildDir;
        int influence = 1;

        System.out.println("Conviction: " + rc.getConviction());
        System.out.println("Influence: " + rc.getInfluence());

        if (rc.getRoundNum() == 1) {
            //try building a robot to southwest, if this isn't possible build north
            rc.setFlag(0);
            buildDir = Direction.SOUTHWEST;
            if (!rc.canBuildRobot(toBuild, buildDir, influence)) 
                buildDir = Direction.NORTH;
            if (rc.canBuildRobot(toBuild, buildDir, influence)) {
                rc.buildRobot(toBuild, buildDir, influence);
                RobotInfo rinfo = rc.senseRobotAtLocation(rc.getLocation().add(buildDir));
                botIDs.add(rinfo.getID());
            }
        }
        else if (rc.getRoundNum() == 20) {
            //try building a robot to northeast, if this isn't possible build south
            rc.setFlag(1);
            buildDir = Direction.NORTHEAST;
            if (!rc.canBuildRobot(toBuild, buildDir, influence)) 
                buildDir = Direction.SOUTH;
            if (rc.canBuildRobot(toBuild, buildDir, influence)) {
                rc.buildRobot(toBuild, buildDir, influence);
                RobotInfo rinfo = rc.senseRobotAtLocation(rc.getLocation().add(buildDir));
                botIDs.add(rinfo.getID());
            }
        }

        if (botIDs.size() >= 2) {
            int flag0 = -1;
            int flag1 = -1;
            if (rc.canGetFlag(botIDs.get(0))) {
                flag0 = rc.getFlag(botIDs.get(0));      //southwest bot flag
            }
            if (rc.canGetFlag(botIDs.get(1))) {
                flag1 = rc.getFlag(botIDs.get(1));      //southwest bot flag
            }

            if (yBound0 == -1 && flag0 != -1 && ((flag0 >> 6) & 1) == 1) {
                yBound0 = flag0 % 64;
            }
            if (xBound0 == -1 && flag0 != -1 && ((flag0 >> 13) & 1) == 1) {
                xBound0 = (flag0 >> 7) % 64;
            }
            if (yBound1 == -1 && flag1 != -1 && ((flag1 >> 6) & 1) == 1) {
                yBound1 = flag1 % 64;
            }
            if (xBound1 == -1 && flag1 != -1 && ((flag1 >> 13) & 1) == 1) {
                xBound1 = (flag1 >> 7) % 64;
            }
        }

        if (xBound0 != -1) 
            System.out.println("xBound0: " + xBound0);
        if (yBound0 != -1) 
            System.out.println("yBound0: " + yBound0);
        if (xBound1 != -1) 
            System.out.println("xBound1: " + xBound1);
        if (yBound1 != -1) 
            System.out.println("yBound1: " + yBound1);
        

        /*for (Direction dir : directions) {
            if (rc.canBuildRobot(toBuild, dir, influence)) {
                rc.buildRobot(toBuild, dir, influence);
            } else {
                break;
            }
        }*/
    }

    static void runPolitician() throws GameActionException {
        Team ally = rc.getTeam();
        Team enemy = rc.getTeam().opponent();
        int actionRadius = rc.getType().actionRadiusSquared;
        final int senseRadius = 25;

        /*
        POLITICIAN FLAG CODE:

        Treat the flag as a 24 bit binary number, want to transmit boundary location

        00000000 | 00000000 | 00000000
        ^ bit 23                     ^ bit 0

        bits[5:0] = mod64 of y coord boundary of map
        bit 6 = 1 iff bot has finished the exploration of y boundary

        bits[12:7]] = mod64 digits of x coord boundary of map
        bit 13 = 1 iff bot has finished exploration of x boundary

        */

        if (sign == 0) {    //robot is uninitialized
            RobotInfo[] nearbyAllies = rc.senseNearbyRobots(senseRadius, ally);
            for (RobotInfo rinfo : nearbyAllies) {
                if (rinfo.type == RobotType.ENLIGHTENMENT_CENTER) {
                    if (rc.getFlag(rinfo.getID()) == 0) {
                        sign = -1;      //go southwest
                    }
                    else if (rc.getFlag(rinfo.getID()) == 1) {
                        sign = 1;       //go northeast
                    }
                    break;
                }
            }
        }

        System.out.println("Sign: " + sign);

        //check if done exploring
        if (foundXBound && foundYBound) {
            //robot is done exploring
            System.out.println("Exploration over!");
            return;
        }

        if (!foundYBound) {
            //sense y boundary
            int breakpoint = 0;
            for (int i = 1; i <= 5; i++) {
                if (!rc.onTheMap(rc.getLocation().translate(0, i * sign))) {
                    breakpoint = i-1;
                    break;
                }
            }
            if (breakpoint != 0) {
                //we found y boundary
                int ybound_mod64 = (rc.getLocation().y + breakpoint * sign) % 64;
                foundYBound = true;
                rc.setFlag(rc.getFlag(rc.getID()) + ybound_mod64 + (1 << 6));
            }
        }

        if (!foundXBound) {
            //sense x boundary
            int breakpoint = 0;
            for (int i = 1; i <= 5; i++) {
                if (!rc.onTheMap(rc.getLocation().translate(i * sign, 0))) {
                    breakpoint = i-1;
                    break;
                }
            }
            if (breakpoint != 0) {
                //we found x boundary
                int xbound_mod64 = (rc.getLocation().x + breakpoint * sign) % 64;
                foundXBound = true;
                rc.setFlag(rc.getFlag(rc.getID()) + xbound_mod64 * (1 << 7) + (1 << 13));
            }
        }


        //figure out where to move next
        Direction heading = Direction.CENTER;

        if (sign == -1) {
            if (!foundXBound && !foundYBound)
                heading = Direction.SOUTHWEST;
            else if (!foundXBound) 
                heading = Direction.WEST;
            else if (!foundYBound)
                heading = Direction.SOUTH;
        }
        else if (sign == 1) {
            if (!foundXBound && !foundYBound)
                heading = Direction.NORTHEAST;
            else if (!foundXBound) 
                heading = Direction.EAST;
            else if (!foundYBound)
                heading = Direction.NORTH;
        }

        if (rc.canMove(heading)) {
            rc.move(heading);
        }
 
        /*RobotInfo[] attackable = rc.senseNearbyRobots(actionRadius, enemy);
        if (attackable.length != 0 && rc.canEmpower(actionRadius)) {
            System.out.println("empowering...");
            rc.empower(actionRadius);
            System.out.println("empowered");
            return;
        }
        if (tryMove(randomDirection()))
            System.out.println("I moved!");*/
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

