package com.example.user.wifiprob;



import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.app.NavUtils;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;

public class settings extends AppCompatActivity {

    RadioGroup systemChoice;
    RadioButton detSystem;
    RadioButton probSystem;
    RadioGroup systemSettings;
    RadioButton knnOrNoPedo;
    RadioButton wknnOrPedo;
    Button saveSets;
    Spinner enviroList;

    Intent i;


    //SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
    private static final String TAG = "settings";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        final SharedPreferences sharedPref = this.getSharedPreferences("Settings",Context.MODE_PRIVATE);

        i = new Intent(getApplicationContext(), MainActivity.class);
        setTitle("Settings");

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        String[] enLis = new String[2];
        enLis[0] = "University";
        enLis[1] = "Apartment";

        enviroList = (Spinner) findViewById(R.id.spinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, enLis);
        enviroList.setAdapter(adapter);


        detSystem = (RadioButton) findViewById(R.id.detSystem);
        probSystem = (RadioButton) findViewById(R.id.probSystem);

        knnOrNoPedo = (RadioButton) findViewById(R.id.knnOrNoPedo);
        wknnOrPedo = (RadioButton) findViewById(R.id.wknnOrPedo);


        //setCheckboxes(sharedPref);

        probSystem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                detSystem.setChecked(false);
                knnOrNoPedo.setText(R.string.no_pedo_data);
                wknnOrPedo.setText(R.string.pedo_data);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putInt("sys", 1);
                editor.apply();
            }
        });

        detSystem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                probSystem.setChecked(false);
                knnOrNoPedo.setText(R.string.knn);
                wknnOrPedo.setText(R.string.wknn);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putInt("sys", 0);
                editor.apply();
            }
        });

        knnOrNoPedo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putInt("sysSet", 0);
                editor.apply();
            }
        });

        wknnOrPedo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putInt("sysSet", 1);
                editor.apply();
            }
        });

        int sys = sharedPref.getInt("sys",0);

        int sysSet = sharedPref.getInt("sysSet",0);

        if(sys==0){
            detSystem.setChecked(true);
            knnOrNoPedo.setText(R.string.knn);
            wknnOrPedo.setText(R.string.wknn);
        }
        else{
            probSystem.setChecked(true);
            knnOrNoPedo.setText(R.string.no_pedo_data);
            wknnOrPedo.setText(R.string.pedo_data);
        }

        if(sysSet==0){
            knnOrNoPedo.setChecked(true);
        }
        else{
            wknnOrPedo.setChecked(true);
        }




    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                Log.d(TAG,"SAVE SETTINGS, RETURN TO PREVIOUS ACTIVITY");
                i.putExtra("det",detSystem.isChecked());
                i.putExtra("prob",probSystem.isChecked());
                i.putExtra("knn",knnOrNoPedo.isChecked());
                i.putExtra("wknn",wknnOrPedo.isChecked());
                startActivityIfNeeded(i,0);
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }



}
