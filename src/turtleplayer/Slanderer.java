package turtleplayer;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public strictfp class Slanderer {
    private static RobotController rc;
    public static void start (RobotController rc){
        Slanderer.rc = rc;
    }
    public static void processTurn() throws GameActionException {
        //if (tryMove(randomDirection()))
        //    System.out.println("I moved!");
    }
}
