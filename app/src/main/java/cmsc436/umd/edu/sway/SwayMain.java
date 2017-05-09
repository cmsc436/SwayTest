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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

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

    private final String MD = "METHOD";
    private final String LN = "LISTENER";

    /***************TEST DURATION AND DEFINATION***************/
    // total test time = TEST_DURATION + PRETEST_DURATION
    private final int TEST_DURATION = 10000; // how long the test will last (in milliseconds)
    private final int TEST_INTERVAL = 1000; // interval for update during test

    // wait period between the patient taking position
    // and the test starting to record data
    private final int PRETEST_DURATION = 5000;
    private final int PRETEST_INTERVAL = 1000; //interval for update of pre-test



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
    final static String TEST_TYPE = "TEST_TPE";
    final static String USER_ID = "USER_ID";

    private final static String SHARED_PREF_ID = "com.umd.cmsc436.sway.SHARED_PREF";
    private final static String SHARED_PREf_HEIGHT_ID = "com.umd.cmsc436.sway.SHARED_PREF.height";

    static float HEIGHT;



    /*************** SERVICE FOR GETTING READING***************/
    MeasurementService measurementService; // current Test Type
    boolean isServiceBound = false; // used to safely disconnect the service



    /*************** BOOK KEEPING VARS ***************/
    TextView textView; // for updating the what the app is doing
    Bitmap bitmapMain; // do i need this?
    boolean isDone;// is the test done

    float finalScore; // what the metric outputs
    boolean isDoubleTapped = false;
    boolean instructionsCalled = false;

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
    boolean isListening; // is it currently listening

    // key Phrases
    HashSet<String> RETURN_KEY_PHRASE = new HashSet<>();
    HashSet<String> CONTINUE_KEY_PHRASE = new HashSet<>();
    String[] CONT = {"go", "continue", "start","begin"};
    String[] RET = {"back", "go back", "return"};

    /*************** TEXT TO SPEECH ***************/
    TextToSpeech tts; //Text to Speech Main Object
    Bundle ttsParams; // TTS Bundle used to ID Utterances

    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.action_menu, menu);
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sway_main);
        //CollapsingToolbarLayout collapsingToolbarLayout = (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar);
        //Typeface tf = Typeface.createFromAsset(getAssets(),"fonts/Raleway-Regular.ttf");
        //Typeface tf = Typeface.createFromAsset(getAssets(),"fonts/Raleway-SemiBold.ttf");

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        ((BottomNavigationView) findViewById(R.id.bottom_navigation))
                .setOnNavigationItemSelectedListener(navigationItemSelectedListener);

        //actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white_48px);
        //actionBar.setDisplayShowHomeEnabled(true);
        //getSupportActionBar().setDisplayShowTitleEnabled(false);




        Log.d(MD,"------------------- ON CREATE ");
        Collections.addAll(RETURN_KEY_PHRASE, RET);
        Collections.addAll(CONTINUE_KEY_PHRASE,CONT);
        isDone = false;

        HEIGHT = getSharedPreferences(SHARED_PREF_ID, MODE_PRIVATE)
                .getFloat(SHARED_PREf_HEIGHT_ID,-1);

        if(HEIGHT == -1) promptForHeight();
        Log.d("HEIGHT",HEIGHT+"");

        //////////////////////////////////////////////////////////////////
        //                              TTS                             //
        //////////////////////////////////////////////////////////////////
        tts = new TextToSpeech(this,onInitListener); // Responsible for "Talking:
        tts.setVoice(new Voice(
                "Main",
                Locale.getDefault(),
                Voice.QUALITY_NORMAL,
                Voice.LATENCY_NORMAL,
                false,
                new HashSet<String>()));
        ttsParams = new Bundle(); // the Bundle used by TTS to recognize its Engine's Utterance
        ttsParams.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,"1");// setting up KV pair
        // there have been time then the listener is not set up correctly, it is device specific
        int message =  tts.setOnUtteranceProgressListener(utteranceProgressListener);


        textView = (TextView) findViewById(R.id.sway_main_text);
        getPermission();

        //////////////////////////////////////////////////////////////////
        //                       SETTING CLASS VARIABLES                //
        //////////////////////////////////////////////////////////////////
        currentIntent = getIntent(); // gets the intent to be used to access all of the variables

        currentTest = TrialMode.getAppendage(currentIntent); // sets up the current Test Type
        isTrial = (currentTest != null); // if current test is null then this is PRACTICE mode else it is TRIAL mode

        Toast.makeText(this,"TEST: "+currentTest,Toast.LENGTH_LONG).show();
        if(isTrial){
            trialNumber = TrialMode.getTrialOutOf(currentIntent);
            currentTrial = TrialMode.getTrialNum(currentIntent);
            Info.setTestType(currentTest);
            Info.setUserId(TrialMode.getPatientId(currentIntent));
        }

        //////////////////////////////////////////////////////////////
        //                     SPEECH RECOGNITION (SR)              //
        //////////////////////////////////////////////////////////////
        // will take care of taking in vocal input
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecogIntent = getSpeechRecognitionIntent(); // intent used for SR
        speechRecognizer.setRecognitionListener(new SpeechRecognitionListener()); // Listener to react to when speech
