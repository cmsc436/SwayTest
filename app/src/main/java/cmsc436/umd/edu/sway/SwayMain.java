package cmsc436.umd.edu.sway;


import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.hookedonplay.decoviewlib.DecoView;
import com.hookedonplay.decoviewlib.charts.SeriesItem;
import com.hookedonplay.decoviewlib.events.DecoEvent;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import edu.umd.cmsc436.frontendhelper.TrialMode;
import edu.umd.cmsc436.sheets.Sheets;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class SwayMain extends AppCompatActivity {

    //DEBUG LOG CODE
    private final String MD = "METHOD";
    private final String LN = "LISTENER";

    /***************TEST DURATION AND DEFINATION***************/
    // total test time = TEST_DURATION + PRETEST_DURATION
    private final int TEST_DURATION = 10000; // how long the test will last (in milliseconds)
    private final int TEST_INTERVAL = 100; // interval for update during test

    // wait period between the patient taking position
    // and the test starting to record data
    private final int PRETEST_DURATION = 5000;
    private final int PRETEST_INTERVAL = 100; //interval for update of pre-test



    /*************** INTENT CONSTANTS ***************/
    // Intent constants
    final static String HEATMAP = "HEAT_MAP";
    final static String PATHMAP = "PATH_MAP";
    final static String FINAL_SCORE = "FINAL_SCORE";
    final static String AVG_BW_POINTS = "AVG_BW_POINTS";
    final static String AVG_FR_CENTER = "AVG_FR_CENTER";
    final static String VAR_BW_POINTS = "VAR_BW_POINTS";
    final static String VAR_FR_CENTER = "VAR_FR_CENTER";
    final static String STD_DEV_BW_POINTS = "STD_DEV_BW_POINTS";
    final static String STD_DEV_FR_CENTER = "STD_DEV_FR_CENTER";
    final static String TRIAL_NUM = "TRIAL_COUNT";


    /*************** SERVICE FOR GETTING READING***************/
    MeasurementService measurementService; // current Test Type
    boolean isServiceBound = false; // used to safely disconnect the service



    /*************** BOOK KEEPING VARS ***************/
    TextView textView; // for updating the what the app is doing
    boolean isDone;// is the test done

    float finalScore; // what the metric outputs
    boolean isDoubleTapped = false; // was there a double tap
    boolean instructionsCalled = false; // where instructions called

    // arc animation
    DecoView arcView;
    int arcAnimationIndex;

    ///////////////////////////
    //     TRIAL MODE VARS   //
    ///////////////////////////
    Intent currentIntent; // used to access all of all of the parameters
    boolean isTrial; // if the app is in Trial mode or not
    int trialNumber; // number of total trials
    int currentTrial ; // current trial number
    Sheets.TestType currentTest;// current Test Type




    /***************Speech Recognition***************/
    SpeechRecognizer speechRecognizer; // main object
    Intent speechRecogIntent; // intent that defines the recognition

    // key Phrases
    HashSet<String> RETURN_KEY_PHRASE = new HashSet<>();
    HashSet<String> CONTINUE_KEY_PHRASE = new HashSet<>();
    String[] CONT = {"go", "continue", "start","begin"};
    String[] RET = {"back", "go back", "return"};

    /*************** TEXT TO SPEECH ***************/
    TextToSpeech tts; //Text to Speech Main Object
    Bundle ttsParams; // TTS Bundle used to ID Utterances
    final static String INITIAL_TTS_SPEECH_ID = "1";
    final static String INTERMEDIATE_TTS_SPEECH_ID = "2";
    final static String ENDING_TTS_SPEECH_ID = "3";






    /***********************************************************************************************
     ***********************************************************************************************
     *                                      MAIN CODE
     ***********************************************************************************************
     **********************************************************************************************/

    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.action_menu, menu);
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sway_main);

        //////////////////////////////////////////////////////////////////
        //                       SETTING CLASS VARIABLES                //
        //////////////////////////////////////////////////////////////////

        // top toolbar, had the help icon as well
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // bottom action bar, has back, instruction and spoken instructions
        ActionBar actionBar = getSupportActionBar();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        // setting the listner for back
        ((BottomNavigationView) findViewById(R.id.bottom_navigation))
                .setOnNavigationItemSelectedListener(navigationItemSelectedListener);

        // progress arc
        arcView = (DecoView) findViewById(R.id.sway_main_deco_view);

        // phrases that will start the test
        Log.d(MD,"------------------- ON CREATE ");
        isDone = false;

        // text view that will say: Start Test, Testing and Done
        textView = (TextView) findViewById(R.id.sway_main_text);
        getPermission(); // acquiring permissions for recording the audio for Voice Recognition

        currentIntent = getIntent(); // gets the intent to be used to access all of the variables
        currentTest = TrialMode.getAppendage(currentIntent); // sets up the current Test Type
        isTrial = (currentTest != null); // if current test is null then this is PRACTICE mode else it is TRIAL mode

        // If we are in Trail Mode, initialize the appropriate variables
        if(isTrial){
            trialNumber = TrialMode.getTrialOutOf(currentIntent);
            currentTrial = TrialMode.getTrialNum(currentIntent);
            Info.setTestType(currentTest);
            Info.setUserId(TrialMode.getPatientId(currentIntent));
        }


        //////////////////////////////////////////////////////////////////
        //                              TTS                             //
        //////////////////////////////////////////////////////////////////
        tts = new TextToSpeech(this,onInitListener); // Responsible for "Talking"

        // adjusting the voice quality
        tts.setVoice(new Voice(
                "Main",
                Locale.getDefault(),
                Voice.QUALITY_NORMAL,
                Voice.LATENCY_NORMAL,
                false,
                new HashSet<String>()));
        ttsParams = new Bundle(); // the Bundle used by TTS to recognize its Engine's Utterance
        ttsParams.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,INITIAL_TTS_SPEECH_ID);// setting up KV pair

        // setting the Progress listner, so certain speech start correct part of the app
        tts.setOnUtteranceProgressListener(utteranceProgressListener);



        //////////////////////////////////////////////////////////////
        //                     SPEECH RECOGNITION (SR)              //
        //////////////////////////////////////////////////////////////
        // will take care of taking in vocal input
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecogIntent = getSpeechRecognitionIntent(); // intent used for SR
        speechRecognizer.setRecognitionListener(new SpeechRecognitionListener()); // Listener to react to when speech
