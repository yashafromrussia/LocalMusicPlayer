package com.yprikhodko.localmusicplayer;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;

import java.io.IOException;

/**
 * Created by yasha on 12/12/14.
 */
public class Song {
    private long id;
    private String title;
    private String artist;
    private Uri artwork;

    public Song(long songID, String songTitle, String songArtist, Uri songArtwork) {
        id = songID;
        title = songTitle;
        artist = songArtist;
        artwork = songArtwork;
    }

    public long getID(){return id;}
    public String getTitle(){return title;}
    public String getArtist(){return artist;}
    public Uri getArtwork(){return artwork;}
    public Bitmap getArtworkBitmap(Context ctx) {
        Bitmap bitmap = null;
        try {
            bitmap = MediaStore.Images.Media.getBitmap(ctx.getContentResolver(), artwork);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bitmap;
    }
}
