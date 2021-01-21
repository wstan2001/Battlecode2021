package v3arefactor;

import battlecode.common.*;

import static v3arefactor.Flag.encodeInstruction;
import static v3arefactor.Pathing.*;
import static v3arefactor.RobotPlayer.*;

public class SlandererLogic {

    static void runSlanderer() throws GameActionException {
        int sensorRadius = rc.getType().sensorRadiusSquared;
        Team ally = rc.getTeam();
        Team enemy = ally.opponent();

        rc.setFlag(encodeInstruction(Flag.OPCODE.SLAND, 0, 0, 0));

        RobotInfo[] nearbyEnemy = rc.senseNearbyRobots(sensorRadius, enemy);
        MapLocation selfLoc = rc.getLocation();
        MapLocation closestMuck = new MapLocation(-1, -1);
        for (RobotInfo rinfo : nearbyEnemy) {
            if (rinfo.getType() == RobotType.MUCKRAKER) {
                if (closestMuck.equals(new MapLocation(-1, -1)) || selfLoc.distanceSquaredTo(rinfo.getLocation()) < selfLoc.distanceSquaredTo(closestMuck))
                    closestMuck = rinfo.getLocation();
            }
        }

        if (!closestMuck.equals(new MapLocation(-1, -1))) {
            //if there is a muckraker nearby
            Direction away = oppositeDir(selfLoc.directionTo(closestMuck));
            moveSlander(away);
        }
        else {
            Direction away = Direction.CENTER;
            RobotInfo[] nearbyAlly = rc.senseNearbyRobots(5, ally);
            for (RobotInfo rinfo : nearbyAlly) {
                if (rinfo.getType() == RobotType.ENLIGHTENMENT_CENTER) {
                    //don't clog own EC
                    away = oppositeDir(selfLoc.directionTo(rinfo.getLocation()));
                    break;
                }
            }
            if (away != Direction.CENTER)
                moveSlander(away);
            else if (rng.nextDouble() < 0.2)
                moveSlander(directions[rng.nextInt(8)]);
            //slanderers generally don't move unless too close to EC or muckraker nearby
        }
    }
}
