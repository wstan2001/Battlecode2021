package turtleplayer;

import battlecode.common.*;

public strictfp class Muckraker {
    private static RobotController rc;
    public static void start (RobotController rc){
        Muckraker.rc = rc;
    }
    public static void processTurn() throws GameActionException {
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
        //if (tryMove(randomDirection()))
        //    System.out.println("I moved!");
    }
}
