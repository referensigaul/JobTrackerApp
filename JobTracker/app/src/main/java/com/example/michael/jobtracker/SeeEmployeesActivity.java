package com.example.michael.jobtracker;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.RadialGradient;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cz.destil.settleup.gui.MultiSpinner;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static android.R.attr.id;
import static android.os.Build.VERSION_CODES.M;
import static java.security.AccessController.getContext;

public class SeeEmployeesActivity extends AppCompatActivity {
    ListView employeeList;
    OkHttpClient client = new OkHttpClient();
    List<Map<String, String>> items = new ArrayList<>();
    int curEmployee;
    String curLink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_see_employees);

        employeeList = (ListView)findViewById(R.id.employee_listView);

        HttpUrl getUrl = HttpUrl.parse("https://jobtracker-161217.appspot.com/employees");
        Request getRequest = new Request.Builder()
                .url(getUrl)
                .build();

        client.newCall(getRequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            //when response received create list from results
            @Override
            public void onResponse(Call call, Response response) throws IOException {

                final String employees = response.body().string();

                try{
                    final JSONArray jsonEmployees = new JSONArray(employees);
                    employeeList = (ListView) findViewById(R.id.employee_listView);

                    for(int i = 0; i < jsonEmployees.length(); i++){
                        HashMap<String, String> m = new HashMap<>();
                        m.put("firstName", jsonEmployees.getJSONObject(i).getString("first_name"));
                        m.put("lastName", jsonEmployees.getJSONObject(i).getString("last_name"));
                        m.put("self", jsonEmployees.getJSONObject(i).getString("self"));
                        items.add(m);
                    }
                    final SimpleAdapter itemsAdapter = new SimpleAdapter(
                            SeeEmployeesActivity.this,
                            items,
                            R.layout.emp_item,
                            new String[]{"firstName", "lastName"},
                            new int[]{R.id.textView_fname, R.id.textView_lname});

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            employeeList.setAdapter(itemsAdapter);
                        }
                    });
                }catch(Exception e){
                    e.printStackTrace();
                }
            }//end onResponse
        });//end new call
        employeeList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, final View view, final int i, final long l) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        employeeDetails(view, i);
                    }
                });
            }
        });
    }//end on create

    public void employeeDetails(View view, int itemNum){
        optionDialogFragment choice = new optionDialogFragment();
        curEmployee = itemNum;
        //for(int i = 0; i < items.size(); i++){
        //    testText.append(items.get(i) + "\n");
        //}
        //testText.append(items.get(itemNum) + "\n");
        Map<String, String> tmp = items.get(itemNum);
        //testText.append(tmp.get("self") + "\n");
        curLink = tmp.get("self");
        choice.show(getSupportFragmentManager(), "not sure tag for");
    }
    public void goToMain(View view){
        Intent goHome = new Intent(this,MainActivity.class);
        startActivity(goHome);
    }

    public static class optionDialogFragment extends DialogFragment{
        private SeeEmployeesActivity activity;

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState){
            activity = (SeeEmployeesActivity) getActivity();

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(R.string.popup_message)
                    .setPositiveButton(R.string.popup_Continue, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Intent mIntent = new Intent(getContext(),EditEmployee.class);
                            mIntent.putExtra("employee_link", activity.curLink);
                            startActivity(mIntent);
                        }
                    })
                    .setNegativeButton(R.string.popup_back, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                        }
                    });

            return builder.create();
        }
    }// end fragment class
}
