package turtleplayer;

import battlecode.common.*;

public strictfp class RobotPlayer {
    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        // This is the RobotController object. You use it to perform actions from this robot,
        // and to get information on its current status.

        System.out.println("I'm a " + rc.getType() + " and I just got created!");
        AwarenessModule aw = new AwarenessModule();
        System.out.println("AW:"+ Clock.getBytecodeNum());
        System.out.println("UT: " + Clock.getBytecodeNum());
        Team team = rc.getTeam();
        Utilities.start(team, rc, aw, rc.getLocation(), rc.getType());
        switch (rc.getType()) {
            case ENLIGHTENMENT_CENTER: EnlightenmentCenter.start(rc, aw); break;
            case POLITICIAN:           Politician.start(rc, aw);          break;
            case SLANDERER:            Slanderer.start(rc, aw);           break;
            case MUCKRAKER:            Muckraker.start(rc, aw);           break;
        }
        System.out.println("START: " + Clock.getBytecodeNum());
        while (true) {
            try {
                MapLocation location = rc.getLocation();
                int roundNumber = rc.getRoundNum();
                aw.update(rc,true,rc.getRoundNum(), rc.getType(), location);
                System.out.println("UPDATE: "+Clock.getBytecodeNum());
                switch (rc.getType()) {
                    case ENLIGHTENMENT_CENTER: EnlightenmentCenter.processTurn(location, roundNumber); break;
                    case POLITICIAN:           Politician.processTurn();          break;
                    case SLANDERER:            Slanderer.processTurn();           break;
                    case MUCKRAKER:            Muckraker.processTurn();           break;
                }
                Clock.yield();
            } catch (Exception e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
            }
        }
    }

    private static void checkByteCodeCost(){
        int before = Clock.getBytecodeNum();
        int after = Clock.getBytecodeNum();
        int clockGetBytecodeCost = after- before;
        System.out.println("Clock.getBytecodeNum(): " + clockGetBytecodeCost);
        before = Clock.getBytecodeNum();
        System.out.println("Test"+ before);
        after = Clock.getBytecodeNum();
        int sysoutCost = after-before-clockGetBytecodeCost;
        System.out.println("Sysout cost:" + clockGetBytecodeCost);
        int totalCost = sysoutCost + clockGetBytecodeCost;
        int cost;

        int a = 233;
        int b = 168;

        before = Clock.getBytecodeNum();
        System.out.println(a+b);
        after = Clock.getBytecodeNum();
        cost = after - before;
        System.out.println("Add cost: "+ cost);


        before = Clock.getBytecodeNum();
        System.out.println(a-b);
        after = Clock.getBytecodeNum();
        cost = after - before;
        System.out.println("Sub cost: "+ cost);


        before = Clock.getBytecodeNum();
        System.out.println(a*b);
        after = Clock.getBytecodeNum();
        cost = after - before;
        System.out.println("Mult cost: "+ cost);


        before = Clock.getBytecodeNum();
        System.out.println(multiply(a,b));
        after = Clock.getBytecodeNum();
        cost = after - before;
        System.out.println("Mult funct cost: "+ cost);


        before = Clock.getBytecodeNum();
        System.out.println(a*2);
        System.out.println(a*3);
        System.out.println(a*4);
        System.out.println(a*5);
        System.out.println(a*6);
        System.out.println(a*7);
        System.out.println(a*8);
        System.out.println(a*9);
        System.out.println(a*10);
        System.out.println(a*11);
        after = Clock.getBytecodeNum();
        cost = after - before;
        System.out.println("Mult 5 cost: "+ cost);


        before = Clock.getBytecodeNum();
        for (int i = 2; i <= 11; i++) {
            System.out.println(a*i);
        }
        after = Clock.getBytecodeNum();
        cost = after - before;
        System.out.println("Mult 5 for loop cost: "+ cost);



        before = Clock.getBytecodeNum();
        System.out.println(a/b);
        after = Clock.getBytecodeNum();
        cost = after - before;
        System.out.println("Div cost: "+ cost);



        before = Clock.getBytecodeNum();
        MapLocation c = new MapLocation(100,56);
        after = Clock.getBytecodeNum();
        cost = after - before;
        System.out.println("Maplocation initialize cost: "+ cost);


        MapLocation d = new MapLocation(245,5678);


        before = Clock.getBytecodeNum();
        System.out.println(new MapLocation(c.x+d.x,c.y+d.y));
        after = Clock.getBytecodeNum();
        cost = after - before;
        System.out.println("MapLocation cost: "+ cost);


        before = Clock.getBytecodeNum();
        System.out.println(sum(c,d));
        after = Clock.getBytecodeNum();
        cost = after - before;
        System.out.println("MapLocation func cost: "+ cost);



    }

    private static int  multiply(int a, int b){
        return a * b;
    }

    private static MapLocation sum(MapLocation a, MapLocation b){
        return new MapLocation(a.x+b.x,a.y+b.y);
    }

}

