package com.birch.shark;

/**
 * Created by Nick on 12/12/2014.
 */
import android.graphics.Bitmap;
import android.graphics.Color;

public class GraphLib {
    //public static final int TRANS_COLOR = Color.WHITE;
    public static final int TRANS_COLOR = Color.BLACK;

    //create a Bitmap which puts the background into the sprite bitmap.
    //top is sprite, bottom is bitmap, topX and topY is the origin of the sprite
    public static Bitmap BitBlit(Bitmap top, Bitmap bottom, int topX, int topY){
        final int w = top.getWidth();
        final int h = top.getHeight();

        int[] pixelsTop = new int[w*h];
        int[] pixelsBottom = new int[w*h];

        top.getPixels(pixelsTop, 0, w, 0, 0, w, h);
        bottom.getPixels(pixelsBottom, 0, w, topX, topY, w, h);

        for(int i = 0; i < pixelsTop.length; ++i){
            if(pixelsTop[i] == TRANS_COLOR){ pixelsTop[i] = pixelsBottom[i];}
        }

        return Bitmap.createBitmap(pixelsTop, w, h, Bitmap.Config.ARGB_8888);
    }

}