package cmsc436.umd.edu.sway;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import edu.umd.cmsc436.sheets.Sheets;

/**
 * Created by Shubham Patel
 * Abstraction of Sheet436 library
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

    // Sends the Trial data, and the two bitmaps via Sheets436 Library
    public void sendData(float[] rawData, Bitmap heatmap, Bitmap pathmap,Sheets.TestType testType){
        sheets.writeTrials(testType,Info.USER_ID,rawData);
        String title = Info.getPicturePrefix(false)+testType.toId() +"_" +(new SimpleDateFormat("yyyddMM_HHmmss")).format(Calendar.getInstance().getTime());
        sheets.uploadToDrive(Info.FOLDER_ID,title,heatmap);

        title = Info.getPicturePrefix(true) + testType.toId() +"_"+ (new SimpleDateFormat("yyyddMM_HHmmss")).format(Calendar.getInstance().getTime());
        sheets.uploadToDrive(Info.FOLDER_ID,title,pathmap);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data){
        sheets.onActivityResult(requestCode,resultCode,data);
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        sheets.onRequestPermissionsResult(requestCode,permissions,grantResults);
    }

    @Override
    public int getRequestCode(Sheets.Action action) {
        return Info.getActionCode(action);
    }

    @Override
    public void notifyFinished(Exception e) {
        if(e!=null){
            Log.e("SHEETS-API",e.toString());
        }
    }
}
