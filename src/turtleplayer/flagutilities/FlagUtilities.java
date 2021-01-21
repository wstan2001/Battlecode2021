package turtleplayer.flagutilities;

import static turtleplayer.Utilities.bound;

public strictfp class FlagUtilities {

    enum FlagMeaning{
        ENEMY_PRESENT,       // Alerts ECs and nearby Robots to the presence of enemies
                                  // contains Enemy (Relative) Location, Type, Influence
        FRIENDLY_PRESENT,    // Broadcasts the presence of friendlies
                                  // contains Friendly (Relative) Location, Type, Influence
        FRIENDLY_ID_PART_1,  // Broadcasts the ID of friendly units in 2 parts
        FRIENDLY_ID_PART_2,       // contains influence
                                  // used by units to communicate other EC's units' for their flags to be seen by
                                  // their EC and then communicate their EC flag for both ECs to know each other
        SELF_INFO,           // Broadcasts information about the unit itself
        TERRAIN_MAPPING,     // Broadcasts the passabilities of nearby locations
        INVALID
    }

    public static FlagMessage decodeFlag(int flagCode){
        int prefix = getPartOfFlag(flagCode,23,20);
        switch(prefix){
            case 0x0F:
            case 0x0E:
            case 0x0D:
            case 0x0C:
                return new UnitPresent(flagCode);
            case 0x0B: return new IDMessage(flagCode);
            case 0x0A: return new MoveCommand(flagCode);
            case 0x09: return new SelfStatus(flagCode);
            case 0x08: return new TerrainMapping(flagCode);
            default: return new InvalidFlagMessage();
        }
    }

    /*
     * returns part of an integer from [bitHigh:bitLow] inclusive
     *
     * requires 31 >= bitHigh >= bitLow >= 0
     *
     * @param flagCode the integer
     * @param bitHigh the largest index of the part of the flagCode
     * @param bitLow the smallest index of the part of the flagCode
     */
    static int getPartOfFlag(int flagCode, int bitHigh, int bitLow){
        //System.out.println((flagCode << (31-bitHigh)) >>> (31+bitLow-bitHigh));
        return (flagCode << (31-bitHigh)) >>> (31+bitLow-bitHigh);
    }

    /*
     * constructs a flagCode given the parts and the length of each part, in order, such that the first part
     * occupies the least significant bits
     * requires parts.length == bitLengths.length
     * requires sum of bitLengths.length = 24
     */
    static int getFlag(int[] parts, int[] bitLengths){
        int totalShiftSoFar = 0;
        int flagCode = 0;
        for (int i = 0; i < parts.length; i++) {
            //System.out.println("part "+ parts[i] + ", flagCode: " + flagCode + ", bounded: "+ bound(parts[i],0,(1 << bitLengths[i]) -1));
            flagCode |= (bound(parts[i],0,(1 << bitLengths[i]) -1)<< totalShiftSoFar);
            totalShiftSoFar += bitLengths[i];
        }
        return flagCode;
    }

    static int buildFlagCode(boolean[] bits){
        int flagCode = 0;
        for (int i = 0; i < bits.length; i++) {
            if(bits[i]) {
                flagCode |= (1 << i);
            }
        }
        return flagCode;
    }

}
