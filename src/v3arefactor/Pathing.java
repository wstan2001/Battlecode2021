package v3arefactor;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;

import static v3arefactor.RobotPlayer.*;

public class Pathing {

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

    static MapLocation getAbsCoords(MapLocation loc) throws GameActionException {
        //given mod 128 coordinates, return the absolute coordinates
        int selfX = RobotPlayer.rc.getLocation().x % 128;
        int selfY = RobotPlayer.rc.getLocation().y % 128;

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

        if (Math.abs(dx) >= 64 || Math.abs(dy) >= 64) {
            System.out.println("Something went very wrong when decoding coordinates!");
            System.out.println(loc);
            System.out.println("dx: " + dx + " dy: " + dy);
        }

        return new MapLocation(RobotPlayer.rc.getLocation().x + dx, RobotPlayer.rc.getLocation().y + dy);
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
