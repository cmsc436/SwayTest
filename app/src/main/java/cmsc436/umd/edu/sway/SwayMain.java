package cmsc436.umd.edu.sway;


import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import edu.umd.cmsc436.sheets.Sheets;

public class SwayMain extends AppCompatActivity {
    HashSet<String> RETURN_KEY_PHRASE = new HashSet<>();
    HashSet<String> CONTINUE_KEY_PHRASE = new HashSet<>();

    String[] CONT = {"GO", "Continue", "Start","Begin"};
    String[] RET = {"BACK", "GO BACK", "RETURN"};


    // total test time = TEST_DURATION + PRETEST_DURATION
    private final int TEST_DURATION = 10000; // how long the test will last (in milliseconds)
    private final int TEST_INTERVAL = 1000; // interval for update during test

    // wait period between the patient taking position
    // and the test starting to record data
    private final int PRETEST_DURATION = 5000;
    private final int PRETEST_INTERVAL = 1000; //interval for update of pre-test

    // Intent constants
    final static String HEATMAP = "HEAT_MAP";
    final static String PATHMAP = "PATH_MAP";
    final static String FINAL_SCORE = "FINAL_SCORE";
    final static String TRIAL_NUM = "TRIAL_COUNT";
    final static String TEST_TYPE = "TEST_TPE";
    final static String USER_ID = "USER_ID";

    // service to access all of the data
    MeasurementService measurementService;
    boolean isServiceBound = false;

    // Object responsible for sending info to the sheets
    SheetManager sheetManager;

    //Text to Speech
    TextToSpeech tts;

    //Speech Recognition
    SpeechRecognizer speechRecognizer;
    Intent speechRecogIntent;
    boolean isListening;

    TextView textView;
    ImageView imageView;
    Bitmap bitmapMain;

    //final score
    float finalScore;

    //trial number
    int trialNumber;

    // TTS Bundle
    Bundle ttsParams;

    // current Test Type
    Sheets.TestType currentTest;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sway_main);

        Collections.addAll(RETURN_KEY_PHRASE, RET);
        Collections.addAll(CONTINUE_KEY_PHRASE,CONT);

        sheetManager = new SheetManager(this); // takes care of sending the data to oGoogle Sheets

        tts = new TextToSpeech(this,onInitListener); // Responsible for "Talking:

        ttsParams = new Bundle(); // the Bundle used by TTS to recognize its Engine's Utterance
        ttsParams.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,"1");// setting up KV pair

        // there have been time then the listener is not set up correctly, it is device specific
        int message =  tts.setOnUtteranceProgressListener(utteranceProgressListener);
        if(message == TextToSpeech.ERROR) Toast.makeText(this,"PROBLEM SETTING UTTRANCE ",Toast.LENGTH_LONG).show();
        else Toast.makeText(this,"GOOD SETTING UTTRANCE ",Toast.LENGTH_LONG).show();


        textView = (TextView) findViewById(R.id.sway_text);
        imageView = (ImageView) findViewById(R.id.image_view);
        getPermission();

        // sets up the current Test Type
        currentTest = (Info.getTestType() != null) ?
                Info.getTestType() : (Sheets.TestType) getIntent().getSerializableExtra(TEST_TYPE);

