package com.example.app;

import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import com.example.app.interfaces.ApiConfig;
import com.example.app.managers.AppConfig;
import com.example.app.models.chord_model;
import com.gauravk.audiovisualizer.visualizer.BarVisualizer;
import com.gauravk.audiovisualizer.visualizer.CircleLineVisualizer;
import com.google.android.material.snackbar.Snackbar;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Random;

import kotlin.jvm.internal.Intrinsics;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class MainActivity extends AppCompatActivity {

    Button buttonStart, buttonStop, buttonGet;
    ImageButton buttonPlayLastRecordAudio, buttonPausePlayingRecording,
            buttonStopPlayingRecording;
    String AudioSavePathInDevice = null;
    MediaRecorder mediaRecorder;
    Random random;
    View loader;
    TextView directions;

    LinearLayout outputLayout;
    String RandomAudioFileName = "1234567890";

    public static final int RequestPermissionCode = 1;
    MediaPlayer mediaPlayer = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BarVisualizer mVisualizer = findViewById(R.id.blast);
//        https://github.com/gauravk95/audio-visualizer-android
        buttonStart = (Button) findViewById(R.id.button_record);
        buttonStop = (Button) findViewById(R.id.button_stop_record);
        buttonPlayLastRecordAudio = (ImageButton) findViewById(R.id.button_play);
        buttonPausePlayingRecording = (ImageButton) findViewById(R.id.button_pause);
        buttonStopPlayingRecording = (ImageButton) findViewById(R.id.button_stop_play);
        buttonGet = (Button) findViewById(R.id.get_button);
        directions = (TextView) findViewById(R.id.directions);
        loader = findViewById(R.id.loader);
        outputLayout = findViewById(R.id.output_layout);
        buttonStop.setEnabled(false);
        buttonPlayLastRecordAudio.setEnabled(false);
        buttonStopPlayingRecording.setEnabled(false);
        ExtAudioRecorder extAudioRecorder = ExtAudioRecorder.getInstanse(false);
        random = new Random();

        buttonStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                outputLayout.removeAllViews();
                AudioSavePathInDevice = null;
                if (checkPermission()) {

                    AudioSavePathInDevice =
                            Environment.getExternalStorageDirectory().getAbsolutePath() + "/" +
                                    CreateRandomAudioFileName(5) + "_Recording.WAV";
//                    MediaRecorderReady();
                    extAudioRecorder.setOutputFile(AudioSavePathInDevice);
//                    mediaPlayer = null;
                    Log.d(
                            "eeee_start",
                            extAudioRecorder.getState().toString());
                    try {
                        extAudioRecorder.prepare();
                        extAudioRecorder.start();
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                    }
                    buttonStart.setEnabled(false);
                    buttonStop.setEnabled(true);
                    buttonGet.setVisibility(View.GONE);
                    buttonPausePlayingRecording.setEnabled(false);
                    buttonStopPlayingRecording.setEnabled(false);
                    buttonPlayLastRecordAudio.setEnabled(false);
                    directions.setVisibility(View.GONE);
                    Toast.makeText(MainActivity.this, "Recording started",
                            Toast.LENGTH_SHORT).show();
                } else {
                    requestPermission();
                }
            }
        });

        buttonStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                extAudioRecorder.stop();
//                extAudioRecorder.release();
                extAudioRecorder.reset();
                Log.d(
                        "eeee_stop",
                        extAudioRecorder.getState().toString());
                buttonStop.setEnabled(false);
                buttonPlayLastRecordAudio.setEnabled(true);
                buttonStart.setEnabled(true);
                buttonStopPlayingRecording.setEnabled(false);
                buttonPausePlayingRecording.setEnabled(false);
