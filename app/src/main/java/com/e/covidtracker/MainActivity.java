//main activity

package com.e.covidtracker;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.robinhood.spark.SparkView;
import com.robinhood.ticker.TickerUtils;
import com.robinhood.ticker.TickerView;

import org.angmarch.views.NiceSpinner;
import org.angmarch.views.OnSpinnerItemSelectedListener;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {
//    constants
    private static final String BASE_URL = "https://covidtracking.com/api/v1/";
    private static final String ALL_STATES = "All States";
    private static final String TAG = "MyActivity";

//    variables
    private List<CovidData> nationalDailyData;
    private Map<String, List<CovidData>> perStateDailyData;
    CovidSparkAdapter adapter = new CovidSparkAdapter();
    private List<CovidData> currentData;
    private SparkView sparkView;
    private TextView tvMetricLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
//        setup contents
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        define reused components
        sparkView = findViewById(R.id.sparkView);
        tvMetricLabel = findViewById(R.id.tvMetricLabel);
//        create retrofit object
        Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss").create();
        Retrofit retrofit = new Retrofit.Builder().baseUrl(BASE_URL).addConverterFactory(GsonConverterFactory.create(gson)).build();
//        tie covid service API to retrofit
        CovidService covidService = retrofit.create(CovidService.class);

        //fetch national data
        covidService.getNationalData().enqueue(new Callback<List<CovidData>>() {
            @Override
            public void onResponse(Call<List<CovidData>> call, Response<List<CovidData>> response) {
                List<CovidData> nationalData = response.body();
                if (nationalData == null) {
                    Log.w(TAG, "Did not receive a valid response body");
                    return;
                }
//                set up listeners for data manipulation
                setupEventListeners();
                nationalDailyData = nationalData;
//                reverse list to return data in chronological order
                Collections.reverse(nationalDailyData);
//                update view with selected data
                updateDisplayWithData(nationalDailyData);
            }

            @Override
            public void onFailure(Call<List<CovidData>> call, Throwable t) {
            }
        });

        //fetch state data
        covidService.getStatesData().enqueue(new Callback<List<CovidData>>() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onResponse(Call<List<CovidData>> call, Response<List<CovidData>> response) {
                List<CovidData> statesData = response.body();

                if (statesData == null) {
                    Log.w(TAG, "Did not receive a valid response body");
                    return;
                }
//                reverse list to return data in chronological order
                Collections.reverse(statesData);
//                remove faulty data points with negative values
                for (int i = 0; i < statesData.size(); i++) {
                    if (statesData.get(i).deathIncrease < 0 || statesData.get(i).positiveIncrease < 0 || statesData.get(i).negativeIncrease < 0) {
                        statesData.remove(i);
                    }
                }
//                create a Map<String, List<CovidData>> of the data ordered by states
                perStateDailyData = statesData.stream().collect(Collectors.groupingBy(w -> w.state));
//                update spinner dropdown with state data
                updateSpinnerWithStateData(perStateDailyData.keySet());
            }

            @Override
            public void onFailure(Call<List<CovidData>> call, Throwable t) {
            }
        });
    }
//populates spinner dropdown for state data selection
    private void updateSpinnerWithStateData(Set<String> stateNames) {
//        create list of state abbreviations
        List<String> stateAbbreviationList = new ArrayList<>(stateNames);
        Collections.sort(stateAbbreviationList);
//        add "All States" option at the top of the spinner
        stateAbbreviationList.add(0, ALL_STATES);

//        attach data to spinner
        NiceSpinner spinnerSelectState = findViewById(R.id.spinnerSelect);
        spinnerSelectState.attachDataSource(stateAbbreviationList);
        spinnerSelectState.setOnSpinnerItemSelectedListener((parent, view, position, id) -> {
            String selectedState = (String)parent.getItemAtPosition(position);
            List<CovidData> selectedData;
            if (perStateDailyData.get(selectedState) == null) {
                selectedData = nationalDailyData;
            }
            else {
                selectedData = perStateDailyData.get(selectedState);
            }
            updateDisplayWithData(selectedData);
        });
    }
