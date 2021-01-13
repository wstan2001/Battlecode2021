package turtleplayer;

import battlecode.common.Direction;
import battlecode.common.RobotType;
import battlecode.common.Team;
import turtleplayer.flagutilities.RelativeLocation;

import java.util.*;

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

    private static final int[] USED_RADIUS_SQUARED_VALUES = new int[] {2, 9, 12, 20, 25, 30 ,50};
    private static final Map<Integer, Set<RelativeLocation>> radiusSquaredRelativeLocations = new HashMap<>();
    private static final Set<RelativeLocation> muckrakerDetectionNotSensed = new HashSet<>();

    private static int radiusSquared(int primaryAxis, int secondaryAxis){
        return primaryAxis*primaryAxis+secondaryAxis*secondaryAxis;
    }
    private static void addAllRotations(Set<RelativeLocation> locationList, int x, int y){
        locationList.add(new RelativeLocation(x,y));
        locationList.add(new RelativeLocation(-y,x));
        locationList.add(new RelativeLocation(-x,-y));
        locationList.add(new RelativeLocation(y,-x));
    }

    public static Set<RelativeLocation> getRelativeLocationsWithin(int radiusSquared){
        return radiusSquaredRelativeLocations.get(radiusSquared);
    }

    public static Set<RelativeLocation> getMuckrakerDetectionNotSensed(){
        return muckrakerDetectionNotSensed;
    }

    static void start(){
        for (int usedRadiusSquaredValue : USED_RADIUS_SQUARED_VALUES) {
            Set<RelativeLocation> locationList = new HashSet<>();
            locationList.add(new RelativeLocation(0, 0));
            int primaryAxis = 1;
            int secondaryAxis = 0;
            while (radiusSquared(primaryAxis, secondaryAxis) <= usedRadiusSquaredValue) {
                while (radiusSquared(primaryAxis, secondaryAxis) <= usedRadiusSquaredValue) {
                    addAllRotations(locationList, primaryAxis, secondaryAxis);
                    secondaryAxis++;
                }
                secondaryAxis = 0;
                primaryAxis++;
            }
            radiusSquaredRelativeLocations.put(usedRadiusSquaredValue, Collections.unmodifiableSet(locationList));
        }
        muckrakerDetectionNotSensed.addAll(
                radiusSquaredRelativeLocations.get(RobotType.MUCKRAKER.detectionRadiusSquared));
        muckrakerDetectionNotSensed.removeAll(
                radiusSquaredRelativeLocations.get(RobotType.MUCKRAKER.sensorRadiusSquared));

    }


}
