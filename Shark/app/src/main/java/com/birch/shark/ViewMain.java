package com.birch.shark;

/**
 * Created by Nick on 12/12/2014.
 */

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class ViewMain extends SurfaceView implements SurfaceHolder.Callback{
    public class ViewThread extends Thread {
        private final int MIN_POINTS_BEFORE_AD = 1000;

        private final String FILE_NAME = "Shark_Score_File";
        private final String KEY_SCORE = "Shark_Key_Score";

        //member variables of Thread
        private SurfaceHolder mSurfaceHolder;
        private Context mContext;
        private Handler mHandler;
        private boolean mRun = false; //indicate whether the surface has been created & is ready to draw
        private boolean mStarted = false;
        private int mPhysScreenHeight = 0;
        private int mPhysScreenWidth = 0;
        private int mCumulativePoints = 0;

        private Bitmap mSprite;
        private Bitmap mBackgroundImage;
        private Bitmap mBarrier;
        private Bitmap mGap;
        private Bitmap mWarning;
        private Bitmap mBoost;
        private Bitmap mExplode1;
        private Bitmap mExplode2;
        private Bitmap mExplode3;

        public GameData mState;

        public void setHandler(Handler h){
            mHandler = h;
        }

        public ViewThread(SurfaceHolder holder, Context context){
            mContext = context;
            mSurfaceHolder = holder;

            SharedPreferences prefs = mContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
            int score = prefs.getInt(KEY_SCORE, 0);
            if(score == 0) { //if we haven't stored a score before, store one now
                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt(KEY_SCORE, 0);
                editor.commit();
            }

            //Bitmaps
            Resources res = context.getResources();
            mBackgroundImage = BitmapFactory.decodeResource(res, R.drawable.background);
            mSprite = BitmapFactory.decodeResource(res, R.drawable.shark);
            mBarrier = BitmapFactory.decodeResource(res, R.drawable.wall);
            mGap = BitmapFactory.decodeResource(res, R.drawable.gap);
            mBoost = BitmapFactory.decodeResource(res, R.drawable.boost);
            mExplode1 = BitmapFactory.decodeResource(res, R.drawable.explode1);
            mExplode2 = BitmapFactory.decodeResource(res, R.drawable.explode2);
            mExplode3 = BitmapFactory.decodeResource(res, R.drawable.explode3);
            mWarning = BitmapFactory.decodeResource(res, R.drawable.warning);

            mState = new GameData();
            mState.initBarriers(GameData.SCREEN_WIDTH - GameData.BARRIER_WIDTH);
        }

        @Override
        public void run(){
            while(mRun) {
                if(mState.mState == GameData.STATE_PAUSE){
                    continue;
                }

                Canvas c = null;
                try {
                    c = mSurfaceHolder.lockCanvas(null);
                    synchronized (mSurfaceHolder) {
                        final int state = mState.mState;
                        if(state == GameData.STATE_MENU){
                            c.drawBitmap(mBackgroundImage, 0, 0, null);
                            writeInstructions(c);
                        }
                        else if(state == GameData.STATE_PLAY){
                            final boolean gameContinue = GameEngine.gameLoop(mState); //update the state
                            final int frame = 0;
                            doDraw(c, mState, frame); //update the screen
                            if(!gameContinue){
                                mCumulativePoints += mState.mPoints;
                                mState.setState(GameData.STATE_EXPLODE);
                            }
                        }
                        else if (state == GameData.STATE_EXPLODE){
                            final int frame = GameEngine.explodeLoop(mState); //AAAAAAHHHHH!!!!!
                            doDraw(c, mState, frame);
                            if(frame == GameEngine.MAX_EXPLODE_FRAME){
                                mState.setState(GameData.STATE_END);
                                //mState.setState(GameData.STATE_AD); uncomment this line to re-enable ads
                            }
                        }
                        else if(state == GameData.STATE_AD){
                            mState.setState(GameData.STATE_END);
                            c.drawBitmap(mBackgroundImage, 0, 0, null);
                            if(mCumulativePoints >= MIN_POINTS_BEFORE_AD) {
                                mCumulativePoints = 0;
                                final int anyValue = 0;
                                //The following null check is a bandaid. the line below would throw
                                //a null pointer exception.
                                if(mHandler != null){
                                    mHandler.sendEmptyMessage(anyValue);
                                }
                            }
                        }
                        else if(state == GameData.STATE_END){
                            c.drawBitmap(mBackgroundImage, 0, 0, null);

                            SharedPreferences highScores = mContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
                            int highScore = highScores.getInt(KEY_SCORE, 2);
                            if(mState.mPoints > highScore){
                                highScore = mState.mPoints;
                                SharedPreferences.Editor editor = highScores.edit();
                                editor.putInt(KEY_SCORE, highScore);
                                editor.commit();
                            }
                            writeScore(c, highScore);
                        }
                    }
                } finally { //if something goes wrong, we make sure to unlock mSurfaceHolder
                    if (c != null) {
                        mSurfaceHolder.unlockCanvasAndPost(c);
                    }
                }
            }
        }

        public void doDraw(Canvas canvas, GameData state, int explodeFrame){
            if(canvas == null || state == null) {return;}

            state.scaleValues();

            canvas.drawBitmap(mBackgroundImage, 0, 0, null);
            for(int i = 0; i < state.barriers.length; ++i){
                canvas.drawBitmap(mBarrier, state.scaledBarriers[i], 0, null);
                if(state.barriers[i] < GameData.SCREEN_WIDTH - GameData.BARRIER_WIDTH * 2
                        && state.barriers[i] > 0 && state.cracked[i] && !state.gapOpen[i]){
                    //draw cracked image
                    Bitmap crack = GraphLib.BitBlit(mWarning, mBarrier, 0, 0);
                    canvas.drawBitmap(crack, state.scaledBarriers[i], state.scaledGapHeight[i], null);
                }
                else if(state.barriers[i] < GameData.SCREEN_WIDTH - GameData.BARRIER_WIDTH * 2
                        && state.barriers[i] > 0 && state.gapOpen[i]) {
                    Bitmap gap;
                    if (state.gapWarning[i]){
                        gap = GraphLib.BitBlit(mWarning, mBackgroundImage, state.scaledBarriers[i], state.scaledGapHeight[i]);
                    } else {
                        gap = GraphLib.BitBlit(mGap, mBackgroundImage, state.scaledBarriers[i], state.scaledGapHeight[i]);
                    }
                    canvas.drawBitmap(gap, state.scaledBarriers[i], state.scaledGapHeight[i], null);
                }
            }
            Bitmap sprite;
            switch(explodeFrame) {
                case 1:
                    sprite = GraphLib.BitBlit(mExplode1, mBackgroundImage, state.scaledPlayerOffset, state.scaledHeight);
                    break;
                case 2:
                    sprite = GraphLib.BitBlit(mExplode2, mBackgroundImage, state.scaledPlayerOffset, state.scaledHeight);
                    break;
                case 3:
                    sprite = GraphLib.BitBlit(mExplode3, mBackgroundImage, state.scaledPlayerOffset, state.scaledHeight);
                    break;
                default:
                    if(GameEngine.isBoosting()){
                        sprite = GraphLib.BitBlit(mBoost, mBackgroundImage, state.scaledPlayerOffset, state.scaledHeight);
                    }
                    else{
                        sprite = GraphLib.BitBlit(mSprite, mBackgroundImage, state.scaledPlayerOffset, state.scaledHeight);
                    }
                    break;
            }
            canvas.drawBitmap(sprite, state.scaledPlayerOffset, state.scaledHeight, null);
            //code for points display
            String points = Integer.toString(state.mPoints);
            String heat = Integer.toString(state.mHeat);
            Paint paint = new Paint();
            paint.setColor(Color.WHITE);
            final int fontSize = (int)(64 * state.mYScaleFactor);
            paint.setTextSize(fontSize);
            canvas.drawText(points, 0, fontSize, paint);
            /*paint.setColor(Color.RED);
            canvas.drawText(heat, 0, 72, paint); */

        }

        public void setRunning(boolean b){
            mRun = b;
        }

        public void surfaceChange(int width, int height){
            synchronized(mSurfaceHolder){
                mState.setScaleFactor(width, height);
                mPhysScreenHeight = height;
                mPhysScreenWidth = width;

                mSprite = Bitmap.createScaledBitmap(mSprite, mState.scaledPlayerWidth, mState.scaledPlayerHeight, true);
                mBoost = Bitmap.createScaledBitmap(mBoost, mState.scaledPlayerWidth, mState.scaledPlayerHeight, true);
                mExplode1 = Bitmap.createScaledBitmap(mExplode1, mState.scaledPlayerWidth, mState.scaledPlayerHeight, true);
                mExplode2 = Bitmap.createScaledBitmap(mExplode2, mState.scaledPlayerWidth, mState.scaledPlayerHeight, true);
                mExplode3 = Bitmap.createScaledBitmap(mExplode3, mState.scaledPlayerWidth, mState.scaledPlayerHeight, true);
                mBarrier = Bitmap.createScaledBitmap(mBarrier, mState.scaledBarrierWidth, mState.scaledScreenHeight, true);
                mGap = Bitmap.createScaledBitmap(mGap, mState.scaledBarrierWidth, mState.scaledBarrierGapHeight, true);
                mWarning = Bitmap.createScaledBitmap(mWarning, mState.scaledBarrierWidth, mState.scaledBarrierGapHeight, true);
                mBackgroundImage = Bitmap.createScaledBitmap(mBackgroundImage, mState.scaledScreenWidth, mState.scaledScreenHeight, true);
            }
        }

        public void saveStateToBundle(Bundle bundle){
            if(bundle == null){return;}
            //TODO: save the state of the game to bundle
        }

        public void loadStateFromBundle(Bundle bundle){
            if(bundle == null){return;}
            //TODO: load the state of the game from the bundle
        }

        private void writeInstructions(Canvas c){
            Resources res = mContext.getResources();

            final int fontSize = (int)(100 * mState.mYScaleFactor);
            final int startHeight = (int)((GameData.SCREEN_HEIGHT / 2) * mState.mYScaleFactor);
            Paint paint = new Paint();
            paint.setColor(Color.WHITE);
            paint.setTextSize(fontSize);

            String toPrint = res.getString(R.string.menu_text_3);
            int startWidth = (mPhysScreenWidth / 2) - (fontSize * toPrint.length() / 4);
            c.drawText(toPrint, startWidth, startHeight, paint);
        }

        private void writeScore(Canvas c, int highScore){
            Resources res = mContext.getResources();

            final int startPoint = (int)((GameData.SCREEN_WIDTH / 4) * mState.mXScaleFactor);
            final int startHeight = (int)((GameData.SCREEN_HEIGHT / 4) * mState.mYScaleFactor);
            Paint paint = new Paint();
            paint.setColor(Color.WHITE);
            final int fontSize = (int)(100 * mState.mYScaleFactor);
            paint.setTextSize(fontSize);
            String toPrint = res.getString(R.string.end_text_1);
            c.drawText(toPrint, startPoint, startHeight, paint);
            toPrint = Integer.toString(mState.mPoints);
            c.drawText(toPrint, startPoint, startHeight + fontSize, paint);
            toPrint = res.getString(R.string.end_text_2);
            c.drawText(toPrint, startPoint , startHeight + (fontSize * 2), paint);
            c.drawText(Integer.toString(highScore), startPoint, startHeight + (fontSize * 3), paint);
        }
    }


    //member variables for View2
    private Context mContext;
    private ViewThread mThread;

    public ViewThread getThread() {
        return mThread;
    }

    public void makeNewThread() {
            if(!mThread.isAlive()) {
            mThread = new ViewThread(getHolder(), mContext);
        }
    }

    public ViewMain(Context context, AttributeSet attrs){
        super(context, attrs);

        SurfaceHolder holder = getHolder();
        holder.addCallback(this);
        mContext = context;
        mThread = new ViewThread(holder, context);

        setFocusable(true);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event){
        if(event == null){return false;}
        return GameEngine.handleInput(event, mThread.mState, mThread.mPhysScreenWidth);
    }

    //SurfaceHolder.Callback code:
    public void surfaceDestroyed(SurfaceHolder holder){
        boolean retry = true;
        while(retry){
            try{
                mThread.setRunning(false);
                mThread.join();
                retry = false;
            }
            catch(InterruptedException ignore){
            }
        }
    }

    public void surfaceCreated(SurfaceHolder holder){
        mThread.setRunning(true);
        if(!mThread.mStarted){
            mThread.mStarted = true;
            mThread.start();
        }
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        mThread.surfaceChange(width, height);
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus){
        if(!hasWindowFocus) {
            mThread.mState.setState(GameData.STATE_PAUSE);
        }
    }
}
