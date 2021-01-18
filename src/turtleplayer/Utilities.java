package turtleplayer;

import battlecode.common.*;
import turtleplayer.flagutilities.RelativeLocation;

import java.util.*;

public strictfp class Utilities {

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

    private static final int[] endingLimits = new int[41];
    private static final RelativeLocation[] relativeLocations = new RelativeLocation[85];
    private static final int[] xLocs = new int[]{1,1,2,2,2,3,3,3,4,4,3,4,4,5,5,5,4,5,6,6,6};
    private static final int[] yLocs = new int[]{0,1,0,1,2,0,1,2,0,1,3,2,3,0,1,2,4,3,0,1,2};

    public static RelativeLocation[] getRelativeLocations(){
        return relativeLocations;
    }
    public static int getEndingLimit (int radiusSquared){
        return endingLimits[radiusSquared];
    }

    static void start(){
        System.out.println("UT1: "+ Clock.getBytecodeNum());
        relativeLocations[0] = new RelativeLocation(0,0);
        int counter = 1;
        for (int i = 0; i < xLocs.length; i++) {
            int x = xLocs[i];
            int y = yLocs[i];
            relativeLocations[counter++] = new RelativeLocation(x,y);
            relativeLocations[counter++] = new RelativeLocation(-y,x);
            relativeLocations[counter++] = new RelativeLocation(-x,-y);
            relativeLocations[counter++] = new RelativeLocation(y,-x);
        }
        System.out.println("UT2: "+ Clock.getBytecodeNum());
        endingLimits[2] = 9;  //2
        endingLimits[9] = 25; //6
        endingLimits[12] = 29; //7
        endingLimits[20] = 49; //12
        endingLimits[25] = 57; //14
        endingLimits[30] = 65; //16
        endingLimits[40] = 85; //21
        System.out.println("UT3: "+ Clock.getBytecodeNum());
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
    static boolean tryMove(Direction dir, RobotController rc) throws GameActionException {
        System.out.println("I am trying to move " + dir + "; " + rc.isReady() + " " + rc.getCooldownTurns() + " " + rc.canMove(dir));
        if (rc.canMove(dir)) {
            rc.move(dir);
            return true;
        } else return false;
    }

}
