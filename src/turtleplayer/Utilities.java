package turtleplayer;

import battlecode.common.*;
import turtleplayer.flagutilities.RelativeLocation;

public strictfp class Utilities {

    static final RobotType[] spawnableRobot = {
            RobotType.POLITICIAN,
            RobotType.SLANDERER,
            RobotType.MUCKRAKER,
    };

    static final Direction[] directions = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST,
    };

    public final static double INFLUENCE_EXP_FACTOR = 1.5849;
    public final static double LOG_BASE = Math.log(INFLUENCE_EXP_FACTOR);

    public static final int ENLIGHTENMENT_CENTER_INDEX = RobotType.ENLIGHTENMENT_CENTER.ordinal();
    public static Team ALLY_TEAM;
    public static Team ENEMY_TEAM;
    public static int ALLY_TEAM_INDEX;
    public static int ENEMY_TEAM_INDEX;
    public static int NEUTRAL_TEAM_INDEX;
    public static IncompleteRobotInfo ParentEC;

    public static RobotController RC;
    public static AwarenessModule AM;

    private static final int[] endingLimits = new int[41];
    private static final int[] xLocs = new int[]{1,1,2,2,1,2,3,3,1,3,2,4,4,1,3,4,2,4,3,5,5,1,5,2,4,5,3,6,6,1,6,2};
    private static final int[] yLocs = new int[]{0,1,0,1,2,2,0,1,3,2,3,0,1,4,3,2,4,3,4,0,1,5,2,5,4,3,5,0,1,6,2,6};
    private static final RelativeLocation[] relativeLocations = new RelativeLocation[129];

    public static RelativeLocation[] getRelativeLocations(){
        return relativeLocations;
    }
    public static int getEndingLimit (int radiusSquared){
        return endingLimits[radiusSquared];
    }



    static void start(Team allyTeam, RobotController rc, AwarenessModule am, MapLocation mapLocation, RobotType type){
        ALLY_TEAM = allyTeam;
        ALLY_TEAM_INDEX = allyTeam.ordinal();
        ENEMY_TEAM = allyTeam.opponent();
        ENEMY_TEAM_INDEX = ENEMY_TEAM.ordinal();
        NEUTRAL_TEAM_INDEX = Team.NEUTRAL.ordinal();
        RC = rc;
        AM = am;
        if(type != RobotType.ENLIGHTENMENT_CENTER){
            ParentEC = getParentEC(mapLocation);
        }
        System.out.println("UT1: "+ Clock.getBytecodeNum());
        relativeLocations[0] = new RelativeLocation(0,0);
        int counter = 1;
        for (int i = 0; i < xLocs.length; i++) {
            int x = xLocs[i];
            int y = yLocs[i];
            relativeLocations[counter++] = new RelativeLocation(x,y);
            relativeLocations[counter++] = new RelativeLocation(-y,x);
            relativeLocations[counter++] = new RelativeLocation(-x,-y);
            relativeLocations[counter++] = new RelativeLocation(y,-x);
        }
        System.out.println("UT2: "+ Clock.getBytecodeNum());
        endingLimits[2] = 9;  //2
        endingLimits[9] = 29; //7
        endingLimits[12] = 37; //9
        endingLimits[20] = 69; //17
        endingLimits[25] = 81; //20
        endingLimits[30] = 97; //24
        endingLimits[40] = 129; //32
        System.out.println("UT3: "+ Clock.getBytecodeNum());
    }

    /**
     * Returns a random Direction.
     *
     * @return a random Direction
     */
    static Direction randomDirection() {
        return directions[(int) (Math.random() * directions.length)];
    }

    /**
     * Returns a random spawnable RobotType
     *
     * @return a random RobotType
     */
    static RobotType randomSpawnableRobotType() {
        return spawnableRobot[(int) (Math.random() * spawnableRobot.length)];
    }


    /**
     * Attempts to move in a given direction.
     *
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMove(Direction dir, RobotController rc) throws GameActionException {
        System.out.println("I am trying to move " + dir + "; " + rc.isReady() + " " + rc.getCooldownTurns() + " " + rc.canMove(dir));
        if (rc.canMove(dir)) {
            rc.move(dir);
            return true;
        } else return false;
    }

    public static int bound(int num, int lb, int ub){
        if(num < lb){
            return lb;
        }else if (num > ub){
            return ub;
        }
        return num;
    }

    public static double bound(double num, double lb, double ub){
        if(num < lb){
            return lb;
        }else if (num > ub){
            return ub;
        }
        return num;
    }

    private static IncompleteRobotInfo getParentEC(MapLocation robotMapLocation){
        int length = AM.numIncompleteRobotInfo[ENLIGHTENMENT_CENTER_INDEX];
        for (int i = 0; i < length; i++) {
            IncompleteRobotInfo possibleParentEC = AM.incompleteRobotInfo[ENLIGHTENMENT_CENTER_INDEX][i];
            if(possibleParentEC.lastSeenMapLocation != null &&
                    possibleParentEC.lastSeenMapLocation.distanceSquaredTo(robotMapLocation) < 3){
                return possibleParentEC;
            }
        }
        return null;
    }

}
