package com.example.michael.jobtracker;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import cz.destil.settleup.gui.MultiSpinner;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static android.R.id.edit;
import static android.icu.lang.UCharacter.GraphemeClusterBreak.T;
import static android.icu.lang.UCharacter.GraphemeClusterBreak.V;
import static android.os.Build.VERSION_CODES.M;
import static okhttp3.Protocol.get;

public class EditEmployee extends AppCompatActivity {
    String FILENAME = "numJobs.txt";
    String employeeLink;
    String jobs;
    String checkedJobs;
    OkHttpClient client;
    List<String> jobList = new ArrayList<>();
    TextView testBox;
    String jobLinks;
    String[] custArray;
    List<String> jobId = new ArrayList<>();
    int numJobs =0;

    private MultiSpinner.MultiSpinnerListener mMultiSpinnerListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_employee);


        Bundle extras = getIntent().getExtras();
        if(extras != null){
            employeeLink = extras.getString("employee_link");
        }

        client = new OkHttpClient();

        //Make request to get all jobs
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
                jobs = response.body().string();
                JSONObject tmpOb;
                try{
                    JSONArray jsonJobs = new JSONArray(jobs);
                    numJobs = jsonJobs.length();
                    FileOutputStream mFos = openFileOutput(FILENAME, MODE_PRIVATE);
                    mFos.write(numJobs);
                    mFos.close();
                }catch(Exception e){
                    e.printStackTrace();
                }

                custArray = new String[numJobs];

                try {
                    JSONArray jsonJobs = new JSONArray(jobs);
                    for (int i = 0; i < jsonJobs.length(); i++) {
                        tmpOb = (JSONObject) jsonJobs.get(i);
                        custArray[i] = tmpOb.get("customer").toString();
                        jobId.add(tmpOb.get("id").toString());
                    }
                }catch(Exception e){
                    e.printStackTrace();
                }

                //call setItems to set list for multispinner, calling in UI main thread
                jobList = Arrays.asList(custArray);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        MultiSpinner multiSpinner_jobs = (MultiSpinner) findViewById(R.id.multi_spinner_edit2);
                        multiSpinner_jobs.setItems(jobList, getString(R.string.jobsListHeader), mMultiSpinnerListener);
                    }
                });
                //Call  to get employee data
                HttpUrl getUrl_edit = new HttpUrl.Builder()
                        .scheme("https")
                        .host("jobtracker-161217.appspot.com")
                        .addPathSegments(employeeLink)
                        .build();
                //testBox.setText("full path = " + getUrl_edit.toString());
                Request edit_getRequest = new Request.Builder()
                        .url(getUrl_edit)
                        .build();
