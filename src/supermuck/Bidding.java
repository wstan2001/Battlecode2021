package supermuck;

import battlecode.common.GameActionException;

import static supermuck.RobotPlayer.*;

public class Bidding {
    static int previousNumVotes = 0;
    static int previousBidAmount = 0;
    static double aggression=2;
    static double avgLosingBid=5;
    static int numLosingBids = 1;
    static boolean previousRoundSkipped =false;

    static double getAggressionDecayRate(int roundNum, double proportionVotesNecessary){
        return 0.7 + (roundNum*0.1/1500.0) + Math.max(0.0,Math.min(proportionVotesNecessary-0.45,0.2))/2.0;
    }
    static double getAggressionIncreaseRate(int roundNum, double proportionVotesNecessary){
        return 2 + roundNum*0.5/1500 + Math.max(0.0,Math.min(proportionVotesNecessary-0.45,0.2))*4;
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
        double proportionVotesNecessary = (751.0-numTeamVotes)/(1500.01-roundNumber);
        boolean hasWonBid = numTeamVotes > previousNumVotes;
        if(!previousRoundSkipped) {
            if (hasWonBid) {
                //System.out.println("Won, start with: "+ aggression);
                aggression *= getAggressionDecayRate(roundNumber, proportionVotesNecessary);
                //System.out.println("Won, end with: "+ aggression);
            } else {
                avgLosingBid = (avgLosingBid * ((double) numLosingBids) / (numLosingBids + 1.0)) + previousBidAmount / (numLosingBids + 1.0);
                numLosingBids++;
                //System.out.println("Lost, start with: "+ aggression);
                aggression += getAggressionIncreaseRate(roundNumber, proportionVotesNecessary);
                //System.out.println("Lost, end with: "+ aggression);
            }
        }
        // we want at 0.8+, bid every turn, at 0.5 bid frequently, at 0.3- bid sometimes, 0.1
        double minBidFrequency = ((double)roundNumber*roundNumber/0.8e7)+0.25;
        double biddingFrequency = minBidFrequency+Math.min(0.8,proportionVotesNecessary)*(1.0/0.8)*(1-minBidFrequency);
        double exceedingFactor = (Math.min(200,Math.max(100,aggression))/100)- 1;
        boolean shouldSkip = rng.nextDouble() <= exceedingFactor*exceedingFactor || rng.nextDouble() > biddingFrequency;
        int bidAmount;
        if(shouldSkip){
            previousRoundSkipped = true;
            bidAmount = 0;
        }else {
            if (influenceLeft < 100) { // slow down bro leave some for the others
                previousRoundSkipped = true;
                bidAmount = 0;
            }
            else if (aggression > 0.1 * influenceLeft) {
                previousRoundSkipped = true;
                bidAmount = 0;
                //need to build up more influence before bidding again
            }
            else {
                previousRoundSkipped =false;
                bidAmount = convToIntRandomly(aggression);
            }
        }
        previousNumVotes = numTeamVotes;

        previousBidAmount = bidAmount;
        if(rc.canBid(bidAmount) && numTeamVotes < 751) {
            rc.bid(bidAmount);
        }
    }
}
