package com.yprikhodko.localmusicplayer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.provider.MediaStore;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by yasha on 12/12/14.
 */
public class SongAdapter extends RecyclerView.Adapter<SongViewHolder> {

    private ArrayList<Song> songs;
    private Context ctx;

    public SongAdapter(ArrayList<Song> songs, Context ctx) {
        this.songs = songs;
        this.ctx = ctx;
    }

    @Override
    public SongViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View itemView = LayoutInflater.
                from(viewGroup.getContext()).
                inflate(R.layout.song_list, viewGroup, false);

        return new SongViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(SongViewHolder songViewHolder, int i) {
        Song song = songs.get(i);
        songViewHolder.vTitle.setText(song.getTitle());
        songViewHolder.vArtist.setText(song.getArtist());

        Bitmap bitmap = null;
        try {
            bitmap = MediaStore.Images.Media.getBitmap(ctx.getContentResolver(), song.getArtwork());
            songViewHolder.vArtwork.setImageBitmap(bitmap);
        } catch (Exception exception) {
            // log error
        }

    }

    @Override
    public int getItemCount() {
        return songs.size();
    }
}
