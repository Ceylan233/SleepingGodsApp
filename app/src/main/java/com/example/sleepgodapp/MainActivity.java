package com.example.sleepgodapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import java.lang.reflect.Field;

public class MainActivity extends AppCompatActivity {

    TextView noticeText;
    EditText inputText;
    ImageButton btnPlay;
    ImageButton btnClear;
    Button btnSearch;
    TextView volumeText;

    SeekBar seekBar;
    TextView timeText;

    MediaPlayer mediaPlayer;
    Handler handler = new Handler();
    Handler bgmHandler = new Handler();

    boolean isUserSeeking = false;
    boolean isBgmSeeking = false;

    // BGM UI
    ImageButton btnBgmPlay;
    SeekBar bgmSeekBar;
    SeekBar volumeBar;
    Switch loopSwitch;
    TextView bgmTime;

    boolean isBgmPlaying = false;

    // ================= BGM广播 =================
    BroadcastReceiver bgmReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            int progress = intent.getIntExtra("progress",0);
            int duration = intent.getIntExtra("duration",0);

            bgmSeekBar.setMax(duration);

            if(isBgmSeeking) return;

            bgmSeekBar.setProgress(progress);
            bgmTime.setText(format(progress)+" / "+format(duration));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        inputText = findViewById(R.id.inputText);
        btnSearch = findViewById(R.id.btnSearch);
        btnPlay = findViewById(R.id.btnPlay);
        seekBar = findViewById(R.id.seekBar);
        timeText = findViewById(R.id.timeText);
        btnClear = findViewById(R.id.btnClear);

        btnBgmPlay = findViewById(R.id.btnBgmPlay);
        bgmSeekBar = findViewById(R.id.bgmSeekBar);
        volumeBar = findViewById(R.id.volumeBar);
        loopSwitch = findViewById(R.id.loopSwitch);
        bgmTime = findViewById(R.id.bgmTime);

        noticeText = findViewById(R.id.noticeText);
        volumeText = findViewById(R.id.volumeText);
        String version = "";

        try {
            version = getPackageManager()
                    .getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {}

        noticeText.setText(
                "公告\n\n" +
                        "本软件功能需要依赖桌游本体\n" +
                        "地城扩展请加前缀DC(例如 DC2.1)\n" +
                        "多数内容为图像识别，可能不准确。\n" +
                        "反馈bug语音错误请发送邮箱 958987692@qq.com\n" +
                        "感谢使用 v" + version
        );

        IntentFilter filter = new IntentFilter("BGM_PROGRESS");

        if (android.os.Build.VERSION.SDK_INT >= 33) {
            registerReceiver(bgmReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(bgmReceiver, filter);
        }

        btnSearch.setOnClickListener(v -> searchAudio());
        btnPlay.setOnClickListener(v -> togglePlay());
        btnClear.setOnClickListener(v -> inputText.setText(""));

        // ================= 语音拖动 =================
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(fromUser && mediaPlayer != null){
                    timeText.setText(format(progress)+" / "+format(mediaPlayer.getDuration()));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isUserSeeking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

                if(mediaPlayer != null){

                    int pos = seekBar.getProgress();
                    mediaPlayer.seekTo(pos);

                    handler.postDelayed(() -> {

                        if(!mediaPlayer.isPlaying()){
                            mediaPlayer.start();
                        }

                        if(mediaPlayer.isPlaying()){
                            btnPlay.setImageResource(android.R.drawable.ic_media_pause);
                            updateSeekBar();
                        }else{
                            btnPlay.setImageResource(android.R.drawable.ic_media_play);
                        }

                    },100);
                }

                isUserSeeking = false;
            }
        });

        // ================= BGM按钮 =================
        btnBgmPlay.setOnClickListener(v -> {

            Intent i = new Intent(this,MusicService.class);

            if(isBgmPlaying){
                i.setAction(MusicService.ACTION_PAUSE);
            }else{
                i.setAction(MusicService.ACTION_PLAY);
            }

            startService(i);

            // ⭐ 延迟确认真实状态
            bgmHandler.postDelayed(() -> {

                isBgmPlaying = !isBgmPlaying;

                if(isBgmPlaying){
                    btnBgmPlay.setImageResource(android.R.drawable.ic_media_pause);
                }else{
                    btnBgmPlay.setImageResource(android.R.drawable.ic_media_play);
                }

            },100);
        });

