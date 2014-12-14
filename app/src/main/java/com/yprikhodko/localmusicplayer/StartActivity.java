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
import android.provider.MediaStore;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.Element;
import android.support.v8.renderscript.RenderScript;
import android.support.v8.renderscript.ScriptIntrinsicBlur;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.IconTextView;
import android.widget.ImageView;
import android.widget.TextView;

import com.joanzapata.android.iconify.IconDrawable;
import com.joanzapata.android.iconify.Iconify;
import com.shamanland.fab.FloatingActionButton;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.util.ArrayList;


public class StartActivity extends ActionBarActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;

    private ArrayList<Song> songList;
    private MusicService musicService;
    private Intent playIntent;
    private boolean musicBound = false;
    private SongsFragment currentFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        setSupportActionBar((Toolbar) findViewById(R.id.main_toolbar));

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();
        songList = new ArrayList<Song>();

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.playBtn);
        fab.setImageDrawable(new IconDrawable(this, Iconify.IconValue.fa_play_circle)
                .colorRes(R.color.white));

        IconTextView previewPlayBtn = (IconTextView) findViewById(R.id.previewPlayBtn);
        previewPlayBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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

    // Connect to the service
    private ServiceConnection musicConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            // Get service
            musicService = binder.getService();
            // Pass song list
            musicService.setSongs(songList);
            musicBound = true;

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
                            break;
                        case MusicService.PAUSED:
                            previewPlayBtn.setText("{fa-play}");
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

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        // update the main content by replacing fragments
        FragmentManager fragmentManager = getSupportFragmentManager();
        currentFragment = SongsFragment.newInstance(position + 1);
        fragmentManager.beginTransaction()
                .replace(R.id.container, currentFragment)
                .commit();
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

    public void onSectionAttached(int number) {
        switch (number) {
            case 1:
                mTitle = getString(R.string.title_section1);
                break;
            case 2:
                mTitle = getString(R.string.title_section2);
                break;
            case 3:
                mTitle = getString(R.string.title_section3);
                break;
        }
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.start, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
