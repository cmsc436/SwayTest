package cmsc436.umd.edu.sway;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.provider.MediaStore;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import edu.umd.cmsc436.sheets.Sheets;

public class SwayMain extends AppCompatActivity {

    /**
     *          -----------------------------------------------
     *          ----------------- PLEASE READ -----------------
     *          -----------------------------------------------
     *
     *                    **PULLING SENSOR READINGS**
     *
     * This is how you retrieve/pull data from sensors
     * MUST BE DONE AFTER onStart() SO THE SERVICE CAN BIND TO THE ACTIVITY:
     *
     *
     *  -> When measurementService.startReading() is called, the service will
     *      start to record the measurements
     *
     *  -> measurementService.stopReading() will stop recording data, ONLY AFTER THIS
     *      can you get a full list of the measurements
     *
     *  -> measurementService.getDataList() will give you a list of
     *      x, y coordinates as well as time
     *
     *  -> MeasurementService class has getters for getting current readings in case
     *      you want to utilize them in real time while the test is running
     *
     *  -> If you want to change the interval at which the sensor is being read
     *      use measurementService.setSensorDelay(int), this will re-register
     *      the sensors with changed time. (DEFAULT IS 5 ms)
     *
     *
     *
     *                    **SENSOR DATA REPRESENTATION: DataPoint**
     *
     *  The Sensor Data is represented as a DataPoint object, this is a simple object
     *  that contains gravity in x and y(phone's z) and time adjusted by
     *  the initial resting position. This is just to make the the code a bit simplified,
     *  since sensor readings are represented as floats and time as,
     *  having one object to work with may make things easier
     *
     *
     *
     *                    **NOTES ABOUT WHAT I ADDED**
     *
     *  -> ServiceConnection is there to establish and terminate
     *      the binding with Measurement Service
     *
     *  -> CountDownTimers, this is just to show how to the Service mechanisms work
     *      -> The *_DURATION and *_INTERVAL are there because the Doc mentioned that
     *          the would like to modify the durations of the test remotely
     *          we will have to work with the Front Interface team on how exactly it will be done
     *      -> The PRE-test is there because they said it may be ok let the patient wait in the
     *          test patient for a bit before starting to record the reading
     *
     *
     *
     *                                  TODO
     *              -> Get the metric and heat map working
     *                  (maybe display results for now, until we
     *                  hook up our app to the main Interface)
     *
     *              -> Figure out how the test will start and end with
     *                  voice or buttons, what ever works for Monday's demo.
     *                  We don't need to worry about trials and test types,
     *                  the Front Interface team will take care of that
     *              -> Have a flow for that a patient can test through,
     *                  maybe a simple UI to work with?
     *
     *              -> Send Data to Google Sheets (I can take care of that Sunday night)
     *
     *              -> Get the app the a Demo quality
     */

    // total test time = TEST_DURATION + PRETEST_DURATION
    private final int TEST_DURATION = 10000; // how long the test will last (in milliseconds)
    private final int TEST_INTERVAL = 1000; // interval for update during test

    // wait period between the patient taking position
    // and the test starting to record data
    private final int PRETEST_DURATION = 5000;
    private final int PRETEST_INTERVAL = 1000; //interval for update of pre-test

    // service to access all of the data
    MeasurementService measurementService;
    boolean isServiceBound = false;

    // Object responsible for sending info to the sheets
    SheetManager sheetManager;

    //Text to Speech
    TextToSpeech tts;

    TextView textView;
    ImageView imageView;
    Bitmap bitmapMain;

    //final score
    float finalScore;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sway_main);

        sheetManager = new SheetManager(this);
        tts = new TextToSpeech(this,onInitListener);

        textView = (TextView) findViewById(R.id.sway_text);
        imageView = (ImageView) findViewById(R.id.image_view);
        getPermission();

