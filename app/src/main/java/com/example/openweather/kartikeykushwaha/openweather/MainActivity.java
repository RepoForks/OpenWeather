package com.example.openweather.kartikeykushwaha.openweather;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.IntentSender;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.EditText;

import com.example.openweather.kartikeykushwaha.openweather.DataModels.WeatherByCityName.Sys;
import com.example.openweather.kartikeykushwaha.openweather.DataModels.WeatherByCityName.Weather;
import com.example.openweather.kartikeykushwaha.openweather.DataModels.WeatherByCityName.WeatherSearchResultDM;
import com.example.openweather.kartikeykushwaha.openweather.OpenWeatherAPI.OpenWeatherApi;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener{

    @Bind(R.id.city_input_field) EditText cityNameField;

    private GoogleApiClient googleLocationApiClient;
    //Boolean to maintain whether an error is being resolved or not
    private boolean resolvingGooglePlayConnectionError = false;
    // Request code to use when launching the resolution activity
    private static final int REQUEST_RESOLVE_ERROR = 1001;
    // Unique tag for the error dialog fragment
    private static final String DIALOG_ERROR = "dialog_error";
    //Key used to maintain the boolean value of 'resolvingGooglePlayConnectionError'
    private static final String STATE_RESOLVING_ERROR = "resolving_error";

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        outState.putBoolean(STATE_RESOLVING_ERROR, resolvingGooglePlayConnectionError);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        resolvingGooglePlayConnectionError = savedInstanceState != null
                && savedInstanceState.getBoolean(STATE_RESOLVING_ERROR, false);

        // Attempt to connect to google play services
        if(googleLocationApiClient == null) {
            googleLocationApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        OpenWeatherApi.OpenWeatherCurrentDataApiInterface openWeatherCurrentDataApiInterface
                = OpenWeatherApi.getOpenWeatherCurrentDataApiInterface();

        Call<WeatherSearchResultDM> call = openWeatherCurrentDataApiInterface.getWeatherByCordinates("","");

        call.enqueue(new Callback<WeatherSearchResultDM>() {
            @Override
            public void onResponse(Response<WeatherSearchResultDM> response) {

            }

            @Override
            public void onFailure(Throwable t) {

            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!resolvingGooglePlayConnectionError)
            googleLocationApiClient.connect();
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

        Observable<WeatherSearchResultDM> WeatherByCityNameObservable
                = openWeatherCurrentDataApiInterface
                .getWeatherByCityName(cityNameField.getText().toString());

        Subscription weatherByCityNameSubscription =  WeatherByCityNameObservable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.newThread())
                .subscribe(new Subscriber<WeatherSearchResultDM>() {
                    @Override
                    public void onCompleted() {
                        Log.i(TAG, "Completed");
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.i(TAG, "Error");
                        e.printStackTrace();
                    }

                    @Override
                    public void onNext(WeatherSearchResultDM weatherSearchResultDM) {
                        Log.i(TAG, "Next called");

                        Sys sysReply = weatherSearchResultDM.getSys();
                        List<Weather> weathersReply = weatherSearchResultDM.getWeather();
                        weathersReply.get(0).getMain();
                    }
                });
    }

    @Override
    public void onConnected(Bundle bundle) {
        /* Successfully Connected to Google Play services.
        *1. Attempt to get the last known location. */

    }

    @Override
    public void onConnectionSuspended(int i) {
        // The connection has been interrupted.
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

        if(resolvingGooglePlayConnectionError) {
            // Already attempting a resolution
            return;
        } else if(connectionResult.hasResolution()) {
            try {
                resolvingGooglePlayConnectionError = true;
                connectionResult.startResolutionForResult(this, REQUEST_RESOLVE_ERROR);
            } catch (IntentSender.SendIntentException ex) {
                // Error in resolution intent; try again.
                ex.printStackTrace();
                googleLocationApiClient.connect();
            }
        } else {
            // Show dialog using GoogleApiAvailability.getErrorDialog()
            showErrorDialog(connectionResult.getErrorCode());
            resolvingGooglePlayConnectionError = true;
        }
    }

    /* Creates a dialog for an error message */
    private void showErrorDialog(int errorCode) {
        // Create a fragment for the error dialog
        ErrorDialogFragment dialogFragment = new ErrorDialogFragment();
        // Pass the error that should be displayed
        Bundle args = new Bundle();
        args.putInt(DIALOG_ERROR, errorCode);
        dialogFragment.setArguments(args);
        dialogFragment.show(getSupportFragmentManager(), "errordialog");
    }

    /* Called from ErrorDialogFragment when the dialog is dismissed. */
    public void onDialogDismissed() {
        resolvingGooglePlayConnectionError = false;
    }

    /* A fragment to display an error dialog */
    public static class ErrorDialogFragment extends DialogFragment {
        public ErrorDialogFragment() { }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Get the error code and retrieve the appropriate dialog
            int errorCode = this.getArguments().getInt(DIALOG_ERROR);
            return GoogleApiAvailability.getInstance().getErrorDialog(
                    this.getActivity(), errorCode, REQUEST_RESOLVE_ERROR);
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            ((MainActivity) getActivity()).onDialogDismissed();
        }
    }
}
