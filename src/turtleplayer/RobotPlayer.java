package turtleplayer;
import battlecode.common.*;

public strictfp class RobotPlayer {
    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        // This is the RobotController object. You use it to perform actions from this robot,
        // and to get information on its current status.
        System.out.println("I'm a " + rc.getType() + " and I just got created!");
        switch (rc.getType()) {
            case ENLIGHTENMENT_CENTER: EnlightenmentCenter.start(rc); break;
            case POLITICIAN:           Politician.start(rc);          break;
            case SLANDERER:            Muckraker.start(rc);           break;
            case MUCKRAKER:            Slanderer.start(rc);           break;
        }
        while (true) {
            try {
                switch (rc.getType()) {
                    case ENLIGHTENMENT_CENTER: EnlightenmentCenter.processTurn(); break;
                    case POLITICIAN:           Politician.processTurn();          break;
                    case SLANDERER:            Muckraker.processTurn();           break;
                    case MUCKRAKER:            Slanderer.processTurn();           break;
                }
                Clock.yield();
            } catch (Exception e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
            }
        }
    }
}
