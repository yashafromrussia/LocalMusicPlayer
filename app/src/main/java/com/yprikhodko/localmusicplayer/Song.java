package com.yprikhodko.localmusicplayer;

import android.net.Uri;

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
}
