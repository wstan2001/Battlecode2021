package turtleplayer;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.Team;

import static turtleplayer.Utilities.randomDirection;
import static turtleplayer.Utilities.tryMove;

public strictfp class Muckraker {
    private static RobotController rc;
    private static AwarenessModule aw;
    public static void start (RobotController rc, AwarenessModule aw){
        Muckraker.rc = rc;
        Muckraker.aw = aw;
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
        if (tryMove(randomDirection(),rc))
            System.out.println("I moved!");
    }
}
