package com.kinetiqa.glacier.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.kinetiqa.glacier.R;
import com.kinetiqa.glacier.core.StatisticsManager;
import com.kinetiqa.glacier.database.DatabaseHelper;
import com.kinetiqa.glacier.menu.MenuItem;
import com.kinetiqa.glacier.menu.MenuManager;
import com.kinetiqa.glacier.utils.TimeConversion;

import java.util.List;


public class FragmentStats extends Fragment {
    private SharedPreferences sharedPreferences;

    private View fragmentView;
    private static List<MenuItem> mostViewedMenuItems;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.stats_fragment, container, false);
        this.fragmentView = view;
        init();
        return view;
    }

    private void init() {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

        initAwards();
        initMainStats();
        initMostViewed();
    }

    private void initAwards() {
        ImageView award1stImageView = (ImageView) fragmentView.findViewById(R.id.award_1st);
        int totalItemsCompleted = StatisticsManager.getInstance(getActivity()).getNumCompletedItemsUnderMenuFolder(MenuManager.getInstance().getRootMenuFolder());
        if (totalItemsCompleted >= 1) {
            award1stImageView.setImageResource(R.drawable.award_1st_video);
        }

        ImageView award2DaysImageView = (ImageView) fragmentView.findViewById(R.id.award_2_days);
        if (sharedPreferences.getBoolean("two_consecutive_days", false)) {
            award2DaysImageView.setImageResource(R.drawable.award_2_days);
        }

        ImageView award7DaysImageView = (ImageView) fragmentView.findViewById(R.id.award_7_days);
        if (sharedPreferences.getBoolean("seven_consecutive_days", false)) {
            award7DaysImageView.setImageResource(R.drawable.award_7_days);
        }

        ImageView award50PointsImageView = (ImageView) fragmentView.findViewById(R.id.award_50_points);
        if (sharedPreferences.getBoolean("award_50", false)) {
            award50PointsImageView.setImageResource(R.drawable.award_50);
        }

        ImageView award100PointsImageView = (ImageView) fragmentView.findViewById(R.id.award_100_points);
        if (sharedPreferences.getBoolean("award_100", false)) {
            award100PointsImageView.setImageResource(R.drawable.award_100);
        }

        ImageView award500PointsImageView = (ImageView) fragmentView.findViewById(R.id.award_500_points);
        if (sharedPreferences.getBoolean("award_500", false)) {
            award500PointsImageView.setImageResource(R.drawable.award_500);
        }

        ImageView award1000PointsImageView = (ImageView) fragmentView.findViewById(R.id.award_1000_points);
        if (sharedPreferences.getBoolean("award_1000", false)) {
            award1000PointsImageView.setImageResource(R.drawable.award_1000);
        }

        ImageView award10000PointsImageView = (ImageView) fragmentView.findViewById(R.id.award_10000_points);
        if (sharedPreferences.getBoolean("award_10000", false)) {
            award10000PointsImageView.setImageResource(R.drawable.award_10000);
        }
    }

    private void initMainStats() {
        long lifeTimeMinutes = DatabaseHelper.getInstance(getActivity())
                .getTimeSpentLifetime();
        lifeTimeMinutes = Math.round(lifeTimeMinutes / 60);

        long lastWeekMinutes = DatabaseHelper.getInstance(getActivity()).getTimeSpentWatchingPastWeek();

        long lifeTimePoints = (int) sharedPreferences.getLong("points", 0);

        long numberDaysPracticed = DatabaseHelper.getInstance(getActivity()).getNumberDaysPracticed();
        if (numberDaysPracticed == 0) {
            numberDaysPracticed = 1;
        }
        float numberWeeksPracticed = (float) numberDaysPracticed / 7;
        if (numberWeeksPracticed == 0) {
            numberWeeksPracticed = 1;
        }
        int lifetimeMessages = DatabaseHelper.getInstance(getActivity()).getNumTotalMessages();

        TextView award_stat_lifetime_text = (TextView) fragmentView.findViewById(R.id.award_stat_lifetime_text);
        award_stat_lifetime_text.setText(String.valueOf(lifeTimeMinutes));

        TextView award_stat_avgtimeweekly_text = (TextView) fragmentView.findViewById(R.id.award_stat_avgtimeweekly_text);
        Integer averageWeeklyPracticed = Math.round(lifeTimeMinutes
                / numberWeeksPracticed);
        award_stat_avgtimeweekly_text.setText(String
                .valueOf(averageWeeklyPracticed));

        TextView award_stat_avgpointsweekly_text = (TextView) fragmentView.findViewById(R.id.award_stat_avgpointsweekly_text);
        Integer averageWeeklyPoints = Math.round(lifeTimePoints
                / numberWeeksPracticed);
        award_stat_avgpointsweekly_text.setText(String
                .valueOf(averageWeeklyPoints));

        TextView award_stat_time_last_week_text = (TextView) fragmentView.findViewById(R.id.award_stat_time_last_week_text);
        award_stat_time_last_week_text.setText(String
                .valueOf(lastWeekMinutes));

        TextView award_stat_avgtimedaily_text = (TextView) fragmentView.findViewById(R.id.award_stat_avgtimedaily_text);
        Integer averageDailyPracticed = Math.round(lifeTimeMinutes
                / numberDaysPracticed);
        award_stat_avgtimedaily_text.setText(String
                .valueOf(averageDailyPracticed));

        TextView award_stat_avgpointsdaily_text = (TextView) fragmentView.findViewById(R.id.award_stat_avgpointsdaily_text);
        Integer averageDailyPoints = Math.round(lifeTimePoints
                / numberDaysPracticed);
        award_stat_avgpointsdaily_text.setText(String
                .valueOf(averageDailyPoints));

        TextView award_stat_messages_text = (TextView) fragmentView.findViewById(R.id.award_stat_messages_text);
        award_stat_messages_text.setText(String.valueOf(lifetimeMessages));

        renderItemCompletionProgressBar();
    }

    /**
     * Renders the item progress bar
     */
    private void renderItemCompletionProgressBar() {
        int menuItemsCompleted = StatisticsManager.getInstance(getActivity()).getNumCompletedItemsUnderMenuFolder(MenuManager.getInstance().getRootMenuFolder());
        int menuItems = StatisticsManager.getInstance(getActivity()).getNumItemsUnderMenuFolder(MenuManager.getInstance().getRootMenuFolder());
        TextView award_stat_item_progress_text = (TextView) fragmentView.findViewById(R.id.award_stat_item_progress_text);
        award_stat_item_progress_text.setText(menuItemsCompleted + "/"
                + menuItems);

        float decimalPercentage = (menuItemsCompleted / (float) menuItems);

        // Sets progress bar layout width
        LinearLayout progress_bar = (LinearLayout) fragmentView.findViewById(R.id.main_progress_bar_progress);
        progress_bar.setLayoutParams(new LinearLayout.LayoutParams(0, 25,
                decimalPercentage));
    }

    private void initMostViewed() {
        mostViewedMenuItems = StatisticsManager.getInstance(getActivity()).getMostViewedMenuItems();

        ListView mostViewedListView = (ListView) fragmentView.findViewById(R.id.stats_most_viewed_list);
        mostViewedListView.setAdapter(mostViewedAdapter);
        mostViewedListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                // TODO
            }
        });
    }

    private BaseAdapter mostViewedAdapter = new BaseAdapter() {
        @Override
        public int getCount() {
            return mostViewedMenuItems.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = LayoutInflater.from(parent.getContext()).inflate(
                    R.layout.item_most_viewed, parent, false);

            MenuItem menuItem = mostViewedMenuItems.get(position);

            TextView title = (TextView) view.findViewById(R.id.item_mvd_title);
            title.setText(menuItem.getTitle());

            TextView info = (TextView) view.findViewById(R.id.item_mvd_info);
            info.setText(menuItem.getNumViews() + " views");

            return view;
        }
    };
}
