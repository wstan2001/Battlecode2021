package turtleplayer;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.Team;

import static turtleplayer.Utilities.randomDirection;
import static turtleplayer.Utilities.tryMove;

public strictfp class Politician {
    private static RobotController rc;
    private static AwarenessModule aw;
    public static void start (RobotController rc, AwarenessModule aw){
        Politician.rc = rc;
        Politician.aw =aw;
    }
    public static void processTurn() throws GameActionException {
        assert rc != null;
        assert rc.getTeam() != null;
        assert rc.getTeam().opponent() != null;
        Team enemy = rc.getTeam().opponent();
        int actionRadius = rc.getType().actionRadiusSquared;
        RobotInfo[] attackable = rc.senseNearbyRobots(actionRadius, enemy);
        if (attackable.length != 0 && rc.canEmpower(actionRadius)) {
            System.out.println("empowering...");
            rc.empower(actionRadius);
            System.out.println("empowered");
            return;
        }
        if (tryMove(randomDirection(),rc))
            System.out.println("I moved!");
    }
}
