package com.example.openweather.kartikeykushwaha.openweather.OpenWeatherAPI;

import com.example.openweather.kartikeykushwaha.openweather.DataModels.WeatherByCityName.WeatherByCityNameDM;
import com.squareup.okhttp.HttpUrl;
import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.logging.HttpLoggingInterceptor;

import java.io.IOException;

import retrofit.GsonConverterFactory;
import retrofit.Retrofit;
import retrofit.RxJavaCallAdapterFactory;
import retrofit.http.GET;
import retrofit.http.Query;
import rx.Observable;

/**
 * Created by kartikeykushwaha on 31/12/15.
 */
public class OpenWeatherApi {

    private static final String openWeatherApiKey = "ab12e7324b24e56636efb32f2e3549b1";

    private static final String apiUrl = "http://api.openweathermap.org";
    private static final String apiVersion = "/data/2.5/";
    private static final String baseUrl = apiUrl + apiVersion;

    private static OpenWeatherCurrentDataApiInterface openWeatherCurrentDataApiInterface;

    public static OpenWeatherCurrentDataApiInterface getOpenWeatherCurrentDataApiInterface() {

        if(openWeatherCurrentDataApiInterface == null) {

            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            OpenWeatherApiKeyInsertionInterceptor apiKeyInterceptor =
                    new OpenWeatherApiKeyInsertionInterceptor(openWeatherApiKey);

            OkHttpClient okHttpClient = new OkHttpClient();
            okHttpClient.interceptors().add(apiKeyInterceptor);                //Add the api key by default
            okHttpClient.interceptors().add(loggingInterceptor);               //Enable logging


            Retrofit retrofitClient = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                    .build();

            openWeatherCurrentDataApiInterface =
                    retrofitClient.create(OpenWeatherCurrentDataApiInterface.class);
        }

        return openWeatherCurrentDataApiInterface;
    }


    public interface OpenWeatherCurrentDataApiInterface {

        @GET("weather")
        Observable<WeatherByCityNameDM> getWeatherByCityName(
                @Query("q") String cityName);
    }

    private static class OpenWeatherApiKeyInsertionInterceptor implements Interceptor {

        private String mApiKey;

        public OpenWeatherApiKeyInsertionInterceptor(String apiKey) {
            mApiKey = apiKey;
        }

        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            HttpUrl url = request.httpUrl().newBuilder().addQueryParameter("appid", mApiKey).build();
            request = request.newBuilder().url(url).build();
            return chain.proceed(request);
        }
    }

}
