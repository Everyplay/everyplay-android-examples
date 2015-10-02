package com.everyplay.android.everyplayrecord;

import com.everyplay.Everyplay.Everyplay;
import com.everyplay.Everyplay.EveryplayFaceCamPreviewOrigin;
import com.everyplay.Everyplay.EveryplayFaceCamColor;
import com.everyplay.Everyplay.IEveryplayListener;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.media.AudioManager;
import android.media.SoundPool;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;

public class EveryplayRecordActivity extends Activity implements IEveryplayListener, OnClickListener {
    private static final String TAG = "EveryplayRecord";

    static final Boolean USE_EVERYPLAY_AUDIO_BOARD = false;

    private GLSurfaceView mGLView;

    private static String CLIENT_ID = "b459897317dc88c80b4515e380e1378022f874d2";
    private static String CLIENT_SECRET = "f1a162969efb1c27aac6977f35b34127e68ee163";
    private static String REDIRECT_URI = "https://m.everyplay.com/auth";

    private LinearLayout buttons;

    Handler handler;
    Runnable restartRunnable;

    private SoundPool soundPool;
    private EveryplayRecordAudioGenerator stream1 = null;
    private EveryplayRecordAudioGenerator stream2 = null;
    private EveryplayRecordAudioGenerator streamActive = null;
    private int sound_pew;
    private int sound_pow;
    private float _effect1Pitch = 1.0f;
    private float _effect2Pitch = 1.0f;

    private int buttonCnt = 0;

    Button addButton(String text, String tag) {
        Button a = new Button(this);

        a.setWidth(380);
        a.setText(text);
        a.setTag(tag);
        a.setId(buttonCnt++);
        a.setOnClickListener(this);
        buttons.addView(a);

        return a;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create a GLSurfaceView instance and set it
        // as the ContentView for this Activity
        mGLView = new EveryplayRecordSurfaceView(this);
        setContentView(mGLView);

        buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.VERTICAL);
        if (!USE_EVERYPLAY_AUDIO_BOARD) {
            addButton("Everyplay", "everyplay");
            addButton("Start recording", "rec");
            Button playLastRecording = addButton("Play last recording", "play_last_recording");
            playLastRecording.setVisibility(View.GONE);

            addButton("Test video playback", "test_video_playback");
            addButton("Show sharing modal", "sharing_modal");

            Button hudRecord = addButton("HUD record off", "hud_record");
            hudRecord.setVisibility(View.GONE);

            addButton("Request Rec Permission", "facecam_test");

            stream1 = new EveryplayRecordAudioGenerator(getResources().openRawResource(R.raw.loop));
            stream1.play();
            streamActive = stream1;
        } else {
            addButton("Play song #1", "play1_a");
            addButton("Unload song #1", "unload1_a");
            addButton("Pause song", "pause_a");
            addButton("Resume song", "resume_a");
            addButton("Rewind song", "rewind_a");
            addButton("Stop song", "stop_a");

            this.addContentView(buttons, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            buttons = new LinearLayout(this);
            buttons.setOrientation(LinearLayout.VERTICAL);
            buttons.setX(400);

            addButton("Play song #2", "play2_a");
            addButton("Unload song #2", "unload2_a");
            addButton("Effect #1", "effect1_a");
            addButton("Effect #2", "effect2_a");
            addButton("Start recording", "rec");
            addButton("Play last recording", "play_last_recording");

            soundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);
            sound_pew = soundPool.load(this, R.raw.pew, 1);
            sound_pow = soundPool.load(this, R.raw.pow, 1);
        }

        this.addContentView(buttons, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        init();
    }