//                directions.setVisibility(View.GONE);
                mVisualizer.setVisibility(View.VISIBLE);
                buttonGet.setVisibility(View.VISIBLE);
                Toast.makeText(MainActivity.this, "Recording Completed",
                        Toast.LENGTH_SHORT).show();
            }
        });

        buttonPlayLastRecordAudio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) throws IllegalArgumentException,
                    SecurityException, IllegalStateException {

                buttonStop.setEnabled(false);
                buttonStart.setEnabled(false);
                buttonPausePlayingRecording.setEnabled(true);
                buttonStopPlayingRecording.setEnabled(true);
                mVisualizer.setVisibility(View.VISIBLE);
                if (mediaPlayer == null) {
                    mediaPlayer = new MediaPlayer();
                    try {
                        mediaPlayer.setDataSource(AudioSavePathInDevice);
                        Log.d("CCC", AudioSavePathInDevice);
                        mediaPlayer.prepare();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                mediaPlayer.start();
                int audioSessionId = mediaPlayer.getAudioSessionId();
                if (audioSessionId != -1)
                    mVisualizer.setAudioSessionId(audioSessionId);
                Toast.makeText(MainActivity.this, "Recording Playing",
                        Toast.LENGTH_SHORT).show();
                if (!(mediaPlayer.isPlaying())) {
                    buttonStart.setEnabled(true);
                }
            }
        });

        buttonPausePlayingRecording.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mVisualizer.setVisibility(View.VISIBLE);
                buttonStop.setEnabled(false);
                buttonStart.setEnabled(true);
                buttonStopPlayingRecording.setEnabled(true);
                buttonPlayLastRecordAudio.setEnabled(true);
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                    mVisualizer.release();
                    mVisualizer.clearAnimation();
                    MediaRecorderReady();
                } else {

                }

            }
        });
        buttonStopPlayingRecording.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mVisualizer.setVisibility(View.GONE);
                buttonStop.setEnabled(false);
                buttonStart.setEnabled(true);
                buttonStopPlayingRecording.setEnabled(false);
                buttonPausePlayingRecording.setEnabled(false);
                buttonPlayLastRecordAudio.setEnabled(true);

                if (mediaPlayer != null) {
                    mediaPlayer.stop();
                    mediaPlayer.reset();
                    mediaPlayer.release();
                    mediaPlayer = null;
                    mVisualizer.release();
                    mVisualizer.clearAnimation();
                    MediaRecorderReady();
                }

            }
        });
        buttonGet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                uploadFile();
                mVisualizer.setVisibility(View.GONE);
                buttonStop.setEnabled(false);
                buttonStart.setEnabled(true);
                buttonStopPlayingRecording.setEnabled(false);
                buttonPausePlayingRecording.setEnabled(false);
                buttonPlayLastRecordAudio.setEnabled(true);
            }
        });


    }


    public void MediaRecorderReady() {
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB);
        mediaRecorder.setOutputFile(AudioSavePathInDevice);
    }

    public String CreateRandomAudioFileName(int string) {
        StringBuilder stringBuilder = new StringBuilder(string);
        int i = 0;
        while (i < string) {
            stringBuilder.append(RandomAudioFileName.
                    charAt(random.nextInt(RandomAudioFileName.length())));

            i++;
        }
        return stringBuilder.toString();
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(MainActivity.this, new
                String[]{WRITE_EXTERNAL_STORAGE, RECORD_AUDIO}, RequestPermissionCode);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case RequestPermissionCode:
                if (grantResults.length > 0) {
                    boolean StoragePermission = grantResults[0] ==
                            PackageManager.PERMISSION_GRANTED;
                    boolean RecordPermission = grantResults[1] ==
                            PackageManager.PERMISSION_GRANTED;

                    if (StoragePermission && RecordPermission) {
                        Toast.makeText(MainActivity.this, "Permission Granted",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "Permission Denied", Toast.LENGTH_SHORT).show();
                    }
                }
                break;
        }
    }

    public boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(getApplicationContext(),
                WRITE_EXTERNAL_STORAGE);
        int result1 = ContextCompat.checkSelfPermission(getApplicationContext(),
                RECORD_AUDIO);
        return result == PackageManager.PERMISSION_GRANTED &&
                result1 == PackageManager.PERMISSION_GRANTED;
    }

    public static String getMimeType(String url) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        return type;
    }

    public boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        return cm.getActiveNetworkInfo() != null &&
                cm.getActiveNetworkInfo().isConnectedOrConnecting();
    }

    private void uploadFile() {
        if (isOnline()) {
            outputLayout.removeAllViews();
            loader.setVisibility(View.VISIBLE);
            // Map is used to multipart the file using okhttp3.RequestBody
            File file = new File(AudioSavePathInDevice);
            String mime = getMimeType(file.getAbsolutePath());
            // Parsing any Media type file
            RequestBody requestBody = RequestBody.create(MediaType.parse(mime), file);
            MultipartBody.Part fileToUpload = MultipartBody.Part.createFormData("file", file.getName(), requestBody);
            RequestBody filename = RequestBody.create(MediaType.parse("text/plain"), file.getName());

            ApiConfig getResponse = AppConfig.getRetrofit().create(ApiConfig.class);

            Call call = getResponse.uploadFile(fileToUpload, filename);
            Log.v("upload", "upload");
            call.enqueue((Callback) (new Callback() {
                public void onResponse(@NotNull Call call, @NotNull Response response) {
                    Intrinsics.checkParameterIsNotNull(call, "call");
                    Intrinsics.checkParameterIsNotNull(response, "response");
                    if (response.isSuccessful()) {
                        chord_model serverResponse = (chord_model) response.body();
                        if (serverResponse.getChords() != null) {
                            loader.setVisibility(View.GONE);
//                        Toast.makeText(getApplicationContext(), serverResponse.getChords().toString(), Toast.LENGTH_SHORT).show();
                            directions.setVisibility(View.VISIBLE);

                            List<String> chords = serverResponse.getChords();
                            List<String> altChords = serverResponse.getAlternativeChords();
                            List<String> altChords2 = serverResponse.getAlternativeChords2();
                            List<String> timeChords = serverResponse.getTime();
//                            List<String> basses = serverResponse.getBass();


                            for (int i = 0; i < chords.size(); i++) {
                                String chord = chords.get(i);
                                String altChord = altChords.get(i);
                                String altChord2 = altChords2.get(i);
                                String timeChord = timeChords.get(i);
//                                String bass = basses.get(i);
                                View chordView = getLayoutInflater().inflate(R.layout.chord_item_layout, null);
                                TextView chordText = chordView.findViewById(R.id.chord_text);
                                TextView timeText = chordView.findViewById(R.id.chord_time);
                                TextView altchordText = chordView.findViewById(R.id.alt_chord_text);
                                TextView altchordText2 = chordView.findViewById(R.id.alt_chord_text2);
                                chordText.setText(chord);
                                altchordText.setText(altChord);
                                altchordText2.setText(altChord2);
                                timeText.setText(timeChord + " sec");

                                chordView.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        if (altchordText.getVisibility() == View.VISIBLE) {
                                            altchordText.setVisibility(View.GONE);
                                            altchordText2.setVisibility(View.VISIBLE);
                                        } else if (altchordText2.getVisibility() == View.VISIBLE) {
                                            altchordText2.setVisibility(View.GONE);
                                        } else {
                                            altchordText.setVisibility(View.VISIBLE);
                                        }
                                    }
                                });

                                outputLayout.addView(chordView);
                            }

                        } else {
                            loader.setVisibility(View.GONE);
                            Toast.makeText(getApplicationContext(), "No chords detected!", Toast.LENGTH_SHORT).show();
                        }
                    }
//                progressDialog.dismiss();
                }

                @Override
                public void onFailure(Call call, Throwable t) {
                    loader.setVisibility(View.GONE);
                    Log.v("Response", "wasnt successfull");
                    onFailPressed();
                }
            }));
        } else {
            checkNetworkConnection();
            Log.v("Response", "No internet connection");

        }
    }

    public void checkNetworkConnection() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("No internet Connection");
        builder.setIcon(android.R.drawable.presence_offline);
        builder.setMessage("Please turn on internet connection to continue");
        builder.setNegativeButton("close", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Closing ReChord")
                .setMessage("Are you sure you want to exit?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }

                })
                .setNegativeButton("No", null)
                .show();
    }

    public void onFailPressed() {
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Failed request")
                .setMessage("Please try again later.")
                .setPositiveButton("Ok!", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }

                })
                .show();
    }

    private void stopRecording() throws Exception {

    }
}


