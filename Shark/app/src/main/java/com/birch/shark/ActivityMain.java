package com.birch.shark;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.ads.*;

public class ActivityMain extends Activity {

    private ViewMain mView;
    private ViewMain.ViewThread mThread;
    private InterstitialAd mInterstitial;
    private Handler messageHandler;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mView = (ViewMain) findViewById(R.id.SV1);
        mThread = mView.getThread();

        mInterstitial = new InterstitialAd(this);
        messageHandler = new Handler(){
            public void handleMessage(Message msg){
                super.handleMessage(msg);
                if(mInterstitial != null && mInterstitial.isLoaded()){
                    mInterstitial.show();
                }
            }
        };
        mThread.setHandler(messageHandler);
        mInterstitial.setAdUnitId(getString(R.string.AdUnitId));

        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice("A820944ECAE89FDB51CA8FF7C15B21AC")
                .addTestDevice("0486E3BFDB02C9A9FE9F4989A13FC8A7")
                .build();
        mInterstitial.loadAd(adRequest);

        mInterstitial.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                AdRequest adRequest = new AdRequest.Builder()
                        .addTestDevice("A820944ECAE89FDB51CA8FF7C15B21AC")
                        .addTestDevice("0486E3BFDB02C9A9FE9F4989A13FC8A7")
                        .build();
                mInterstitial.loadAd(adRequest);
            }
        });

        if(savedInstanceState != null){
            //TODO: uncomment below when ready
            //mThread.loadStateFromBundle(savedInstanceState);
        }
    }

    @Override
    protected void onPause(){
        super.onPause();
        mThread.mState.setState(GameData.STATE_PAUSE);
    }

    @Override
    protected void onResume(){
        super.onResume();
    }

    @Override
    protected void onRestart(){
        super.onRestart();
        setContentView(R.layout.activity_main);

        mView = (ViewMain) findViewById(R.id.SV1);
        mView.makeNewThread();
        
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState){
        super.onSaveInstanceState(outState);

        //TODO: uncomment this code when it's ready
        //mThread.saveStateToBundle(outState);
    }
}
