package com.kinetiqa.glacier.ui;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.kinetiqa.glacier.R;
import com.kinetiqa.glacier.components.MenuErrorMessage;
import com.kinetiqa.glacier.core.PointsManager;
import com.kinetiqa.glacier.core.SidebarManager;
import com.kinetiqa.glacier.core.StatisticsManager;
import com.kinetiqa.glacier.core.connection.receiver.AlarmReceiver;
import com.kinetiqa.glacier.database.DatabaseHelper;
import com.kinetiqa.glacier.dialogs.DialogInfo;
import com.kinetiqa.glacier.dialogs.DialogLogin;
import com.kinetiqa.glacier.menu.Menu;
import com.kinetiqa.glacier.menu.MenuFolder;
import com.kinetiqa.glacier.menu.MenuItem;
import com.kinetiqa.glacier.menu.MenuManager;
import com.kinetiqa.glacier.ui.components.SidebarItem;
import com.kinetiqa.glacier.utils.TimeConversion;
import java.util.List;


public class Home extends FragmentActivity {

    public static int IN_FOCUS = SidebarManager.MENU;

    public static Home h;
    private SharedPreferences sharedPreferences;
    private FrameLayout fragmentContainer;
    private Fragment fragment;

    private ListView sidebarListView;

    private RelativeLayout searchListContainer;
    private ListView searchListView;
    private List<MenuItem> searchedItems;
    private String searchString;
    private long lastSearchTime = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home);
        h = this;
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        checkLogin();
        setRecurringAlarm();
        initButtons();
        initSearchListeners();
        initSidebar();
        showView(SidebarManager.MENU);
    }

    @Override
    protected void onResume() {
        super.onResume();
        initUserProfile();
        initSidebar();
        StatisticsManager.getInstance(this).checkDailyAwards();
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (searchListContainer.getVisibility() == View.VISIBLE) {
            searchListContainer.setVisibility(View.GONE);
            return;
        }

        if (fragment != null) {
            if (fragment instanceof FragmentMainMenu) {
                ((FragmentMainMenu) fragment).onBackPress();
                return;
            }
        }

        super.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void checkLogin() {
        // Check if prompt for login is needed
        if (!sharedPreferences.getBoolean("skip_login", false)) {
            DialogLogin dialogLogin = new DialogLogin(Home.this, true);
            dialogLogin.show();
        }
    }

    private void initUserProfile() {
        ImageView profileIcon = (ImageView) findViewById(R.id.profile_icon);

        TextView profileName = (TextView) findViewById(R.id.profile_name);
        profileName.setText(sharedPreferences.getString("username", "Unknown"));

        TextView pointsTextView = (TextView) findViewById(R.id.points_available);
        pointsTextView.setText(String.valueOf(PointsManager.getInstance(this).getPoints()));
    }

    private void initButtons() {
        // Points container located in the top right corner displays the points
        RelativeLayout pointsButton = (RelativeLayout) findViewById(R.id.right_menu_points_container);
        pointsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO: display on click

            }
        });


        // User profile button is split into an ImageButton and TextView
        ImageButton userIconImageButton = (ImageButton) findViewById(R.id.profile_icon);
        userIconImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO: display on click

            }
        });

        TextView userIconTextView = (TextView) findViewById(R.id.profile_name);
        userIconImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO: display on click

            }
        });

        RelativeLayout settingsButton = (RelativeLayout) findViewById(R.id.nav_settings_container);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getApplicationContext(), Settings.class);
                startActivity(i);
            }
        });
    }

    private void initSearchListeners() {
        searchListContainer = (RelativeLayout) findViewById(R.id.search_list_container);
        searchListView = (ListView) findViewById(R.id.search_list);
        searchListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                Menu openedMenu = searchedItems.get(position);
                MenuErrorMessage errorMessage = MenuManager.getInstance().getOpenMenuErrorMessages(Home.this, openedMenu);
                if (errorMessage != null) {
                    DialogInfo infoDialog = new DialogInfo(Home.this, errorMessage.getTitle(), errorMessage.getDesc());
                    infoDialog.show();
                    return;
                }

                if (openedMenu instanceof MenuFolder) {
                    // TODO
                } else {
                    Intent i = null;
                    if (((MenuItem) openedMenu).getMediaType() == MenuManager.MEDIA_VIDEO) {
                        i = new Intent(Home.this, MediaVideo.class);
                        i.putExtra("id", openedMenu.getID());
                    } else if (((MenuItem) openedMenu).getMediaType() == MenuManager.MEDIA_DOCUMENT) {
                        i = new Intent(Home.this, MediaDocument.class);
                        i.putExtra("id", openedMenu.getID());
                    }

                    if (i != null) {
                        startActivity(i);
                    }
                }
            }
        });

        Drawable x = getResources().getDrawable(R.drawable.ic_action_remove_dark);
        x.setBounds(0, 0, x.getIntrinsicWidth(), x.getIntrinsicHeight());

        final EditText searchField = (EditText) findViewById(R.id.search_field);
        searchField.setCompoundDrawables(null, null, searchField.getText()
                .toString().equals("") ? null : x, null);

        searchField.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                searchString = s.toString();
                if (searchString.equals("")) {
                    searchListContainer.setVisibility(View.GONE);
                } else {
                    searchListContainer.setVisibility(View.VISIBLE);
                    if (lastSearchTime < 0 || (System.currentTimeMillis() - lastSearchTime) > 1000) {
                        lastSearchTime = System.currentTimeMillis();
                        SearchTask searchTask = new SearchTask();
                        searchTask.execute();
                    }
                }
            }

            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
            }
        });
    }

    public void initSidebar() {
        sidebarListView = (ListView) findViewById(R.id.nav_side_menu);
        sidebarListView.setAdapter(sideMenuAdapter);
        sidebarListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                SidebarManager.setInFocus(Home.this, position);
                SidebarItem sidebarItem = SidebarManager.getSidebarItem(Home.this, position);

                showView(sidebarItem.getId());
            }
        });
    }

    public void showView(int inFocusID) {
        Button backButton = (Button) findViewById(R.id.nav_back_btn);
        backButton.setVisibility(View.GONE);

        ImageButton homeButton = (ImageButton) findViewById(R.id.nav_home_btn);
        homeButton.setVisibility(View.VISIBLE);

        IN_FOCUS = inFocusID;
        switch (inFocusID) {
            case SidebarManager.MENU:
                fragment = new FragmentMainMenu();
                break;
            case SidebarManager.FAVOURITES:
                fragment = new FragmentFavourites();
                break;
            case SidebarManager.COMPOSE_NEW_MESSAGE:
                fragment = new FragmentComposeMessage();
                break;
            case SidebarManager.INBOX:
                fragment = new FragmentInbox();
                break;
            case SidebarManager.OUTBOX:
                fragment = new FragmentOutbox();
                break;
            case SidebarManager.LEADERS:
                fragment = new FragmentLeaders();
                break;
            case SidebarManager.STATS:
                fragment = new FragmentStats();
                break;
        }

        if (fragment != null) {
            transitionFragment();
            sidebarListView.setAdapter(sideMenuAdapter);
        }
    }

    private void transitionFragment() {
        transitionFragmentOut();
    }

    private void transitionFragmentOut() {
        Animation fadeOutAnimation = AnimationUtils.loadAnimation(this, R.anim.dialog_fade_out);
        fadeOutAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                FragmentManager fm = getSupportFragmentManager();
                FragmentTransaction transaction = fm.beginTransaction();
                transaction.replace(R.id.main_fragment, fragment);
                transaction.commit();

                transitionFragmentIn();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        if (fragmentContainer != null) {
            fragmentContainer.startAnimation(fadeOutAnimation);
        } else {
            fragmentContainer = (FrameLayout) findViewById(R.id.main_fragment);
            FragmentManager fm = getSupportFragmentManager();
            FragmentTransaction transaction = fm.beginTransaction();
            transaction.replace(R.id.main_fragment, fragment);
            transaction.commit();
        }
    }

    private void transitionFragmentIn() {
        Animation fadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.dialog_fade_in);
        fadeInAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        if (fragmentContainer != null) {
            fragmentContainer.startAnimation(fadeInAnimation);
        }
    }


    /**
     * Sets a service to sync with server
     */
    private void setRecurringAlarm() {
        Intent alarmReceiver = new Intent(Home.this, AlarmReceiver.class);
        PendingIntent syncIntent = PendingIntent.getBroadcast(getApplicationContext(),
                0, alarmReceiver, PendingIntent.FLAG_CANCEL_CURRENT);

        AlarmManager alarms = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarms.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 1000 * 60 * 20, syncIntent);
    }

    // ------------------------------------------------------------------------

    private BaseAdapter sideMenuAdapter = new BaseAdapter() {
        @Override
        public int getCount() {
            return SidebarManager.NUM_DISPLAYED_SIDEBAR_ITEMS;
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
                    R.layout.item_side_menu, parent, false);

            SidebarItem sidebarItem = SidebarManager.getSidebarItem(Home.this, position);

            TextView title = (TextView) view.findViewById(R.id.item_menu_text);
            title.setText(sidebarItem.getTitle());

            ImageView typeIcon = (ImageView) view
                    .findViewById(R.id.item_menu_icon);
            typeIcon.setImageResource(sidebarItem.getIcon());

            RelativeLayout sidebarItemContainer = (RelativeLayout) view.findViewById(R.id.item_side_menu_container);
            if (IN_FOCUS == sidebarItem.getId()) {
                // The current element is focused
                sidebarItemContainer.setBackgroundResource(R.drawable.bg_blue_selected);
            } else {
                sidebarItemContainer.setBackgroundResource(R.drawable.bg_blue_selector);
            }

            return view;
        }
    };

    private BaseAdapter searchMenuAdapter = new BaseAdapter() {

        @Override
        public int getCount() {
            return searchedItems.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            View view = LayoutInflater.from(parent.getContext()).inflate(
                    R.layout.item_main_menu, null);

            if (searchString == null) {
                searchString = "";
            }
            MenuItem searchItem = searchedItems.get(position);
            TextView title = (TextView) view.findViewById(R.id.item_menu_title);
            String highlightedTitle = searchItem.getTitle().replaceAll(searchString, "<b>" + searchString + "</b>");
            title.setText(Html.fromHtml(highlightedTitle));

            TextView desc = (TextView) view.findViewById(R.id.item_menu_desc);
            String highlightedDesc = searchItem.getDesc().replaceAll(searchString, "<b>" + searchString + "</b>");
            if (searchItem.getMediaType() == MenuManager.MEDIA_VIDEO) {
                long durationOfVideo = searchItem.getVideoLength();
                desc.setText(Html.fromHtml("(" + TimeConversion.convertMillisecondsToTime(durationOfVideo) + ") " + highlightedDesc));
            } else {
                desc.setText(Html.fromHtml(highlightedDesc));
            }


            int numSecondsViewingMenu = searchItem.getInteractionTime();
            TextView achievement_minutes = (TextView) view
                    .findViewById(R.id.item_menu_stats_time);
            achievement_minutes.setText(TimeConversion
                    .secondsToTime(numSecondsViewingMenu));

            RelativeLayout statsCompletedContainer = (RelativeLayout) view.findViewById(R.id.item_menu_stats_completed_container);
            statsCompletedContainer.setVisibility(View.GONE);


            ImageView typeImageView = (ImageView) view.findViewById(R.id.item_menu_type);
            // ImageView completedImageView = (ImageView) view.findViewById(R.id.item_menu_completed);
            ImageView favouritedImageView = (ImageView) view.findViewById(R.id.item_menu_favourited);

            boolean isCompleted = searchItem.isCompleted();
            if (isCompleted) {
                // completedImageView.setVisibility(View.VISIBLE);
                if (((MenuItem) searchItem).getMediaType() == MenuManager.MEDIA_VIDEO) {
                    typeImageView.setImageResource(R.drawable.ic_action_video_green_complete);
                } else if (((MenuItem) searchItem).getMediaType() == MenuManager.MEDIA_DOCUMENT) {
                    typeImageView.setImageResource(R.drawable.ic_action_doc_green_complete);
                }

            } else {
                if (((MenuItem) searchItem).getMediaType() == MenuManager.MEDIA_VIDEO) {
                    typeImageView.setImageResource(R.drawable.ic_action_video_green);
                } else if (((MenuItem) searchItem).getMediaType() == MenuManager.MEDIA_DOCUMENT) {
                    typeImageView.setImageResource(R.drawable.ic_action_doc_green);
                }

            }

            if (searchItem.isBookmarked()) {
                favouritedImageView.setVisibility(View.VISIBLE);
            }

            ImageView lockedImageView = (ImageView) view.findViewById(R.id.item_menu_locked);
            if (!searchItem.isPermitted()) {
                lockedImageView.setVisibility(View.VISIBLE);
            }

            return view;
        }

    };


    private class SearchTask extends AsyncTask<Void, Void, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(Void... voids) {
            searchedItems = DatabaseHelper.getInstance(getApplicationContext()).searchMenuItems(searchString);
            return "";
        }

        @Override
        protected void onPostExecute(String result) {
            searchListView.setAdapter(searchMenuAdapter);
        }
    }
}
