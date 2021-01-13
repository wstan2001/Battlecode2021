package turtleplayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PassabilityKnowledge{
    private final List<Boolean> flagKnowledge;
    private Double sensedKnowledge;

    private Optional<Double> getSensedKnowledge(){
        return Optional.ofNullable(sensedKnowledge);
    }
    private Optional<Boolean> getFlagKnowledge(int index){
        return Optional.ofNullable(flagKnowledge.get(index));
    }

    PassabilityKnowledge(){
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