        // ================= BGM拖动 =================
        bgmSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            int seekPos = 0;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(fromUser){
                    seekPos = progress;
                    bgmTime.setText(format(progress)+" / "+format(bgmSeekBar.getMax()));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isBgmSeeking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

                Intent i = new Intent(MainActivity.this,MusicService.class);
                i.setAction(MusicService.ACTION_SEEK);
                i.putExtra("seek",seekPos);
                startService(i);

                bgmHandler.postDelayed(() -> isBgmSeeking = false,100);
            }
        });

        // ================= 音量 =================
        volumeBar.setMax(100);
        volumeBar.setProgress(50);

        volumeBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                float v = progress / 100f;
                volumeText.setText("音量 "+progress+"%");

                Intent i = new Intent(MainActivity.this,MusicService.class);
                i.setAction(MusicService.ACTION_VOLUME);
                i.putExtra("volume",v);
                startService(i);
            }
            public void onStartTrackingTouch(SeekBar seekBar){}
            public void onStopTrackingTouch(SeekBar seekBar){}
        });

        // ================= 循环 =================
        loopSwitch.setOnCheckedChangeListener((b,isChecked)->{
            Intent i = new Intent(MainActivity.this,MusicService.class);
            i.setAction(MusicService.ACTION_LOOP);
            i.putExtra("loop",isChecked);
            startService(i);
        });
    }

    private void searchAudio(){

        String input=inputText.getText().toString().trim().toLowerCase();

        if(input.isEmpty()){
            Toast.makeText(this,"请输入名称",Toast.LENGTH_SHORT).show();
            return;
        }

        int resId = getRawId("voice_"+input.replace(".","_"));

        if(resId==0){
            Toast.makeText(this,"未找到语音",Toast.LENGTH_SHORT).show();
            return;
        }

        if(mediaPlayer!=null) mediaPlayer.release();

        mediaPlayer = MediaPlayer.create(this,resId);

        new Handler().postDelayed(() -> {

            mediaPlayer.start();

            handler.postDelayed(() -> {

                if(mediaPlayer.isPlaying()){
                    btnPlay.setImageResource(android.R.drawable.ic_media_pause);
                    updateSeekBar();
                }else{
                    btnPlay.setImageResource(android.R.drawable.ic_media_play);
                }

            },100);

            seekBar.setMax(mediaPlayer.getDuration());

        },1000);
    }

    private void togglePlay(){

        if(mediaPlayer == null) return;

        try{

            if(mediaPlayer.isPlaying()){

                mediaPlayer.pause();
                btnPlay.setImageResource(android.R.drawable.ic_media_play);

            }else{

                mediaPlayer.start();

                handler.postDelayed(() -> {

                    if(mediaPlayer.isPlaying()){
                        btnPlay.setImageResource(android.R.drawable.ic_media_pause);
                        updateSeekBar();
                    }else{
                        btnPlay.setImageResource(android.R.drawable.ic_media_play);
                    }

                },100);
            }

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void updateSeekBar(){

        handler.postDelayed(new Runnable(){
            @Override
            public void run(){

                if(mediaPlayer != null){

                    if(!isUserSeeking){

                        int cur = mediaPlayer.getCurrentPosition();
                        int total = mediaPlayer.getDuration();

                        seekBar.setProgress(cur);
                        timeText.setText(format(cur)+" / "+format(total));
                    }

                    handler.postDelayed(this,50);
                }
            }
        },0);
    }

    private String format(int ms){
        int s = ms/1000;
        return String.format("%02d:%02d",s/60,s%60);
    }

    private int getRawId(String name){
        try{
            Field f = R.raw.class.getField(name);
            return f.getInt(f);
        }catch(Exception e){
            return 0;
        }
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(bgmReceiver);
        if(mediaPlayer!=null) mediaPlayer.release();
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}