//        speechRecognizer.setRecognitionListener(recognitionListener); // Listener to react to when speech

//        final Intent instructionsIntent = new Intent(this, FragmentPagerSupport.class);
//        instructionsIntent.putExtras(currentIntent);
//        instructionsIntent.setAction(currentIntent.getAction());

        /*AlertDialog.Builder builder = new AlertDialog.Builder(SwayMain.this);
        builder.setNeutralButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });
        builder.setTitle("Instructions");
        builder.setMessage("Alert message to be shown");
        final AlertDialog instrDialog = builder.create();*/

        /*ImageButton instrButton = (ImageButton) findViewById(R.id.instr_button);
        instrButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //instrDialog.show();
                startActivity(instructionsIntent);
                finish();
            }
        });*/

//        ((Button) findViewById(R.id.sway_main_button_tts_instr)).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                speakTextInstruction(currentTest);
//            }
//        });

    }


    // binding the service to the activity
    @Override
    protected void onStart() {
        super.onStart();
        currentIntent = getIntent(); // gets the intent to be used to access all of the variables

        currentTest = TrialMode.getAppendage(currentIntent); // sets up the current Test Type
        isTrial = (currentTest != null); // if current test is null then this is PRACTICE mode else it is TRIAL mode


        if(isTrial){
            Log.e("IS TRIAL", "NOT IN TRIAL");
            trialNumber = TrialMode.getTrialOutOf(currentIntent);
            currentTrial = TrialMode.getTrialNum(currentIntent);
            Info.setTestType(currentTest);
            Info.setUserId(TrialMode.getPatientId(currentIntent));
        }
        Log.d(MD,"-------------------onStart");
        bindService(
                new Intent(this,MeasurementService.class),
                serviceConnection,
                Context.BIND_AUTO_CREATE
        );


    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(MD,"-------------------onResume");
        if(!isDone)initializeTestingProcedure();
        if(instructionsCalled){
            tts.speak(getString(R.string.sway_tts_start_instr),TextToSpeech.QUEUE_ADD,ttsParams,"1");
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        isDoubleTapped = false;
        tts.stop();
        preTest.cancel();
        duringTest.cancel();
    }

    //unbinding the activity to the service
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(MD,"-------------------onDestroy");
        if(isServiceBound) unbindService(serviceConnection);
        tts.shutdown();
        if(speechRecognizer != null) speechRecognizer.destroy();
    }

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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(MD,"-------------------onActivityResult");
        isDone = false;
        Log.e("SWAY_MAIN_AC_RESULT","DATA: "+data.getFloatExtra(TrialMode.KEY_SCORE,-1));
        setResult(resultCode,data);
        finish();
    }



    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    public void initializeTestingProcedure(){
        Log.d(MD,"initializeTestingProcedure");
        tts.stop();

        //textView.setText("Test Starting");
    }

    /***********************************************************************************************
     *                                      MENU
     **********************************************************************************************/

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
                    speakTextInstruction(currentTest);
                    return true;
            }
            return false;
        }
    };

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_help:
                startInstruction();
                return true;
        }
        return false;
    }

    private void startInstruction(){
        Intent instructionsIntent = new Intent(this, FragmentPagerSupport.class);
        if(isTrial)instructionsIntent.putExtras(currentIntent.getExtras());
        instructionsIntent.setAction(currentIntent.getAction());
        startActivity(instructionsIntent);
        instructionsCalled = true;
//        finish();
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
            tts.speak(getString(R.string.test_instr_practice), TextToSpeech.QUEUE_FLUSH, ttsParams, "1");

        else {
            switch (t) {
                case SWAY_OPEN_APART:
                    tts.speak(getString(R.string.test_instr_1), TextToSpeech.QUEUE_FLUSH, ttsParams, "1");
                    break;
                case SWAY_OPEN_TOGETHER:
                    tts.speak(getString(R.string.test_instr_2), TextToSpeech.QUEUE_FLUSH, ttsParams, "1");
                    break;
                case SWAY_CLOSED:
                    tts.speak(getString(R.string.test_instr_3), TextToSpeech.QUEUE_FLUSH, ttsParams, "1");
                    break;
                default:
                    tts.speak("TO START THE TEST. DOUBLE TAP. OR SAY. GO. ", TextToSpeech.QUEUE_FLUSH, ttsParams, "1");
                    break;

            }
        }
//        else if (t == Sheets.TestType.SWAY_OPEN_APART)
//            tts.speak(getString(R.string.test_instr_1), TextToSpeech.QUEUE_FLUSH, ttsParams, "1");
//        else if (t == Sheets.TestType.SWAY_OPEN_TOGETHER)
//            tts.speak(getString(R.string.test_instr_2), TextToSpeech.QUEUE_FLUSH, ttsParams, "1");
//        else if (t == Sheets.TestType.SWAY_CLOSED)
//            tts.speak(getString(R.string.test_instr_3), TextToSpeech.QUEUE_FLUSH, ttsParams, "1");
//        else{
//            tts.speak("DOUBLE TAP TO START THE TEST", TextToSpeech.QUEUE_FLUSH, ttsParams, "1");
//
//        }


    }

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
                    Log.e("TTS", "This Language is not supported");
                    //textView.setText("YOUR LANGUAGE IS NOT SUPPORTED, PLEASE SWITCH TO ENGLISH");
                }else{
                    tts.speak(getString(R.string.sway_tts_start_instr),TextToSpeech.QUEUE_ADD,ttsParams,"1");
                }

            }else {
                //textView.setText("TEXT-TO-SPEECH IS NOT SUPPORTED, DO TWO FINGER DOUBLE TAP TO START");
                Log.e("TTS", "TTS FAILED");
            }

        }
    };

    // TODO HANDLE VOICE RECOG WHEN THE INTRO IS GOING
    private UtteranceProgressListener utteranceProgressListener = new UtteranceProgressListener() {
        @Override
        public void onStart(String utteranceId) {
            Log.d(MD+LN,"UtteranceProgressListener - onStart");
//            SwayMain.this.runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    Log.e("Speech", "STARTED SPEECH RECOG");
//                    speechRecognizer.startListening(speechRecogIntent);
//                }
//            });
        }

        // TODO REMOVE PRESTART WHEN SPEECH IS IMPLEMENTED
        @Override
        public void onDone(String utteranceId) {
            Log.d(MD+LN,"UtteranceProgressListener - onDone");
            if(utteranceId.equals("1")){
                SwayMain.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        speechRecognizer.startListening(speechRecogIntent);
                    }
                });            }
            else if(utteranceId.equals("2")) {
                SwayMain.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.e("STARTING", "Staring Preest");
                        textView.setText(R.string.sway_main_testing);
                        preTest.start();
                        isDoubleTapped = true;
                    }
                });
            }
            else if(utteranceId.equals("3")){
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
            Log.d(MD+LN,"UtteranceProgressListener - onError");

        }


    };

    /***********************************************************************************************
     *                                  COUNT DOWN DEFINITION
     **********************************************************************************************/

    // PRETEST used are a wait time, it is just here so the patient
    // is already in the testing position before the starting to take the measurements
    private CountDownTimer preTest = new CountDownTimer(PRETEST_DURATION,PRETEST_INTERVAL) {
        @Override
        public void onTick(long millisUntilFinished) {
            Log.d(MD+LN,"CountDownTimer - onTick - PRE");
            //XDR

            //textView.setText("PRETEST(DB): "+millisUntilFinished/1000);
        }

        @Override
        public void onFinish() {
            Log.d(MD+LN,"CountDownTimer - onFinish - PRE");
            duringTest.start();
            measurementService.restartReading();
            measurementService.startReading();
        }
    };

    //  This is where the testing will take in to place, here the patient's reading will be recorded
    //  and analysed. --ON FINISH- IS WHERE THE CORE LOGIC OF THE TEST WILL TAKE PLACE
    private CountDownTimer duringTest = new CountDownTimer(TEST_DURATION,TEST_INTERVAL) {

        @Override
        public void onTick(long millisUntilFinished) {
            Log.d(MD+LN,"CountDownTimer - onTick - DURING");
            //textView.setText("TESTING(DB): "+millisUntilFinished/1000);
        }

        @Override
        public void onFinish() {
            tts.speak(getString(R.string.tts_testing_done),TextToSpeech.QUEUE_ADD, ttsParams, "3");
            Log.d(MD+LN,"CountDownTimer - onFinish - DURING");

        }
    };

    private void testDone(){
        textView.setText(R.string.sway_main_test_done);
        // stops recording the data
        measurementService.stopReading();
        // now you can call measurementService.getDataList()
        // which will return the recorded data
        List<MeasurementService.DataPoint> l = measurementService.getDataList();
        Log.e("DataCollection", ""+l.size());


        // Added the replacement using DisplayImages :: BEGIN
        DisplayImages visuals = new DisplayImages(l, measurementService.getInitialReading());
        finalScore = visuals.getMetric();
        Intent intent =  new Intent(SwayMain.this,DisplayResult.class); // intent for result class
        intent.putExtra(HEATMAP, compressToByteArray(visuals.getQuadrantAnalysis())); //heat map
        intent.putExtra(PATHMAP, compressToByteArray(visuals.getPath())); // path map
        intent.putExtra(FINAL_SCORE, finalScore); // metric

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
        intent.putExtra(TRIAL_NUM,trialNumber); // trial number TODO I DON'T THINK WE NEED THIS
        isDone = true; // set to true so returning from results we can send the info to Front end
//            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivityForResult(intent,Info.ACTIVITY_FOR_RESULT); // starting the Result activity

        Log.e("DIFFXY",""+visuals.getMeanCenterDifferenceFromStart());

        // END
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
    // sets up the intent that defins the type of Speech Recognition
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
    // returns 1 if the keu phrase is for going back
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
        public void onBufferReceived(byte[] buffer) {
//            Log.d(MD+LN,"SpeechRecognitionListener - onBufferReceived");
        }

        @Override
        public void onEndOfSpeech() {
            Log.d(MD+LN,"SpeechRecognitionListener - onEndOfSpeech");
        }

        @Override
        public void onError(int error) {
            Log.d(MD+LN,"SpeechRecognitionListener - onError   "+ error);
            speechRecognizer.startListening(speechRecogIntent);

            //Log.d(TAG, "error = " + error);
        }

        @Override
        public void onEvent(int eventType, Bundle params) {
//            Log.d(MD+LN,"SpeechRecognitionListener - onEvent");
        }

        @Override
        public void onPartialResults(Bundle partialResults) {
//            Log.d(MD+LN,"SpeechRecognitionListener - onPartialResults");
        }

        @Override
        public void onReadyForSpeech(Bundle params) {
//            Log.d(MD+LN,"SpeechRecognitionListener - onReadyForSpeech");
        }

        @Override
        public void onResults(Bundle results) {
            Log.d(MD+LN,"SpeechRecognitionListener - onResult");
            ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            // matches are the return values of speech recognition engine
            // Use these values for whatever you wish to do

            int result = interpretSpeech(matches);
            Log.e("SPEECH", Arrays.toString(matches.toArray()) + "\n\t\t\tRESULT: "+result);
            //TODO ADD GOING BACK TO VR
//            if(result == 1) setResult(RESULT_CANCELED);
            if(result == 0){
                speechRecognizer.destroy();
                tts.speak(getString(R.string.countdown),TextToSpeech.QUEUE_ADD, ttsParams, "2");
            }
            else restartSpeech();
        }

        @Override
        public void onRmsChanged(float rmsdB) {
//            Log.d(MD+LN,"SpeechRecognitionListener - onRmsChanged");
        }
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
            tts.speak(getString(R.string.countdown),TextToSpeech.QUEUE_ADD, ttsParams, "2");
            Log.e("TOUCH","2");

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

    private void getPermission(){
        Log.d(MD,"getPermission");
        if(ContextCompat.checkSelfPermission(SwayMain.this,
                Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED){
            // Should we show an explanation?
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
            //TODO ADD GOING BACK TO VR
//            if(result == 1) setResult(RESULT_CANCELED);
            if(result == 0){
                speechRecognizer.destroy();
                tts.speak(getString(R.string.countdown),TextToSpeech.QUEUE_ADD, ttsParams, "2");
            }
            else restartSpeech();
        }

        @Override
        public void onRmsChanged(final float rmsdB) {
        }
    };


    private void promptForHeight(){
        View promptView  = LayoutInflater.from(this).inflate(R.layout.dialog_height_input,null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setView(promptView);
        final EditText editText = (EditText) promptView.findViewById(R.id.dialog_height_edit_text);


        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("Set Height", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SharedPreferences sharedPreferences =
                                SwayMain.this.getSharedPreferences(SHARED_PREF_ID,0);
                        sharedPreferences.edit()
                                .putFloat(
                                        SHARED_PREf_HEIGHT_ID,
                                        Float.valueOf(editText.getText().toString()))
                                .apply();
                        HEIGHT = Float.valueOf(editText.getText().toString());
                        Log.d("HEIGHT",HEIGHT+"");
                    }
                });

        alertDialogBuilder.create().show();



    }
}
