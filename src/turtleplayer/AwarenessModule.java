package turtleplayer;

import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import com.sun.istack.internal.Nullable;

import java.util.*;

public strictfp class AwarenessModule {
    static class IncompleteRobotInfo{
        private final boolean couldBeSlanderer;
        private final RobotType robotType;
        private final boolean isEnemy;
        @Nullable
        private Integer robotId;
        @Nullable
        private MapLocation lastSeenMapLocation;
        @Nullable
        private Integer lastSeenRoundNumber;
        @Nullable
        private Integer lastSeenConviction;

        IncompleteRobotInfo(boolean isEnemy, boolean trueSense, RobotType robotType, Integer robotId,
                            MapLocation mapLocation, Integer roundNumber, Integer conviction){
            this.isEnemy = isEnemy;
            this.couldBeSlanderer = (!trueSense) &&
                    (robotType == RobotType.POLITICIAN || robotType == RobotType.SLANDERER);
            this.robotType = robotType;
            this.robotId = robotId;
            this.lastSeenMapLocation = mapLocation;
            this.lastSeenRoundNumber = roundNumber;
            this.lastSeenConviction = conviction;
        }

        public Optional<Integer> getRobotId(){
            return Optional.ofNullable(robotId);
        }
        public Optional<MapLocation> getLastSeenMapLocation(){
            return Optional.ofNullable(lastSeenMapLocation);
        }
        public Optional<Integer> getLastSeenRoundNumber(){
            return Optional.ofNullable(lastSeenRoundNumber);
        }
        public Optional<Integer> getLastSeenConviction(){
            return Optional.ofNullable(lastSeenConviction);
        }
        public RobotType getRobotType(){
            return robotType;
        }
        public boolean getCouldBeSlanderer(){
            return couldBeSlanderer;
        }
        public boolean isEnemy() {
            return isEnemy;
        }

        public void setLastSeenConviction(Integer lastSeenConviction) {
            this.lastSeenConviction = lastSeenConviction;
        }
        public void setLastSeenMapLocation(MapLocation lastSeenMapLocation) {
            this.lastSeenMapLocation = lastSeenMapLocation;
        }
        public void setLastSeenRoundNumber(Integer lastSeenRoundNumber) {
            this.lastSeenRoundNumber = lastSeenRoundNumber;
        }
        public void setRobotId(Integer robotId) {
            this.robotId = robotId;
        }

    }
    static class PassabilitiyKnowledge{
        private final List<Boolean> flagKnowledge;
        private Double sensedKnowledge;

        private Optional<Double> getSensedKnowledge(){
            return Optional.ofNullable(sensedKnowledge);
        }
        private Optional<Boolean> getFlagKnowledge(int index){
            return Optional.ofNullable(flagKnowledge.get(index));
        }

        PassabilitiyKnowledge(){
            flagKnowledge = new ArrayList<>();
            for (int i = 0; i < 8; i++) {
                flagKnowledge.add(null);
            }
            sensedKnowledge = null;
        }
        boolean haveAnyKnowledge(){
            return getFlagKnowledge(0).isPresent() || getSensedKnowledge().isPresent();
        }
        double bestEstimate(){
            if(getSensedKnowledge().isPresent()){
                return sensedKnowledge;
            }else{
                double estimate =0;
                for (int i = 0; i < 8; i++) {
                    if(getFlagKnowledge(i).isPresent()){
                        if(flagKnowledge.get(i)) {
                            estimate += (1 << (7 - i));
                        }
                    }else{
                        estimate += (1 << (7-i))/2.0f;
                    }
                }
                estimate += 0.5;
                estimate *= (9.0f/256);
                return 1.0/estimate;
            }
        }
        void updateWithSensed(double sensedPassability){
            sensedKnowledge = sensedPassability;
        }
        void updateWithFlag(int bitNumber, boolean bitValue){
            flagKnowledge.set(bitNumber,bitValue);
        }
    }

    private final Set<IncompleteRobotInfo> knownRobotInfo; // (all are guaranteed to be friendly)
    private final Map<MapLocation, PassabilitiyKnowledge> mapKnowledge;
    AwarenessModule(){
        knownRobotInfo = new HashSet<>();
        mapKnowledge = new HashMap<>();
    }

    void update(RobotController rc){
        RobotInfo[] robotInfos = rc.senseNearbyRobots();

    }
}
