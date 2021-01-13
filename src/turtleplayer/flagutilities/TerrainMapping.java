package turtleplayer.flagutilities;

import battlecode.common.Direction;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/*
    implementation of FlagMessage for a flag signalling the robot's status
 */
public strictfp class TerrainMapping implements FlagMessage{

    private final int bitNumber;
    private final Direction direction;
    private final boolean[] locationTurnsBit;

    /*
     * requires 0 <= bitNumber <= 7, where 0 is the most significant and 7 the least
     */
    public TerrainMapping(int bitNumber, Direction direction, double[] passabilities){
        this.bitNumber= bitNumber;
        locationTurnsBit = new boolean[15];
        for (int i = 0; i < 15; i++) {
            double turns = 1.0/passabilities[i]; // 1- 10
            int toBits = Math.min(255,(int) ((turns-1)*256/9));
            locationTurnsBit[i] = ((toBits >>> (7-bitNumber)) & 1) == 1;
        }
        this.direction = direction;
    }

    private final static int PREFIX_BIT_MASK = 0x00F00000;
    private final static int PREFIX_CORRECT = 0x00800000;

    public static boolean hasCorrectPrefix(int flagCode){
        return (flagCode & PREFIX_BIT_MASK) == PREFIX_CORRECT;
    }

    public TerrainMapping(int flagCode){
        this.locationTurnsBit = new boolean[15];
        for (int i = 0; i < 15; i++) {
            locationTurnsBit[i] = ((flagCode >>> i) & 1) == 1;
        }
        int directionNumber = FlagUtilities.getPartOfFlag(flagCode,16,15);
        this.bitNumber = FlagUtilities.getPartOfFlag(flagCode,19,17);
        switch(directionNumber){
            case 0:{
                this.direction = Direction.NORTH;
                break;
            }
            case 1:{
                this.direction = Direction.EAST;
                break;
            }
            case 2: {
                this.direction = Direction.SOUTH;
                break;
            }
            default: {
                this.direction = Direction.WEST;
            }
        }
    }

    public int getBitNumber() {
        return bitNumber;
    }

    public Map<RelativeLocation, Boolean> getTurnBits(){
        int width = (direction == Direction.NORTH || direction == Direction.SOUTH)? 3: 5;
        int height = (direction == Direction.WEST || direction == Direction.EAST) ? 3 : 5;
        int startingX, startingY;
        if(direction == Direction.WEST ){
            startingX = -4;
            startingY = -2;
        }else if(direction == Direction.EAST){
            startingX = 0;
            startingY = -2;
        }else if(direction == Direction.NORTH){
            startingX = -2;
            startingY = 0;
        }else if(direction == Direction.SOUTH){
            startingX = -2;
            startingY = -4;
        }else{
            System.err.println("INVALID DIRECTION SELECTED");
            return null;
        }
        int endingX = startingX+width;
        int endingY = startingY+ height;
        Map<RelativeLocation,Boolean> turnBits = new HashMap<>();
        int counter = 0;
        for (int x = startingX; x < endingX; x++) {
            for(int y= startingY; y < endingY; y++){
                turnBits.put(new RelativeLocation(x,y),locationTurnsBit[counter]);
                counter++;
            }
        }
        return turnBits;
    }

    public int getFlagCode(){
        int turnsFlagCode = FlagUtilities.buildFlagCode(locationTurnsBit);
        int directionNumber;
        switch(direction){
            case NORTH:{
                directionNumber = 0;
                break;
            }
            case EAST:{
                directionNumber = 1;
                break;
            }
            case SOUTH:{
                directionNumber = 2;
                break;
            }
            default:{
                directionNumber = 3;
            }
        }
        return FlagUtilities.getFlag(
                new int[]{
                        turnsFlagCode,
                        directionNumber,
                        bitNumber,
                        0x08
                }, new int[]{
                        15,2,3,4
                }
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TerrainMapping that = (TerrainMapping) o;
        return bitNumber == that.bitNumber && direction == that.direction && Arrays.equals(locationTurnsBit, that.locationTurnsBit);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(bitNumber, direction);
        result = 31 * result + Arrays.hashCode(locationTurnsBit);
        return result;
    }

    @Override
    public String toString() {
        return "TerrainMapping{" +
                "bitNumber=" + bitNumber +
                ", direction=" + direction +
                ", locationTurnsBit=" + Arrays.toString(locationTurnsBit) +
                '}';
    }
}
