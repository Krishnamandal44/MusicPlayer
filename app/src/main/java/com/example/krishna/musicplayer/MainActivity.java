package com.example.krishna.musicplayer;

import android.Manifest;
import android.app.ListActivity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.IOException;

import static android.support.constraint.Constraints.TAG;

public class MainActivity extends ListActivity {

    private static final int UPDATE_FREQUENCY = 500;
    private static final int STEP_VALUE = 4000;

    private TextView selectedfile = null;
    private TextView durationCurrentTime = null;
    private TextView endTime = null;
    private TextView audioData = null;
    private SeekBar seekBar = null;
    private MediaPlayer player = null;
    private ImageButton prev = null;
    private ImageButton play = null;
    private ImageButton next = null;
    private MediaCursorAdapter adapter = null;

    private boolean isStarted = true;
    private String currentFile = "";
    private boolean isMovingSeekBar = false;
    private final Handler handler = new Handler();
    private Utilities utils;

    private final Runnable updatePositinRunnable = new Runnable() {
        @Override
        public void run() {
            updatePosition();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        isReadStoragePermissionGranted();
        initialiseView();



    }

    private void initialiseView() {
        selectedfile = (TextView) findViewById(R.id.selecteditem);
        durationCurrentTime = (TextView) findViewById(R.id.durationCurrentTime);
        endTime = (TextView) findViewById(R.id.endTime);
        audioData = (TextView) findViewById(R.id.audioData);
        durationCurrentTime.setText("0.00");
        seekBar = (SeekBar) findViewById(R.id.seekBar);
        prev = (ImageButton) findViewById(R.id.previous);
        play = (ImageButton) findViewById(R.id.play);
        next = (ImageButton) findViewById(R.id.next);
        utils = new Utilities();
        player = new MediaPlayer();
        player.setOnCompletionListener(onCompletion);
        player.setOnErrorListener(onError);
        seekBar.setOnSeekBarChangeListener(seekBarChanged);

        Cursor cursor = getContentResolver().query(MediaStore.Audio.Media.
                EXTERNAL_CONTENT_URI, null, null, null, null);

        if (null != cursor) {
            cursor.moveToFirst();
            adapter = new MediaCursorAdapter(this, R.layout.item, cursor);
            setListAdapter(adapter);
            prev.setOnClickListener(OnButtonClick);
            play.setOnClickListener(OnButtonClick);
            next.setOnClickListener(OnButtonClick);
        }

    }

    public  boolean isReadStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.v(TAG,"Permission is granted1");
                return true;
            } else {

                Log.v(TAG,"Permission is revoked1");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 3);
                return false;
            }
        }
        else { //permission is automatically granted on sdk<23 upon installation
            Log.v(TAG,"Permission is granted1");
            return true;
        }
    }

    public  boolean isWriteStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.v(TAG,"Permission is granted2");
                return true;
            } else {

                Log.v(TAG,"Permission is revoked2");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2);
                return false;
            }
        }
        else { //permission is automatically granted on sdk<23 upon installation
            Log.v(TAG,"Permission is granted2");
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 2:
                Log.d(TAG, "External storage2");
                if(grantResults[0]== PackageManager.PERMISSION_GRANTED){
                    Log.v(TAG,"Permission: "+permissions[0]+ "was "+grantResults[0]);
                    //resume tasks needing this permission
                    initialiseView();
                }else{
//                    progress.dismiss();
                }
                break;

            case 3:
                Log.d(TAG, "External storage1");
                if(grantResults[0]== PackageManager.PERMISSION_GRANTED){
                    Log.v(TAG,"Permission: "+permissions[0]+ "was "+grantResults[0]);
                    //resume tasks needing this permission
                    initialiseView();
                }else{
//                    progress.dismiss();
                }
                break;
        }
    }


    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        currentFile = (String) v.getTag();
        startPlay(currentFile);
    }

    private void startPlay(String file) {
        Log.i("Selected: ", file);
        selectedfile.setText(file);
        seekBar.setProgress(0);
        player.stop();
        player.reset();

        try {
            player.setDataSource(file);
            player.prepare();
            player.start();
            updateProgressBar();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        seekBar.setMax(player.getDuration());
        long totalDuration = player.getDuration();
        long currentDuration = player.getCurrentPosition();

        durationCurrentTime.setText("" + utils.milliSecondsToTimer(currentDuration));
        endTime.setText("" + utils.milliSecondsToTimer(totalDuration));
        play.setImageResource(android.R.drawable.ic_media_pause);
        updatePosition();
        isStarted = true;
    }


    private void stopPlay() {
        player.stop();
        player.reset();
        play.setImageResource(android.R.drawable.ic_media_play);
        handler.removeCallbacks(updatePositinRunnable);
        seekBar.setProgress(0);
        isStarted = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(updatePositinRunnable);
        if(player != null) {
            player.stop();
            player.reset();
            player.release();
            player = null;
        }
    }

    private void updatePosition() {
        handler.removeCallbacks(updatePositinRunnable);
        seekBar.setProgress(player.getCurrentPosition());
        handler.postDelayed(updatePositinRunnable, UPDATE_FREQUENCY);
    }

    /**
     * Background Runnable thread
     */
    private Runnable mUpdateTimeTask = new Runnable() {
        public void run() {
            if (player != null) {
                long totalDuration = player.getDuration();
                long currentDuration = player.getCurrentPosition();
                long playerdata=player.getAudioSessionId();
                audioData.setText("");
//                AudioManager audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
//                audioManager.getActivePlaybackConfigurations().;



                durationCurrentTime.setText("" + utils.milliSecondsToTimer(currentDuration));
                // Updating progress bar
                int progress = (int) (utils.getProgressPercentage(currentDuration, totalDuration));
                //Log.d("Progress", ""+progress);
//            seekBar.setProgress(progress);

                // Running this thread after 100 milliseconds
                handler.postDelayed(this, 100);
            }
        }
    };

    private View.OnClickListener OnButtonClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.play: {

                    if (player.isPlaying()) {
                        handler.removeCallbacks(updatePositinRunnable);
                        player.pause();
                        play.setImageResource(android.R.drawable.ic_media_play);
                    } else {
                        if (isStarted) {
                            player.start();
                            play.setImageResource(android.R.drawable.ic_media_pause);
                            updatePosition();
                        } else {
                            startPlay(currentFile);
                        }
                    }
                    break;
                }

                case R.id.next: {
                    int seekto = player.getCurrentPosition() + STEP_VALUE;
                    if (seekto > player.getDuration())
                        seekto = player.getDuration();
                    player.pause();
                    player.seekTo(seekto);
                    player.start();
                    break;
                }

                case R.id.previous: {
                    int seekto = player.getCurrentPosition() - STEP_VALUE;
                    if (seekto < 0)
                        seekto = 0;
                    player.pause();
                    player.seekTo(seekto);
                    player.start();
                    break;
                }
            }
        }
    };
    private MediaPlayer.OnCompletionListener onCompletion = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mp) {
            stopPlay();
        }

        ;
    };
    private MediaPlayer.OnErrorListener onError = new MediaPlayer.OnErrorListener() {
        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
            return false;
        }
    };

    private SeekBar.OnSeekBarChangeListener seekBarChanged =
            new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (isMovingSeekBar) {
                        player.seekTo(progress);
                        Log.i("OnSeekBarChangeListener", "OnProgressChanged");
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    // remove message Handler from updating progress bar
                    handler.removeCallbacks(mUpdateTimeTask);
                    isMovingSeekBar = true;
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                    handler.removeCallbacks(mUpdateTimeTask);
                    int totalDuration = player.getDuration();
                    int currentPosition = utils.progressToTimer(seekBar.getProgress(), totalDuration);

                    // forward or backward to certain seconds
//                    player.seekTo(currentPosition);
                    // update timer progress again
                    updateProgressBar();
                    isMovingSeekBar = false;
                }
            };

//

    /**
     * Update timer on seekbar
     */
    public void updateProgressBar() {
        handler.postDelayed(mUpdateTimeTask, 100);
    }


}
