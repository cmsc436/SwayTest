package cmsc436.umd.edu.sway;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;


public class DisplayResult extends AppCompatActivity {
    ImageView path;
    ImageView heatmap;
    Button button;
    Bitmap bmp_heat_map;
    Bitmap bmp_path;
    int trial;
    float final_score;
    TextView viewScore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_result);

        /* Get intent from other activity */
        Intent intent = getIntent();

        /* If there is no value for trial, return 0 */
        trial = intent.getIntExtra("TRIAL_COUNT", 0);

        /* Get heat map from intent and set in ImageView */
        byte[] byteArray_heat = intent.getByteArrayExtra("HEAT_MAP");
        bmp_heat_map = BitmapFactory.decodeByteArray(byteArray_heat, 0, byteArray_heat.length);
        heatmap = (ImageView) findViewById(R.id.heatmap_image);
        heatmap.setImageBitmap(bmp_heat_map);

        /* Get Path from intent and set in ImageView*/
        byte[] byteArray_path = intent.getByteArrayExtra("PATH_MAP");
        bmp_path = BitmapFactory.decodeByteArray(byteArray_path, 0, byteArray_path.length);
        path = (ImageView) findViewById(R.id.path_image);
        path.setImageBitmap(bmp_path);

        /* Show fnal score */
        final_score = intent.getFloatExtra("FINAL_SCORE",0.0f);
        viewScore = (TextView) findViewById(R.id.final_score_text);
        viewScore.setText(String.valueOf(final_score));

        button = (Button)findViewById(R.id.back_button);
        /* Sending trial - 1 and click the button to go back to SwayMain activity */
        button.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(trial > 0) {
                    Intent intent2 = new Intent(v.getContext(), SwayMain.class);
                    intent2.putExtra("TRIAL_COUNT", trial - 1);
                    startActivity(intent2);
                }
            }
        });
    }


}
