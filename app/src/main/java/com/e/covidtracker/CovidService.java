//interface service for getting data from API

package com.e.covidtracker;
import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;

public interface CovidService {

    @GET("us/daily.json")
    Call<List<CovidData>> getNationalData();

    @GET("states/daily.json")
    Call<List<CovidData>> getStatesData();
}
