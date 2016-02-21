package com.example.openweather.kartikeykushwaha.openweather;

import android.Manifest;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.EditText;

import com.example.openweather.kartikeykushwaha.openweather.DataModels.WeatherByCityName.Sys;
import com.example.openweather.kartikeykushwaha.openweather.DataModels.WeatherByCityName.Weather;
import com.example.openweather.kartikeykushwaha.openweather.DataModels.WeatherByCityName.WeatherSearchResultDM;
import com.example.openweather.kartikeykushwaha.openweather.OpenWeatherAPI.OpenWeatherRestClient;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener{

    @Bind(R.id.city_input_field) EditText cityNameField;

    private final String TAG = MainActivity.this.getClass().getSimpleName();

    private GoogleApiClient googleLocationApiClient;
    //Boolean to maintain whether an error is being resolved or not
    private boolean resolvingGooglePlayConnectionError = false;
    // Request code to use when launching the resolution activity
    private static final int REQUEST_RESOLVE_ERROR = 1001;
    // Unique tag for the error dialog fragment
    private static final String DIALOG_ERROR = "dialog_error";
    //Key used to maintain the boolean value of 'resolvingGooglePlayConnectionError'
    private static final String STATE_RESOLVING_ERROR = "resolving_error";

    //Request code for accessing location
    private static final int PERMISSION_GET_FINE_LOCATION = 1001;
    // latitude and longitude required to get data by geo-location
    private String latitude, longitude;
    // Boolean to check whether the location has been provided or not
    private boolean hasLocation = false;

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

        OpenWeatherRestClient.OpenWeatherCurrentDataApiInterface openWeatherCurrentDataApiInterface
                = OpenWeatherRestClient.getOpenWeatherCurrentDataApiInterface();

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

    /**
     * Method to fetch the coordinated from google play services.
     */
    private void fetchLastLocationCoordinates() {

        Location lastLocation = null;

        /*
        * Request for location permission.
        * */
        if(ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            lastLocation = LocationServices.FusedLocationApi.getLastLocation(
                    googleLocationApiClient);
        } else {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_GET_FINE_LOCATION);
        }

        if(lastLocation != null) {
            hasLocation = true;
            latitude = String.valueOf(lastLocation.getLatitude());
            longitude = String.valueOf(lastLocation.getLongitude());
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        /* Successfully Connected to Google Play services.
        *1. Attempt to get the last known location. */

        fetchLastLocationCoordinates();
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

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {

            case PERMISSION_GET_FINE_LOCATION:
                // If request is cancelled, the result arrays are empty.
                if(grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Location permission granted.
                    Log.i(TAG, "Permission granted");
                    fetchLastLocationCoordinates();

                } else {
                    // permission denied.
                    Log.i(TAG, "Permission denied");
                }
                break;
        }
    }
}
