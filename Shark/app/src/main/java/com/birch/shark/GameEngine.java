package com.birch.shark;

import android.view.MotionEvent;

/**
 * Created by Nick on 12/12/2014.
 */
public class GameEngine {
    public static final int MAX_EXPLODE_FRAME = 3;

    private static final int MAX_BOOST_FRAME = 15;
    private static final int MAX_DIFFICULTY = 40;
    private static final int MULTIPLIER_BOOST = 6;
    private static final int MULTIPLIER_DEFAULT = 1;
    private static final int DIFFICULTY_INTERVAL = 500; //frames elapsed until difficulty increment
    private static final int DIFFICULTY_STEP = 5; //difficulty increment amount
    private static final int WARNING_INTERVAL = 30;
    private static final int TIME_STEP = 30;
    private static final int ENGINE_HEAT_RATE = 5;
    private static final int ENGINE_COOL_RATE = 1;

    private static int nextBarrier = 0; //the next barrier to leave screen
    private static int explodeFrame = 0;
    private static int boostFrame = 0;
    private static int frame = 0;
    private static int difficulty = 0;
    private static boolean falling = true;
    private static boolean boosting = false;
    private static long prevTime = 0;

    public static void setFalling(boolean T) { falling = T;}
    public static void setBoosting(boolean T) { boosting = T;}
    public static boolean isBoosting() { return boosting;}

    public static boolean gameLoop(GameData data){
        //returns false when the game is over, true otherwise
        //side effect updates the game state
        if(data == null){return false;} //fail gracefully

        final long time = System.currentTimeMillis() ;
        if(time - prevTime < TIME_STEP){ //fix time step
            return true;
        } else {
            prevTime = time;
        }

        ++frame;
        if(frame % DIFFICULTY_INTERVAL == 0 && difficulty < MAX_DIFFICULTY){
            difficulty = difficulty + DIFFICULTY_STEP;
        }
        if(boosting){
            if(boostFrame == 0){
                data.mHeat += ENGINE_HEAT_RATE;
            }
            ++boostFrame;
            if(boostFrame >= MAX_BOOST_FRAME){
                boostFrame = 0;
                boosting = false;
            }
        }

        //open/close the barriers
        openBarriers(data);
        if(checkCollision(data)){
            resetEngine();
            return false; //game over
        }

        final int multiplier;
        if(boosting){
            multiplier = MULTIPLIER_BOOST;
        }
        else{
            multiplier = MULTIPLIER_DEFAULT;
        }

        data.mPoints += multiplier * GameData.POINTS_INCREMENT;

        for(int i = 0; i < GameData.MAX_NUM_OF_BARRIERS; ++i){
            data.barriers[i] -= multiplier * GameData.GAME_SPEED; //Move the barriers forward
            if(data.barriers[i] < 0){ //Check for barriers going off edge
                data.refreshBarrier(i);
                if(i == nextBarrier){
                    ++nextBarrier;
                    nextBarrier %= GameData.MAX_NUM_OF_BARRIERS;
                }
            }
        }

        //make the player fall/rise
        //bounds check to avoid program crash
        //we don't fall while boosting
        if(!boosting) {
            if(data.mHeat - ENGINE_COOL_RATE >= 0) {
                data.mHeat -= ENGINE_COOL_RATE;
            }
            if (!falling) {
                final int nextHeight = data.mHeight - GameData.DROP_SPEED;
                if (nextHeight > 0) {
                    data.mHeight = nextHeight;
                }
            } else { //move player upwards while not falling
                final int nextHeight = data.mHeight + GameData.DROP_SPEED;
                if (nextHeight + GameData.PLAYER_HEIGHT < GameData.SCREEN_HEIGHT) {
                    data.mHeight = nextHeight;
                }
            }
        }

        return true; //continue game
    }

    private static void openBarriers(GameData data){
    //openBarriers must be called before checkCollision
    //because it opens certain barriers on collision,
    //which would otherwise cause an incorrect game over
        for(int i = 0; i < GameData.MAX_NUM_OF_BARRIERS; ++i){
            if(data.cracked[i]){
                final int leftmost = GameData.PLAYER_OFFSET;
                final int rightmost = GameData.PLAYER_OFFSET + GameData.PLAYER_WIDTH;
                if(boosting){
                    if(rightmost > data.barriers[i] && leftmost < data.barriers[i] + GameData.BARRIER_WIDTH){
                        if(!(data.mHeight + GameData.PLAYER_HEIGHT > data.gapHeight[i] + GameData.BARRIER_GAP_HEIGHT && data.mHeight < data.gapHeight[i])){
                            data.gapOpen[i] = true;
                        }
                    }
                }
                continue;
            }
            data.framesOpen[i]++;
            if(data.framesOpen[i] < WARNING_INTERVAL && difficulty > 0){
                data.gapWarning[i] = true;
            }
            else if(data.framesOpen[i] < difficulty + WARNING_INTERVAL && difficulty > 0){
                data.gapOpen[i] = false;
                data.gapWarning[i] = false;
            } else {
                if(data.framesOpen[i] > GameData.MAX_FRAME_OPEN){
                    data.framesOpen[i] = 0;
                }
                data.gapOpen[i] = true;
                data.gapWarning[i] = false;
            }
        }
    }

