package com.example.michael.jobtracker;


import android.content.Intent;
import android.content.res.Resources;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cz.destil.settleup.gui.MultiSpinner;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class AddEmployeeActivity extends AppCompatActivity {

    OkHttpClient client = new OkHttpClient();
    List<String> jobList = new ArrayList<>();
    String[] jobLinkList;

    private MultiSpinner.MultiSpinnerListener mMultiSpinnerListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_employee);

        //Get data for skills list
        final EditText hrWageField = (EditText) findViewById(R.id.hrwage_editText);

        //listener removes default value when user click into wage field, if left blank
        //it will reset field to default of 0.00, helps avoid errors with null float value
        hrWageField.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if(b){
                    hrWageField.setText("");
                }else{
                    String tmpWage = hrWageField.getText().toString();
                    if(tmpWage == null || tmpWage.isEmpty() ){
                        hrWageField.setText("0.00");
                    }
                }
            }
        });//end focus listener

        /////Request jobs from db to populate multispinner
        HttpUrl getUrl = HttpUrl.parse("https://jobtracker-161217.appspot.com/jobs");
        Request getRequest = new Request.Builder()
                .url(getUrl)
                .build();
        client.newCall(getRequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            //when response received create list from results and set for multispinner list
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String jobs = response.body().string();
                JSONObject tmpOb;
                int numJobs=0;

                //get number of jobs
                try{
                    JSONArray jsonJobs = new JSONArray(jobs);
                    numJobs = jsonJobs.length();
                }catch(Exception e){
                    e.printStackTrace();
                }

                String[] custArray = new String[numJobs];
                jobLinkList = new String[numJobs];

                //create array with customer name for multispinner list
                //create list with employee links
                try {
                    JSONArray jsonJobs = new JSONArray(jobs);
                    for (int i = 0; i < jsonJobs.length(); i++) {
                        tmpOb = (JSONObject) jsonJobs.get(i);
                        custArray[i] = tmpOb.get("customer").toString();
                        jobLinkList[i] = tmpOb.get("self").toString();
                    }
                }catch(Exception e){
                    e.printStackTrace();
                }

                //call setItems to set list for multispinner, calling in UI main thread
                jobList = Arrays.asList(custArray);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        MultiSpinner multiSpinner_jobs = (MultiSpinner) findViewById(R.id.multi_spinner2);
                        multiSpinner_jobs.setItems(jobList, getString(R.string.jobsListHeader), mMultiSpinnerListener);
                    }
                });


            }
        });
        //set skills multispinner with data from array file
        MultiSpinner multiSpinner_skills;
        Resources res = getResources();
        String[] tmpList = res.getStringArray(R.array.skillsList);
        List<String> skillList = Arrays.asList(tmpList);
        multiSpinner_skills = (MultiSpinner) findViewById(R.id.multi_spinner);
        multiSpinner_skills.setItems(skillList, getString(R.string.skillHeader), mMultiSpinnerListener);
    }

    public void AddEmployee(View view){
        final MultiSpinner skillsField = (MultiSpinner) findViewById(R.id.multi_spinner);
        final MultiSpinner jobsField = (MultiSpinner) findViewById(R.id.multi_spinner2);

        //get first name from form
        final EditText fnameField = (EditText) findViewById(R.id.fname_editText);
        String fname = fnameField.getText().toString();
        //get last name
        final EditText lnameField = (EditText) findViewById(R.id.lname_editText);
        String lname = lnameField.getText().toString();
        //get hourly wage field
        final EditText hrWageField = (EditText) findViewById(R.id.hrwage_editText);

        //get checked values from mutlispinners
        boolean[] skillsSelected = skillsField.getSelected().clone();
        boolean[] jobsSelected = jobsField.getSelected().clone();

        //Buffers and strings for creating JSON body for request
        StringBuffer bodyBuffer = new StringBuffer();
        StringBuffer skillsBuffer = new StringBuffer();
        StringBuffer jobsBuffer = new StringBuffer();
        String skillsText;
        String jobsText;
        String bodyText;

        //Getting skills array from file
        Resources res2 = getResources();
        String[] tmpSkillList = res2.getStringArray(R.array.skillsList);

        //skills section
        for(int i = 0; i < skillsSelected.length; i++){
            if(skillsSelected[i] == true){
                skillsBuffer.append("\"");
                skillsBuffer.append(tmpSkillList[i]);
                skillsBuffer.append("\",");
            }
        }
        if(skillsBuffer.length() > 0) {
            skillsBuffer.setLength(skillsBuffer.length() - 1);
        }
        skillsText = skillsBuffer.toString();

        //jobs section
        for(int i = 0; i < jobsSelected.length; i++){
            if(jobsSelected[i] == true){
                jobsBuffer.append("\"");
                jobsBuffer.append(jobLinkList[i]);
                jobsBuffer.append("\",");
            }
        }
        if(jobsBuffer.length() > 0){
            jobsBuffer.setLength(jobsBuffer.length() - 1);
        }
        jobsText = jobsBuffer.toString();

        //Create body and send request
        boolean goodRequest = false;
        if(!fname.isEmpty() && !lname.isEmpty()){
            bodyBuffer.append("{\"first_name\":\"");
            bodyBuffer.append(fname);
            bodyBuffer.append("\",");

            bodyBuffer.append("\"last_name\": \"");
            bodyBuffer.append(lname);
            bodyBuffer.append("\"");
            goodRequest = true;
        }

        //Only if first and last name were included do we attempt to build and send the request
        if(goodRequest){
            if(!hrWageField.getText().toString().isEmpty()){
                bodyBuffer.append(", \"hourly_wage\": \"");
                bodyBuffer.append(hrWageField.getText().toString());
                bodyBuffer.append("\"");
            }
            if(!skillsText.isEmpty()){
                bodyBuffer.append(", \"skills\": [");
                bodyBuffer.append(skillsText);
                bodyBuffer.append("]");
            }
            if(!jobsText.isEmpty()){
                bodyBuffer.append(", \"jobs_assigned\": [");
                bodyBuffer.append(jobsText);
                bodyBuffer.append("]");
            }

            bodyBuffer.append("}");
            bodyText = bodyBuffer.toString();

            //Set up actual client and request
            MediaType JSON = MediaType.parse("application/json; charset=utf-8");
            OkHttpClient client = new OkHttpClient();
            HttpUrl reqUrl = HttpUrl.parse("https://jobtracker-161217.appspot.com/employees");
            RequestBody body = RequestBody.create(JSON, bodyText);
            Request request = new Request.Builder()
                    .url(reqUrl)
                    .post(body)
                    .build();

            //Send Request and set up response handler
            try {
                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        //If request successful pop up message and return to main page
                        if(response.code() == 201){
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast addEmpSuccess = Toast.makeText(AddEmployeeActivity.this, R.string.add_emp_success, Toast.LENGTH_LONG );
                                    addEmpSuccess.setGravity(Gravity.BOTTOM, 0, 300);
                                    addEmpSuccess.show();
                                }
                            });
                            //Return to main activity
                            Intent mIntent = new Intent(AddEmployeeActivity.this, MainActivity.class);
                            startActivity(mIntent);
                        }
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }else{
            //If request unsuccessful pop up failed message and return to main page
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast addEmpFailed = Toast.makeText(AddEmployeeActivity.this, R.string.add_emp_failed, Toast.LENGTH_LONG );
                    addEmpFailed.setGravity(Gravity.CENTER, 0, 0);
                    addEmpFailed.show();
                }
            });
            Intent mIntent = new Intent(AddEmployeeActivity.this, MainActivity.class);
            startActivity(mIntent);
        }//end goodRequest
    }
}
