package cmsc436.umd.edu.sway;

import edu.umd.cmsc436.sheets.Sheets;

/**
 * Static class that stores all of the info
 */

public class Info {

    final static String MAIN_SPREADSHEET_ID = "1YvI3CjS4ZlZQDYi5PaiA7WGGcoCsZfLoSFM0IdvdbDU";
    final static String PRIVATE_SPREADSHEET_ID = "1otFtLXPcvSGhkduMjdBYNDNM7JytDK5ISLi9KgGWUHE";
    final static String FOLDER_ID = "0B21g-Kd0CHS7YUp6c3RFWWdzclE";
    final static String FILE_NAME = "BALANCE_";
    final static String FILE_PICTURE_MOTION ="MOTION_";
    final static String FILE_PICTIURE_HEATMAP = "HEATMAP_";
    final static int ACTIVITY_FOR_RESULT = 1;
    private static Sheets.TestType TEST_TYPE = null;
    static String USER_ID = "null";
    final static String APP_NAME = "SWAY";

    public static int getActionCode(Sheets.Action action){
        switch (action){
            case REQUEST_PERMISSIONS : return 1000;
            case REQUEST_ACCOUNT_NAME: return 1001;
            case REQUEST_PLAY_SERVICES: return 1002;
            case REQUEST_AUTHORIZATION: return 1003;
            case REQUEST_CONNECTION_RESOLUTION: return  1004;
        }
        return 0;
    }

    public static String getPicturePrefix(boolean motion){
        if (motion) return FILE_NAME+FILE_PICTURE_MOTION;
        return FILE_NAME+FILE_PICTIURE_HEATMAP;
    }

    public static Sheets.TestType getTestType(){
        if(TEST_TYPE == null) return Sheets.TestType.SWAY_OPEN_APART;
        return TEST_TYPE;
    }

    public static void setTestType(Sheets.TestType t){
        TEST_TYPE = t;
    }
    public static float getTestTypeToFloat(Sheets.TestType t){
        switch (t){
            case SWAY_OPEN_APART: return 1.0f;
            case SWAY_OPEN_TOGETHER: return 2.0f;
            case SWAY_CLOSED:return 3.0f;
        }
        return -1f;
    }

    public static void setUserId(String id){
        USER_ID = id;
    }



}
