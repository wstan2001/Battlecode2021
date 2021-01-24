package v3b;

public strictfp class Flag {

    public enum OPCODE {
        MOVE, //move to specified location   [0(4) | x-coord(6) | y-coord (6) | random data (8)]
        SCOUT, //tells units to scout [1(4) | random data (20)]
        TROOP,  //general infantry politician
        ENEMYEC, // units sending coordinates of important locations [5(4) | x_coord (6) | y_coord (6) | data (8)] data will depend on information it sends
        NEUTRALEC,
        ALLYEC,
        BOMB,
        STALK,
        SLAND,
        INVALID
        //example: let's say we want to tell a unit to move to (15, 16). 15 = 001111 and 16 = 010000, the move opcode is 0000, and the data is 8 random bits. So a valid opcode for this would be
        //0000 | 001111 | 010000 | 10101010 = 245913.
        //a scout opcode would be something like 0001 | 10101010101010101010, since we don't care about the last 20 bits.
    }

    static int opcodeToInt(OPCODE op) //need to be changed when new opcode added
    {
        switch(op)
        {
            case MOVE: return 0;
            case SCOUT: return 1;
            case TROOP: return 2;                   //general low influence poli for defense
            case ENEMYEC: return 3;                 //data should contain floor of (enemy EC's influence / 20)
            case NEUTRALEC: return 4;
            case ALLYEC: return 5;
            case BOMB: return 6;                //politician with lots of influence that has target location
            case STALK: return 7;               //rightmost 20 bits will report the ID of the muckraker it is stalking
            case SLAND: return 8;               //slanderers will display this flag, giving allies true sense, rightmost 20 bits has RoundNum of creation
        }
        return -1;
    }
    static OPCODE intToOpcode(int op) //need to be changed when new opcode added
    {
        switch(op)
        {
            case 0: return OPCODE.MOVE;
            case 1: return OPCODE.SCOUT;
            case 2: return OPCODE.TROOP;
            case 3: return OPCODE.ENEMYEC;
            case 4: return OPCODE.NEUTRALEC;
            case 5: return OPCODE.ALLYEC;
            case 6: return  OPCODE.BOMB;
            case 7: return OPCODE.STALK;
            case 8: return OPCODE.SLAND;
        }
        return OPCODE.INVALID;
    }
    static int encodeInstruction(OPCODE op, int x, int y, int data) //MOVE, SCOUT, NEEDECID, etc
    {
        if(opcodeToInt(op) == -1)
            System.out.println("INVALID OPCODE RECEIVED");
        return (opcodeToInt(op) << 20) + (x << 13) + (y << 6) + data;
    }
    static OPCODE opcode(int instr)
    {
        if(intToOpcode((instr >> 20) % 16) == OPCODE.INVALID)
            System.out.println("INVALID INSTRUCTION RECEIVED");
        return intToOpcode((instr >> 20) % 16);
    }
    static int instrX(int instr)
    {
        return (instr >> 13) % 128;
    }
    static int instrY(int instr)
    {
        return (instr >> 6) % 128;
    }
    static int instrData(int instr)
    {
        return instr % 64;
    }
}
