package v3arefactor;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.Team;

import static v3arefactor.Pathing.*;

import static v3arefactor.RobotPlayer.*;

public class MuckrakerLogic {

    static void runMuckraker() throws GameActionException {
        Team enemy = rc.getTeam().opponent();
        int sensorRadius = rc.getType().sensorRadiusSquared;
        MapLocation curLoc = rc.getLocation();

        if (!wandering && rng.nextDouble() < 0.003) {
            //any muck has a 0.3% chance of beginning to move in random directions
            wandering = true;
        }

        rc.setFlag(0);              //don't send outdated info
        scanForEC();

        if(ecID == 0) {
            ecID = getECID();
            int flag = getECFlag();
            if (flag != 0) {
                executeInstr(flag);
            }
        }

        for (RobotInfo robot : rc.senseNearbyRobots(sensorRadius, enemy)) {
            if (robot.type.canBeExposed()) {
                // It's a slanderer... go get them!
                if (rc.canExpose(robot.location)) {
                    rc.expose(robot.location);
                    return;
                }
                else {
                    //decide whether to make it a new target
                    if (!rc.canSenseRobot(curTarget) ||
                            curLoc.distanceSquaredTo(robot.getLocation()) < curLoc.distanceSquaredTo(rc.senseRobot(curTarget).getLocation())) {
                        curTarget = robot.getID();
                    }
                }
            }
        }

        //try to chase current target, if exists
        if (!chase(curTarget)) {
            curTarget = -1;
            //try to find a current target or move to location
            if (generalDir != -1) {
                if (wandering)
                    moveDir(directions[rng.nextInt(8)]);
                else
                    moveDir(directions[generalDir]);
            }
            else if (targetLoc.x != -1 && targetLoc.y != -1)
                moveToLoc(targetLoc);
        }
    }
}
