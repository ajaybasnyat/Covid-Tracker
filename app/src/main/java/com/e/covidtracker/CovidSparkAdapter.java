//adapter class that ties data to sparkview

package com.e.covidtracker;
import android.graphics.RectF;
import com.robinhood.spark.SparkAdapter;
import java.util.List;

class CovidSparkAdapter extends SparkAdapter {
//    data members of the adapter
    List<CovidData> dailyData;
    ChartOptions.Metric metric = ChartOptions.Metric.POSITIVE;
    ChartOptions.TimeScale timescale = ChartOptions.TimeScale.ALL;

//    default constructor
    public CovidSparkAdapter() {
    }

//    returns total number of data entries
    @Override
    public int getCount() {
        if (dailyData !=  null) {
            return dailyData.size();
        }
        else {
            return 0;
        }
    }

//    retrieves data from a given index
    @Override
    public Object getItem(int index) {
        return dailyData.get(index);
    }

//    used to populate sparkview with data
    @Override
    public float getY(int index) {
        if (metric == ChartOptions.Metric.POSITIVE) {
            return dailyData.get(index).positiveIncrease;
        }
        else if (metric == ChartOptions.Metric.NEGATIVE) {
            return dailyData.get(index).negativeIncrease;
        }
        else {
            return dailyData.get(index).deathIncrease;
        }
    }

//    define bounds for timeline selection
    @Override
    public RectF getDataBounds() {
        RectF bounds = super.getDataBounds();
        if (timescale == ChartOptions.TimeScale.MONTH) {
            bounds.left = getCount() - (float)30;
        }
        else if (timescale == ChartOptions.TimeScale.WEEK) {
            bounds.left = getCount() - (float)7;
        }
        return bounds;
    }
}
