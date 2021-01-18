package turtleplayer;

import battlecode.common.Clock;

public class PassabilityKnowledge{
    public boolean[] flagKnowledge;
    public boolean[] validFlagKnowledge;
    public double sensedKnowledge;

    PassabilityKnowledge(double sensedKnowledge){
        this.sensedKnowledge = sensedKnowledge;
    }

    PassabilityKnowledge(boolean flagBit, int bitNumber){
        flagKnowledge = new boolean[8];
        flagKnowledge[bitNumber] =  flagBit;
        validFlagKnowledge = new boolean[8];
        sensedKnowledge = -1.0;
    }

    PassabilityKnowledge(){
        flagKnowledge = new boolean[8];
        validFlagKnowledge = new boolean[8];
        sensedKnowledge = -1.0;
    }

    boolean haveAnyKnowledge(){
        return sensedKnowledge > -0.5 || validFlagKnowledge[0];
    }
    double bestEstimate(){
        if(sensedKnowledge > -0.5){
            return sensedKnowledge;
        }else{
            double estimate =0;
            for (int i = 0; i < 8; i++) {
                if(validFlagKnowledge[i]){
                    if(flagKnowledge[i]) {
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
}