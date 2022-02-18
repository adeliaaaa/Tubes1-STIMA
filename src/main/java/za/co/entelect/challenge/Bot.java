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
        int cyberTruckHere = isThereCyberTruck(myCar);
        List<Object> nextLeft = null;
        List<Object> nextRight= null;
        List<Object> nextBlock = getblockspeed(myCar, myCar.position.lane, myCar.position.block );

        if (myCar.position.lane < 4 ){
            nextRight = getblockspeed(myCar, myCar.position.lane+1,myCar.position.block);
        }
        if (myCar.position.lane > 1){
            nextLeft = getblockspeed( myCar, myCar.position.lane-1,myCar.position.block);
        }

        //Fix first if too damaged to move so speed can be consistent
        if(myCar.damage >= 3) {
            return FIX;
        }

        // ambil jalan yang tidak ada halangan
        if (occurences(nextBlock,cyberTruckHere) > 0 || stuckbehindplayer(myCar, opponent)) {
            // Kalau punya lizard langsung dipakai
            if (hasPowerUp(PowerUps.LIZARD, myCar.powerups)) {
                return LIZARD;
            }
            if (myCar.position.lane == 1) {
                // gak boleh ke kiri
                int rightObstacle = occurences(nextRight,cyberTruckHere);
                int laneObstacle = occurences(nextBlock,cyberTruckHere);
                if (rightObstacle > laneObstacle) {
                    return ACCELERATE;
                }
                else if (rightObstacle == laneObstacle) {
                    // hitung mana yang punya lebih banyak powerups
                    int lanePowerUps = countPowerUps(nextBlock);
                    int rightPowerUps = countPowerUps(nextRight);
                    if (lanePowerUps >= rightPowerUps) {
                        return ACCELERATE;
                    }
                    else {
                        return TURN_RIGHT;
                    }
                }
                else {
                    return TURN_RIGHT;
                }
            }
            else if (myCar.position.lane == 4) {
                // gak boleh ke kanan;
                int leftObstacle = occurences(nextLeft,cyberTruckHere);
                int laneObstacle = occurences(nextBlock,cyberTruckHere);
                if (leftObstacle > laneObstacle) {
                    return ACCELERATE;
                }
                else if (leftObstacle == laneObstacle) {
                    // hitung mana yang punya lebih banyak powerups
                    int lanePowerUps = countPowerUps(nextBlock);
                    int leftPowerUps = countPowerUps(nextLeft);
                    if (lanePowerUps >= leftPowerUps) {
                        return ACCELERATE;
                    }
                    else {
                        return TURN_LEFT;
                    }
                }
                else {
                    return TURN_LEFT;
                }
            }
            else {
                if (occurences(nextLeft,cyberTruckHere) == 0) {
                    return TURN_LEFT;
                }
                else if (occurences(nextRight,cyberTruckHere) == 0) {
                    return TURN_RIGHT;
                }
                else {
                    int leftObstacle = occurences(nextLeft,cyberTruckHere);
                    int rightObstacle = occurences(nextRight,cyberTruckHere);
                    int laneObstacle = occurences(nextBlock,cyberTruckHere);
                    int lanePowerUps = countPowerUps(nextBlock);
                    int rightPowerUps = countPowerUps(nextRight);
                    int leftPowerUps = countPowerUps(nextLeft);
                    if (laneObstacle < leftObstacle && laneObstacle < rightObstacle) {
                        return ACCELERATE;
                    }
                    else if (leftObstacle > rightObstacle) {
                        return TURN_RIGHT;
                    }
                    else if (leftObstacle < rightObstacle) {
                        return TURN_LEFT;
                    }
                    else {
                        if (lanePowerUps >= rightPowerUps && lanePowerUps >= leftPowerUps) {
                            return ACCELERATE;
                        }
                        else if (leftPowerUps > rightPowerUps) {
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

        if (hasPowerUp(PowerUps.BOOST, myCar.powerups) && occurences(nextBlock,cyberTruckHere) == 0 && opponent.position.block > myCar.position.block) {
            return BOOST;
        }

        // batas aman kondisi mobil

        //menggunakan emp jika ada opponet didepan kita dan lane kanan kiri current player
        if ((opponent.position.lane - myCar.position.lane) >= -1 && (opponent.position.lane - myCar.position.lane) <= 1 && opponent.position.block > myCar.position.block){
            if (hasPowerUp(PowerUps.EMP, myCar.powerups) ) {
                return EMP;
            }
        }

        if (hasPowerUp(PowerUps.TWEET, myCar.powerups)){
            Position truck_loc = new Position();
            truck_loc.lane = opponent.position.lane;
            truck_loc.block = opponent.position.block + opponent.speed;
            int next_round_block = myCar.position.block + myCar.speed;
            if (myCar.position.lane == truck_loc.lane && next_round_block == truck_loc.block) {
                // jangan tweet
                return ACCELERATE;
            }
            else {
                return new TweetCommand(truck_loc.lane , truck_loc.block);
            }
        }

        //menggunakan oil jika ada opponent di belakang kita sejauh 3/2/1 blocks dari player
        if (opponent.position.lane == myCar.position.lane && myCar.position.block  - opponent.position.block <= 3 && myCar.position.block > opponent.position.block){
            if (hasPowerUp(PowerUps.OIL, myCar.powerups)) {
                return OIL;
            }
        }

        if (hasPowerUp(PowerUps.BOOST, myCar.powerups) && !(myCar.boosting)) {
            return BOOST;
        }
        
        return ACCELERATE;
    }
    private int isThereCyberTruck(Car player) {
        List<Lane[]> map = gameState.lanes;
        List<Object> blocks = new ArrayList<>();
        int startBlock = map.get(0)[0].position.block;
        int lane = player.position.lane;
        int block = player.position.block;
        int howmany = 0;
        Lane[] laneList = map.get(lane - 1);
        for (int i = max(block - startBlock, 0); i <= block - startBlock + player.speed+2; i++) {
            if (laneList[i].isOccupiedByCyberTruck) howmany++;
            if (laneList[i] == null || laneList[i].terrain == Terrain.FINISH) {
                break;
            }
        }
        return howmany;
    }
    private List<Object> getblockspeed(Car player, int lane, int block) {
        List<Lane[]> map = gameState.lanes;
        List<Object> blocks = new ArrayList<>();
        int startBlock = map.get(0)[0].position.block;

        Lane[] laneList = map.get(lane - 1);
        for (int i = max(block - startBlock, 0); i <=  block - startBlock + player.speed +1; i++) {
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

    // mengembalikan damage obstacles tiap lane block yang bisa dilewati
    private int occurences(List obstacles , int cybertruck){
        int amountobstacle = 0;
        amountobstacle += Collections.frequency(obstacles, Terrain.MUD);
        amountobstacle += Collections.frequency(obstacles, Terrain.OIL_SPILL);
        amountobstacle += Collections.frequency(obstacles, Terrain.WALL) * 2;
        amountobstacle += cybertruck*2;
        return amountobstacle;
    }

    // mengembalikan banyaknya power up tiap lane block yang bisa dilewati
    private int countPowerUps(List lane){
        int amount = 0;
        amount += Collections.frequency(lane, PowerUps.BOOST);
        amount += Collections.frequency(lane, PowerUps.OIL);
        amount += Collections.frequency(lane, PowerUps.TWEET);
        amount += Collections.frequency(lane, PowerUps.LIZARD);
        amount += Collections.frequency(lane, PowerUps.EMP);
        return amount;
    }

    // Memeriksa jika mobil akan terjebak di belakang mobil
    private Boolean stuckbehindplayer(Car playerme, Car playeropponent ){
        int myLane = playerme.position.lane;
        int myBlock = playerme.position.block;
        int myNextBlock = playerme.position.block + playerme.speed;
        int oppLane = opponent.position.lane;
        int oppBlock = opponent.position.block;
        int oppNextBlock = opponent.position.block + opponent.speed;
        if (myLane == oppLane) {
            return (myNextBlock >= oppNextBlock && myBlock < oppBlock);
        } 
        else {
            return false;
        }
    }

}
