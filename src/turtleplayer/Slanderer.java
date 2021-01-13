package turtleplayer;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public strictfp class Slanderer {
    private static RobotController rc;
    private static AwarenessModule aw;
    public static void start (RobotController rc, AwarenessModule aw){
        Slanderer.rc = rc;
        Slanderer.aw = aw;
    }
    public static void processTurn() throws GameActionException {
        //if (tryMove(randomDirection()))
        //    System.out.println("I moved!");
    }
}