    public static int explodeLoop(GameData data){
        //if(data == null){}
        if(explodeFrame >= MAX_EXPLODE_FRAME){
            explodeFrame = 0;
        }
        ++explodeFrame;
        return explodeFrame;
    }

    private static boolean checkCollision(GameData data){
        //check barriers[nextBarrier] and barriers[nextBarriers + 1]
        //because we track barriers for slightly longer than they are a threat
        if(data == null){return true;} //fail gracefully

        final int rightmost = GameData.PLAYER_OFFSET + GameData.PLAYER_WIDTH;
        final int leftmost = GameData.PLAYER_OFFSET;
        final int barrierTwo = (nextBarrier + 1) % GameData.MAX_NUM_OF_BARRIERS;

        if(!data.gapOpen[nextBarrier]){
            if(rightmost > data.barriers[nextBarrier] && leftmost < data.barriers[nextBarrier] + GameData.BARRIER_WIDTH){
                return true;
            }
        } else if(!data.gapOpen[barrierTwo]){
            if(rightmost > data.barriers[barrierTwo] && leftmost < data.barriers[barrierTwo] + GameData.BARRIER_WIDTH){
                return true;
            }
        }

        if(rightmost > data.barriers[nextBarrier] && leftmost < data.barriers[nextBarrier] + GameData.BARRIER_WIDTH){
            if(data.mHeight + GameData.PLAYER_HEIGHT > data.gapHeight[nextBarrier] + GameData.BARRIER_GAP_HEIGHT ){
                return true;
            }
            if(data.mHeight < data.gapHeight[nextBarrier]){
                return true;
            }
        }
        if(rightmost > data.barriers[barrierTwo] && leftmost < data.barriers[barrierTwo] + GameData.BARRIER_WIDTH){
            if(data.mHeight + GameData.PLAYER_HEIGHT > data.gapHeight[barrierTwo] + GameData.BARRIER_GAP_HEIGHT ){
                return true;
            }
            if(data.mHeight < data.gapHeight[barrierTwo]){
                return true;
            }
        }

        return false;
    }

    public static boolean handleInput(MotionEvent event, GameData data, int PhysScreenWidth){
        if(event == null || data == null){return false;} //fail gracefully
        final int state = data.mState;
        final boolean actionUp = event.getAction() == MotionEvent.ACTION_UP;
        final boolean actionDown = event.getAction() == MotionEvent.ACTION_DOWN;
        final boolean actionBoost = event.getX() < PhysScreenWidth/2 && data.mHeat + ENGINE_HEAT_RATE < GameData.MAX_HEAT;
        //final boolean actionBoost = event.getY() < data.mScreenHeight/2 && data.mHeat + ENGINE_HEAT_RATE < GameData.MAX_HEAT;

        if(state == GameData.STATE_PAUSE){
            if(actionDown){
                data.restoreState();
            }
        }
        if(state == GameData.STATE_MENU){
            if(actionDown){
                data.setState(GameData.STATE_PLAY);
            }
            else if(actionUp){return false;}
        }
        if(state == GameData.STATE_PLAY){
            if(actionDown){
                if(actionBoost) {
                    GameEngine.setBoosting(true);
                    return false;
                } else {
                    GameEngine.setFalling(false);
                }
            }
            else if(actionUp){
                GameEngine.setFalling(true);
                return false;
            }
        }
        if(state == GameData.STATE_END){
            if(actionDown){
                data.setState(GameData.STATE_MENU);
                data.resetState(GameData.SCREEN_WIDTH - GameData.BARRIER_WIDTH);
            }
            else if(actionUp){return false;}
        }
        return true;
    }

    private static void resetEngine(){
        frame = 0;
        boostFrame = 0;
        boosting = false;
        falling = true;
        difficulty = 0;
        nextBarrier = 0;
        prevTime = 0;
    }
}