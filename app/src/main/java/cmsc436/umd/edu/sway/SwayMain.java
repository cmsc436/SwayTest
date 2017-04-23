package cmsc436.umd.edu.sway;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sway_main);

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



    // TODO DEFINE WHAT HAPPENS IN PRETEST
    private CountDownTimer preTest = new CountDownTimer(PRETEST_DURATION,PRETEST_INTERVAL) {
        @Override
        public void onTick(long millisUntilFinished) {

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

        }

        @Override
        public void onFinish() {
            // stops recording the data
            measurementService.stopReading();
            // now you can call measurementService.getDataList()
            // which will return the recorded data
        }
    };


}
