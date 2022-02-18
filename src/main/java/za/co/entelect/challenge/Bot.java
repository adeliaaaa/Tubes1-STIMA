package za.co.entelect.challenge;

import za.co.entelect.challenge.command.*;
import za.co.entelect.challenge.entities.*;
import za.co.entelect.challenge.enums.Terrain;
import za.co.entelect.challenge.enums.PowerUps;

import java.util.*;

import static java.lang.Math.max;

public class Bot {

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
        int cyberTruckHere = isThereCyberTruck(myCar,myCar.position.lane);
        int cyberTruckRight = 0;
        int cyberTruckLeft = 0;
        List<Object> nextLeft = null;
        List<Object> nextRight= null;
        List<Object> nextBlock = getblockspeed(myCar, myCar.position.lane, myCar.position.block );
        if (myCar.position.lane < 4 ){
            nextRight = getblockspeed(myCar, myCar.position.lane+1,myCar.position.block);
            cyberTruckRight = isThereCyberTruck(myCar, myCar.position.lane+1);
        }
        if (myCar.position.lane > 1){
            nextLeft = getblockspeed( myCar, myCar.position.lane-1,myCar.position.block);
            cyberTruckLeft = isThereCyberTruck(myCar, myCar.position.lane-1);
        }

        //Greedy by Damage

        //Meminimalisir damage yang diterima
        //Fix first if too damaged to move so speed can be consistent
        if(myCar.damage > 1) {
            return FIX;
        }

        // Meminimalisir damage yang diterima
        // ambil jalan yang tidak ada halangan
        if (occurences(nextBlock,cyberTruckHere) > 0 || stuckbehindplayer(myCar, opponent)) {
            if (myCar.position.lane == 1) {
                // gak boleh ke kiri
                int rightObstacle = occurences(nextRight,cyberTruckRight);
                int laneObstacle = occurences(nextBlock,cyberTruckHere);
                if (rightObstacle != 0 && laneObstacle != 0){
                    if (hasPowerUp(PowerUps.LIZARD, myCar.powerups)) {
                        return LIZARD;
                    }
                }
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
                // gak boleh ke kanan
                int leftObstacle = occurences(nextLeft,cyberTruckLeft);
                int laneObstacle = occurences(nextBlock,cyberTruckHere);
                if (leftObstacle != 0 && laneObstacle != 0){
                    if (hasPowerUp(PowerUps.LIZARD, myCar.powerups)) {
                        return LIZARD;
                    }
                }
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
                if (occurences(nextLeft,cyberTruckLeft) == 0) {
                    return TURN_LEFT;
                }
                else if (occurences(nextRight,cyberTruckRight) == 0) {
                    return TURN_RIGHT;
                }
                else {
                    int leftObstacle = occurences(nextLeft,cyberTruckLeft);
                    int rightObstacle = occurences(nextRight,cyberTruckRight);
                    int laneObstacle = occurences(nextBlock,cyberTruckHere);
                    int lanePowerUps = countPowerUps(nextBlock);
                    int rightPowerUps = countPowerUps(nextRight);
                    int leftPowerUps = countPowerUps(nextLeft);
                    if (laneObstacle != 0 && leftObstacle != 0 && rightObstacle != 0){
                        if (hasPowerUp(PowerUps.LIZARD, myCar.powerups)) {
                            return LIZARD;
                        }    
                    }
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

        // menyelamatkan speed dulu
        if (myCar.speed < 6) {
            if (hasPowerUp(PowerUps.BOOST, myCar.powerups) && !(myCar.boosting)) {
                return BOOST;
            }
            else {
                return ACCELERATE;
            }
        }

        // Batas aman kondisi mobil

        // Memberikan damage pada player musuh sebesar mungkin
        //menggunakan emp jika ada opponet didepan kita dan lane kanan kiri current player
        if ((opponent.position.lane - myCar.position.lane) >= -1 && (opponent.position.lane - myCar.position.lane) <= 1 && opponent.position.block > myCar.position.block){
            if (hasPowerUp(PowerUps.EMP, myCar.powerups) ) {
                return EMP;
            }
        }
        
        // Memberikan damage pada player musuh sebesar mungkin
        // Menggunakan twett
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
        
        // Memberikan damage pada player musuh sebesar mungkin
        //menggunakan oil jika ada opponent di belakang kita sejauh 3/2/1 blocks dari player
        if (hasPowerUp(PowerUps.OIL, myCar.powerups) && opponent.position.block < myCar.position.block) {
            if (opponent.position.lane == myCar.position.lane && ((myCar.position.block  - opponent.position.block <= 3) || (opponent.position.block + opponent.speed >= myCar.position.block) || (occurences(nextBlock, cyberTruckHere) == 0))){
                return OIL;
            }
        }

        // minimalisir damage
        if (hasPowerUp(PowerUps.BOOST, myCar.powerups) && !(myCar.boosting)) {
            return BOOST;
        }
        
        return ACCELERATE;
    }
    
    // mengembalikan jumlah cybertruck pada lane tertentu
    private int isThereCyberTruck(Car player, int lane) {
        List<Lane[]> map = gameState.lanes;
        List<Object> blocks = new ArrayList<>();
        int startBlock = map.get(0)[0].position.block;
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
    // mengembalikan list yang berisi terrain map dari position pemain sampai ke speed pemain + 1
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
    // mengembalikan true jika memiliki powerup yang diperiksa
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
        int maxSpeed;
        if (playerme.speed <= 3) {
            maxSpeed = 6;
        }
        else if (playerme.speed <= 6) {
            maxSpeed = 9;
        }
        else {
            maxSpeed = 15;
        }
        int myNextBlock = playerme.position.block + maxSpeed;
        int oppLane = playeropponent.position.lane;
        int oppBlock = playeropponent.position.block;
        int oppNextBlock = playeropponent.position.block + playeropponent.speed;
        if (myLane == oppLane) {
            return (myNextBlock >= oppNextBlock && myBlock < oppBlock);
        } 
        else {
            return false;
        }
    }

}
