package cn.myapps.coolweather;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.io.IOException;

import cn.myapps.coolweather.gson.Forecast;
import cn.myapps.coolweather.gson.Weather;
import cn.myapps.coolweather.util.HttpUtil;
import cn.myapps.coolweather.util.Utility;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WeatherActivity extends AppCompatActivity {
    private ScrollView weatherLayout;
    private TextView tvTitleCity;
    private TextView tvTitleUpdateTime;
    private TextView tvDegreeText;
    private TextView tvWeatherInfoText;
    private LinearLayout forecastLayout;
    private TextView tvAQIText;
    private TextView tvPM25Text;
    private TextView tvComfortText;
    private TextView tvCarWashText;
    private TextView tvSportText;
    private ImageView ivBingPic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= 21) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
        setContentView(R.layout.activity_weather);
        initViews();
    }

    private void initViews() {
        weatherLayout = (ScrollView) findViewById(R.id.weather_layout);
        tvTitleCity = (TextView) findViewById(R.id.tv_title_city);
        tvTitleUpdateTime = (TextView) findViewById(R.id.tv_title_update_time);
        tvDegreeText = (TextView) findViewById(R.id.tv_degree_text);
        tvWeatherInfoText = (TextView) findViewById(R.id.tv_weather_info_text);
        forecastLayout = (LinearLayout) findViewById(R.id.forecast_layout);
        tvAQIText = (TextView) findViewById(R.id.tv_aqi_text);
        tvPM25Text = (TextView) findViewById(R.id.tv_pm25_text);
        tvComfortText = (TextView) findViewById(R.id.tv_comfort_text);
        tvCarWashText = (TextView) findViewById(R.id.tv_car_wash_text);
        tvSportText = (TextView) findViewById(R.id.tv_sport_text);
        ivBingPic = (ImageView) findViewById(R.id.iv_bing_pic);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String weatherString = prefs.getString("weather", null);
        if (weatherString != null) {
            Weather weather = Utility.handleWeatherResponse(weatherString);
            showWeatherInfo(weather);
        } else {
            String weatherId = getIntent().getStringExtra("weather_id");
            weatherLayout.setVisibility(View.INVISIBLE);
            requestWeather(weatherId);
        }
        String bingPicString = prefs.getString("bing_pic", null);
        if (bingPicString != null) {
            Glide.with(this).load(bingPicString).into(ivBingPic);
        } else {
            loadBingPic();
        }
    }

    private void requestWeather(String weatherId) {
        String weatherUrl = "http://guolin.tech/api/weather?cityid=" +
                weatherId + "&key=bc0418b57b2d4918819d3974ac1285d9";
        HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(WeatherActivity.this, "获取天气信息失败",
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseText = response.body().string();
                final Weather weather = Utility.handleWeatherResponse(responseText);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (weather != null && weather.status.equals("ok")) {
                            SharedPreferences.Editor editor = PreferenceManager
                                    .getDefaultSharedPreferences(WeatherActivity.this).edit();
                            editor.putString("weather", responseText);
                            editor.apply();
                            showWeatherInfo(weather);
                        } else {
                            Toast.makeText(WeatherActivity.this, "获取天气信息失败",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
        loadBingPic();
    }

    private void showWeatherInfo(Weather weather) {
        String cityName = weather.basic.cityName;
        String updateTime = weather.basic.update.updateTime.split(" ")[1];
        String degree = weather.now.temperature + "\u2103";
        String weatherInfo = weather.now.more.info;
        tvTitleCity.setText(cityName);
        tvTitleUpdateTime.setText(updateTime);
        tvDegreeText.setText(degree);
        tvWeatherInfoText.setText(weatherInfo);
        forecastLayout.removeAllViews();
        for (Forecast forecast : weather.forecastList) {
            View view = LayoutInflater.from(this).
                    inflate(R.layout.activity_weather_forecast_item, forecastLayout, false);
            TextView tvDateText = (TextView) view.findViewById(R.id.tv_fi_date_text);
            TextView tvInfoText = (TextView) view.findViewById(R.id.tv_fi_info_text);
            TextView tvMaxText = (TextView) view.findViewById(R.id.tv_fi_max_text);
            TextView tvMinText = (TextView) view.findViewById(R.id.tv_fi_min_text);
            tvDateText.setText(forecast.date);
            tvInfoText.setText(forecast.more.info);
            tvMaxText.setText(forecast.temperature.max);
            tvMinText.setText(forecast.temperature.min);
            forecastLayout.addView(view);
        }
        if (weather.aqi != null) {
            tvAQIText.setText(weather.aqi.city.aqi);
            tvPM25Text.setText(weather.aqi.city.pm25);
        }
        String comfort = "舒适度：" + weather.suggestion.comfort.info;
        String carwash = "洗车指数：" + weather.suggestion.carwash.info;
        String sport = "运动指数：" + weather.suggestion.sport.info;
        tvComfortText.setText(comfort);
        tvCarWashText.setText(carwash);
        tvSportText.setText(sport);
        weatherLayout.setVisibility(View.VISIBLE);
    }

    private void loadBingPic() {
        String requestBingPic = "http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequest(requestBingPic, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String bingPic = response.body().string();
                SharedPreferences.Editor editor = PreferenceManager
                        .getDefaultSharedPreferences(WeatherActivity.this).edit();
                editor.putString("bing_pic", bingPic);
                editor.apply();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Glide.with(WeatherActivity.this).load(bingPic).into(ivBingPic);
                    }
                });
            }
        });
    }
}
