package turtleplayer;

import battlecode.common.*;

import static turtleplayer.Utilities.directions;

public strictfp class EnlightenmentCenter {
    private static RobotController rc;
    private static AwarenessModule aw;
    public static void start (RobotController rc, AwarenessModule aw){
        EnlightenmentCenter.rc = rc;
        EnlightenmentCenter.aw = aw;
    }
    public static void processTurn() throws GameActionException {
        RobotType toBuild = RobotType.SLANDERER;//randomSpawnableRobotType();
        int influence = 50;
        for (Direction dir : directions) {
            if (rc.canBuildRobot(toBuild, dir, influence)) {
                rc.buildRobot(toBuild, dir, influence);
            } else {
                break;
            }
        }

    }
}