//        tts.setOnUtteranceProgressListener(utteranceProgressListener);
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
    }

    public void clickMe(View v){
        tts.stop();
        (findViewById(R.id.sway_start)).setOnClickListener(restartActivity);
        ((Button)findViewById(R.id.sway_start)).setText("Restart");
        textView.setText("Test Starting");
        speakText();
    }

    public void showPic(View v){
        textView.setVisibility(View.INVISIBLE);
        imageView.setVisibility(View.VISIBLE);

    }

    public void showScore(View v){
        imageView.setVisibility(View.INVISIBLE);
        textView.setText("Final Score: "+ finalScore);
        textView.setVisibility(View.VISIBLE);
    }


    private void speakText(){
        tts.speak(getString(R.string.test_instr_debug),TextToSpeech.QUEUE_ADD,null,null);
        while(tts.isSpeaking()){

        }
        preTest.start();
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

//    private UtteranceProgressListener utteranceProgressListener = new UtteranceProgressListener() {
//        @Override
//        public void onStart(String utteranceId) {
//
//        }
//
//        @Override
//        public void onDone(String utteranceId) {
//            Log.e("XXX", "onDone Called");
//            preTest.start();
//        }
//
//        @Override
//        public void onError(String utteranceId) {
//
//        }
//    };

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
            bitmapMain = getDrawing(l);
//            String title = (new SimpleDateFormat("yyyddMM_HHmmss")).format(Calendar.getInstance().getTime());

//            MediaStore.Images.Media.insertImage(getContentResolver(), bitmapMain, title , "");
            imageView.setImageBitmap(bitmapMain);

            finalScore = getMetric(l);

//            sheetManager.sendData(finalScore,
//                    new float[]{},
//                    bitmapMain,
//                    Sheets.TestType.HEAD_SWAY);
        }
    };

    private Bitmap getDrawing(List<MeasurementService.DataPoint> l){
        final int BITMAP_SIZE = 900;
        final float ACCELERATION_LIMIT = 4.5f;
        final float CONSTANT = (BITMAP_SIZE/2) / ACCELERATION_LIMIT;

        Path path = new Path();
        Paint paint = new Paint();


        paint.setAntiAlias(true);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);

        Bitmap bitmap = Bitmap.createBitmap(
                BITMAP_SIZE,
                BITMAP_SIZE,
                Bitmap.Config.ARGB_8888);


        Canvas canvas = new Canvas(bitmap);

        path.moveTo(BITMAP_SIZE/2,BITMAP_SIZE/2);
        canvas.drawColor(Color.LTGRAY);

        paint.setStrokeWidth(15);
        paint.setColor(Color.GREEN);
        canvas.drawCircle(BITMAP_SIZE/2,BITMAP_SIZE/2,BITMAP_SIZE *.125f,paint);
        paint.setColor(Color.YELLOW);
        canvas.drawCircle(BITMAP_SIZE/2,BITMAP_SIZE/2,BITMAP_SIZE *.25f,paint);
        paint.setColor(Color.RED);
        canvas.drawCircle(BITMAP_SIZE/2,BITMAP_SIZE/2,BITMAP_SIZE *.375f,paint);
        paint.setColor(Color.DKGRAY);
        canvas.drawCircle(BITMAP_SIZE/2,BITMAP_SIZE/2,BITMAP_SIZE *.5f,paint);


        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(5);
        for(MeasurementService.DataPoint p: l){
            path.lineTo((p.getX() * CONSTANT)+ BITMAP_SIZE/2,(p.getY() * CONSTANT)+ BITMAP_SIZE/2);
        }

        canvas.drawPath(path,paint);


        return bitmap;
    }

    private float getMetric(List<MeasurementService.DataPoint> l){
        float distance = 0.0f;
        MeasurementService.DataPoint prv = measurementService.getInitialReading();

        for(MeasurementService.DataPoint d : l){
            distance += Math.sqrt(
              Math.pow(prv.getX() - d.getX(),2) +
              Math.pow(prv.getY() - d.getY(),2)
            );
            prv = d;
        }

        return distance;
    }

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
    }


    // part of the Sheet API, it piggybacks on the
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        sheetManager.onRequestPermissionsResult(requestCode,permissions,grantResults);
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);
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

}
