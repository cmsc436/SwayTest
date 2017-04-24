package cmsc436.umd.edu.sway;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.NonNull;

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



    @Override
    public int getRequestCode(Sheets.Action action) {
        return Info.getActionCode(action);
    }

    @Override
    public void notifyFinished(Exception e) {

    }
}
