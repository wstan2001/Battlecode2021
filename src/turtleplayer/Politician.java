package turtleplayer;

import battlecode.common.*;

public strictfp class Politician {
    private static RobotController rc;
    public static void start (RobotController rc){
        Politician.rc = rc;
    }
    public static void processTurn() throws GameActionException {
        Team enemy = rc.getTeam().opponent();
        int actionRadius = rc.getType().actionRadiusSquared;
        RobotInfo[] attackable = rc.senseNearbyRobots(actionRadius, enemy);
        if (attackable.length != 0 && rc.canEmpower(actionRadius)) {
            System.out.println("empowering...");
            rc.empower(actionRadius);
            System.out.println("empowered");
            return;
        }
        //if (tryMove(randomDirection()))
        //    System.out.println("I moved!");
    }
}
