package za.co.entelect.challenge;

import za.co.entelect.challenge.command.*;
import za.co.entelect.challenge.entities.*;
import za.co.entelect.challenge.enums.Terrain;
import za.co.entelect.challenge.enums.PowerUps;

import java.util.*;

import static java.lang.Math.max;

public class Bot {

    private static final int maxSpeed = 9;
    private List<Integer> directionList = new ArrayList<>();

    private Random random;
    private GameState gameState;
    private Car opponent;
    private Car myCar;
    
    private final static Command ACCELERATE = new AccelerateCommand();
    private final static Command LIZARD = new LizardCommand();
    private final static Command OIL = new OilCommand();
    private final static Command BOOST = new BoostCommand();
    private final static Command EMP = new EmpCommand();
    private final static Command FIX = new FixCommand();

    private final static Command TURN_RIGHT = new ChangeLaneCommand(1);
    private final static Command TURN_LEFT = new ChangeLaneCommand(-1);

    public Bot(Random random, GameState gameState) {
        this.random = random;
        this.gameState = gameState;
        this.myCar = gameState.player;
        this.opponent = gameState.opponent;

        directionList.add(-1);
        directionList.add(1);
    }

    public Command run() {
        List<Object> blocks = getBlocksInFront(myCar.position.lane, myCar.position.block);
        List<Object> leftBlocks;
        List<Object> rightBlocks;
        if (myCar.position.lane == 1) {
            leftBlocks = null;
            rightBlocks = getBlocksInFront(myCar.position.lane+1, myCar.position.block);
        }
        else if (myCar.position.lane == 4) {
            leftBlocks = getBlocksInFront(myCar.position.lane-1, myCar.position.block);
            rightBlocks = null;
        }
        else {
            leftBlocks = getBlocksInFront(myCar.position.lane-1, myCar.position.block);
            rightBlocks = getBlocksInFront(myCar.position.lane+1, myCar.position.block);
        }

        /*
        if (myCar.damage >= 5) {
            return new FixCommand();
        }
        if (blocks.contains(Terrain.MUD)) {
            int i = random.nextInt(directionList.size());
            return new ChangeLaneCommand(directionList.get(i));
        }
        return new AccelerateCommand(); */
        	/*
    	kalau ada power up langsung diambil
    	ambil jalan yang tidak ada halangan
    	kalau speedcar < 6 gunakan boost
    	Kalau ada mobil di belakang sejauh 2/1 block
    	Oil
    	Emp
    	Kalo ada mobil di depan
    	Lizard
    	Kalo ada rintangan di depan sama di serong kanan/kiri
    	Tweet
    	Kalo musuh udah 5 di depan kita
    	Kalo ada rintangan di paling kiri atau paling kanan, kasih tweet di sebelah rintangannya
    	Accelerate
    	Kalau damagecar > 2 tidak pakai accelerate , kalau tidak pakai


	Decelerate: think again
    	Fix
    	Kalau damage >= 3 gunakan fix
    	Nothing
    	Turn Left
    	Kalau misal di lane 2,3,4 dan kalau ada obstacle didepan sejauh 1 block
    	Turn Right
    	Kalau misal di lane 1,2,3 dan kalau ada obstacle didepan sejauh 1 block
        */

        // Written by ME :
        List<Object> nextBlock = blocks.subList(1,myCar.speed+2);
        List<Object> nextLeft = null;
        List<Object> nextRight = null;

        if (leftBlocks != null) {
            nextLeft = leftBlocks.subList(0,myCar.speed+2);
        }
        if (rightBlocks != null) {
            nextRight = rightBlocks.subList(0,myCar.speed+2);
        }

        // ambil jalan yang tidak ada halangan
        if (nextBlock.contains(Terrain.MUD) || nextBlock.contains(Terrain.OIL_SPILL) || nextBlock.contains(Terrain.WALL)) {
            // Kalau punya lizard langsung dipakai
            if (hasPowerUp(PowerUps.LIZARD, myCar.powerups)) {
                return LIZARD;
            }
            if (myCar.position.lane == 1) {
                // gak boleh ke kiri
                return TURN_RIGHT;
            }
            else if (myCar.position.lane == 4) {
                // gak boleh ke kanan;
                return TURN_LEFT;
            }
            else {
                if (!(nextLeft.contains(Terrain.MUD) || nextLeft.contains(Terrain.OIL_SPILL) || nextLeft.contains(Terrain.WALL))) {
                    return TURN_LEFT;
                }
                else if (!(nextRight.contains(Terrain.MUD) || nextRight.contains(Terrain.OIL_SPILL) || nextRight.contains(Terrain.WALL))) {
                    return TURN_RIGHT;
                }
                else {
                    Random num = new Random();
                    int low = 0;
                    int high = 1;
                    int res = num.nextInt(high-low) + low;
                    if (res == 0) {
                        return TURN_LEFT;
                    }
                    else {
                        return TURN_RIGHT;
                    }
                }
            }
        }
        
        return ACCELERATE;
    }

    /**
     * Returns map of blocks and the objects in the for the current lanes, returns the amount of blocks that can be
     * traversed at max speed.
     **/
    private List<Object> getBlocksInFront(int lane, int block) {
        List<Lane[]> map = gameState.lanes;
        List<Object> blocks = new ArrayList<>();
        int startBlock = map.get(0)[0].position.block;

        Lane[] laneList = map.get(lane - 1);
        for (int i = max(block - startBlock, 0); i <= block - startBlock + Bot.maxSpeed; i++) {
            if (laneList[i] == null || laneList[i].terrain == Terrain.FINISH) {
                break;
            }

            blocks.add(laneList[i].terrain);

        }
        return blocks;
    }

    private Boolean hasPowerUp(PowerUps powerUpToCheck, PowerUps[] available) {
        for (PowerUps powerUp: available) {
            if (powerUp.equals(powerUpToCheck)) {
                return true;
            }
        }
        return false;
    }

}
