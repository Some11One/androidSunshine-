package com.ndmelentev.sunshine;

import android.app.Activity;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.menu.ActionMenuItemView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.google.gson.internal.LinkedTreeMap;
import com.ndmelentev.sunshine.weatherStructure.City;

import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.QueryMap;

interface OpenWeatherMapService {
    @GET("data/2.5/forecast/daily")
    Call<Forecast> fetchWeather(@QueryMap Map<String, String> query);
}

class Forecast {
    City city;
    List list;

    @Override
    public String toString() {
        return "list";
    }

}

public class ForecastFragment extends Fragment implements Callback<Forecast> {

    // saved cities key for sharedPreferences
    private final static String SAVED_CITIES_KEY = "SAVED_CITIES";
    // log
    private final String LOG_TAG = ForecastFragment.class.getSimpleName();
    // call back
    private OnAnotherFragmentClickedListener mCallback;
    // adapter
    private ArrayAdapter<String> arrayAdapter;

    // default constructor
    public ForecastFragment() {
    }

    @Override
    public void onAttach(Activity context) {
        super.onAttach(context);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mCallback = (OnAnotherFragmentClickedListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnHeadlineSelectedListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_forecast, container, false);

        // create adapter
        arrayAdapter = new ArrayAdapter<>(
                getActivity(),
                R.layout.list_item_forecast,
                R.id.list_item_forecast_textview,
                new ArrayList<String>());

        // finding listView
        ListView listView = (ListView) rootView.findViewById(R.id.listview_forecast);

        // set ArrayAdapter on ListView
        listView.setAdapter(arrayAdapter);

        // set onClickListener for adapter children
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String forecast = arrayAdapter.getItem(position);
                mCallback.onDetailsSelected(forecast);
            }
        });

        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.forecastfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            updateWeather();
            return true;
        }
        if (id == R.id.action_settings) {
            mCallback.onSettingsSelected();
            return true;
        }
        if (id == R.id.action_viewMap) {
            openPreferredLocationInMap();
            return true;
        }
        if (id == R.id.action_choose_city){
            chooseCity();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // let user decide witch city weather to show
    private void chooseCity(){

        // get saved cities
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        final Set<String> savedCity = sharedPreferences.getStringSet(SAVED_CITIES_KEY,  new HashSet<String>());

        // no saved cities
        if (savedCity.size() == 0){
            Toast.makeText(getActivity(), "No saved cities", Toast.LENGTH_SHORT).show();
            return;
        }

        // prepare dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        CharSequence[] charSequences = new CharSequence[savedCity.size()];
        int index = 0;
        for (String f : savedCity) {
            charSequences[index] = getCity(f);
            index++;
        }
        builder.setTitle("Choose city")
                .setItems(charSequences, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String newCity = (String) savedCity.toArray()[which];
                        updateWeather(newCity);
                        sharedPreferences.edit().putString(getActivity().getString(R.string.pref_location_key)
                                , newCity).apply();
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    // get City name from postal code
    public String getCity(String postal){
        switch (postal){
            case "143320": return Cities.MOSCOW.getName();
            case "198504": return Cities.SAINT_PETERSBURG.getName();
            case "141070": return Cities.KOROLEV.getName();
            default:
                return "";
        }
    }

    // fetch weather using postal code
    private void updateWeather(String postalCode){
        fetchWeatherAsync(postalCode);
    }

    // fetch weather using postal code from sharedPreferences
    private void updateWeather() {
        // load shared Preferences
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

        // checking current postal code preference
        final String weatherPostalCode = sharedPreferences.getString(getActivity().getString(R.string.pref_location_key)
                , getActivity().getString(R.string.pref_location_default));

        // check if user wants to save city
        final Set<String> savedCity = sharedPreferences.getStringSet(SAVED_CITIES_KEY,  new HashSet<String>());
        if (!savedCity.contains(weatherPostalCode)){
            // ask to add city
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage("Save location: " + getCity(weatherPostalCode) + "?")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Set<String> newSavedCities = new HashSet<>(savedCity.size() + 1);
                            newSavedCities.addAll(savedCity);
                            newSavedCities.add(weatherPostalCode);
                            sharedPreferences.edit().putStringSet(SAVED_CITIES_KEY, newSavedCities).apply();
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener(){
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    });
            AlertDialog dialog = builder.create();
            dialog.show();
        }

        // fetching weather
        updateWeather(weatherPostalCode);
    }

    private void openPreferredLocationInMap() {
        // postal code from sharedPreferences
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String weatherPostalCode = sharedPreferences.getString(getActivity().getString(R.string.pref_location_key)
                , getActivity().getString(R.string.pref_location_default));

        // geolocation from postal code
        Uri geoLocation = Uri.parse("geo:0,0?").buildUpon()
                .appendQueryParameter("q", String.valueOf(weatherPostalCode))
                .build();

        // build intent
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(geoLocation);
        if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Log.d(LOG_TAG, "Couldn't call " + geoLocation + ", no map app");
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        updateWeather();
    }

    // Fetching weather using Retrofit
    public void fetchWeatherAsync(String postalCode) {

        // no postal code
        if (postalCode == null)
            return;

        // Will contain the raw JSON response as a string.
        String forecastJsonStr = null;

        // Call
        Call<Forecast> call = null;

        // Url query data
        String format = "json";
        String units = "metric";
        String weatherApiKey = "532e8988afaac5282222643392cc8fd7";
        int numDays = 7;

        try {

            // Base URL
            final String FORECAST_BASE_URL = "http://api.openweathermap.org/";

            // Query parameters
            final String QUERY_PARAM = "q";
            final String FORMAT_PARAM = "mode";
            final String UNITS_PARAM = "units";
            final String DAYS_PARAM = "cnt";
            final String API_KEY_PARAM = "APPID";

            // building Uri
            Map<String, String> queryParams = new HashMap<>();
            queryParams.put(QUERY_PARAM, postalCode);
            queryParams.put(FORMAT_PARAM, format);
            queryParams.put(UNITS_PARAM, units);
            queryParams.put(DAYS_PARAM, String.valueOf(numDays));
            queryParams.put(API_KEY_PARAM, weatherApiKey);

            // Weather url
            URL urlBase = new URL(FORECAST_BASE_URL);

            // create retrofit call
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(urlBase.toString())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            OpenWeatherMapService service = retrofit.create(OpenWeatherMapService.class);
            call = service.fetchWeather(queryParams);
            call.enqueue(this);

        } catch (IOException e) {
            Log.e(LOG_TAG, "Error ", e);
            e.printStackTrace();
        }
    }

    // response from OpenWeatherMap.com
    @Override
    public void onResponse(Call<Forecast> call, Response<Forecast> response) {

        // clear weather view
        arrayAdapter.clear();

        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        final String weatherPostalCode = sharedPreferences.getString(getActivity().getString(R.string.pref_location_key)
                , getActivity().getString(R.string.pref_location_default));
        String cityName = getCity(weatherPostalCode);
        ((ActionMenuItemView)getActivity().findViewById(R.id.action_choose_city)).setTitle("CITY\\" + cityName);

        // day
        int i = 0;

        if ((response.body() == null) || (response.body().list == null)){
            Toast.makeText(getActivity(), "No data found for the city", Toast.LENGTH_SHORT).show();
            return;
        }

        // get weather
        for (Object obj : response.body().list) {
            LinkedTreeMap dayForecast = (LinkedTreeMap) obj;

            // data to be retrieved
            String day;
            String description;
            String highAndLow;

            // day

            //create a Gregorian Calendar, which is in current date
            GregorianCalendar gc = new GregorianCalendar();
            //add i dates to current date of calendar
            gc.add(GregorianCalendar.DATE, i);
            //get that date, format it, and "save" it on variable day
            Date time = gc.getTime();
            SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd", Locale.US);
            day = shortenedDateFormat.format(time);

            // description

            description = (String) ((LinkedTreeMap) ((ArrayList) dayForecast.get("weather")).get(0)).get("description");

            // highAndLow

            double high = (double) ((LinkedTreeMap) dayForecast.get("temp")).get("max");
            double low = (double) ((LinkedTreeMap) dayForecast.get("temp")).get("min");
            // check if user wants temperature in fahrenheit
            String temperatureOption = sharedPreferences.getString(getActivity().getString(R.string.pref_temperature_key),
                    getActivity().getString(R.string.pref_temperature_default));
            if (temperatureOption.equals("2")) {
                high = celsiusToFahrenheit(high);
                low = celsiusToFahrenheit(low);
            }
            highAndLow = formatHighLows(high, low);

            // putting it all together

            arrayAdapter.add(day + " - " + description + " - " + highAndLow);
            i++;
        }
    }

    // Failure of request to OpenWeatherMap.com
    @Override
    public void onFailure(Call<Forecast> call, Throwable t) {
        Toast.makeText(getActivity(), t.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
    }

    /**
     * Prepare the weather high/lows for presentation.
     */
    private String formatHighLows(double high, double low) {
        return Math.round(high) + "/" + Math.round(low);
    }

    // Convert celsius to fahrenheit
    private double celsiusToFahrenheit(double high) {
        return high * 1.8 + 32;
    }

    // cities and their postal code
    enum Cities{
        MOSCOW{
            public String getPostal(){
                return "143320";
            }
            public String getName(){
                return "Moscow";
            }
        }, SAINT_PETERSBURG{
            public String getPostal(){
                return "198504";
            }
            public String getName(){
                return "Saint Petersburg";
            }
        }, KOROLEV{
            public String getPostal(){
                return "141070";
            }
            public String getName(){
                return "Korolev";
            }
        };

        public abstract String getPostal();
        public abstract String getName();
    }

    // Container Activity must implement this interface
    public interface OnAnotherFragmentClickedListener {
        void onDetailsSelected(String info);

        void onSettingsSelected();
    }

}
