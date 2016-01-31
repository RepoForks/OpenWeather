package com.example.openweather.kartikeykushwaha.openweather;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;

import com.example.openweather.kartikeykushwaha.openweather.DataModels.WeatherByCityName.Sys;
import com.example.openweather.kartikeykushwaha.openweather.DataModels.WeatherByCityName.Weather;
import com.example.openweather.kartikeykushwaha.openweather.DataModels.WeatherByCityName.WeatherByCityNameDM;
import com.example.openweather.kartikeykushwaha.openweather.OpenWeatherAPI.OpenWeatherApi;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener{

    @Bind(R.id.city_input_field) EditText cityNameField;

    private GoogleApiClient googleLocationApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        //Connect to google play services
        if(googleLocationApiClient != null) {
            googleLocationApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
    }

    @Override
    protected void onStart() {
        googleLocationApiClient.connect();
        super.onStart();
    }

    @Override
    protected void onStop() {
        googleLocationApiClient.disconnect();
        super.onStop();
    }

    @OnClick(R.id.button_submit_city)
    public void submitCityNameAction() {

        final String TAG = "WeatherByCityName";

        OpenWeatherApi.OpenWeatherCurrentDataApiInterface openWeatherCurrentDataApiInterface
                = OpenWeatherApi.getOpenWeatherCurrentDataApiInterface();

        Observable<WeatherByCityNameDM> WeatherByCityNameObservable
                = openWeatherCurrentDataApiInterface
                .getWeatherByCityName(cityNameField.getText().toString());

        WeatherByCityNameObservable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.newThread())
                .subscribe(new Subscriber<WeatherByCityNameDM>() {
                    @Override
                    public void onCompleted() {
                        Log.i(TAG, "Completed");
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.i(TAG, "Error thrown");
                        e.printStackTrace();
                    }

                    @Override
                    public void onNext(WeatherByCityNameDM weatherByCityNameDM) {
                        Log.i(TAG, "Next called");

                        Sys sysReply = weatherByCityNameDM.getSys();
                        List<Weather> weathersReply = weatherByCityNameDM.getWeather();
                        weathersReply.get(0).getMain();
                    }
                });
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnected(Bundle bundle) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }
}
