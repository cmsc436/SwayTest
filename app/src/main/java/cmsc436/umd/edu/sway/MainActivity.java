package cmsc436.umd.edu.sway;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button my_btn = (Button) findViewById(R.id.start_btn);
//        my_btn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Intent my_intent = new Intent(MainActivity.this,Sub1.class);
//                startActivity(my_intent);
//
//            }
//        });
    }


    public void start_sway(View view) {
        Intent start_intent = new Intent(this, Sub1.class);
        startActivity(start_intent);
    }
}