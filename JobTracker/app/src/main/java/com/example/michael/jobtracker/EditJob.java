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

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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

public class EditJob extends AppCompatActivity {
    String FILENAME = "numEmp.txt";
    String jobLink;
    String employees;
    String checkedEmployees;
    OkHttpClient client;
    List<String> employeeList = new ArrayList<>();
    String employeeLinks;
    String[] employeeArray;
    List<String> empId = new ArrayList<>();
    int numEmps =0;

    private MultiSpinner.MultiSpinnerListener mMultiSpinnerListener;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_job);

        Bundle extras = getIntent().getExtras();
        if(extras != null){
            jobLink = extras.getString("job_link");
        }

        client = new OkHttpClient();

        //Make request to get all employees
        HttpUrl getUrl = HttpUrl.parse("https://jobtracker-161217.appspot.com/employees");
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
                employees = response.body().string();
                JSONObject tmpOb;
                try{
                    JSONArray jsonJobs = new JSONArray(employees);
                    numEmps = jsonJobs.length();
                    FileOutputStream mFos = openFileOutput(FILENAME, MODE_PRIVATE);
                    mFos.write(numEmps);
                    mFos.close();
                }catch(Exception e){
                    e.printStackTrace();
                }
                employeeArray = new String[numEmps];

                try {
                    JSONArray jsonJobs = new JSONArray(employees);
                    for (int i = 0; i < jsonJobs.length(); i++) {
                        tmpOb = (JSONObject) jsonJobs.get(i);
                        employeeArray[i] = tmpOb.get("first_name").toString() + " " + tmpOb.get("last_name").toString();
                        empId.add(tmpOb.get("id").toString());
                    }
                }catch(Exception e){
                    e.printStackTrace();
                }

                //call setItems to set list for multispinner, calling in UI main thread
                employeeList = Arrays.asList(employeeArray);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        MultiSpinner multiSpinner_employees = (MultiSpinner) findViewById(R.id.multi_spinner_edit_job);
                        multiSpinner_employees.setItems(employeeList, getString(R.string.employeeListHeader), mMultiSpinnerListener);
                    }
                });
                //Call  to get job data
                HttpUrl getUrl_edit = new HttpUrl.Builder()
                        .scheme("https")
                        .host("jobtracker-161217.appspot.com")
                        .addPathSegments(jobLink)
                        .build();
                //testBox.setText("full path = " + getUrl_edit.toString());
                Request edit_getRequest = new Request.Builder()
                        .url(getUrl_edit)
                        .build();

                client.newCall(edit_getRequest).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        final String currentJob = response.body().string();

                        try{
                            final JSONObject jsonJob = new JSONObject(currentJob);
                            employeeLinks = jsonJob.getString("employees");
                            JSONArray tmpEmployees = new JSONArray(employeeLinks);
                            StringBuffer tmpCheckedEmployees = new StringBuffer();
                            FileInputStream mFis = openFileInput(FILENAME);
                            int num = mFis.read();
                            mFis.close();

                            for(int i = 0; i < tmpEmployees.length(); i++){
                                for(int j = 0; j < num; j++){
                                    if(Objects.equals(tmpEmployees.get(i), "/employees/" + empId.get(j) ) ){
                                        tmpCheckedEmployees.append(employeeArray[j]);
                                        tmpCheckedEmployees.append(", ");
                                    }
                                }
                            }
                            if(tmpCheckedEmployees.length() > 2){
                                tmpCheckedEmployees.setLength(tmpCheckedEmployees.length() - 2);
                            }
                            checkedEmployees = tmpCheckedEmployees.toString();

                            //Fill edit text fields with existing employee data
                            final EditText customerBox = (EditText) findViewById(R.id.customer_editText2);
                            final EditText bidBox = (EditText) findViewById(R.id.bid_editText2);
                            final EditText startDateBox = (EditText) findViewById(R.id.start_date_editText2);
                            final TextView completionDateBox = (TextView) findViewById(R.id.completion_date_editText2);
                            final TextView skillsBox = (TextView) findViewById(R.id.curSkills_textView);
                            //testBox.setText(curEmployee.get("first_name"));

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        customerBox.setText(jsonJob.getString("customer"));
                                        bidBox.setText(jsonJob.getString("bid"));
                                        startDateBox.setText(jsonJob.getString("start_date"));
                                        completionDateBox.setText(jsonJob.getString("completion_date"));
                                        skillsBox.setText(checkedEmployees);
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
            }//end on response
        });//end job newcall



    }//end onCreate

    public void UpdateJob(View v){
        final MultiSpinner employeesField = (MultiSpinner) findViewById(R.id.multi_spinner_edit_job);

        //get first name from form
        final EditText customerField = (EditText) findViewById(R.id.customer_editText2);
        String customer = customerField.getText().toString();
        //get last name
        final EditText bidField = (EditText) findViewById(R.id.bid_editText2);
        String bid = bidField.getText().toString();
        //get hourly wage field
        final EditText startDateField = (EditText) findViewById(R.id.start_date_editText2);
        String startDate = startDateField.getText().toString();

        final EditText completionDateField = (EditText) findViewById(R.id.completion_date_editText2);
        String completionDate = completionDateField.getText().toString();


        //get checked values from mutlispinners
        boolean[] employeesSelected = employeesField.getSelected().clone();

        //Buffers and strings for creating JSON body for request
        StringBuffer bodyBuffer = new StringBuffer();       
        StringBuffer employeesBuffer = new StringBuffer();        
        String employeesText;
        String bodyText;
        
        //jobs section
        for(int i = 0; i < employeesSelected.length; i++){
            if(employeesSelected[i] == true){
                employeesBuffer.append("\"");
                employeesBuffer.append("/employees/" + empId.get(i));
                employeesBuffer.append("\",");
            }
        }
        if(employeesBuffer.length() > 0){
            employeesBuffer.setLength(employeesBuffer.length() - 1);
        }
        employeesText = employeesBuffer.toString();

        //Set up Patch request
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        OkHttpClient client = new OkHttpClient();
        HttpUrl updateJobUrl = new HttpUrl.Builder()
                .scheme("https")
                .host("jobtracker-161217.appspot.com")
                .addPathSegments(jobLink)
                .build();

        //Create body and send request
        bodyBuffer.append("{");

        if(!customer.isEmpty()) {
            bodyBuffer.append("\"customer\":\"");
            bodyBuffer.append(customer);
            bodyBuffer.append("\",");
        }
        if(!bid.isEmpty()) {
            bodyBuffer.append("\"bid\": \"");
            bodyBuffer.append(bid);
            bodyBuffer.append("\",");
        }
        if(!startDate.isEmpty()){
            bodyBuffer.append("\"start_date\": \"");
            bodyBuffer.append(startDate);
            bodyBuffer.append("\",");
        }
        if(!completionDate.isEmpty()){
            bodyBuffer.append("\"completion_date\": \"");
            bodyBuffer.append(completionDate);
            bodyBuffer.append("\",");
        }
        if(!employeesText.isEmpty()){
            bodyBuffer.append("\"employees\": [");
            bodyBuffer.append(employeesText);
            bodyBuffer.append("],");
        }
        //Remove commma at end
        bodyBuffer.setLength(bodyBuffer.length() - 1);
        bodyBuffer.append("}");
        bodyText = bodyBuffer.toString();

        //Set up actual client and request
        RequestBody body = RequestBody.create(JSON, bodyText);
        Request request = new Request.Builder()
                .url(updateJobUrl)
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
                                Toast updateEmpSuccess = Toast.makeText(EditJob.this, R.string.update_job_success, Toast.LENGTH_LONG );
                                updateEmpSuccess.setGravity(Gravity.BOTTOM, 0, 300);
                                updateEmpSuccess.show();
                            }
                        });
                        //Return to employee list activity
                        Intent mIntent = new Intent(EditJob.this, SeeJobsActivity.class);
                        startActivity(mIntent);
                    }else{
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast updateEmpFailed = Toast.makeText(EditJob.this, R.string.update_job_failed, Toast.LENGTH_LONG );
                                updateEmpFailed.setGravity(Gravity.CENTER, 0, 0);
                                updateEmpFailed.show();
                            }
                        });
                        Intent mIntent = new Intent(EditJob.this, SeeJobsActivity.class);
                        startActivity(mIntent);
                    }
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }//end editEmployee

    public void DeleteJob(View v){
        HttpUrl deleteUrl = new HttpUrl.Builder()
                .scheme("https")
                .host("jobtracker-161217.appspot.com")
                .addPathSegments(jobLink)
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
                            Toast deleteEmpSuccess = Toast.makeText(EditJob.this, R.string.deleteJobSuccess, Toast.LENGTH_LONG );
                            deleteEmpSuccess.setGravity(Gravity.CENTER, 0, 300);
                            deleteEmpSuccess.show();
                        }
                    });
                    //Return to main activity
                    Intent refreshSeeJob = new Intent(EditJob.this, SeeJobsActivity.class);
                    startActivity(refreshSeeJob);
                }
            }
        });
    }//end deleteJob
}
