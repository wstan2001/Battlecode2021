package turtleplayer;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

import static turtleplayer.Utilities.randomDirection;
import static turtleplayer.Utilities.tryMove;

public strictfp class Slanderer {
    private static RobotController rc;
    private static AwarenessModule aw;
    public static void start (RobotController rc, AwarenessModule aw){
        Slanderer.rc = rc;
        Slanderer.aw = aw;
    }
    public static void processTurn() throws GameActionException {
        if (tryMove(randomDirection(),rc))
            System.out.println("I moved!");
    }
}
