package com.example.openweather.kartikeykushwaha.openweather.OpenWeatherAPI;

import com.example.openweather.kartikeykushwaha.openweather.DataModels.WeatherByCityName.WeatherSearchResultDM;

import java.io.IOException;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.GsonConverterFactory;
import retrofit2.Retrofit;
import retrofit2.RxJavaCallAdapterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;
import rx.Observable;

/**
 * Created by kartikeykushwaha on 31/12/15.
 */
public class OpenWeatherRestClient {

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

            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .addInterceptor(loggingInterceptor)     //Add the api key by default
                    .addInterceptor(apiKeyInterceptor)      //Enable logging
                    .build();

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
        Observable<WeatherSearchResultDM> getWeatherByCityName(
                @Query("q") String cityName);

        @GET("weather")
        Observable<WeatherSearchResultDM> getWeatherByCoordinates(
                @Query("lat") String latitude,
                @Query("lon") String longitude);
    }

    private static class OpenWeatherApiKeyInsertionInterceptor implements Interceptor {

        private String mApiKey;

        public OpenWeatherApiKeyInsertionInterceptor(String apiKey) {
            mApiKey = apiKey;
        }

        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            HttpUrl url = request.url().newBuilder().addQueryParameter("appid", mApiKey).build();
            request = request.newBuilder().url(url).build();
            return chain.proceed(request);
        }
    }

}
