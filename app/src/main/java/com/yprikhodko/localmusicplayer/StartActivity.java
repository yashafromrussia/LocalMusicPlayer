package com.yprikhodko.localmusicplayer;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.Element;
import android.support.v8.renderscript.RenderScript;
import android.support.v8.renderscript.ScriptIntrinsicBlur;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.IconTextView;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.joanzapata.android.iconify.IconDrawable;
import com.joanzapata.android.iconify.Iconify;
import com.shamanland.fab.FloatingActionButton;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.util.ArrayList;


public class StartActivity extends ActionBarActivity {

    private ArrayList<Song> songList;
    private MusicService musicService;
    private Intent playIntent;
    private boolean musicBound = false;
    private SongsFragment currentFragment;
    private FloatingActionButton fab;
    private SeekBar seekBar;
    private TextView currentPosition;
    private TextView totalDuration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        setSupportActionBar((Toolbar) findViewById(R.id.main_toolbar));

        // update the main content by replacing fragments
        FragmentManager fragmentManager = getSupportFragmentManager();
        currentFragment = SongsFragment.newInstance(1);
        fragmentManager.beginTransaction()
                .replace(R.id.container, currentFragment)
                .commit();

        songList = new ArrayList<Song>();

        fab = (FloatingActionButton) findViewById(R.id.playBtn);
        fab.setImageDrawable(new IconDrawable(this, Iconify.IconValue.fa_play)
                .colorRes(R.color.dark));

        IconTextView previewPlayBtn = (IconTextView) findViewById(R.id.previewPlayBtn);
        previewPlayBtn.setOnClickListener(togglePlayBtn);
        fab.setOnClickListener(togglePlayBtn);

        seekBar = (SeekBar) findViewById(R.id.seekBar);
        currentPosition = (TextView) findViewById(R.id.currentPosition);
        totalDuration = (TextView) findViewById(R.id.totalDuration);

