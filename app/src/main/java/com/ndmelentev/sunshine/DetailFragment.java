package com.ndmelentev.sunshine;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class DetailFragment extends Fragment {

    // details key
    public static final String DETAILS_KEY = "INFO";
    // log
    private static final String LOG_TAG = "DetailFragment";
    // share #
    private static final String FORECAST_SHARE_HASHTAG = "#SunshineApp";
    // call back
    OnAnotherFragmentClickedListener mCallback;
    // forecast
    private String mForecastString;

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
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.detailfragment, menu);

        // share
        MenuItem menuItem = menu.findItem(R.id.action_share);
        shareWeather(menuItem);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings){
            mCallback.onSettingsSelected();
        }
        return super.onOptionsItemSelected(item);
    }

    private void shareWeather(MenuItem item) {
        // get the provider and hold onto it to set/change the share intent
        ShareActionProvider mShareActionProvider =
                (ShareActionProvider) MenuItemCompat.getActionProvider(item);

        if (mShareActionProvider != null){
            mShareActionProvider.setShareIntent(createShareForecastIntent());
        }
        else{
            Log.d(LOG_TAG, "Share Action Provider is null?");
        }

    }

    private Intent createShareForecastIntent(){
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT,
                mForecastString + FORECAST_SHARE_HASHTAG);
        return shareIntent;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_detail, container, false);

        // get details data
        Bundle args = getArguments();
        mForecastString = args.getString(DETAILS_KEY);
        ((TextView) rootView.findViewById(R.id.detail_text))
                .setText(mForecastString);

        return rootView;
    }

    // update data
    public void updateDetailView(String info){
        mForecastString = info;
        ((TextView) getActivity().findViewById(R.id.detail_text))
                .setText(mForecastString);
    }

    // Container Activity must implement this interface
    public interface OnAnotherFragmentClickedListener {
        void onSettingsSelected();
    }
}
