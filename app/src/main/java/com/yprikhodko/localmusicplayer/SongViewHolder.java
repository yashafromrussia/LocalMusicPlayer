package com.yprikhodko.localmusicplayer;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Created by yasha on 12/12/14.
 */
public class SongViewHolder extends RecyclerView.ViewHolder {
    protected TextView vArtist;
    protected TextView vTitle;
    protected ImageView vArtwork;

    public SongViewHolder(View v) {
        super(v);
        vTitle = (TextView) v.findViewById(R.id.song_title);
        vArtist = (TextView) v.findViewById(R.id.song_artist);
        vArtwork = (ImageView) v.findViewById(R.id.song_art);
    }
}