//        // TODO REMOVE LATER ONLY FOR DEBUGGING
        if(currentTest == null) currentTest = null;


        trialNumber = getIntent().getIntExtra(TRIAL_NUM,0); // trials to repeat

        // will take care of taking in vocal input
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecogIntent = getSpeechReconitionIntent();
        speechRecognizer.setRecognitionListener(new SpeechRecognitionListener());
    }

    // binding the service to the activity
    @Override
    protected void onStart() {
        super.onStart();
        bindService(
                new Intent(this,MeasurementService.class),
                serviceConnection,
                Context.BIND_AUTO_CREATE
        );
    }

    //unbinding the activity to the service
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(isServiceBound) unbindService(serviceConnection);
        tts.shutdown();
        if(speechRecognizer != null) speechRecognizer.destroy();
    }

    // starting the test
    // TODO replace this with something even better
    public void clickMe(View v){
        tts.stop();
        (findViewById(R.id.sway_start)).setOnClickListener(restartActivity);
        ((Button)findViewById(R.id.sway_start)).setText("Restart");
        textView.setText("Test Starting");
        speakText(currentTest);
    }

    // shows the loaded picture
    public void showPic(View v){
        textView.setVisibility(View.INVISIBLE);
        imageView.setVisibility(View.VISIBLE);

    }

    // shows score
    public void showScore(View v){
        imageView.setVisibility(View.INVISIBLE);
        textView.setText("Final Score: "+ finalScore);
        textView.setVisibility(View.VISIBLE);

    }

    // Handles Intro Speech depending on the Test Type
    private void speakText(Sheets.TestType t){
        if(t == null)  tts.speak(getString(R.string.test_instr_practice),TextToSpeech.QUEUE_ADD,ttsParams,"1");
        else if( t == Sheets.TestType.SWAY_OPEN_APART) tts.speak(getString(R.string.test_instr_1),TextToSpeech.QUEUE_ADD,ttsParams,"1");
        else if(t == Sheets.TestType.SWAY_OPEN_TOGETHER) tts.speak(getString(R.string.test_instr_2),TextToSpeech.QUEUE_ADD,ttsParams,"1");
        else if(t == Sheets.TestType.SWAY_CLOSED) tts.speak(getString(R.string.test_instr_3),TextToSpeech.QUEUE_ADD,ttsParams,"1");


    }

    // Responsible for connecting to the Measurement Service
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            measurementService = ((MeasurementService.LocalBinder)service).getService();
            isServiceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            if(measurementService.unhookHandler()){
                isServiceBound = false;
            }
        }
    };

    private TextToSpeech.OnInitListener onInitListener = new TextToSpeech.OnInitListener() {
        @Override
        public void onInit(int status) {
            if( status == TextToSpeech.SUCCESS){
                int result = tts.setLanguage(Locale.US);
                if(result==TextToSpeech.LANG_MISSING_DATA ||
                        result==TextToSpeech.LANG_NOT_SUPPORTED){
                    Log.e("error", "This Language is not supported");
                }

            }

        }
    };

    // TODO HANDLE VOICE RECOG WHEN THE INTRO IS GOING
    private UtteranceProgressListener utteranceProgressListener = new UtteranceProgressListener() {
        @Override
        public void onStart(String utteranceId) {
            SwayMain.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.e("Speech", "STARTED SPEECH RECOG");
                    speechRecognizer.startListening(speechRecogIntent);
                }
            });
        }

        // TODO REMOVE PRESTART WHEN SPEECH IS IMPLEMENTED
        @Override
        public void onDone(String utteranceId) {
            SwayMain.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.e("STARTING","Staring Preest");
                    preTest.start();
                }
            });
        }

        @Override
        public void onError(String utteranceId) {

        }
    };

    // TODO DEFINE WHAT HAPPENS IN PRETEST
    private CountDownTimer preTest = new CountDownTimer(PRETEST_DURATION,PRETEST_INTERVAL) {
        @Override
        public void onTick(long millisUntilFinished) {
            textView.setText("PRETEST(DB): "+millisUntilFinished/1000);
        }

        @Override
        public void onFinish() {
            // when the pre-test step is done the actual test will start
            // this is when MeasurementService will start recording the data
            // TODO Add anything else
            duringTest.start();
            measurementService.startReading();
        }
    };

    // TODO DEFINE WHAT HAPPENS DURING THE TEST
    private CountDownTimer duringTest = new CountDownTimer(TEST_DURATION,TEST_INTERVAL) {

        @Override
        public void onTick(long millisUntilFinished) {
            textView.setText("TESTING(DB): "+millisUntilFinished/1000);
        }

        @Override
        public void onFinish() {
            textView.setText("TEST DONE");
            // stops recording the data
            measurementService.stopReading();
            // now you can call measurementService.getDataList()
            // which will return the recorded data
            List<MeasurementService.DataPoint> l = measurementService.getDataList();

            (findViewById(R.id.sway_show_image)).setVisibility(View.VISIBLE);
            (findViewById(R.id.sway_show_score)).setVisibility(View.VISIBLE);

            Log.e("DataCollection", ""+l.size());

            /**
             * Adding Block of code here to get the images
             * */

            // Added the replacement using DisplayImages :: BEGIN
            DisplayImages visuals = new DisplayImages(l, measurementService.getInitialReading());
            //bitmapMain = visuals.getPath(); // Change to get Quadrant Analysis :: TESTING
            bitmapMain = visuals.getQuadrantAnalysis();
            imageView.setImageBitmap(bitmapMain);
            finalScore = visuals.getMetric();

            Intent intent =  new Intent();
            intent.putExtra(HEATMAP, bitmapMain);
            intent.putExtra(PATHMAP, visuals.getPath());
            intent.putExtra(FINAL_SCORE, finalScore);
            intent.putExtra(TRIAL_NUM,trialNumber);
            // END

//            sheetManager.sendData(finalScore,
//                    new float[]{1,2,3,4,5,6,7,87,9},
//                    bitmapMain,
//                    Info.getTestType());
        }
    };

    private void getPermission(){
        if (ContextCompat.checkSelfPermission(SwayMain.this,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(SwayMain.this,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(SwayMain.this,
                        new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        1);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }

        }
        if(ContextCompat.checkSelfPermission(SwayMain.this,
                Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED){
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(SwayMain.this,
                    Manifest.permission.RECORD_AUDIO)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(SwayMain.this,
                        new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        1);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }


    }


    // part of the Sheet API, it piggybacks on the
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        sheetManager.onRequestPermissionsResult(requestCode,permissions,grantResults);
//        super.onRequestPermissionsResult(requestCode,permissions,grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        sheetManager.onActivityResult(requestCode,resultCode,data);
    }

    View.OnClickListener restartActivity = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            restartActivity();
        }
    };

    //some times java is stupid
    private void restartActivity(){
        this.recreate();
    }

    // sets up the intent used by the Speech Recognition
    private Intent getSpeechReconitionIntent(){
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,
                Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,
                this.getPackageName());
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,5);

        return intent;
    }

    //Cross references speech recognized with key phrases

    private int interprateSpeech(ArrayList<String> speechList){
        for(String s:speechList){
            if(RETURN_KEY_PHRASE.contains(s)) return 1;
            if(CONTINUE_KEY_PHRASE.contains(s)) return 0;
        }
        return -1;
    }

    // to restart listening
    private void restartSpeech(){
        speechRecognizer.stopListening();
        speechRecognizer.startListening(speechRecogIntent);
    }

    protected class SpeechRecognitionListener implements RecognitionListener {

        @Override
        public void onBeginningOfSpeech() {
            Log.d("SPEECH", "onBeginingOfSpeech");
        }

        @Override
        public void onBufferReceived(byte[] buffer) {

        }

        @Override
        public void onEndOfSpeech() {
            Log.d("SPEECH", "onEndOfSpeech");
        }

        @Override
        public void onError(int error) {
            speechRecognizer.startListening(speechRecogIntent);

            //Log.d(TAG, "error = " + error);
        }

        @Override
        public void onEvent(int eventType, Bundle params) {

        }

        @Override
        public void onPartialResults(Bundle partialResults) {

        }

        @Override
        public void onReadyForSpeech(Bundle params) {
            Log.d("SPEECH", "onReadyForSpeech"); //$NON-NLS-1$
        }

        @Override
        public void onResults(Bundle results) {
            //Log.d(TAG, "onResults"); //$NON-NLS-1$
            ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            // matches are the return values of speech recognition engine
            // Use these values for whatever you wish to do
            Log.e("SPEECH", Arrays.toString(matches.toArray()));
            int result = interprateSpeech(matches);

            if(result == 1) setResult(RESULT_CANCELED);
            if(result == 0) preTest.start();
            else restartSpeech();
        }

        @Override
        public void onRmsChanged(float rmsdB) {
        }
    }
}
