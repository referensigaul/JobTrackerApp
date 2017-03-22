package com.example.michael.jobtracker;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void see_jobs_btn(View view){
        Intent intent = new Intent(MainActivity.this, SeeJobsActivity.class);
        startActivity(intent);
    }

    public void add_jobs_btn(View view){
        Intent intent = new Intent(MainActivity.this, AddJobActivity.class);
        startActivity(intent);
    }

    public void see_emp_btn(View view){
        Intent intent = new Intent(MainActivity.this, SeeEmployeesActivity.class);
        startActivity(intent);
    }

    public void add_emp_btn(View view){
        Intent intent = new Intent(MainActivity.this, AddEmployeeActivity.class);
        startActivity(intent);
    }


}
