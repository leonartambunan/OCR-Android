package com.nio.ocr.ektp.mobile;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.util.Log;

import java.io.IOException;

public final class BeepManager {

    private static final String TAG = BeepManager.class.getSimpleName();

    private static final float BEEP_VOLUME = 0.20f;

    private final Activity activity;
    private MediaPlayer mediaPlayer;

    public BeepManager(Activity activity) {
        this.activity = activity;
        this.mediaPlayer = null;
        activity.setVolumeControlStream(AudioManager.STREAM_MUSIC);
        mediaPlayer = buildMediaPlayer(activity);
        AudioManager audioService = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
    }


    public void playBeepSoundAndVibrate() {
        if (mediaPlayer != null)  {
            mediaPlayer.start();
        }
    }

   /*public synchronized static void playPingSound(Context context) {

        try {

            AssetFileDescriptor file = context.getResources().openRawResourceFd(R.raw.beep);
            MediaPlayer meidaPlayer = new MediaPlayer();
            meidaPlayer.setDataSource(
                    file.getFileDescriptor(),
                    file.getStartOffset(),
                    file.getLength()
            );
            file.close();
            meidaPlayer.prepare();
            meidaPlayer.setVolume(0.5F, 0.5F);
            meidaPlayer.start();

            meidaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    mp.release();
                }
            });
        } catch (Exception e) {
            Log.e(Constants.TAG,e.getMessage(),e);
        }
    }*/

    private static MediaPlayer buildMediaPlayer(Context activity) {
        MediaPlayer mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        // When the beep has finished playing, rewind to queue up another one.
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            public void onCompletion(MediaPlayer player) {
                player.seekTo(0);
            }
        });

        AssetFileDescriptor file = activity.getResources().openRawResourceFd(R.raw.beep);
        try {
            mediaPlayer.setDataSource(file.getFileDescriptor(), file.getStartOffset(), file.getLength());
            file.close();
            mediaPlayer.setVolume(BEEP_VOLUME, BEEP_VOLUME);
            mediaPlayer.prepare();
        } catch (IOException ioe) {
            Log.w(TAG, ioe);
        }
        return mediaPlayer;
    }

    public void releaseMediaPlayer() {
        if (mediaPlayer!=null) {
            mediaPlayer.release();
        }
    }
}
