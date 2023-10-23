package com.artemkilivnik.voicerecorder;

import android.Manifest;
import android.content.ContextWrapper;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.airbnb.lottie.LottieAnimationView;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_AUDIO_PERMISSION_CODE = 101;
    private MediaRecorder mediaRecorder;
    private MediaPlayer mediaPlayer;
    private ImageView ibRecord, ibPlay;
    private TextView tvTime, tvRecordingPath;
    private ImageView ivSimpleBg;
    private boolean isRecording = false;
    private boolean isPlaying = false;

    private int seconds = 0;
    private int dummySeconds = 0;
    private int playableSeconds = 0;
    private String path = null;
    private LottieAnimationView lavPlaying;

    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().hide();

        initializeViews();
        setupListeners();
    }

    private void initializeViews() {
        ibRecord = findViewById(R.id.ib_record);
        ibPlay = findViewById(R.id.ib_play);
        tvTime = findViewById(R.id.tv_time);
        tvRecordingPath = findViewById(R.id.tv_recording_path);
        ivSimpleBg = findViewById(R.id.iv_simple_bg);
        lavPlaying = findViewById(R.id.lav_playing);
        mediaPlayer = new MediaPlayer();
    }

    private void setupListeners() {
        ibRecord.setOnClickListener(view -> {
            if (checkRecordingPermission()) {
                toggleRecording();
            } else {
                requestRecordingPermission();
            }
        });

        ibPlay.setOnClickListener(view -> {
            if (!isPlaying) {
                startPlaying();
            } else {
                stopPlaying();
            }
        });
    }

    private void toggleRecording() {
        if (!isRecording) {
            startRecording();
        } else {
            stopRecording();
        }
    }

    private void startRecording() {
        isRecording = true;
        executorService.execute(() -> {
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setOutputFile(getRecordingFilePath());
            path = getRecordingFilePath();
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

            try {
                mediaRecorder.prepare();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            mediaRecorder.start();
            runOnUiThread(() -> {
                ivSimpleBg.setVisibility(View.VISIBLE);
                lavPlaying.setVisibility(View.GONE);
                tvRecordingPath.setText(getRecordingFilePath());
                playableSeconds = 0;
                seconds = 0;
                dummySeconds = 0;
                ibRecord.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.recording_pause));
                runTimer();
            });
        });
    }

    private void stopRecording() {
        mediaRecorder.stop();
        mediaRecorder.release();
        mediaRecorder = null;
        playableSeconds = seconds;
        dummySeconds = seconds;
        seconds = 0;
        isRecording = false;

        runOnUiThread(() -> {
            ivSimpleBg.setVisibility(View.VISIBLE);
            lavPlaying.setVisibility(View.GONE);
            handler.removeCallbacksAndMessages(null);
            ibRecord.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.recording_active));
        });
    }

    private void startPlaying() {
        if (isRecording) {
            stopRecording();
        }

        if (path != null) {
            try {
                mediaPlayer.setDataSource(getRecordingFilePath());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            Toast.makeText(getApplicationContext(), "No Recording Present", Toast.LENGTH_LONG).show();
            return;
        }

        try {
            mediaPlayer.prepare();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        mediaPlayer.start();
        isPlaying = true;
        ibPlay.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.recording_stop_playing));
        ivSimpleBg.setVisibility(View.GONE);
        lavPlaying.setVisibility(View.VISIBLE);
        runTimer();
    }

    private void stopPlaying() {
        mediaPlayer.stop();
        mediaPlayer.release();
        seconds = 0;
        playableSeconds = dummySeconds;
        mediaPlayer = null;
        mediaPlayer = new MediaPlayer();
        isPlaying = false;
        handler.removeCallbacksAndMessages(null);
        ivSimpleBg.setVisibility(View.VISIBLE);
        lavPlaying.setVisibility(View.GONE);
        ibPlay.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.recording_play));
    }

    private void runTimer() {
        handler = new Handler();
        handler.post(new Runnable() {
            @Override
            public void run() {
                int minutes = (seconds % 3600) / 60;
                int sec = seconds % 60;
                String time = String.format(Locale.getDefault(), "%02d:%02d", minutes, sec);
                tvTime.setText(time);

                if (isRecording || (isPlaying && playableSeconds != -1)) {
                    seconds++;
                    playableSeconds--;

                    if (playableSeconds == -1 && isPlaying) {
                        mediaPlayer.stop();
                        mediaPlayer.release();
                        isPlaying = false;
                        mediaPlayer = null;
                        mediaPlayer = new MediaPlayer();
                        playableSeconds = dummySeconds;
                        seconds = 0;
                        handler.removeCallbacksAndMessages(null);
                        ivSimpleBg.setVisibility(View.VISIBLE);
                        lavPlaying.setVisibility(View.GONE);
                        ibPlay.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.recording_play));
                        return;
                    }
                }

                handler.postDelayed(this, 1000);

            }
        });
    }

    private String getRecordingFilePath() {
        ContextWrapper contextWrapper = new ContextWrapper(getApplicationContext());
        File music = contextWrapper.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        File file = new File(music, "testFile" + ".mp3");
        return file.getPath();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull @NotNull String[] permissions, @NonNull @NotNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_AUDIO_PERMISSION_CODE) {
            if (grantResults.length > 0) {
                boolean permissionToRecord = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                if (permissionToRecord) {
                    Toast.makeText(getApplicationContext(), "Permission Given", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Permission Denied", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private boolean checkRecordingPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_DENIED) {
            return false;
        }
        return true;
    }

    private void requestRecordingPermission() {
        ActivityCompat.requestPermissions(
                MainActivity.this,
                new String[]{Manifest.permission.RECORD_AUDIO},
                REQUEST_AUDIO_PERMISSION_CODE
        );
    }
}