//        speechRecognizer.setRecognitionListener(recognitionListener); // Listener to react to when speech
        Collections.addAll(RETURN_KEY_PHRASE, RET);
        Collections.addAll(CONTINUE_KEY_PHRASE,CONT);

    }

    @Override
    protected void onStart() {
        super.onStart();
//        currentIntent = getIntent(); // gets the intent to be used to access all of the variables
//        currentTest = TrialMode.getAppendage(currentIntent); // sets up the current Test Type
//        isTrial = (currentTest != null); // if current test is null then this is PRACTICE mode else it is TRIAL mode
//
//
//        if(isTrial){
//            Log.e("IS TRIAL", "NOT IN TRIAL");
//            trialNumber = TrialMode.getTrialOutOf(currentIntent);
//            currentTrial = TrialMode.getTrialNum(currentIntent);
//            Info.setTestType(currentTest);
//            Info.setUserId(TrialMode.getPatientId(currentIntent));
//        }
        Log.d(MD,"-------------------onStart");
        // binding the measurements service to the activity
        bindService(
                new Intent(this,MeasurementService.class),
                serviceConnection,
                Context.BIND_AUTO_CREATE
        );


    }

    /*
        Here majority of the callback procedure will start
     */
    @Override
    protected void onResume() {
        super.onResume();
        Log.d(MD,"-------------------onResume");
        // if we come from instructions activity, restart the initial TTS phrase
        if(!isDone)initializeTestingProcedure();
        if(instructionsCalled){
            tts.speak(getString(R.string.sway_tts_start_instr),
                    TextToSpeech.QUEUE_ADD,
                    ttsParams,
                    INITIAL_TTS_SPEECH_ID);

            textView.setText("Start Test");
        }
        // setup the progress arc and the animations for it
        initArc();
        initAnimation();

    }


    /*
        If the activity is paused,
        meaning another activity has take the foreground then:
        -> reset the doubleTap indicator, so the user can double tap again
        -> stop TTS from taking
        -> stop the counters
     */
    @Override
    protected void onPause() {
        super.onPause();
        isDoubleTapped = false;
        tts.stop();
        preTest.cancel();
        duringTest.cancel();
    }

    /*
        Upon destruction fof the activity:
        -> service will unbind
        -> TTS and Speech Recognizer will be shutdown
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(MD,"-------------------onDestroy");
        if(isServiceBound) unbindService(serviceConnection);
        tts.shutdown();
        if(speechRecognizer != null) speechRecognizer.destroy();
    }

    /*
        If the back button is pressed, and the test is not done,
        the activity will close with RESULT_CANCELED
     */
    @Override
    public void onBackPressed() {
        Log.d(MD,"-------------------onBackPressed");
        if(!isDone) {
            Intent i = new Intent();
            i.putExtra(TrialMode.KEY_SCORE,finalScore);
            setResult(RESULT_CANCELED,i);
            finish();
        }
        super.onBackPressed();
    }

    /*
        When the result from DisplayResult.java is triggered,
        this just piggy backs the data back to Front End Team.
        The result code for finishing will be the same as that of Display Result.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(MD,"-------------------onActivityResult");
        isDone = false;
        Log.e("SWAY_MAIN_AC_RESULT","DATA: "+data.getFloatExtra(TrialMode.KEY_SCORE,-1));
        setResult(resultCode,data);
        finish();
    }


    // setting whe all text to this font
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    // any of the booking keeping (if needed) goes here
    public void initializeTestingProcedure(){
        Log.d(MD,"initializeTestingProcedure");
        tts.stop();
        //textView.setText("Test Starting");
    }

    /***********************************************************************************************
     *                                      Arc Animation
     **********************************************************************************************/

    // setting the background and foreground arc colors and width
    private void initArc(){
        // Create background track
        arcView.addSeries(new SeriesItem.Builder(Color.argb(255, 218, 218, 218))
                .setRange(0, 100, 100)
                .setInitialVisibility(false)
                .setLineWidth(32)
                .build());

        //Create data series track
        SeriesItem seriesItem1 = new SeriesItem.Builder(Color.argb(255, 0, 51, 153))
                .setRange(0, PRETEST_DURATION+TEST_DURATION, 0)
                .setLineWidth(32f)
                .build();
        // index to refer to the foreground arc for
        arcAnimationIndex = arcView.addSeries(seriesItem1);
    }

    // opening animation, when the acr popes up
    private void initAnimation(){
        arcView.addEvent(new DecoEvent.Builder(DecoEvent.EventType.EVENT_SHOW, true)
                .setDelay(500)
                .setDuration(1000)
                .build());
    }

    // updating the acr animations, increasing (or decreasing) the values of front arc
    private void animateArc(float f){
        arcView.addEvent(new DecoEvent.Builder(f).setIndex(arcAnimationIndex).setDelay(0).build());

    }


    /***********************************************************************************************
     *                                      MENU
     **********************************************************************************************/

    // Listener for main actions done by the bottom navigation bar
    BottomNavigationView.OnNavigationItemSelectedListener navigationItemSelectedListener = new BottomNavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()){
                case R.id.home:
                    onBackPressed();
                    return true;
                case R.id.action_help:
                    startInstruction();
                    return true;
                case R.id.action_replay:
                    speakTextInstruction(currentTest); // TTS for current test will start
                    return true;
            }
            return false;
        }
    };

    // for the top navigation bar, it only has instructions button
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_help:
                startInstruction();
                return true;
        }
        return false;
    }

    // starts the instructions activity,
    // appropriate addon will be added for the activity to handle
    private void startInstruction(){
        Intent instructionsIntent = new Intent(this, FragmentPagerSupport.class);
        if(isTrial)instructionsIntent.putExtras(currentIntent.getExtras());
        instructionsIntent.setAction(currentIntent.getAction());
        startActivity(instructionsIntent);
        instructionsCalled = true;
    }



    // Responsible for connecting to the Measurement Service
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(MD+LN,"ServiceConnection - onServiceConnected");
            measurementService = ((MeasurementService.LocalBinder)service).getService();
            isServiceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(MD+LN,"ServiceConnection - onServiceDisconnected");
            if(measurementService.unhookHandler()){
                isServiceBound = false;
            }
        }
    };

    /***********************************************************************************************
     *                                  TEXT - TO - SPEECH
     **********************************************************************************************/

    // Handles Intro Speech depending on the Test Type
    private void speakTextInstruction(Sheets.TestType t){
        speechRecognizer.stopListening();
        measurementService.restartReading();
        if(preTest != null) preTest.cancel();
        if(duringTest != null) duringTest.cancel();
        Log.d(MD,"speakText");
        if (!isTrial)
            tts.speak(getString(R.string.test_instr_practice), TextToSpeech.QUEUE_FLUSH, ttsParams, INITIAL_TTS_SPEECH_ID);

        else {
            switch (t) {
                case SWAY_OPEN_APART:
                    tts.speak(getString(R.string.test_instr_1), TextToSpeech.QUEUE_FLUSH, ttsParams, INITIAL_TTS_SPEECH_ID);
                    break;
                case SWAY_OPEN_TOGETHER:
                    tts.speak(getString(R.string.test_instr_2), TextToSpeech.QUEUE_FLUSH, ttsParams, INITIAL_TTS_SPEECH_ID);
                    break;
                case SWAY_CLOSED:
                    tts.speak(getString(R.string.test_instr_3), TextToSpeech.QUEUE_FLUSH, ttsParams, INITIAL_TTS_SPEECH_ID);
                    break;
                default:
                    tts.speak(getString(R.string.test_instr_default), TextToSpeech.QUEUE_FLUSH, ttsParams, INITIAL_TTS_SPEECH_ID);
                    break;

            }
        }

    }

    /*
        Initialises the TTS, it is a resource intensive process.
        thus it occurs in a separate thread.
        That's why initial instructions can will start here.
        This is also why there is a pause when the activity starts.
     */
    private TextToSpeech.OnInitListener onInitListener = new TextToSpeech.OnInitListener() {
        @Override
        public void onInit(int status) {
            Log.d(MD+LN,"TextToSpeech.OnInitListener - onInit");
            // called after trying to set up the TTS Engine
            if( status == TextToSpeech.SUCCESS){
                Log.e("TTS", "SUCCESS");
                int result = tts.setLanguage(Locale.US);
                if(result==TextToSpeech.LANG_MISSING_DATA ||
                        result==TextToSpeech.LANG_NOT_SUPPORTED){
                    // if there is a Problem with TTS
                    Log.e("TTS", "This Language is not supported");
                    Toast.makeText(SwayMain.this,
                            "Language not supported \n Please Swich to English",
                            Toast.LENGTH_LONG).show();
                }else{
                    // Starting speech, This Starts the whole test
                    tts.speak(getString(R.string.sway_tts_start_instr),
                            TextToSpeech.QUEUE_ADD,
                            ttsParams,
                            INITIAL_TTS_SPEECH_ID);
                }

            }else {
                Log.e("TTS", "TTS FAILED");
            }

        }
    };

    /*
        What to do when the speech is done.
        If Initial Speech, start the Speech Recognition
        if Intermediate Speech, start the test
        If Ending Speech, start the end analysis

        Note, all actions must be ran on the Main UI thread
     */
    private UtteranceProgressListener utteranceProgressListener = new UtteranceProgressListener() {
        @Override
        public void onStart(String utteranceId) {
        }

        @Override
        public void onDone(String utteranceId) {
            Log.d(MD+LN,"UtteranceProgressListener - onDone");
            if(utteranceId.equals(INITIAL_TTS_SPEECH_ID)){
                SwayMain.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        speechRecognizer.startListening(speechRecogIntent);
                    }
                });            }
            else if(utteranceId.equals(INTERMEDIATE_TTS_SPEECH_ID)) {
                SwayMain.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.e("STARTING", "Staring PreTest");
                        textView.setText(R.string.sway_main_testing);

                        preTest.start();
                        isDoubleTapped = true;
                    }
                });
            }
            else if(utteranceId.equals(ENDING_TTS_SPEECH_ID)){
                SwayMain.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        testDone();
                    }
                });
            }
        }

        @Override
        public void onError(String utteranceId) {
            Log.e(MD+LN,"UtteranceProgressListener - onError: Utterance: "+utteranceId);
        }

    };


    /***********************************************************************************************
     *                                  COUNT DOWN DEFINITION
     **********************************************************************************************/

    // PRETEST used are a wait time, it is just here so the patient
    // is already in the testing position before the starting to take the measurements
    private CountDownTimer preTest = new CountDownTimer(PRETEST_DURATION,PRETEST_INTERVAL) {
        float time = 0; // to set position of the progress in relation to time passed
        @Override
        public void onTick(long millisUntilFinished) {
            animateArc(time+=PRETEST_INTERVAL);  //arc update
        }

        @Override
        public void onFinish() {
            Log.d(MD+LN,"CountDownTimer - onFinish - PRE");
            duringTest.start();
            measurementService.restartReading();
            measurementService.startReading();
        }
    };

    //  This is where the testing will take in to place
    private CountDownTimer duringTest = new CountDownTimer(TEST_DURATION,TEST_INTERVAL) {
        float time = PRETEST_DURATION;
        @Override
        public void onTick(long millisUntilFinished) {
            animateArc(time+=(TEST_INTERVAL));  // arc update
        }

        // Ending Speech starts,
        // and then core analysis of the test (in DisplayImages) will take place
        @Override
        public void onFinish() {
            tts.speak(getString(R.string.tts_testing_done),TextToSpeech.QUEUE_ADD, ttsParams, ENDING_TTS_SPEECH_ID);
            Log.d(MD+LN,"CountDownTimer - onFinish - DURING");
        }
    };

    /*
        Analysis of the Data
     */
    private void testDone(){
        animateArc(PRETEST_DURATION+TEST_INTERVAL);
        textView.setText(R.string.sway_main_test_done); // text update

        measurementService.stopReading(); // stop taking reading of the data
        // now you can call measurementService.getDataList()
        // which will return the recorded data
        List<MeasurementService.DataPoint> l = measurementService.getDataList();
        Log.d("DataCollection", ""+l.size());

        //////////////////////////////////////////////////////////////////////////////////////////
        //                                 Analysis
        //////////////////////////////////////////////////////////////////////////////////////////

        // starting the metric object
        DisplayImages visuals = new DisplayImages(l, measurementService.getInitialReading());

        finalScore = visuals.getMetric(); // final score for current trial

        Intent intent =  new Intent(SwayMain.this,DisplayResult.class); // intent for result class
        intent.putExtra(HEATMAP, compressToByteArray(visuals.getQuadrantAnalysis())); //heat map
        intent.putExtra(PATHMAP, compressToByteArray(visuals.getPath())); // path map
        intent.putExtra(FINAL_SCORE, finalScore); // metric

        // Extra Raw Data Explained in DisplayImages
        float statData = visuals.getAverageBetweenPoint();
        intent.putExtra(AVG_BW_POINTS,statData);
        statData = visuals.getVarianceBetweenPoint(statData);
        intent.putExtra(VAR_BW_POINTS,statData);
        statData = visuals.getStdDevBetweenPoint(statData);
        intent.putExtra(STD_DEV_BW_POINTS,statData);

        statData = visuals.getAverageFromCenter();
        intent.putExtra(AVG_FR_CENTER,statData);
        statData = visuals.getVarianceFromCenter(statData);
        intent.putExtra(VAR_FR_CENTER,statData);
        statData = visuals.getStdDevFromCenter(statData);
        intent.putExtra(STD_DEV_FR_CENTER,statData);
        intent.putExtra(TRIAL_NUM,trialNumber); // trial number

        isDone = true; // set to true so returning from results we can send the info to Front end

        startActivityForResult(intent,Info.ACTIVITY_FOR_RESULT); // starting the Result activity
        overridePendingTransition(R.anim.slide_in_right,R.anim.slide_out_left); // animation
        Log.e("DIFFXY",""+visuals.getMeanCenterDifferenceFromStart());
    }

    // compress the bitmap to 100, so it can be sent via intent
    private byte[] compressToByteArray(Bitmap b){
        Log.d(MD,"compressToByteArray");
        ByteArrayOutputStream s = new ByteArrayOutputStream();
        b.compress(Bitmap.CompressFormat.PNG,100,s);
        return s.toByteArray();
    }


    /***********************************************************************************************
     *                              Speech Recognition
     **********************************************************************************************/
    // sets up the intent that defines the type of Speech Recognition
    private Intent getSpeechRecognitionIntent(){
        Log.d(MD,"getSpeechRecognitionIntent");
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,
                Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,
                this.getPackageName());
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,10000);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,5);

        return intent;
    }

    // Cross references speech recognized with key phrases
    // returns 1 if the key phrase is for going back
    // return 0 if it is to start the test
    private int interpretSpeech(ArrayList<String> speechList){
        Log.d(MD,"interpretSpeech");
        for(String s:speechList){
            if(RETURN_KEY_PHRASE.contains(s)) return 1;
            if(CONTINUE_KEY_PHRASE.contains(s)) return 0;
        }
        return -1;
    }

    // to restart listening
    private void restartSpeech(){
        Log.d(MD,"restartSpeech");
        speechRecognizer.stopListening();
        speechRecognizer.startListening(speechRecogIntent);
    }

    protected class SpeechRecognitionListener implements RecognitionListener {

        @Override
        public void onBeginningOfSpeech() {
            Log.d(MD+LN,"SpeechRecognitionListener - onBeginningOfSpeech");
        }

        @Override
        public void onBufferReceived(byte[] buffer) {}

        @Override
        public void onEndOfSpeech() {
            Log.d(MD+LN,"SpeechRecognitionListener - onEndOfSpeech");
        }

        @Override
        public void onError(int error) {
            Log.d(MD+LN,"SpeechRecognitionListener - onError   "+ error);
            speechRecognizer.startListening(speechRecogIntent);
        }

        @Override
        public void onEvent(int eventType, Bundle params) {}

        @Override
        public void onPartialResults(Bundle partialResults) {}

        @Override
        public void onReadyForSpeech(Bundle params) {}

        // analysis of the speech result
        @Override
        public void onResults(Bundle results) {
            Log.d(MD+LN,"SpeechRecognitionListener - onResult");
            ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            // matches are the return values of speech recognition engine
            // Use these values for whatever you wish to do

            int result = interpretSpeech(matches);
            Log.e("SPEECH", Arrays.toString(matches.toArray()) + "\n\t\t\tRESULT: "+result);
            if(result == 0){
                speechRecognizer.destroy();
                tts.speak(getString(R.string.countdown),TextToSpeech.QUEUE_ADD, ttsParams, INTERMEDIATE_TTS_SPEECH_ID);
            }
            else restartSpeech();
        }

        @Override
        public void onRmsChanged(float rmsdB) {}
    }



    /***********************************************************************************************
     *                              TOUCH EVENTS USED FOR TWO FINGER TOUCH
     **********************************************************************************************/

    // Listener for Two Finger Tap
    SimpleTwoFingerDoubleTapDetector multiTouchListener = new SimpleTwoFingerDoubleTapDetector() {
        @Override
        public void onTwoFingerDoubleTap() {
            // Do what you want here, I used a Toast for demonstration
            if(speechRecognizer != null) speechRecognizer.destroy();
            tts.stop();
            tts.speak(getString(R.string.countdown),TextToSpeech.QUEUE_ADD, ttsParams, INTERMEDIATE_TTS_SPEECH_ID);
            Log.e("TOUCH","INTERMEDIATE_TTS_SPEECH_ID");

        }
    };
    // On touch event, is two finger are double tapped SimpleTwoFingerDoubleTapDetector Will activate
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(!isDoubleTapped) {
            if (multiTouchListener.onTouchEvent(event)) {
                isDoubleTapped = true;
                return true;
            }
        }
        return super.onTouchEvent(event);
    }

    // This class Defines Two Finger Touch
    abstract class SimpleTwoFingerDoubleTapDetector {
        private final int TIMEOUT = ViewConfiguration.getDoubleTapTimeout() + 100;
        private long mFirstDownTime = 0;
        private boolean mSeparateTouches = false;
        private byte mTwoFingerTapCount = 0;

        private void reset(long time) {
            mFirstDownTime = time;
            mSeparateTouches = false;
            mTwoFingerTapCount = 0;
        }

        boolean onTouchEvent(MotionEvent event) {
            switch(event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    if(mFirstDownTime == 0 || event.getEventTime() - mFirstDownTime > TIMEOUT)
                        reset(event.getDownTime());
                    break;
                case MotionEvent.ACTION_POINTER_UP:
                    if(event.getPointerCount() == 2)
                        mTwoFingerTapCount++;
                    else
                        mFirstDownTime = 0;
                    break;
                case MotionEvent.ACTION_UP:
                    if(!mSeparateTouches)
                        mSeparateTouches = true;
                    else if(mTwoFingerTapCount == 2 && event.getEventTime() - mFirstDownTime < TIMEOUT) {
                        onTwoFingerDoubleTap();
                        mFirstDownTime = 0;
                        return true;
                    }
            }

            return false;
        }

        public abstract void onTwoFingerDoubleTap();
    }


    /***********************************************************************************************
     *                                      Permissions
     **********************************************************************************************/

    // gets Audio and any other future Permissions
    private void getPermission(){
        Log.d(MD,"getPermission");
        if(ContextCompat.checkSelfPermission(SwayMain.this,
                Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED){
            if (ActivityCompat.shouldShowRequestPermissionRationale(SwayMain.this,
                    Manifest.permission.RECORD_AUDIO)) {

            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(SwayMain.this,
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        1);
            }
        }


    }

    /***********************************************************************************************
     *                          EXTRA LISTENERS SINCE THE DEFAULT HAD BUGS
     *                          NEEDS TESTING ON MORE THAN ONE DEVICES
     **********************************************************************************************/

    // Replacement Recognition Listener, to fix some of the issues with stock.
    // I NEED TO TEST ON GOOGLE PIXEL TO SEE IF THE DEFAULT LISTENER WILL WORK OR NOT

    public class BugRecognitionListener implements RecognitionListener {

        private final String CLS_NAME = BugRecognitionListener.class.getSimpleName();

        private boolean doError;
        private boolean doEndOfSpeech;
        private boolean doBeginningOfSpeech;

        public void resetBugVariables() {
            Log.i(CLS_NAME, "resetBugVariables");

            doError = false;
            doEndOfSpeech = false;
            doBeginningOfSpeech = false;
        }

        /**
         * Called when the endpointer is ready for the user to start speaking.
         *
         * @param params parameters set by the recognition service. Reserved for future use.
         */
        @Override
        public void onReadyForSpeech(final Bundle params) {
            doError = true;
            doEndOfSpeech = true;
            doBeginningOfSpeech = true;
        }

        @Override
        public void onBeginningOfSpeech() {
            Log.i(CLS_NAME, "onBeginningOfSpeech: doEndOfSpeech: " + doEndOfSpeech);
            Log.i(CLS_NAME, "onBeginningOfSpeech: doError: " + doError);
            Log.i(CLS_NAME, "onBeginningOfSpeech: doBeginningOfSpeech: " + doBeginningOfSpeech);

            if (doBeginningOfSpeech) {
                doBeginningOfSpeech = false;
                onBeginningOfRecognition();
            }

        }

        public void onBeginningOfRecognition() {
        }

        @Override
        public void onRmsChanged(final float rmsdB) {
        }

        @Override
        public void onBufferReceived(final byte[] buffer) {
        }

        @Override
        public void onEndOfSpeech() {
            Log.i(CLS_NAME, "onEndOfSpeech: doEndOfSpeech: " + doEndOfSpeech);
            Log.i(CLS_NAME, "onEndOfSpeech: doError: " + doError);
            Log.i(CLS_NAME, "onEndOfSpeech: doBeginningOfSpeech: " + doBeginningOfSpeech);

            if (doEndOfSpeech) {
                onEndOfRecognition();
            }
        }

        public void onEndOfRecognition() {
        }

        @Override
        public void onError(final int error) {
            Log.w(CLS_NAME, "onError: doEndOfSpeech: " + doEndOfSpeech);
            Log.w(CLS_NAME, "onError: doError: " + doError);
            Log.w(CLS_NAME, "onError: doBeginningOfSpeech: " + doBeginningOfSpeech);

            if (doError) {
                onRecognitionError(error);
            }
        }

        public void onRecognitionError(final int error) {
        }

        @Override
        public void onResults(final Bundle results) {
        }

        @Override
        public void onPartialResults(final Bundle partialResults) {
        }

        @Override
        public void onEvent(final int eventType, final Bundle params) {
        }

    }

    private final BugRecognitionListener recognitionListener = new BugRecognitionListener() {

        /**
         * MUST CALL SUPER!
         */
        @Override
        public void onReadyForSpeech(final Bundle params) {
            super.onReadyForSpeech(params);
        }

        /**
         * Instead of {@link RecognitionListener#onEndOfSpeech()}
         */
        @Override
        public void onEndOfRecognition() {
        }

        /**
         * Instead of {@link RecognitionListener#onError(int)}
         *
         * @param error the error code
         */
        @Override
        public void onRecognitionError(final int error) {
            speechRecognizer.startListening(speechRecogIntent);

        }

        /**
         * Instead of {@link RecognitionListener#onBeginningOfSpeech()}
         */
        @Override
        public void onBeginningOfRecognition() {
        }

        @Override
        public void onBufferReceived(final byte[] buffer) {
        }

        @Override
        public void onEvent(final int eventType, final Bundle params) {
        }

        @Override
        public void onPartialResults(final Bundle partialResults) {
        }

        @Override
        public void onResults(final Bundle results) {
            Log.d(MD+LN,"SpeechRecognitionListener - onResult");
            ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            // matches are the return values of speech recognition engine
            // Use these values for whatever you wish to do

            int result = interpretSpeech(matches);
            Log.e("SPEECH", Arrays.toString(matches.toArray()) + "\n\t\t\tRESULT: "+result);
            if(result == 0){
                speechRecognizer.destroy();
                tts.speak(getString(R.string.countdown),TextToSpeech.QUEUE_ADD, ttsParams, INTERMEDIATE_TTS_SPEECH_ID);
            }
            else restartSpeech();
        }

        @Override
        public void onRmsChanged(final float rmsdB) {
        }
    };

}
