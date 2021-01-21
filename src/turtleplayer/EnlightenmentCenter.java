package turtleplayer;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public strictfp class EnlightenmentCenter {
    private static RobotController rc;
    private static AwarenessModule aw;

    public static void start (RobotController rc, AwarenessModule aw){
        EnlightenmentCenter.rc = rc;
        EnlightenmentCenter.aw = aw;
    }

    private static final int[] EFFICIENT_SLANDERER_INFLUENCE = new int[]{21,41,63,85,107,130,154,178,203,228,255,282};

    public static void processTurn(MapLocation robotMapLocation, int roundNumber) throws GameActionException {
        if(roundNumber <= 1){

        }
    }
}
