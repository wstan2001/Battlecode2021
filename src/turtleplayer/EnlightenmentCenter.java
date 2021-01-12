package turtleplayer;

import battlecode.common.*;

import static turtleplayer.Utilities.directions;

public strictfp class EnlightenmentCenter {
    private static RobotController rc;
    public static void start (RobotController rc){
        EnlightenmentCenter.rc = rc;
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
