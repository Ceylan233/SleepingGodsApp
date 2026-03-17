package com.example.sleepgodapp;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.IBinder;

public class MusicService extends Service {

    public static final String ACTION_PLAY = "PLAY";
    public static final String ACTION_PAUSE = "PAUSE";
    public static final String ACTION_VOLUME = "VOLUME";
    public static final String ACTION_LOOP = "LOOP";
    public static final String ACTION_SEEK = "SEEK";

    MediaPlayer player;
    Handler handler = new Handler();

    float userVolume = 0.5f;
    Thread fadeThread = null;

    @Override
    public void onCreate() {
        super.onCreate();

        player = MediaPlayer.create(this, R.raw.bgm);

        // 默认不循环，由UI控制
        player.setLooping(false);

        // 播放结束处理
        player.setOnCompletionListener(mp -> {

            if(!player.isLooping()){

                player.pause();
                player.seekTo(0);

                sendProgress();
            }

        });

        startProgressUpdate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent == null || player == null) {
            return START_STICKY;
        }

        String action = intent.getAction();

        if (ACTION_PLAY.equals(action)) {

            if (!player.isPlaying()) {
                fadeIn();
            }

        }
        else if (ACTION_PAUSE.equals(action)) {

            if (player.isPlaying()) {
                fadeOut();
            }

        }
        else if (ACTION_VOLUME.equals(action)) {

            userVolume = intent.getFloatExtra("volume", 0.5f);
            player.setVolume(userVolume, userVolume);

        }
        else if (ACTION_LOOP.equals(action)) {

            boolean loop = intent.getBooleanExtra("loop", false);
            player.setLooping(loop);

        }
        else if (ACTION_SEEK.equals(action)) {

            int pos = intent.getIntExtra("seek", 0);

            if (pos >= 0 && pos <= player.getDuration()) {

                boolean wasPlaying = player.isPlaying();

                player.seekTo(pos);

                if (wasPlaying) {
                    player.start();
                }

                if (!player.isPlaying() && pos < player.getDuration() - 500) {
                    player.start();
                }
            }
        }

        return START_STICKY;
    }

    // 渐入播放
    private void fadeIn(){

        if(fadeThread != null && fadeThread.isAlive()){
            fadeThread.interrupt();
        }

        player.setVolume(0f,0f);
        player.start();

        fadeThread = new Thread(() -> {

            float volume = 0f;

            while(volume < userVolume){

                volume += 0.05f;

                float v = volume;

                player.setVolume(v,v);

                try{
                    Thread.sleep(80);
                }catch(Exception e){
                    return;
                }
            }

        });

        fadeThread.start();
    }

    // 渐出暂停
    private void fadeOut(){

        if(fadeThread != null && fadeThread.isAlive()){
            fadeThread.interrupt();
        }

        fadeThread = new Thread(() -> {

            float volume = userVolume;

            while(volume > 0f){

                volume -= 0.05f;

                float v = volume;

                player.setVolume(v,v);

                try{
                    Thread.sleep(80);
                }catch(Exception e){
                    return;
                }
            }

            player.pause();

        });

        fadeThread.start();
    }

    // 定时发送进度
    private void startProgressUpdate() {

        handler.postDelayed(new Runnable() {

            @Override
            public void run() {

                if (player != null) {

                    sendProgress();

                    handler.postDelayed(this, 200);
                }
            }

        }, 200);
    }

    // 发送当前播放进度
    private void sendProgress(){

        Intent intent = new Intent("BGM_PROGRESS");
        intent.setPackage(getPackageName());

        intent.putExtra("progress", player.getCurrentPosition());
        intent.putExtra("duration", player.getDuration());

        sendBroadcast(intent);
    }

    @Override
    public void onDestroy() {

        if (player != null) {
            player.release();
            player = null;
        }

        handler.removeCallbacksAndMessages(null);

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}