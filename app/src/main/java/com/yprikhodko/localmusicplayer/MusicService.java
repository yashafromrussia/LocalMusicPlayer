package com.yprikhodko.localmusicplayer;

import android.app.Service;
import android.content.ContentUris;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import java.util.ArrayList;

/**
 * Created by yasha on 12/12/14.
 */
public class MusicService extends Service implements
        MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener {

    // Media player
    private MediaPlayer player;
    // Song list
    private ArrayList<Song> songs;
    // Current position
    private int songPos;
    // Our binder
    private final IBinder musicBind = new MusicBinder();
    private OnSongChangedListener onSongChangedListener;

    public void onCreate(){
        // Create the service
        super.onCreate();
        // Initialize position
        songPos = 0;
        // Create player
        player = new MediaPlayer();
        initMusicPlayer();
    }

    public void initMusicPlayer(){
        // Set player properties
        player.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        // Set player event listeners
        player.setOnPreparedListener(this);
        player.setOnCompletionListener(this);
        player.setOnErrorListener(this);
    }

    public void setSongs(ArrayList<Song> songs){
        this.songs = songs;
    }

    public class MusicBinder extends Binder {
        MusicService getService() {
            return MusicService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return musicBind;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // Stop media player
        player.stop();
        player.reset();
        player.release();
        return false;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {

    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        // Start playback
        mp.start();
    }


    public void setSong(int songIndex){
        songPos = songIndex;
    }

    public void playSong(){
        // Play a song
        player.reset();
        // Get song
        Song playSong = songs.get(songPos);
        long currSongID = playSong.getID();
        Uri trackUri = ContentUris.withAppendedId(
                android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                currSongID);
        // Try playing the track... but it might be missing so try and catch
        try {
            player.setDataSource(getApplicationContext(), trackUri);
        } catch(Exception e) {
            Log.e("MUSIC SERVICE", "Error setting data source", e);
        }

        player.prepareAsync();
        onSongChangedListener.onSongChanged(playSong);
    }

    public interface OnSongChangedListener {
        public void onSongChanged(Song song);
    }
    // Sets a callback to execute when we switch songs.. ie: update UI
    public void setOnSongChangedListener(OnSongChangedListener listener) {
        onSongChangedListener = listener;
    }

    public Song getCurrentSong() {
        return songs.get(songPos);
    }
}