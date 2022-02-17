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
    private final static Command NOTHING = new DoNothingCommand();

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

        List<Object> nextBlock = blocks.subList(1,myCar.speed+1);
        List<Object> nextLeft = null;
        List<Object> nextRight = null;

        if (leftBlocks != null) {
            nextLeft = leftBlocks.subList(0,myCar.speed+1);
        }
        if (rightBlocks != null) {
            nextRight = rightBlocks.subList(0,myCar.speed+1);
        }

        //Fix first if too damaged to move so speed can be consistent
        if(myCar.damage >= 3) {
            return FIX;
        }

        // ambil jalan yang tidak ada halangan
        if (nextBlock.contains(Terrain.MUD) || nextBlock.contains(Terrain.OIL_SPILL) || nextBlock.contains(Terrain.WALL) || stuckbehindplayer(myCar, opponent)) {
            // Kalau punya lizard langsung dipakai
            if (hasPowerUp(PowerUps.LIZARD, myCar.powerups)) {
                return LIZARD;
            }
            if (myCar.position.lane == 1) {
                // gak boleh ke kiri
                int rightObstacle = occurences(nextRight);
                int laneObstacle = occurences(nextBlock);
                if (rightObstacle >= laneObstacle) {
                    return ACCELERATE;
                }
                else {
                    return TURN_RIGHT;
                }
            }
            else if (myCar.position.lane == 4) {
                // gak boleh ke kanan;
                int leftObstacle = occurences(nextLeft);
                int laneObstacle = occurences(nextBlock);
                if (leftObstacle >= laneObstacle) {
                    return ACCELERATE;
                }
                else {
                    return TURN_LEFT;
                }
            }
            else {
                if (!(nextLeft.contains(Terrain.MUD) || nextLeft.contains(Terrain.OIL_SPILL) || nextLeft.contains(Terrain.WALL))) {
                    return TURN_LEFT;
                }
                else if (!(nextRight.contains(Terrain.MUD) || nextRight.contains(Terrain.OIL_SPILL) || nextRight.contains(Terrain.WALL))) {
                    return TURN_RIGHT;
                }
                else {
                    int leftObstacle = occurences(nextLeft);
                    int rightObstacle = occurences(nextRight);
                    int laneObstacle = occurences(nextBlock);
                    if (laneObstacle < leftObstacle && laneObstacle < rightObstacle) {
                        return NOTHING;
                    }
                    else if (leftObstacle > rightObstacle) {
                        return TURN_RIGHT;
                    }
                    else if (leftObstacle < rightObstacle) {
                        return TURN_LEFT;
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
        }

        // menggunakan boost jika speed mobil player lebih kecil 5
        if (myCar.speed <= 5){
            if (hasPowerUp(PowerUps.BOOST, myCar.powerups)) {
                return BOOST;
            }
        }

        // Batas aman kondisi damage dan speed myCar

        //menggunakan emp jika ada opponet didepan kita dan lane yang sama
        if (opponent.position.lane == myCar.position.lane && opponent.position.block > myCar.position.block){
            if (hasPowerUp(PowerUps.EMP, myCar.powerups)) {
                return EMP;
            }
        }

        if (hasPowerUp(PowerUps.TWEET, myCar.powerups)){
            Position truck_loc = new Position();
            truck_loc.lane = opponent.position.lane;
            truck_loc.block = opponent.position.block + opponent.speed + 1;
            int next_round_block = myCar.position.block + myCar.speed;
            if (myCar.position.lane == truck_loc.lane && next_round_block <= truck_loc.block) {
                // jangan tweet
                return ACCELERATE;
            }
            else {
                return new TweetCommand(truck_loc.lane , (truck_loc.block));
            }
        }

        //menggunakan oil jika ada opponent di belakang kita sejauh 3/2/1 blocks dari player
        if (opponent.position.lane == myCar.position.lane && myCar.position.block  - opponent.position.block <= 3 && myCar.position.block > opponent.position.block){
            if (hasPowerUp(PowerUps.OIL, myCar.powerups)) {
                return OIL;
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

    // mengembalikan jumlah mud dan oil_spill di block yang akan dilewati
    private int occurences(List obstacles){
        int amountobstacle = 0;
        amountobstacle += Collections.frequency(obstacles, Terrain.MUD);
        amountobstacle += Collections.frequency(obstacles, Terrain.OIL_SPILL);
        return amountobstacle;
    }

    // Memeriksa jika mobil akan terjebak di belakang mobil
    private Boolean stuckbehindplayer(Car playerme, Car playeropponent ){
        int myLane = playerme.position.lane;
        int myNextBlock = playerme.position.block + playerme.speed;
        int oppLane = opponent.position.lane;
        int oppNextBlock = opponent.position.block + opponent.speed;
        if (myLane == oppLane) {
            return (myNextBlock >= oppNextBlock);
        } 
        else {
            return false;
        }
    }

}
