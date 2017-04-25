package cmsc436.umd.edu.sway;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import edu.umd.cmsc436.sheets.Sheets;

/**
 *
 */

public class SheetManager implements Sheets.Host {
    private Activity activity;
    private Sheets sheets;

    public SheetManager(Activity activity){
        this.activity = activity;
        sheets = new Sheets(
                this,
                activity,
                Info.APP_NAME,
                Info.MAIN_SPREADSHEET_ID,
                Info.PRIVATE_SPREADSHEET_ID);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data){
        sheets.onActivityResult(requestCode,resultCode,data);
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        sheets.onRequestPermissionsResult(requestCode,permissions,grantResults);
    }


    public void sendData(float distance, float[] rawData, Bitmap bitmap, Sheets.TestType testType){
        sheets.writeData(testType,Info.USER_ID,distance);
        sheets.writeTrials(testType,Info.USER_ID,rawData);

        String title = Info.FILE_ENDING+(new SimpleDateFormat("yyyddMM_HHmmss")).format(Calendar.getInstance().getTime());

        sheets.uploadToDrive(Info.FOLDER_ID,title,bitmap);
    }

    
    @Override
    public int getRequestCode(Sheets.Action action) {
        return Info.getActionCode(action);
    }

    @Override
    public void notifyFinished(Exception e) {

    }
}
