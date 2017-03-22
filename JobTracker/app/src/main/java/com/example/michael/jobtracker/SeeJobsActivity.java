package com.example.michael.jobtracker;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;


import org.json.JSONArray;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SeeJobsActivity extends AppCompatActivity {

    ListView jobList;
    OkHttpClient client = new OkHttpClient();
    List<Map<String, String>> items = new ArrayList<>();
    int curJob;
    String curLink;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_see_jobs);

        jobList = (ListView)findViewById(R.id.jobs_listView);

        HttpUrl getUrl = HttpUrl.parse("https://jobtracker-161217.appspot.com/jobs");
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

                final String jobsStr = response.body().string();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //testText.setText(jobsStr);
                    }
                });

                try{
                    final JSONArray jsonJobs = new JSONArray(jobsStr);
                    jobList = (ListView) findViewById(R.id.jobs_listView);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try{
                                //testText.setText("jsonarray length = " + jsonJobs.length());
                                //testText.append("index 0: " + jsonJobs.get(0));
                            }catch(Exception e){
                                e.printStackTrace();
                            }

                        }
                    });
                    for(int i = 0; i < jsonJobs.length(); i++){
                        HashMap<String, String> m = new HashMap<>();
                        m.put("customer", jsonJobs.getJSONObject(i).getString("customer"));
                        m.put("bid", jsonJobs.getJSONObject(i).getString("bid"));
                        m.put("self", jsonJobs.getJSONObject(i).getString("self"));
                        items.add(m);
                    }
                    final SimpleAdapter itemsAdapter = new SimpleAdapter(
                            SeeJobsActivity.this,
                            items,
                            R.layout.job_item,
                            new String[]{"customer", "bid"},
                            new int[]{R.id.textView_customer, R.id.textView_bid});

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            jobList.setAdapter(itemsAdapter);
                        }
                    });
                }catch(Exception e){
                    e.printStackTrace();
                }

            }//end onResponse
        });//end new call
        jobList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, final View view, final int i, final long l) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        jobDetails(view, i);                    }
                });
            }
        });
    }//end onCreate
    public void jobDetails(View view, int itemNum){
        SeeJobsActivity.optionDialogFragment choice = new SeeJobsActivity.optionDialogFragment();
        curJob = itemNum;
        Map<String, String> tmp = items.get(itemNum);
        curLink = tmp.get("self");
        choice.show(getSupportFragmentManager(), "not sure tag for");
    }
    public void goToMain(View view){
        Intent goHome = new Intent(this,MainActivity.class);
        startActivity(goHome);
    }

    public static class optionDialogFragment extends DialogFragment {
        private SeeJobsActivity activity;

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState){
            activity = (SeeJobsActivity) getActivity();

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(R.string.popup_message_jobs)
                    .setPositiveButton(R.string.popup_Continue, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {

                            Intent mIntent = new Intent(getContext(),EditJob.class);
                            mIntent.putExtra("job_link", activity.curLink);
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
