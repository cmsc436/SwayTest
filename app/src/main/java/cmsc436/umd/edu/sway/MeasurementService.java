package cmsc436.umd.edu.sway;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class MeasurementService extends Service {
    /*
        If anyone is reading, IDK if we should also use magnetic fields for anything (one group used it)
        also Gravimeter vs Accelerometer what is the best?
     */

    // handler thread name used for initializing the Handler Thread
    private final String HANDLER_THREAD_NAME = getString(R.string.measurement_service_handler_name);

    //Reading Delay in MICRO-seconds
    private int SENSOR_READING_DELAY = 5000; // take a data every 5 milliseconds

    //initial calibration, used as the center for all other data recording
    private DataPoint initialReading;

    // this DataPoint records the very last reading of the test
    private DataPoint lastReading;

    private DataPoint currentReading;

    // indicator for starting to record data
    private boolean startReading = false;

    // indicator to know if the recording is stopped.
    // while this is false, data cannot be retrieved
    private boolean readingStopped = false;

    // these will be thread safe used to store the reading data
    List<Float> concurrentAccelerationData;
    List<Float> concurrentMagneticData;
    List<DataPoint> concurrentDataList;

    // Sensor Objects
    SensorManager sensorManager;
    SensorEventListener sensorEventListener;
    Sensor magneticSensor; // might not use it
    Sensor accelerationSensor;

    // threads handling
    HandlerThread handlerThread;
    Handler handler;

    //binder object, will be returned when binding to an activity
    private final IBinder localBinder = new LocalBinder();

    public MeasurementService() {} //don't need it, will never be called but have to keep it




    @Override
    public IBinder onBind(Intent intent) {
        currentReading = new DataPoint();

        // initializing sync lists
        concurrentAccelerationData = Collections.synchronizedList(new ArrayList<Float>(3));
        concurrentMagneticData = Collections.synchronizedList(new ArrayList<Float>(3));
        concurrentDataList = Collections.synchronizedList(new ArrayList<DataPoint>());


        // Thread Handler initialization
        handlerThread = new HandlerThread(HANDLER_THREAD_NAME, Thread.MAX_PRIORITY);
        handlerThread.start();

        // initialization of the handler
        handler = new Handler(handlerThread.getLooper());

        // initializing sensor elements
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        magneticSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        accelerationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);

        sensorEventListener= new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                if(startReading) {
                    //updating the acceleration reading
                    if (event.sensor.getType() == Sensor.TYPE_GRAVITY) {
                        currentReading.setX(initialReading.getX() - event.values[0]); // phone's x
                        currentReading.setY(initialReading.getY() - event.values[2]); // phone's z
                        currentReading.setTime(initialReading.getTime() - System.currentTimeMillis());

                        concurrentDataList.add(currentReading.getDeepCopy());

//                        concurrentAccelerationData.add(0, event.values[0]);
//                    concurrentAccelerationData.add(1,event.values[1]);
//                        concurrentAccelerationData.add(2, event.values[2]);
                    }
                    //updating magnetic sensor reading
                    else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                        concurrentMagneticData.add(0, event.values[0]);
//                    concurrentMagneticData.add(1,event.values[1]);
                        concurrentMagneticData.add(2, event.values[2]);
                    }
                }else if(initialReading == null && event.sensor.getType() == Sensor.TYPE_GRAVITY){
                    initialReading = new DataPoint(
                            event.values[0],
                            event.values[2],
                            System.currentTimeMillis());
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {}
        };

        // registering both of the sensors to the sensor manager
       registerSensor();


        return localBinder;
    }

    // getters for both sensor reading
//    public Float[] getMagneticReading(){
//        Float[] toRet = new Float[3];
//        return concurrentMagneticData.toArray(toRet);
//    }

    public DataPoint getCurrentReading(){
        return currentReading.getDeepCopy();
    }

    public DataPoint getInitialReading(){
        return initialReading;
    }

    public DataPoint getLastReading(){
        return lastReading;
    }

    public boolean unhookHandler(){
        return handlerThread.quitSafely();
    }

    // To change the sensor delay,
    public void setSensorReadingDelay(int delay){
        SENSOR_READING_DELAY = delay;
        registerSensor();
    }


    // register sensors
    private void registerSensor(){

        sensorManager.registerListener(
                sensorEventListener,
                accelerationSensor,
                SENSOR_READING_DELAY,
                handler);
        sensorManager.registerListener(
                sensorEventListener,
                magneticSensor,
                SENSOR_READING_DELAY,
                handler);
    }

    // MUST BE CALLED, ELSE NO DATA WILL BE CALLED
    // this starts recording the data
    public DataPoint startReading(){
        startReading = true;
        initialReading.setTime(System.currentTimeMillis());
        return initialReading;
    }

    // MUST BE CALLED, ELSE THE APP WILL KEEP ADDING TO THE DATA LIST
    // stops recording the data
    public void stopReading(){
        startReading = false;
        readingStopped = true;
        lastReading = new DataPoint(
                currentReading.getX(),
                currentReading.getY(),
                currentReading.getTime());
    }

    public List<DataPoint> getDataList(){
        if(readingStopped && !startReading) return concurrentDataList;
        Toast.makeText(this,"READING WAS NOT STOPPED",Toast.LENGTH_LONG).show();
        return null;
    }

    // binding
    public class LocalBinder extends Binder{
        public MeasurementService getService(){
            return MeasurementService.this;
        }
    }

    public class DataPoint {
        private float x;
        private float y;
        private long time;

        public DataPoint (float x, float y, long time){
            this.x = x;
            this.y = y;
            this.time = time;
        }
        public DataPoint(){
            this.x = 0;
            this.y = 0;
            this.time = 0;
        }

        public float getX() {
            return x;
        }

        public void setX(float x) {
            this.x = x;
        }

        public float getY() {
            return y;
        }

        public void setY(float y) {
            this.y = y;
        }

        public long getTime() {
            return time;
        }

        public void setTime(long time) {
            this.time = time;
        }

        public DataPoint getDeepCopy(){
            return new DataPoint(x,y,time);
        }
    }
}