    private void init() {
        Everyplay.configureEveryplay(CLIENT_ID, CLIENT_SECRET, REDIRECT_URI);
        Everyplay.initEveryplay(this, this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // The following call pauses the rendering thread.
        // If your OpenGL application is memory intensive,
        // you should consider de-allocating objects that
        // consume significant memory here.
        mGLView.onPause();
        if (streamActive != null) {
            streamActive.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // The following call resumes a paused rendering thread.
        // If you de-allocated graphic objects for onPause()
        // this is a good place to re-allocate them.
        mGLView.onResume();
        if (streamActive != null) {
            streamActive.resume();
        }
    }

    @Override
    public void onEveryplayShown() {
        Log.d(TAG, "onEveryplayShown");
        if (streamActive != null) {
            streamActive.pause();
        }
    }

    @Override
    public void onEveryplayHidden() {
        Log.d(TAG, "onEveryplayHidden");
        if (streamActive != null) {
            streamActive.resume();
        }
    }

    @Override
    public void onEveryplayReadyForRecording(int enabled) {
        Log.d(TAG, "onEveryplayReadyForRecording: " + enabled);
    }

    @Override
    public void onEveryplayRecordingStarted() {
        Log.d(TAG, "onEveryplayRecordingStarted");

        final Button recordButton = (Button) buttons.findViewWithTag("rec");
        final Button hudButton = (Button) buttons.findViewWithTag("hud_record");

        recordButton.post(new Runnable() {
            @Override
            public void run() {
                recordButton.setText("Stop recording");
                if (hudButton != null) {
                    hudButton.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    @Override
    public void onEveryplayRecordingStopped() {
        Log.d(TAG, "onEveryplayRecordingStopped");

        final Button recordButton = (Button) buttons.findViewWithTag("rec");
        final Button playLastRecording = (Button) buttons.findViewWithTag("play_last_recording");
        final Button hudButton = (Button) buttons.findViewWithTag("hud_record");

        recordButton.post(new Runnable() {
            @Override
            public void run() {
                recordButton.setText("Start recording");
                if (playLastRecording != null) {
                    playLastRecording.setVisibility(View.VISIBLE);
                }
                if (hudButton != null) {
                    hudButton.setVisibility(View.GONE);
                }
            }
        });
        JSONObject data = new JSONObject();
        try {
            data.put("testString", "Hello");
            data.put("testInteger", 42);
            data.put("testFloat", 3.14f);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Everyplay.mergeSessionDeveloperData(data);
    }

    @Override
    public void onEveryplayFaceCamSessionStarted() {
        Log.d(TAG, "onEveryplayFaceCamSessionStarted");
        final Button faceCamButton = (Button) buttons.findViewWithTag("facecam_test");
        faceCamButton.setText("Stop FaceCam");
    }

    @Override
    public void onEveryplayFaceCamRecordingPermission(int granted) {
        Log.d(TAG, "onEveryplayFaceCamRecordingPermission: " + granted);
        if (granted != 0) {
            final Button faceCamButton = (Button) buttons.findViewWithTag("facecam_test");
            faceCamButton.setText("Start FaceCam");
        }
    }

    @Override
    public void onEveryplayFaceCamSessionStopped() {
        Log.d(TAG, "onEveryplayFaceCamSessionStopped");
        final Button faceCamButton = (Button) buttons.findViewWithTag("facecam_test");
        faceCamButton.setText("Start FaceCam");
    }

    @Override
    public void onEveryplayUploadDidStart(int videoId) {
        Log.d(TAG, "onEveryplayUploadDidStart: " + videoId);
    }

    @Override
    public void onEveryplayUploadDidProgress(int videoId, double progress) {}

    @Override
    public void onEveryplayUploadDidComplete(int videoId) {
        Log.d(TAG, "onEveryplayUploadDidComplete: " + videoId);
    }

    @Override
    public void onEveryplayThumbnailReadyAtTextureId(int textureId, int portraitMode) {
        Log.d(TAG, "onEveryplayThumbnailReadyAtTextureId: " + textureId + " portraitMode: " + portraitMode);
    }

    @Override
    public void onEveryplayAccountDidChange() {
        Log.d(TAG, "onEveryplayAccountDidChange");
    }

    @Override
    public void onClick(View view) {
        if (view instanceof Button) {
            String tag = (String) ((Button) view).getTag();

            if (tag.equalsIgnoreCase("everyplay")) {
                Everyplay.showEveryplay();
            } else if (tag.equalsIgnoreCase("rec")) {
                if (Everyplay.isRecording()) {
                    Everyplay.stopRecording();
                } else {
                    Everyplay.startRecording();
                }
            } else if (tag.equalsIgnoreCase("test_video_playback")) {
                Everyplay.playVideo("https://api.everyplay.com/videos?order=popularity&limit=1");
            } else if (tag.equalsIgnoreCase("sharing_modal")) {
                Everyplay.showEveryplaySharingModal();
            } else if (tag.equalsIgnoreCase("hud_record")) {
                EveryplayRecordRenderer.hudEnabled = !EveryplayRecordRenderer.hudEnabled;
                Button button = (Button) view;

                if (EveryplayRecordRenderer.hudEnabled) {
                    button.setText("HUD record on");
                } else {
                    button.setText("HUD record off");
                }
            } else if (tag.equalsIgnoreCase("play_last_recording")) {
                Everyplay.playLastRecording();
            } else if (tag.equalsIgnoreCase("facecam_test")) {
                if (Everyplay.FaceCam.isRecordingPermissionGranted()) {
                    if (Everyplay.FaceCam.isSessionRunning()) {
                        Everyplay.FaceCam.stopSession();
                    } else {
                        Everyplay.setMaxRecordingMinutesLength(1);
                        Everyplay.FaceCam.setPreviewPositionX(16);
                        Everyplay.FaceCam.setPreviewPositionY(16);
                        Everyplay.FaceCam.setPreviewSideWidth(128);
                        Everyplay.FaceCam.setPreviewBorderWidth(4);
                        Everyplay.FaceCam.setPreviewBorderColor(new EveryplayFaceCamColor(1, 0.3f, 1, 1.0f));
                        // Everyplay.FaceCam.setAudioOnly(true);
                        Everyplay.FaceCam.setPreviewOrigin(EveryplayFaceCamPreviewOrigin.BOTTOM_RIGHT);
                        Everyplay.FaceCam.startSession();
                    }
                } else {
                    Everyplay.FaceCam.requestRecordingPermission();
                }
            } else if (tag.equalsIgnoreCase("play1_a")) {
                if (streamActive != null) {
                    streamActive.stop();
                    streamActive = null;
                }
                // stream1 = new EveryplayRecordAudioGenerator(6);
                stream1 = new EveryplayRecordAudioGenerator(getResources().openRawResource(R.raw.loop));
                stream1.play();
                streamActive = stream1;
            } else if (tag.equalsIgnoreCase("unload1_a")) {
                if (stream1 != null) {
                    if (stream1 == streamActive) {
                        streamActive = null;
                    }
                    stream1.stop();
                    stream1 = null;
                }
            } else if (tag.equalsIgnoreCase("pause_a")) {
                if (streamActive != null) {
                    streamActive.pause();
                }
            } else if (tag.equalsIgnoreCase("resume_a")) {
                if (streamActive != null) {
                    streamActive.resume();
                }
            } else if (tag.equalsIgnoreCase("rewind_a")) {
                if (streamActive != null) {
                    streamActive.rewind();
                }
            } else if (tag.equalsIgnoreCase("stop_a")) {
                if (streamActive != null) {
                    streamActive.stop();
                }
            } else if (tag.equalsIgnoreCase("play2_a")) {
                if (streamActive != null) {
                    streamActive.stop();
                    streamActive = null;
                }
                // stream2 = new EveryplayRecordAudioGenerator(5);
                stream2 = new EveryplayRecordAudioGenerator(getResources().openRawResource(R.raw.loop));
                stream2.play();
                streamActive = stream2;
            } else if (tag.equalsIgnoreCase("unload2_a")) {
                if (stream2 != null) {
                    if (stream2 == streamActive) {
                        streamActive = null;
                    }
                    stream2.stop();
                    stream2 = null;
                }
            } else if (tag.equalsIgnoreCase("effect1_a")) {
                _effect1Pitch += 0.1f;
                if (_effect1Pitch >= 2.0) {
                    _effect1Pitch = 0.2f;
                }
                soundPool.play(sound_pew, 1, 1, 1, 0, _effect1Pitch);
            } else if (tag.equalsIgnoreCase("effect2_a")) {
                _effect2Pitch += 0.1f;
                if (_effect2Pitch >= 2.0) {
                    _effect2Pitch = 0.2f;
                }
                soundPool.play(sound_pow, 1, 1, 1, 0, _effect2Pitch);
            }
        }
    }
}
