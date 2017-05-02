package cmsc436.umd.edu.sway;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import edu.umd.cmsc436.frontendhelper.TrialMode;


public class DisplayResult extends AppCompatActivity {
//    ImageView path;
//    ImageView heatmap;
    ImageView imageView;
    Button button;
    Bitmap bmp_heat_map;
    Bitmap bmp_path;
    int trial;
    float final_score;
    float[] rawData;
    TextView viewScore;
    RadioGroup radioGroup;

    // Object responsible for sending info to the sheets
    SheetManager sheetManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        sheetManager = new SheetManager(this); // takes care of sending the data to oGoogle Sheets
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_result);

        /* Get intent from other activity */
        Intent intent = getIntent();

        /* If there is no value for trial, return 0 */
        trial = intent.getIntExtra("TRIAL_COUNT", 0);

        /* Get heat map from intent and set in ImageView */
        byte[] byteArray_heat = intent.getByteArrayExtra(SwayMain.HEATMAP);
        if(byteArray_heat == null || byteArray_heat.length == 0) Log.e("BITMAP", "HEAT BYTEARR NULL/0");
        bmp_heat_map = BitmapFactory.decodeByteArray(byteArray_heat, 0, byteArray_heat.length);
        if(bmp_heat_map == null) Log.e("BITMAP", "BMP HEAT NULL");

        /* Get Path from intent and set in ImageView*/
        byte[] byteArray_path = intent.getByteArrayExtra(SwayMain.PATHMAP);
        bmp_path = BitmapFactory.decodeByteArray(byteArray_path, 0, byteArray_path.length);

        imageView = (ImageView) findViewById(R.id.sway_result_image_view);
        /* Show fnal score */
        final_score = intent.getFloatExtra(SwayMain.FINAL_SCORE,0.0f);
        viewScore = (TextView) findViewById(R.id.final_score_text);
        viewScore.setText("Score: "+String.valueOf(final_score));

        rawData = new float[]{
                Info.getTestTypeToFloat(Info.getTestType()),
                final_score,
                intent.getFloatExtra(SwayMain.AVG_BW_POINTS,-1),
                intent.getFloatExtra(SwayMain.VAR_BW_POINTS,-1),
                intent.getFloatExtra(SwayMain.STD_DEV_BW_POINTS,-1),
                intent.getFloatExtra(SwayMain.AVG_FR_CENTER,-1),
                intent.getFloatExtra(SwayMain.VAR_FR_CENTER,-1),
                intent.getFloatExtra(SwayMain.STD_DEV_FR_CENTER,-1)
        };

        radioGroup = (RadioGroup) findViewById(R.id.sway_display_result_radio_group);
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
                if(checkedId == R.id.sway_result_heat_map) showHeatMap();
                else if(checkedId == R.id.sway_result_path_map) showPathMap();
                else if(checkedId == R.id.sway_result_final_score) showScore();
            }
        });

        button = (Button)findViewById(R.id.back_button);
        /* Sending trial - 1 and click the button to go back to SwayMain activity */
        button.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent();
                i.putExtra(TrialMode.KEY_SCORE,final_score);
                setResult(RESULT_OK,i);
                finish();
            }
        });

    }

    @Override
    protected void onStart() {
        sheetManager.sendData(
                rawData,
                bmp_heat_map,
                bmp_path,
                Info.getTestType());
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ((RadioButton)findViewById(R.id.sway_result_heat_map)).setChecked(true);

    }

    private void showScore(){
        imageView.setVisibility(View.INVISIBLE);
        viewScore.setVisibility(View.VISIBLE);
    }

    private void showHeatMap(){
        imageView.setImageBitmap(bmp_heat_map);
        viewScore.setVisibility(View.INVISIBLE);
        imageView.setVisibility(View.VISIBLE);
    }

    private void showPathMap(){
        imageView.setImageBitmap(bmp_path);
        viewScore.setVisibility(View.INVISIBLE);
        imageView.setVisibility(View.VISIBLE);
    }

    // part of the Sheet API, it piggybacks on the
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        sheetManager.onRequestPermissionsResult(requestCode,permissions,grantResults);
//        super.onRequestPermissionsResult(requestCode,permissions,grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        sheetManager.onActivityResult(requestCode,resultCode,data);
    }


}