        // Set up previous and next buttons
        IconTextView prevBtn = (IconTextView) findViewById(R.id.prevBtn);
        IconTextView nextBtn = (IconTextView) findViewById(R.id.nextBtn);
        prevBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int current = musicService.getCurrentIndex();
                int previous = current - 1;
                // If current was 0, then play the last song in the list
                if (previous < 0)
                    previous = songList.size() - 1;
                musicService.setSong(previous);
                musicService.togglePlay();
            }
        });

        nextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int current = musicService.getCurrentIndex();
                int next = current + 1;
                // If current was the last song, then play the first song in the list
                if (next == songList.size())
                    next = 0;
                musicService.setSong(next);
                musicService.togglePlay();
            }
        });

        // Set our background animation
        final SlidingUpPanelLayout slidingUpPanel = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);
        View dragPanel = findViewById(R.id.dragPanel);
        final TransitionDrawable transition = (TransitionDrawable) dragPanel.getBackground();
        transition.startTransition(1);
        slidingUpPanel.setPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
            Boolean panelExpanded = false;
            ImageView previewArtworkView = (ImageView) findViewById(R.id.previewArtwork);
            ImageView backBtn = (ImageView) findViewById(R.id.backBtn);
            TextView previewSongTitle = (TextView) findViewById(R.id.previewSongTitle);
            TextView previewSongArtist = (TextView) findViewById(R.id.previewSongArtist);
            View previewPlayBtn = findViewById(R.id.previewPlayBtn);
            @Override
            public void onPanelSlide(View view, float v) {}

            @Override
            public void onPanelCollapsed(View view) {
                // Animate background back to grey
                if (panelExpanded) {
                    panelExpanded = false;
                    transition.startTransition(300);
                    previewArtworkView.setVisibility(View.VISIBLE);
                    previewPlayBtn.setVisibility(View.VISIBLE);
                    backBtn.setVisibility(View.INVISIBLE);
                    previewSongTitle.setTextColor(getResources().getColor(R.color.dark));
                    previewSongArtist.setTextColor(getResources().getColor(R.color.dark));
                }
            }

            @Override
            public void onPanelExpanded(View view) {
                // Animate background to glass
                if (!panelExpanded) {
                    panelExpanded = true;
                    transition.reverseTransition(300);
                    previewArtworkView.setVisibility(View.INVISIBLE);
                    previewPlayBtn.setVisibility(View.INVISIBLE);
                    backBtn.setVisibility(View.VISIBLE);
                    previewSongTitle.setTextColor(Color.WHITE);
                    previewSongArtist.setTextColor(Color.WHITE);
                }
            }

            @Override
            public void onPanelAnchored(View view) {}

            @Override
            public void onPanelHidden(View view) {}
        });
    }


    // Play/pause the song on click
    private View.OnClickListener togglePlayBtn = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            musicService.togglePlay();
        }
    };

    // Connect to the service
    private ServiceConnection musicConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            // Get service
            musicService = binder.getService();
            // Pass song list
            musicService.setSongs(songList);
            musicService.setUIControls(seekBar, currentPosition, totalDuration);
            musicBound = true;

            // If we don't have any songs on SD, tell the user about that
            if (songList.size() == 0)
                findViewById(R.id.no_music).setVisibility(View.VISIBLE);

            // Initialize interfaces
            musicService.setOnSongChangedListener(new MusicService.OnSongChangedListener() {
                ImageView artworkView = (ImageView) findViewById(R.id.playerArtwork);
                ImageView previewArtworkView = (ImageView) findViewById(R.id.previewArtwork);
                TextView previewSongTitle = (TextView) findViewById(R.id.previewSongTitle);
                TextView previewSongArtist = (TextView) findViewById(R.id.previewSongArtist);
                IconTextView previewPlayBtn = (IconTextView) findViewById(R.id.previewPlayBtn);
                @Override
                public void onSongChanged(Song song) {
                    Bitmap bitmap;
                    previewSongTitle.setText(song.getTitle());
                    previewSongArtist.setText(song.getArtist());
                    bitmap = song.getArtworkBitmap(getApplicationContext());
                    if (bitmap == null) return; // bitmap might be null.. if it is, dont do anything
                    artworkView.setImageBitmap(bitmap);
                    previewArtworkView.setImageBitmap(bitmap);

                    Bitmap blurredBitmap = bitmap.copy(bitmap.getConfig(), true);

                    applyBlur(25f, blurredBitmap);

                    // Scale the bitmap
                    Matrix matrix = new Matrix();
                    matrix.postScale(3f, 3f);
                    blurredBitmap = Bitmap.createBitmap(blurredBitmap, 0, 0, blurredBitmap.getWidth(), blurredBitmap.getHeight(), matrix, true);
                    ((ImageView) findViewById(R.id.playerBg)).setImageBitmap(blurredBitmap);
                }

                @Override
                public void onPlayerStatusChanged(int status) {
                    switch(status) {
                        case MusicService.PLAYING:
                            previewPlayBtn.setText("{fa-pause}");
                            fab.setImageDrawable(new IconDrawable(getApplicationContext(), Iconify.IconValue.fa_pause)
                                    .colorRes(R.color.dark));
                            break;
                        case MusicService.PAUSED:
                            previewPlayBtn.setText("{fa-play}");
                            fab.setImageDrawable(new IconDrawable(getApplicationContext(), Iconify.IconValue.fa_play)
                                    .colorRes(R.color.dark));
                            break;
                    }
                }
            });

            musicService.setSong(0);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicBound = false;
        }
    };

    /**
     * Applies blur using RenderScript for better performance
     * @param radius - blur radius to apply
     */
    private void applyBlur(float radius, Bitmap bitmap) {
        RenderScript rs = RenderScript.create(this);
        // Use this constructor for best performance, because it uses USAGE_SHARED mode which reuses memory
        final Allocation input = Allocation.createFromBitmap(rs, bitmap);
        final Allocation output = Allocation.createTyped(rs, input.getType());
        final ScriptIntrinsicBlur script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
        script.setRadius(radius);
        script.setInput(input);
        script.forEach(output);
        output.copyTo(bitmap);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Start service when we start the activity
        if (playIntent == null) {
            playIntent = new Intent(this, MusicService.class);
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
            startService(playIntent);
        }
    }

    @Override
    protected void onDestroy() {
        stopService(playIntent);
        if (musicBound) {
            unbindService(musicConnection);
        }
        super.onDestroy();
    }

    public ArrayList<Song> getSongs() {
        return songList;
    }

    public void setSongs(ArrayList<Song> songList) {
        this.songList = songList;
    }

    public MusicService getMusicService() {
        return musicService;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }
}
