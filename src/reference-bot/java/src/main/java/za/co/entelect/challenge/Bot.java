package za.co.entelect.challenge;

import com.sun.org.apache.xpath.internal.operations.Bool;
import za.co.entelect.challenge.command.*;
import za.co.entelect.challenge.entities.*;
import za.co.entelect.challenge.enums.PowerUps;
import za.co.entelect.challenge.enums.Terrain;

import java.util.*;

import static java.lang.Math.max;

import java.security.SecureRandom;

public class Bot {

    private static final int maxSpeed = 9;
    private List<Command> directionList = new ArrayList<>();

    private final Random random;

    private final static Command ACCELERATE = new AccelerateCommand();
    private final static Command LIZARD = new LizardCommand();
    private final static Command OIL = new OilCommand();
    private final static Command BOOST = new BoostCommand();
    private final static Command EMP = new EmpCommand();
    private final static Command FIX = new FixCommand();

    private final static Command TURN_RIGHT = new ChangeLaneCommand(1);
    private final static Command TURN_LEFT = new ChangeLaneCommand(-1);

    public Bot() {
        this.random = new SecureRandom();
        directionList.add(TURN_LEFT);
        directionList.add(TURN_RIGHT);
    }

    public Command run(GameState gameState) {
        Car myCar = gameState.player;
        Car opponent = gameState.opponent;

        //Looking ahead of the car
        List<Object> blocks = getBlocksInFront(myCar.position.lane, myCar.position.block, gameState);
        List<Object> nextBlocks = blocks.subList(0,1);


        //Default

        //Using Powerups which are less likely to happen

        //menggunakan emp jika ada opponet didepan kita dan lane yang sama
        if (opponent.position.lane == myCar.position.lane && opponent.position.block > myCar.position.block){
            if (hasPowerUp(PowerUps.EMP, myCar.powerups)) {
                return EMP;
            }
        }
        //menggunakan oil jika ada opponent di belakang kita sejauh 3/2/1 blocks dari player
        if (opponent.position.lane == myCar.position.lane && myCar.position.block  - opponent.position.block <= 3 && myCar.position.block > opponent.position.block){
            if (hasPowerUp(PowerUps.OIL, myCar.powerups)) {
                return OIL;
            }
        }
        //Fix first if too damaged to move so speed can be consistent
        if(myCar.damage >= 3) {
            return FIX;
        }

        // menggunakan boost jika speed mobil player lebih kecil 5
        if (myCar.speed <= 5){
            if (hasPowerUp(PowerUps.BOOST, myCar.powerups)) {
                return BOOST;
            }
            else{
                return ACCELERATE;
            }
        }

        if (hasPowerUp(PowerUps.TWEET, myCar.powerups)){
            return new TweetCommand(opponent.position.lane , (opponent.position.block + opponent.speed+1));
        }

        //Basic avoidance logic dan lane changing

        //memeriksa jika blocks player ke depannya memiliki rintangan wall atau 2 objek mud/spill atau terjebak di belakang player
        if(blocks.contains(Terrain.WALL) || occurences(blocks) >= 2 || stuckbehindplayer(myCar,opponent)){
            if (hasPowerUp(PowerUps.LIZARD, myCar.powerups)){
                return LIZARD;
            }
            // pindah kalau lane sebelahnya memiliki rintangan yang lebih kecil
            if (myCar.position.lane == 4 && occurences(getBlocksInFront(myCar.position.lane-1, myCar.position.block, gameState)) < 2){
                return TURN_LEFT;
            }
            else if (myCar.position.lane == 1 && occurences(getBlocksInFront(myCar.position.lane+1, myCar.position.block, gameState)) < 2){
                return TURN_RIGHT;
            }
            else if (myCar.position.lane == 2 || myCar.position.lane == 3) {
                if (occurences(getBlocksInFront(myCar.position.lane+1, myCar.position.block, gameState)) < occurences(getBlocksInFront(myCar.position.lane-1, myCar.position.block, gameState))){
                    return TURN_RIGHT;
                }
                else{
                    return TURN_LEFT;
                }
            }
        }else{
            return ACCELERATE;
        }
        return ACCELERATE;
    }

    private Boolean hasPowerUp(PowerUps powerUpToCheck, PowerUps[] available) {
        for (PowerUps powerUp: available) {
            if (powerUp.equals(powerUpToCheck)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns map of blocks and the objects in the for the current lanes, returns
     * the amount of blocks that can be traversed at max speed.
     **/
    private List<Object> getBlocksInFront(int lane, int block, GameState gameState) {
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
    // mengembalikan jumlah mud dan oil_spill di block yang akan dilewati
    private int occurences(List obstacles){
        int amountobstacle = 0;
        amountobstacle += Collections.frequency(obstacles, Terrain.MUD);
        amountobstacle += Collections.frequency(obstacles, Terrain.OIL_SPILL);
        return amountobstacle;
    }
    // Memeriksa jika mobil akan terjebak di belakang mobil
    private Boolean stuckbehindplayer(Car playerme, Car playeropponent ){
        if (playerme.position.block < playeropponent.position.block) {
            return (playerme.position.lane == playeropponent.position.lane && (playerme.position.block + playerme.speed - playeropponent.position.block + playeropponent.speed) <= 0);
        }
        else{
            return (playeropponent.position.block - 1 == playerme.position.block);
        }
    }
}