//
                client.newCall(edit_getRequest).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {

                        final String currentEmployee = response.body().string();

                        try{
                            final JSONObject jsonEmployee = new JSONObject(currentEmployee);
                            jobLinks = jsonEmployee.getString("jobs_assigned");
                            JSONArray tmpJobs = new JSONArray(jobLinks);
                            StringBuffer tmpCheckedJobs = new StringBuffer();
                            FileInputStream mFis = openFileInput(FILENAME);
                            int num = mFis.read();
                            mFis.close();

                            for(int i = 0; i < tmpJobs.length(); i++){
                                for(int j = 0; j < num; j++){
                                    if(Objects.equals(tmpJobs.get(i), "/jobs/" + jobId.get(j) ) ){
                                        tmpCheckedJobs.append(custArray[j]);
                                        tmpCheckedJobs.append(", ");
                                    }
                                }
                            }
                            if(tmpCheckedJobs.length() > 2){
                                tmpCheckedJobs.setLength(tmpCheckedJobs.length() - 2);
                            }
                            checkedJobs = tmpCheckedJobs.toString();

                            //Fill edit text fields with existing employee data
                            final EditText fnameBox = (EditText) findViewById(R.id.fname_editText2);
                            final EditText lnameBox = (EditText) findViewById(R.id.lname_editText2);
                            final EditText hrwageBox = (EditText) findViewById(R.id.hrwage_editText2);
                            final TextView skillsBox = (TextView) findViewById(R.id.curSkills_textView);
                            final TextView jobsBox = (TextView) findViewById(R.id.curJob_textView);
                            //testBox.setText(curEmployee.get("first_name"));

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        fnameBox.setText(jsonEmployee.getString("first_name"));
                                        lnameBox.setText(jsonEmployee.getString("last_name"));
                                        hrwageBox.setText(jsonEmployee.getString("hourly_wage"));
                                        skillsBox.setText(jsonEmployee.getString("skills"));
                                        jobsBox.setText(checkedJobs);
                                    }catch(Exception e){
                                        e.printStackTrace();
                                    }
                                }
                            });
                        }catch(Exception e){
                            e.printStackTrace();
                        }//end try catch
                    }
                });//end employee new call


                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        MultiSpinner multiSpinner_skills;
                        Resources res = getResources();
                        String[] tmpList = res.getStringArray(R.array.skillsList);
                        List<String> skillList = Arrays.asList(tmpList);
                        multiSpinner_skills = (MultiSpinner) findViewById(R.id.multi_spinner_edit);
                        multiSpinner_skills.setItems(skillList, getString(R.string.skillHeader), mMultiSpinnerListener);
                    }
                });
            }//end on response
        });//end job newcall
    }//end on create

    public void UpdateEmployee(View v){
        final MultiSpinner skillsField = (MultiSpinner) findViewById(R.id.multi_spinner_edit);
        final MultiSpinner jobsField = (MultiSpinner) findViewById(R.id.multi_spinner_edit2);

        //get first name from form
        final EditText fnameField = (EditText) findViewById(R.id.fname_editText2);
        String fname = fnameField.getText().toString();
        //get last name
        final EditText lnameField = (EditText) findViewById(R.id.lname_editText2);
        String lname = lnameField.getText().toString();
        //get hourly wage field
        final EditText hrWageField = (EditText) findViewById(R.id.hrwage_editText2);

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
                jobsBuffer.append("/jobs/" + jobId.get(i));
                jobsBuffer.append("\",");
            }
        }
        if(jobsBuffer.length() > 0){
            jobsBuffer.setLength(jobsBuffer.length() - 1);
        }
        jobsText = jobsBuffer.toString();

        //Set up Patch request
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        OkHttpClient client = new OkHttpClient();
        HttpUrl updateEmployeeUrl = new HttpUrl.Builder()
            .scheme("https")
            .host("jobtracker-161217.appspot.com")
            .addPathSegments(employeeLink)
            .build();

        //Create body and send request
        bodyBuffer.append("{");

        if(!fname.isEmpty()) {
            bodyBuffer.append("\"first_name\":\"");
            bodyBuffer.append(fname);
            bodyBuffer.append("\",");
        }
        if(!lname.isEmpty()) {
            bodyBuffer.append("\"last_name\": \"");
            bodyBuffer.append(lname);
            bodyBuffer.append("\",");
        }
        if(!hrWageField.getText().toString().isEmpty()){
            bodyBuffer.append("\"hourly_wage\": \"");
            bodyBuffer.append(hrWageField.getText().toString());
            bodyBuffer.append("\",");
        }
        if(!skillsText.isEmpty()){
            bodyBuffer.append("\"skills\": [");
            bodyBuffer.append(skillsText);
            bodyBuffer.append("],");
        }
        if(!jobsText.isEmpty()){
            bodyBuffer.append("\"jobs_assigned\": [");
            bodyBuffer.append(jobsText);
            bodyBuffer.append("],");
        }
        //Remove commma at end
        bodyBuffer.setLength(bodyBuffer.length() - 1);
        bodyBuffer.append("}");
        bodyText = bodyBuffer.toString();

        //Set up actual client and request
        RequestBody body = RequestBody.create(JSON, bodyText);
        Request request = new Request.Builder()
                .url(updateEmployeeUrl)
                .patch(body)
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
                    if(response.code() == 200){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast updateEmpSuccess = Toast.makeText(EditEmployee.this, R.string.update_emp_success, Toast.LENGTH_LONG );
                                updateEmpSuccess.setGravity(Gravity.BOTTOM, 0, 300);
                                updateEmpSuccess.show();
                            }
                        });
                        //Return to employee list activity
                        Intent mIntent = new Intent(EditEmployee.this, SeeEmployeesActivity.class);
                        startActivity(mIntent);
                    }else{
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast updateEmpFailed = Toast.makeText(EditEmployee.this, R.string.update_emp_failed, Toast.LENGTH_LONG );
                                updateEmpFailed.setGravity(Gravity.CENTER, 0, 0);
                                updateEmpFailed.show();
                            }
                        });
                        Intent mIntent = new Intent(EditEmployee.this, SeeEmployeesActivity.class);
                        startActivity(mIntent);
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }//end editEmployee

    public void DeleteEmployee(View v){
        HttpUrl deleteUrl = new HttpUrl.Builder()
                .scheme("https")
                .host("jobtracker-161217.appspot.com")
                .addPathSegments(employeeLink)
                .build();

        Request deleteRequest = new Request.Builder()
                .url(deleteUrl)
                .delete()
                .build();

        client.newCall(deleteRequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if(response.code() == 200){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast deleteEmpSuccess = Toast.makeText(EditEmployee.this, R.string.deleteEmpSuccess, Toast.LENGTH_LONG );
                            deleteEmpSuccess.setGravity(Gravity.CENTER, 0, 300);
                            deleteEmpSuccess.show();
                        }
                    });
                    //Return to main activity
                    Intent refreshSeeEmp = new Intent(EditEmployee.this, SeeEmployeesActivity.class);
                    startActivity(refreshSeeEmp);
                }
            }
        });
    }//end delete employee
}
