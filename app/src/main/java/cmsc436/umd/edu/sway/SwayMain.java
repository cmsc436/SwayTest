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
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
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

public class SwayMain extends AppCompatActivity {

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



    /*************** SERVICE FOR GETTING READING***************/
    MeasurementService measurementService; // current Test Type
    boolean isServiceBound = false; // used to safely disconnect the service



    /*************** BOOK KEEPING VARS ***************/
    TextView textView; // for updating the what the app is doing
    Bitmap bitmapMain; // do i need this?
    boolean isDone;// is the test done

    float finalScore; // what the metric outputs

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
    String[] CONT = {"GO", "Continue", "Start","Begin"};
    String[] RET = {"BACK", "GO BACK", "RETURN"};

    /*************** TEXT TO SPEECH ***************/
    TextToSpeech tts; //Text to Speech Main Object
    Bundle ttsParams; // TTS Bundle used to ID Utterances


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sway_main);

        Log.e("STAGE", "ONCREATE");
        Collections.addAll(RETURN_KEY_PHRASE, RET);
        Collections.addAll(CONTINUE_KEY_PHRASE,CONT);
        isDone = false;

        //////////////////////////////////////////////////////////////////
        //                              TTS                             //
        //////////////////////////////////////////////////////////////////
        tts = new TextToSpeech(this,onInitListener); // Responsible for "Talking:
        ttsParams = new Bundle(); // the Bundle used by TTS to recognize its Engine's Utterance
        ttsParams.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,"1");// setting up KV pair

        // there have been time then the listener is not set up correctly, it is device specific
        int message =  tts.setOnUtteranceProgressListener(utteranceProgressListener);
        if(message == TextToSpeech.ERROR) Toast.makeText(this,"PROBLEM SETTING UTTRANCE ",Toast.LENGTH_LONG).show();

        textView = (TextView) findViewById(R.id.sway_text);
        getPermission();

        //////////////////////////////////////////////////////////////////
        //                       SETTING CLASS VARIABLES                //
        //////////////////////////////////////////////////////////////////
        currentIntent = getIntent(); // gets the intent to be used to access all of the variables

        currentTest = TrialMode.getAppendage(currentIntent); // sets up the current Test Type
        isTrial = (currentTest != null); // if current test is null then this is PRACTICE mode else it is TRIAL mode

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
    }


    // binding the service to the activity
    @Override
    protected void onStart() {
        super.onStart();
//        if(isTrial && currentTrial != TrialMode.getTrialNum(currentIntent)){
//            currentTrial = TrialMode.getTrialNum(currentIntent);
//            isDone = false;
//        }
        Log.i("STAGE", "ON START");
        bindService(
                new Intent(this,MeasurementService.class),
                serviceConnection,
                Context.BIND_AUTO_CREATE
        );


    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i("STAGE", "ON RESUME");
        if(!isDone)initializeTestingProcedure();

    }

    //unbinding the activity to the service
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(isServiceBound) unbindService(serviceConnection);
        tts.shutdown();
        if(speechRecognizer != null) speechRecognizer.destroy();
    }

    @Override
    public void onBackPressed() {
        if(!isDone) {
            Intent i = new Intent();
            i.putExtra(TrialMode.KEY_SCORE,finalScore);
            setResult(RESULT_CANCELED,i);
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        isDone = false;
        Log.e("SWAY_MAIN_AC_RESULT","DATA: "+data.getFloatExtra(TrialMode.KEY_SCORE,-1));
        setResult(resultCode,data);
        finish();
    }

    public void initializeTestingProcedure(){
        Log.e("CLICK ME","IN CLICK ME");
        tts.stop();
        textView.setText("Test Starting");
        speakText(currentTest);
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

    // Handles Intro Speech depending on the Test Type
    private void speakText(Sheets.TestType t){
        if (!isTrial)
            tts.speak(getString(R.string.test_instr_practice), TextToSpeech.QUEUE_FLUSH, ttsParams, "1");
        else if (t == Sheets.TestType.SWAY_OPEN_APART)
            tts.speak(getString(R.string.test_instr_1), TextToSpeech.QUEUE_FLUSH, ttsParams, "1");
        else if (t == Sheets.TestType.SWAY_OPEN_TOGETHER)
            tts.speak(getString(R.string.test_instr_2), TextToSpeech.QUEUE_FLUSH, ttsParams, "1");
        else if (t == Sheets.TestType.SWAY_CLOSED)
            tts.speak(getString(R.string.test_instr_3), TextToSpeech.QUEUE_FLUSH, ttsParams, "1");
        else{
            tts.speak("DOUBLE TAP TO START THE TEST", TextToSpeech.QUEUE_FLUSH, ttsParams, "1");

        }


    }

    private TextToSpeech.OnInitListener onInitListener = new TextToSpeech.OnInitListener() {
        @Override
        public void onInit(int status) {
            if( status == TextToSpeech.SUCCESS){
                Log.e("TTS", "SUCESS");
                int result = tts.setLanguage(Locale.US);
                if(result==TextToSpeech.LANG_MISSING_DATA ||
                        result==TextToSpeech.LANG_NOT_SUPPORTED){
                    Log.e("error", "This Language is not supported");
                }

            }else {
                Log.e("TTS", "TTS FAILED");
            }

        }
    };

    // TODO HANDLE VOICE RECOG WHEN THE INTRO IS GOING
    private UtteranceProgressListener utteranceProgressListener = new UtteranceProgressListener() {
        @Override
        public void onStart(String utteranceId) {
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
            if(utteranceId.equals("2")) {
                SwayMain.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.e("STARTING", "Staring Preest");
                        preTest.start();
                    }
                });
            }
        }

        @Override
        public void onError(String utteranceId) {

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
            textView.setText("PRETEST(DB): "+millisUntilFinished/1000);
        }

        @Override
        public void onFinish() {
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
    };

    // compress the bitmap to 100, so it can be sent via intent
    private byte[] compressToByteArray(Bitmap b){
        ByteArrayOutputStream s = new ByteArrayOutputStream();
        b.compress(Bitmap.CompressFormat.PNG,100,s);
        return s.toByteArray();
    }


    /***********************************************************************************************
     *                              Speech Recognition
     **********************************************************************************************/
    // sets up the intent that defins the type of Speech Recognition
    private Intent getSpeechRecognitionIntent(){
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

    // Cross references speech recognized with key phrases
    // returns 1 if the keu phrase is for going back
    // return 0 if it is to start the test
    private int interpretSpeech(ArrayList<String> speechList){
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
            int result = interpretSpeech(matches);

            if(result == 1) setResult(RESULT_CANCELED);
            if(result == 0) preTest.start();
            else restartSpeech();
        }

        @Override
        public void onRmsChanged(float rmsdB) {
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
            tts.stop();
            tts.speak(getString(R.string.countdown),TextToSpeech.QUEUE_ADD, ttsParams, "2");
            Log.e("TOUCH","2");
        }
    };
    // On touch event, is two finger are double tapped SimpleTwoFingerDoubleTapDetector Will activate
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(multiTouchListener.onTouchEvent(event))
            return true;
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
        if (ContextCompat.checkSelfPermission(SwayMain.this,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(SwayMain.this,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

            } else {
                ActivityCompat.requestPermissions(SwayMain.this,
                        new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        1);
            }

        }
        if(ContextCompat.checkSelfPermission(SwayMain.this,
                Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED){
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(SwayMain.this,
                    Manifest.permission.RECORD_AUDIO)) {

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(SwayMain.this,
                        new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        1);
            }
        }


    }
}
