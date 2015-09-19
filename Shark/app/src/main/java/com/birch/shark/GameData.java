package com.birch.shark;

/**
 * Created by Nick on 12/12/2014.
 */
import java.util.Random;

public class GameData {
    //global constants
    public static final int GAME_SPEED = 3;
    public static final int DROP_SPEED = 5;
    public static final int MAX_NUM_OF_BARRIERS = 2;
    public static final int POINTS_INCREMENT = 1;
    public static final int MAX_FRAME_OPEN = 100;
    public static final int MAX_HEAT = 10;
    public static final int PROB_CRACKED = 0;

    public static final int STATE_PAUSE = 0;
    public static final int STATE_PLAY = 1;
    public static final int STATE_MENU = 2;
    public static final int STATE_END = 3;
    public static final int STATE_EXPLODE = 4;
    public static final int STATE_AD = 5;

    // Sizes in internal coordinates
    //Coordinates are designed for a 16:9 screen
    public static final int PLAYER_OFFSET = 100; // how far to the right to draw the sprite
    public static final int BARRIER_WIDTH = 70;
    public static final int BARRIER_GAP_HEIGHT = 300; //This should be larger than Player Height
    public static final int PLAYER_WIDTH = 300;
    public static final int PLAYER_HEIGHT = 100;
    public static final int SCREEN_HEIGHT = 900;
    public static final int SCREEN_WIDTH = 1600;

    public int mState;
    private int mPrevState;
    public int mPoints;
    public float mXScaleFactor = 1;
    public float mYScaleFactor = 1;
    public static Random mRandom = new Random();

    public int mHeight; //player height
    public int mHeat; //temperature of player's engine
    public int[] barriers = new int[MAX_NUM_OF_BARRIERS]; //location of barriers to go through
    public int[] gapHeight = new int[MAX_NUM_OF_BARRIERS]; //where the gap in each barrier is located
    public int[] framesOpen = new int[MAX_NUM_OF_BARRIERS]; //how long gap has been open
    public boolean[] gapOpen = new boolean[MAX_NUM_OF_BARRIERS]; //if the gap is open
    public boolean[] gapWarning = new boolean[MAX_NUM_OF_BARRIERS]; //indicate gap is about to close
    public boolean[] cracked = new boolean[MAX_NUM_OF_BARRIERS]; //if you can boost through barrier

    //scaled values
    public int scaledHeight = 0;
    public int[] scaledBarriers = new int[MAX_NUM_OF_BARRIERS];
    public int[] scaledGapHeight = new int[MAX_NUM_OF_BARRIERS];

    public int scaledPlayerOffset = PLAYER_OFFSET;
    public int scaledBarrierWidth = BARRIER_WIDTH;
    public int scaledBarrierGapHeight = BARRIER_GAP_HEIGHT;
    public int scaledPlayerWidth = PLAYER_WIDTH;
    public int scaledPlayerHeight = PLAYER_HEIGHT;
    public int scaledScreenHeight = SCREEN_HEIGHT;
    public int scaledScreenWidth = SCREEN_WIDTH;

    public GameData(){
        mPoints = 0;
        mState = STATE_MENU;
        mPrevState = STATE_MENU;
        mHeight = 0;
        mHeat = 0;
        for(int i = 0; i < MAX_NUM_OF_BARRIERS; ++i){
            barriers[i] = 0;
            gapHeight[i] = 0;
            gapOpen[i] = false;
            gapWarning[i] = false;
            cracked[i] = false;

            scaledBarriers[i] = 0;
            scaledGapHeight[i] = 0;
        }
    }

    public void resetState(int edge){
        mHeight = 0;
        mHeat = 0;
        initBarriers(edge);
        mPoints = 0;
    }

    public void restoreState(){
        mState = mPrevState;
    }

    public void setState(int state){
        if(mState != STATE_PAUSE) {
            mPrevState = mState; //save previous state so we can unpause
        }
        mState = state;
    }

    public void initBarriers(int edge){
        for(int i = 0; i < MAX_NUM_OF_BARRIERS; ++i){
            barriers[i] = edge + (i * edge/2);
            gapHeight[i] = mRandom.nextInt(SCREEN_HEIGHT - BARRIER_GAP_HEIGHT);
            gapOpen[i] = false;
            framesOpen[i] = mRandom.nextInt(MAX_FRAME_OPEN);
            gapWarning[i] = false;
            cracked[i] = false;
        }
    }

    public void refreshBarrier(int barrier){
        gapHeight[barrier] = mRandom.nextInt(SCREEN_HEIGHT - BARRIER_GAP_HEIGHT);
        barriers[barrier] = SCREEN_WIDTH - BARRIER_WIDTH;
        gapOpen[barrier] = false;
        framesOpen[barrier] = mRandom.nextInt(MAX_FRAME_OPEN);
        gapWarning[barrier] = false;
        boolean crack = mRandom.nextInt(100) < PROB_CRACKED;
        cracked[barrier] = crack;
    }

    public void setScaleFactor(int width, int height){
        mXScaleFactor = (float)width/(float)SCREEN_WIDTH;
        mYScaleFactor = (float)height/(float)SCREEN_HEIGHT;

        scaledPlayerOffset = (int)(mXScaleFactor * PLAYER_OFFSET);
        scaledBarrierWidth = (int)(mXScaleFactor * BARRIER_WIDTH);
        scaledBarrierGapHeight = (int)(mYScaleFactor * BARRIER_GAP_HEIGHT);
        scaledPlayerWidth = (int)(mXScaleFactor * PLAYER_WIDTH);
        scaledPlayerHeight = (int)(mYScaleFactor * PLAYER_HEIGHT);
        scaledScreenHeight = (int)(mYScaleFactor * SCREEN_HEIGHT);
        scaledScreenWidth = (int)(mXScaleFactor * SCREEN_WIDTH);
    }

    public void scaleValues(){
        scaledHeight = (int)(mYScaleFactor * mHeight);
        for(int i = 0; i < MAX_NUM_OF_BARRIERS; ++i){
            scaledBarriers[i] = (int)(mXScaleFactor * barriers[i]);
            scaledGapHeight[i] = (int)(mYScaleFactor * gapHeight[i]);
        }
    }

}