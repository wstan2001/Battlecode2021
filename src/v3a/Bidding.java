package v3a;

import battlecode.common.GameActionException;

import static v3a.RobotPlayer.rc;
import static v3a.RobotPlayer.rng;

public class Bidding {
    static int previousNumVotes = 0;
    static int previousBidAmount = 0;
    static double aggression=2;
    static double avgLosingBid=5;
    static int numLosingBids = 1;
    static int numConsecutiveLosses = 0;
    static int numConsecutiveWins = 0;
    static int numRoundsToSkip = 0;
    static boolean previousRoundSkipped =false;
    static boolean hasWonSkipped =false;

    static double getAggressionDecayRate(int roundNum, double proportionVotesNecessary, double aggressionPercentageInfluence){
        return 0.75 + (roundNum*0.1/1500.0) + Math.max(0.0,Math.min(proportionVotesNecessary-0.45,0.2))/2.0 -2.5*Math.min(0.04, aggressionPercentageInfluence);
    }
    static double getAggressionIncreaseRate(int roundNum, double proportionVotesNecessary, double aggressionPercentageInfluence){
        return 1.5 + roundNum*1.5/1500 + Math.max(0.0,Math.min(proportionVotesNecessary-0.45,0.5))*4 + 0.05*Math.max(100,1/Math.min(0.04,aggressionPercentageInfluence));
    }
    static int convToIntRandomly(double d){
        int lb = (int) d;
        double prob = d - lb;
        if(prob > rng.nextDouble()){
            return lb + 1;
        }else{
            return lb;
        }
    }

    static void bid() throws GameActionException {
        int numTeamVotes = rc.getTeamVotes();
        int roundNumber = rc.getRoundNum();
        int influenceLeft = rc.getInfluence();
        double aggressionProportionInfluence = aggression/influenceLeft;
        double proportionVotesNecessary = (751.0-numTeamVotes)/(1500.01-roundNumber);
        boolean hasWonBid = numTeamVotes > previousNumVotes;
        if(hasWonBid){
            numConsecutiveLosses = 0;
            numConsecutiveWins++;
        }else{
            numConsecutiveLosses++;
            numConsecutiveWins = 0;
        }
        if(!previousRoundSkipped) {
            if (hasWonBid) {
                //System.out.println("Won, start with: "+ aggression);
                if(numConsecutiveWins >= 3) {
                    aggression *= getAggressionDecayRate(roundNumber, proportionVotesNecessary, aggressionProportionInfluence);
                    if(aggression < 1.5){
                        aggression = 1.5;
                    }
                }
                //System.out.println("Won, end with: "+ aggression);
            } else {
                avgLosingBid = (avgLosingBid * ((double) numLosingBids) / (numLosingBids + 1.0)) + previousBidAmount / (numLosingBids + 1.0);
                numLosingBids++;
                //System.out.println("Lost, start with: "+ aggression);
                aggression += getAggressionIncreaseRate(roundNumber, proportionVotesNecessary, aggressionProportionInfluence)*numConsecutiveLosses;
                //System.out.println("Lost, end with: "+ aggression);
            }
        }else{
            if(hasWonBid){
                hasWonSkipped = true;
            }
        }
        if(!hasWonBid){
            hasWonSkipped = false;
        }
        if(!hasWonSkipped && influenceLeft > 500 && influenceLeft > 30*aggression){
            numRoundsToSkip = 0;
        }
        // we want at 0.8+, bid every turn, at 0.5 bid frequently, at 0.3- bid sometimes, 0.1
        double minBidFrequency = ((double)roundNumber*roundNumber/0.8e7)+0.25;
        double biddingFrequency = minBidFrequency+Math.min(0.8,proportionVotesNecessary)*(1.0/0.8)*(1-minBidFrequency);
        double exceedingFactor = (Math.min(200,Math.max(100,aggression))/100)- 1;
        boolean shouldSkip = rng.nextDouble() <= exceedingFactor*exceedingFactor
                || rng.nextDouble() > biddingFrequency
                || (hasWonSkipped && proportionVotesNecessary < 0.49)
                || numRoundsToSkip > 0;
        numRoundsToSkip--;
        int bidAmount;
        if(shouldSkip){
            previousRoundSkipped = true;
            bidAmount = 0;
        }else {
            if (influenceLeft < 100) { // slow down bro leave some for the others
                previousRoundSkipped = true;
                numRoundsToSkip = 10;
                bidAmount = 0;
            }
            else if (aggression > 0.1 * influenceLeft) {
                previousRoundSkipped = true;
                numRoundsToSkip = 10;
                bidAmount = 0;
                //need to build up more influence before bidding again
            }
            else {
                previousRoundSkipped =false;
                bidAmount = convToIntRandomly(aggression);
            }
        }
        if(previousRoundSkipped){
            if(influenceLeft > 50 && rng.nextDouble() < 0.8){
                bidAmount = rng.nextInt(4);
            }else {
                bidAmount = 0;
            }
        }
        previousNumVotes = numTeamVotes;

        previousBidAmount = bidAmount;
        if(rc.canBid(bidAmount) && numTeamVotes < 751) {
            rc.bid(bidAmount);
        }
    }
}
