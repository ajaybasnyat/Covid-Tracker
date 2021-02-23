//Enumeration class for metric and timescale selection

package com.e.covidtracker;

public class ChartOptions {
    enum Metric {
        NEGATIVE,
        POSITIVE,
        DEATH
    }
    enum TimeScale {
        WEEK,
        MONTH,
        ALL
    }
}