//listeners for scrobbling and button presses
    private void setupEventListeners() {
//        allows user to scrobble across data
        sparkView.setScrubEnabled(true);
        sparkView.setScrubListener(new SparkView.OnScrubListener() {
            @Override
            public void onScrubbed(Object data) {
                if (data != null) {
                    updateInfoForDate((CovidData) data);
                }
            }
        });
//        define timeline radio group buttons
        RadioGroup radioGroupTimeline = findViewById(R.id.radioGroupTimeline);
        RadioButton radioButtonWeek = findViewById(R.id.radioButtonWeek);
        RadioButton radioButtonMonth = findViewById(R.id.radioButtonMonth);
        RadioButton radioButtonAll = findViewById(R.id.radioButtonAll);
//        define metric radio group buttons
        RadioGroup radioGroupMetric = findViewById(R.id.radioGroupMetric);
        RadioButton radioButtonPositive = findViewById(R.id.radioButtonPositive);
        RadioButton radioButtonNegative = findViewById(R.id.radioButtonNegative);
        RadioButton radioButtonDeath = findViewById(R.id.radioButtonAll);
//        listener for updating data when timeline changed
        radioGroupTimeline.setOnCheckedChangeListener((group, checkedId) -> {
            if (radioButtonWeek.isChecked()) {
                adapter.timescale = ChartOptions.TimeScale.WEEK;
            }
            else if(radioButtonMonth.isChecked()) {
                adapter.timescale = ChartOptions.TimeScale.MONTH;
            }
            else if (radioButtonAll.isChecked()){
                adapter.timescale = ChartOptions.TimeScale.ALL;
            }
            adapter.notifyDataSetChanged();
        });
//        listener for updating data when metric changed
        radioGroupMetric.setOnCheckedChangeListener((group, checkedId) -> {
            if (radioButtonPositive.isChecked()) {
                updateDisplayMetric(ChartOptions.Metric.POSITIVE);
            }
            else if(radioButtonNegative.isChecked()) {
                updateDisplayMetric(ChartOptions.Metric.NEGATIVE);
            }
            else if (radioButtonDeath.isChecked()){
                updateDisplayMetric(ChartOptions.Metric.DEATH);
            }
            adapter.notifyDataSetChanged();
        });
    }
//    updates data when metric is changed
    private void updateDisplayMetric(ChartOptions.Metric metric) {
//        change color of spark line and text based on metric
        int colorRes;
        if (metric == ChartOptions.Metric.NEGATIVE) {
            colorRes = R.color.colorNegative;
        }
        else if (metric == ChartOptions.Metric.POSITIVE) {
            colorRes = R.color.colorPositive;
        }
        else {
            colorRes = R.color.colorDeath;
        }
        int colorInt = ContextCompat.getColor(this, colorRes);
        sparkView.setLineColor(colorInt);
        tvMetricLabel.setTextColor(colorInt);

        adapter.metric = metric;
        adapter.notifyDataSetChanged();

        if (currentData != null) {
            updateInfoForDate(currentData.get(currentData.size() - 1));
        }
    }
//updates display based on new data
    private void updateDisplayWithData(List<CovidData> dailyData) {
//        attach new data to adapter
        adapter.dailyData = dailyData;
//        assign the adapter to the sparkview
        sparkView.setAdapter(adapter);
//        set default view to positive cases from all time when first opening app
        RadioButton radioPositive = findViewById(R.id.radioButtonPositive);
        radioPositive.setChecked(true);
        RadioButton radioAll = findViewById(R.id.radioButtonAll);
        radioAll.setChecked(true);
//        update display
        updateDisplayMetric(ChartOptions.Metric.POSITIVE);
    }
//update num cases and date textviews based on data selected
    private void updateInfoForDate(CovidData covidData) {
//        get value based on metric
        int numCases;
        if (adapter.metric == ChartOptions.Metric.POSITIVE) {
            numCases = covidData.positiveIncrease;
        }
        else if (adapter.metric == ChartOptions.Metric.NEGATIVE) {
            numCases = covidData.negativeIncrease;
        }
        else {
            numCases = covidData.deathIncrease;
        }
//        update metric label text with data
        TextView tvDateLabel = findViewById(R.id.tvDateLabel);
        tvMetricLabel.setText(String.valueOf(numCases));
//        format the date
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.US);
//        update date label
        tvDateLabel.setText(dateFormat.format(covidData.date));
    }
}
