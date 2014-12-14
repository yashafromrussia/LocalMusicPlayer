package com.yprikhodko.localmusicplayer;

import android.app.Service;
import android.content.ContentUris;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.widget.SeekBar;

import java.util.ArrayList;

/**
 * Responsible for music playback. This is the main controller that handles all user actions
 * regarding song playback
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

    public static final int STOPPED = 0;
    public static final int PAUSED = 1;
    public static final int PLAYING = 2;
    private int playerState = STOPPED;
    private SeekBar mSeekBar;
    private int mInterval = 1000;

    // Async thread to update progress bar every second
    private Runnable mProgressRunner = new Runnable() {
        @Override
        public void run() {
        if (mSeekBar != null) {
            mSeekBar.setProgress(player.getCurrentPosition());

            if(player.isPlaying()) {
                mSeekBar.postDelayed(mProgressRunner, mInterval);
            }
        }
        }
    };

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
        mSeekBar.setMax(mp.getDuration());
        mSeekBar.postDelayed(mProgressRunner, mInterval);
    }


    /**
     * Sets a new song to buffer
     * @param songIndex - position of the song in the array
     */
    public void setSong(int songIndex){
        songPos = songIndex;
        playerState = STOPPED;
        onSongChangedListener.onSongChanged(songs.get(songPos));
    }

    /**
     * Toggles on/off song playback
     */
    public void togglePlay() {
        switch(playerState) {
            case STOPPED:
                playSong();
                break;
            case PAUSED:
                player.start();
                onSongChangedListener.onPlayerStatusChanged(playerState = PLAYING);
                mProgressRunner.run();
                break;
            case PLAYING:
                player.pause();
                onSongChangedListener.onPlayerStatusChanged(playerState = PAUSED);
                mSeekBar.removeCallbacks(mProgressRunner);
                break;
        }
    }

    private void playSong() {
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
        mProgressRunner.run();
        onSongChangedListener.onPlayerStatusChanged(playerState = PLAYING);
    }

    public interface OnSongChangedListener {
        public void onSongChanged(Song song);
        public void onPlayerStatusChanged(int status);
    }

    // Sets a callback to execute when we switch songs.. ie: update UI
    public void setOnSongChangedListener(OnSongChangedListener listener) {
        onSongChangedListener = listener;
    }

    /**
     * Sets seekBar to control while playing music
     * @param seekBar - Seek bar instance that's already on our UI thread
     */
    public void setSeekBar(SeekBar seekBar) {
        mSeekBar = seekBar;

        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    // Change current position of the song playback
                    player.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }
}