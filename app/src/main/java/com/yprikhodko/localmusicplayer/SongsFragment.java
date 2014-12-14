package com.yprikhodko.localmusicplayer;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.support.v8.renderscript.*;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.joanzapata.android.iconify.IconDrawable;
import com.joanzapata.android.iconify.Iconify;
import com.shamanland.fab.FloatingActionButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Fragment for displaying songs and music player
 */
public class SongsFragment extends Fragment {

    private RecyclerView songView;
    private ArrayList<Song> songList;
    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static final String ARG_SECTION_NUMBER = "section_number";

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static SongsFragment newInstance(int sectionNumber) {
        SongsFragment fragment = new SongsFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    public SongsFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_start, container, false);

        songList = ((StartActivity) getActivity()).getSongs();

        songView = (RecyclerView) rootView.findViewById(R.id.song_list);
        songView.setHasFixedSize(true);
        GridLayoutManager grid = new GridLayoutManager(getActivity(), 2);
        grid.setOrientation(LinearLayoutManager.VERTICAL);
        songView.setLayoutManager(grid);

        getSongList();
        // sort the tracks
        Collections.sort(songList, new Comparator<Song>() {
            public int compare(Song a, Song b) {
                return a.getTitle().compareTo(b.getTitle());
            }
        });

        ((StartActivity) getActivity()).setSongs(songList);

        SongAdapter songAdt = new SongAdapter(songList, getActivity());
        songView.setAdapter(songAdt);
        songView.addOnItemTouchListener(new RecyclerItemClickListener(getActivity(), new RecyclerItemClickListener.OnItemClickListener(){

            @Override
            public void onItemClick(View view, int position) {
                MusicService musicService = ((StartActivity) getActivity()).getMusicService();
                musicService.setSong(position);
                musicService.playSong();
                Toast.makeText(getActivity(), "Playing: " + songList.get(position).getTitle(), Toast.LENGTH_LONG).show();
            }
        }));

        FloatingActionButton fab = (FloatingActionButton) rootView.findViewById(R.id.playBtn);
        fab.setImageDrawable(new IconDrawable(getActivity(), Iconify.IconValue.fa_play_circle)
                .colorRes(R.color.white));

        return rootView;
    }

    public void setOnSongChangedListener() {
        MusicService musicService = ((StartActivity) getActivity()).getMusicService();
        musicService.setOnSongChangedListener(new MusicService.OnSongChangedListener() {
            @Override
            public void onSongChanged(Song song) {
                Bitmap bitmap;
                ImageView artworkView = (ImageView) getActivity().findViewById(R.id.playerArtwork);
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), song.getArtwork());
                    artworkView.setImageBitmap(bitmap);

                    Bitmap blurredBitmap = bitmap.copy(bitmap.getConfig(), true);

//                    getActivity().findViewById(R.id.player).setBackgroundColor(palette.getLightMutedColor(R.color.accent_material_dark));

                    RenderScript rs = RenderScript.create(getActivity());
                    //this will blur the bitmap with a radius of 8 and save it in bitmap
                    final Allocation input = Allocation.createFromBitmap(rs, blurredBitmap); //use this constructor for best performance, because it uses USAGE_SHARED mode which reuses memory
                    final Allocation output = Allocation.createTyped(rs, input.getType());
                    final ScriptIntrinsicBlur script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
                    script.setRadius(25f);
                    script.setInput(input);
                    script.forEach(output);
                    output.copyTo(blurredBitmap);

                    // create a matrix for the manipulation
                    Matrix matrix = new Matrix();
                    matrix.postScale(5f, 5f);
                    blurredBitmap = Bitmap.createBitmap(blurredBitmap, 0, 0, blurredBitmap.getWidth(), blurredBitmap.getHeight(), matrix, true);
                    ((ImageView) getActivity().findViewById(R.id.playerBg)).setImageBitmap(blurredBitmap);
                } catch (Exception e) {
                    Log.e("NOT FOUND", e.getMessage());
                }
            }
        });
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((StartActivity) activity).onSectionAttached(
                getArguments().getInt(ARG_SECTION_NUMBER));
    }

    public void getSongList() {
        //retrieve song info
        ContentResolver musicResolver = getActivity().getContentResolver();
        Uri musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor musicCursor = musicResolver.query(musicUri, null, null, null, null);

        if (musicCursor != null && musicCursor.moveToFirst()) {
            //get columns
            int titleColumn = musicCursor.getColumnIndex
                    (MediaStore.Audio.Media.TITLE);
            int idColumn = musicCursor.getColumnIndex
                    (MediaStore.Audio.Media._ID);
            int artistColumn = musicCursor.getColumnIndex
                    (MediaStore.Audio.Media.ARTIST);
            int albumColumn = musicCursor.getColumnIndex
                    (MediaStore.Audio.Media.ALBUM_ID);

            //add songs to list
            do {
                long thisId = musicCursor.getLong(idColumn);
                long albumId = musicCursor.getLong(albumColumn);
                String thisTitle = musicCursor.getString(titleColumn);
                String thisArtist = musicCursor.getString(artistColumn);
                final Uri ART_CONTENT_URI = Uri.parse("content://media/external/audio/albumart");
                Uri albumArtUri = ContentUris.withAppendedId(ART_CONTENT_URI, albumId);
                songList.add(new Song(thisId, thisTitle, thisArtist, albumArtUri));
            }
            while (musicCursor.moveToNext());

            musicCursor.close();
        }

    }
}