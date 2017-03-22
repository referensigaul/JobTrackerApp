package com.example.michael.jobtracker;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
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

public class AddJobActivity extends AppCompatActivity {

    OkHttpClient client = new OkHttpClient();
    List<String> employeeList = new ArrayList<>();
    String[]employeeLinkList;
    private MultiSpinner.MultiSpinnerListener mMultiSpinnerListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_job);

        //listener removes default value when user click into bid field, if left blank
        //it will reset field to default of 0.00, helps avoid errors with null float value
        final EditText bidField = (EditText) findViewById(R.id.bid_editText);
        bidField.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if(b){
                    bidField.setText("");
                }else{
                    String tmpWage = bidField.getText().toString();
                    if(tmpWage == null || tmpWage.isEmpty() ){
                        bidField.setText("0.00");
                    }
                }
            }
        });//end focus listener

        ////Request jobs from db to populate multispinner
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
                String employees = response.body().string();
                JSONObject tmpOb;
                int numEmployees=0;

                //get number of employees
                try{
                    JSONArray jsonEmployees = new JSONArray(employees);
                    numEmployees = jsonEmployees.length();
                }catch(Exception e){
                    e.printStackTrace();
                }

                String[] empArray = new String[numEmployees];
                employeeLinkList = new String[numEmployees];

                //create array with names of employees for displaying in multispinner, also
                //create list of all employee ids
                try {
                    JSONArray jsonEmployees = new JSONArray(employees);
                    for (int i = 0; i < jsonEmployees.length(); i++) {
                        tmpOb = (JSONObject) jsonEmployees.get(i);
                        empArray[i] = tmpOb.get("first_name").toString() + " " + tmpOb.get("last_name").toString();
                        //employeeLinkList[i] = tmpOb.get("id").toString();
                        employeeLinkList[i] = tmpOb.get("self").toString();

                    }
                }catch(Exception e){
                    e.printStackTrace();
                }

                //call setItems to set list for multispinner, calling in UI main thread
                employeeList = Arrays.asList(empArray);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        MultiSpinner multiSpinner_employees = (MultiSpinner) findViewById(R.id.multi_spinner3);
                        multiSpinner_employees.setItems(employeeList, getString(R.string.employeeListHeader), mMultiSpinnerListener);
                    }
                });
            }
        });
    }

    public void AddJob(View view){
        /**/
        final MultiSpinner employeesField = (MultiSpinner) findViewById(R.id.multi_spinner3);

        //get data from form
        final EditText customerField = (EditText) findViewById(R.id.customer_editText);
        String customerName = customerField.getText().toString();

        final EditText bidField = (EditText) findViewById(R.id.bid_editText);
        String bidAmount = bidField.getText().toString();

        final EditText startDateField = (EditText) findViewById(R.id.start_date_editText);
        String startDate = startDateField.getText().toString();

        final EditText completionDateField = (EditText) findViewById(R.id.completion_date_editText);
        String completionDate = completionDateField.getText().toString();

        //get checked values from mutlispinners
        boolean[] employeesSelected = employeesField.getSelected().clone();

        //Buffers and strings for creating JSON body for post request to create new job
        StringBuffer bodyBuffer = new StringBuffer();
        StringBuffer employeeBuffer = new StringBuffer();
        String employeeText;
        String bodyText;

        ////Employees section
        for(int i = 0; i < employeesSelected.length; i++){
            if(employeesSelected[i] == true){
                employeeBuffer.append("\"");
                employeeBuffer.append(employeeLinkList[i]);
                employeeBuffer.append("\",");
            }
        }
        if(employeeBuffer.length() > 0){
            employeeBuffer.setLength(employeeBuffer.length() - 1);
        }
        employeeText = employeeBuffer.toString();

        ////Create body and send request
        boolean goodRequest = false;
        if(!customerName.isEmpty()){
            bodyBuffer.append("{\"customer\":\"");
            bodyBuffer.append(customerName);
            bodyBuffer.append("\"");
            goodRequest = true;
        }

        //Only if first and last name were included do we attempt to build and send the request
        if(goodRequest){
            if(!bidAmount.isEmpty()){
                bodyBuffer.append(", \"bid\": \"");
                bodyBuffer.append(bidAmount);
                bodyBuffer.append("\"");
            }
            if(!startDate.isEmpty()){
                bodyBuffer.append(", \"start_date\": \"");
                bodyBuffer.append(startDate);
                bodyBuffer.append("\"");
            }
            if(!completionDate.isEmpty()){
                bodyBuffer.append(", \"completion_date\": \"");
                bodyBuffer.append(completionDate);
                bodyBuffer.append("\"");
            }
            if(!employeeText.isEmpty()){
                bodyBuffer.append(", \"employees\": [");
                bodyBuffer.append(employeeText);
                bodyBuffer.append("]");
            }

            bodyBuffer.append("}");
            bodyText = bodyBuffer.toString();

            //Set up actual client and request
            MediaType JSON = MediaType.parse("application/json; charset=utf-8");
            OkHttpClient client = new OkHttpClient();
            HttpUrl reqUrl = HttpUrl.parse("https://jobtracker-161217.appspot.com/jobs");
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
                                    Toast addJobSuccess = Toast.makeText(AddJobActivity.this, R.string.add_job_success, Toast.LENGTH_LONG );
                                    addJobSuccess.setGravity(Gravity.CENTER, 0, 0);
                                    addJobSuccess.show();
                                }
                            });
                            //Return to main activity
                            Intent mIntent = new Intent(AddJobActivity.this, MainActivity.class);
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
                    Toast addEmpFailed = Toast.makeText(AddJobActivity.this, R.string.add_job_failed, Toast.LENGTH_LONG );
                    addEmpFailed.setGravity(Gravity.BOTTOM, 0, 300);
                    addEmpFailed.show();
                }
            });
            Intent mIntent = new Intent(AddJobActivity.this, MainActivity.class);
            startActivity(mIntent);
        }//end goodRequest
    }

}
