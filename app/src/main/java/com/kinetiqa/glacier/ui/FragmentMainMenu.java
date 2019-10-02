package com.kinetiqa.glacier.ui;


import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.kinetiqa.glacier.R;
import com.kinetiqa.glacier.components.MenuErrorMessage;
import com.kinetiqa.glacier.core.Config;
import com.kinetiqa.glacier.core.StatisticsManager;
import com.kinetiqa.glacier.database.DatabaseHelper;
import com.kinetiqa.glacier.dialogs.DialogInfo;
import com.kinetiqa.glacier.menu.Menu;
import com.kinetiqa.glacier.menu.MenuFolder;
import com.kinetiqa.glacier.menu.MenuItem;
import com.kinetiqa.glacier.menu.MenuManager;
import com.kinetiqa.glacier.utils.TimeConversion;
import com.kinetiqa.glacier.utils.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;


public class FragmentMainMenu extends Fragment {

    private View view;
    private RelativeLayout mainAreaLayout;
    private ListView mainMenuListView;
    private List<Menu> currentSubMenus;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.menu_fragment, container, false);
        this.view = view;
        init();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mainMenuListView != null && currentSubMenus != null) {
            InitMenuTask initMenuTask = new InitMenuTask();
            initMenuTask.execute();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    public void onBackPress() {
        MenuManager.getInstance().closeMenu();
        init();
    }

    private void init() {
        mainAreaLayout = (RelativeLayout) getActivity().findViewById(R.id.main_area);

        MenuFolder currentMenu = MenuManager.getInstance().getCurrentMenuFolder();
        boolean isRootMenu = MenuManager.getInstance().isRootMenuFolder(currentMenu);
        currentSubMenus = currentMenu.getSubMenus();
        InitMenuTask initMenuTask = new InitMenuTask();
        initMenuTask.execute();

        Button backButton = (Button) getActivity().findViewById(R.id.nav_back_btn);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPress();
            }
        });
        ImageButton homeButton = (ImageButton) getActivity().findViewById(R.id.nav_home_btn);
        if (isRootMenu) {
            backButton.setVisibility(View.GONE);
            homeButton.setVisibility(View.VISIBLE);
        } else {
            backButton.setVisibility(View.VISIBLE);
            homeButton.setVisibility(View.GONE);
        }

        TextView title = (TextView) view.findViewById(R.id.main_title);
        title.setText(currentMenu.getTitle());

        TextView desc = (TextView) view.findViewById(R.id.main_desc);
        desc.setText(currentMenu.getDesc());

        mainMenuListView = (ListView) view.findViewById(R.id.main_list_menu);
        mainMenuListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                Menu openedMenu = MenuManager.getInstance().getCurrentMenuFolder().getSubMenus().get(position);
                MenuErrorMessage errorMessage = MenuManager.getInstance().getOpenMenuErrorMessages(getActivity(), openedMenu);
                if (errorMessage != null) {
                    DialogInfo infoDialog = new DialogInfo(getActivity(), errorMessage.getTitle(), errorMessage.getDesc());
                    infoDialog.show();
                    return;
                }

                if (openedMenu instanceof MenuFolder) {
                    MenuManager.getInstance().openedMenu(openedMenu);
                    init();
                } else {
                    Intent i = null;
                    if (((MenuItem) openedMenu).getMediaType() == MenuManager.MEDIA_VIDEO) {
                        i = new Intent(getActivity(), MediaVideo.class);
                        i.putExtra("id", openedMenu.getID());
                    } else if (((MenuItem) openedMenu).getMediaType() == MenuManager.MEDIA_DOCUMENT) {
                        i = new Intent(getActivity(), MediaDocument.class);
                        i.putExtra("id", openedMenu.getID());
                    }

                    if (i != null) {
                        startActivity(i);
                    }
                }
            }
        });
    }

    private void initMainMenuBreadcrumb() {
        LinearLayout breadcrumbContainer = (LinearLayout) view.findViewById(R.id.breadcrumb_container);
        breadcrumbContainer.removeAllViews();

        List<Menu> breadcrumbMenuList = MenuManager.getInstance().getBreadcrumbMenuList();
        for (int i = 0; i < breadcrumbMenuList.size(); i++) {
            Menu menu = breadcrumbMenuList.get(i);

            TextView tv = new TextView(getActivity());
            tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
            if (i == (breadcrumbMenuList.size() - 1)) {
                tv.setTextColor(view.getResources().getColor(R.color.blue900));
            } else {
                tv.setTextColor(view.getResources().getColor(R.color.blue700));
            }
            tv.setText(menu.getTitle());

            TextView tvDivider = new TextView(getActivity());
            tvDivider.setText(" / ");
            tvDivider.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
            tvDivider.setTextColor(view.getResources().getColor(R.color.blue700));

            breadcrumbContainer.addView(tv);
            if (i < (breadcrumbMenuList.size() - 1)) {
                breadcrumbContainer.addView(tvDivider);
            }
        }
    }

    private void initMainMenuStats() {
        MenuFolder currentMenuFolder = MenuManager.getInstance().getCurrentMenuFolder();

        Integer timeWatchingSeconds = StatisticsManager.getInstance(getActivity()).getNumSecondsViewingMenu(currentMenuFolder);
        String timeWatchingContext = "seconds";
        String timeWatchedStr = "0";
        float timeWatched = timeWatchingSeconds;
        if (timeWatched >= 60) {
            timeWatchedStr = String.valueOf(Math.round((timeWatched / 60) * 10) / 10);
            timeWatchingContext = "minutes";
        } else if (timeWatched > 0) {
            timeWatchedStr = String.valueOf(timeWatched);
        }

        TextView achievementTime = (TextView)
                view.findViewById(R.id.stats_practiced);
        achievementTime.setText(timeWatchedStr);
        TextView achievementTimeContext = (TextView)
                view.findViewById(R.id.stats_practiced_desc);
        achievementTimeContext.setText(timeWatchingContext);

        Integer itemsCompleted = StatisticsManager.getInstance(getActivity()).getNumCompletedItemsUnderMenuFolder(currentMenuFolder);
        Integer totalItems = StatisticsManager.getInstance(getActivity()).getNumItemsUnderMenuFolder(currentMenuFolder);

        TextView pointsTextView = (TextView) view.findViewById(R.id.stats_completed);
        pointsTextView.setText(itemsCompleted + "/" + totalItems);

        // Check if the menu has been completed
        if (!currentMenuFolder.isCompleted()) {
            if (itemsCompleted == totalItems) {
                DatabaseHelper.getInstance(getActivity()).setCompleted(currentMenuFolder.getID());
            }
        }
    }


    private BaseAdapter mainMenuAdapter = new BaseAdapter() {

        @Override
        public int getCount() {
            return currentSubMenus.size();
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

            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_main_menu, parent, false);

            Menu menu = currentSubMenus.get(position);

            TextView title = (TextView) view.findViewById(R.id.item_menu_title);
            title.setText(menu.getTitle());

            TextView desc = (TextView) view.findViewById(R.id.item_menu_desc);
            if (menu instanceof MenuItem) {
                if (((MenuItem) menu).getMediaType() == MenuManager.MEDIA_VIDEO) {
                    long durationOfVideo = ((MenuItem) menu).getVideoLength();
                    desc.setText("(" + TimeConversion.convertMillisecondsToTime(durationOfVideo) + ") " + menu.getDesc());
                } else {
                    desc.setText(menu.getDesc());
                }
            } else {
                desc.setText(menu.getDesc());
            }

            TextView achievement_minutes = (TextView) view.findViewById(R.id.item_menu_stats_time);
            achievement_minutes.setText(TimeConversion.secondsToTime(menu.getInteractionTime()));

            RelativeLayout statsCompletedContainer = (RelativeLayout) view.findViewById(R.id.item_menu_stats_completed_container);
            if (menu instanceof MenuFolder) {
                statsCompletedContainer.setVisibility(View.VISIBLE);
                Integer itemsCompleted = StatisticsManager.getInstance(getActivity()).getNumCompletedItemsUnderMenuFolder((MenuFolder) menu);
                Integer totalItems = StatisticsManager.getInstance(getActivity()).getNumItemsUnderMenuFolder((MenuFolder) menu);
                TextView pointsTextView = (TextView) view
                        .findViewById(R.id.item_menu_stats_completed);
                pointsTextView.setText(itemsCompleted + "/" + totalItems);
            } else {
                statsCompletedContainer.setVisibility(View.GONE);
            }

            ImageView typeImageView = (ImageView) view.findViewById(R.id.item_menu_type);
            // ImageView completedImageView = (ImageView) view.findViewById(R.id.item_menu_completed);
            ImageView favouritedImageView = (ImageView) view.findViewById(R.id.item_menu_favourited);

            boolean isCompleted = menu.isCompleted();
            if (isCompleted) {
                // completedImageView.setVisibility(View.VISIBLE);
                if (menu instanceof MenuFolder) {
                    typeImageView.setImageResource(R.drawable.ic_action_collection_blue_complete);
                } else {
                    if (((MenuItem) menu).getMediaType() == MenuManager.MEDIA_VIDEO) {
                        typeImageView.setImageResource(R.drawable.ic_action_video_green_complete);
                    } else if (((MenuItem) menu).getMediaType() == MenuManager.MEDIA_DOCUMENT) {
                        typeImageView.setImageResource(R.drawable.ic_action_doc_green_complete);
                    }
                }
            } else {
                if (menu instanceof MenuFolder) {
                    typeImageView.setImageResource(R.drawable.ic_action_collection_blue);
                } else {
                    if (((MenuItem) menu).getMediaType() == MenuManager.MEDIA_VIDEO) {
                        typeImageView.setImageResource(R.drawable.ic_action_video_green);
                    } else if (((MenuItem) menu).getMediaType() == MenuManager.MEDIA_DOCUMENT) {
                        typeImageView.setImageResource(R.drawable.ic_action_doc_green);
                    }
                }
            }

            boolean isBookmarked = menu.isBookmarked();
            if (isBookmarked) {
                favouritedImageView.setVisibility(View.VISIBLE);
            }

            ImageView lockedImageView = (ImageView) view.findViewById(R.id.item_menu_locked);
            boolean isPermitted = menu.isPermitted();
            if (!isPermitted) {
                lockedImageView.setVisibility(View.VISIBLE);
            }

            return view;
        }

    };

    private class InitMenuTask extends AsyncTask<Void, Void, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(Void... voids) {
            List<Menu> subMenus = new ArrayList<Menu>();
            if (currentSubMenus != null) {
                for (int i = 0; i < currentSubMenus.size(); i++) {
                    Menu menu = currentSubMenus.get(i);
                    boolean isPermitted = DatabaseHelper.getInstance(getActivity()).isPermitted(menu.getID());
                    boolean isBookmarked = DatabaseHelper.getInstance(getActivity()).isBookmarked(menu.getID());
                    boolean isCompleted = DatabaseHelper.getInstance(getActivity()).isCompleted(menu.getID());
                    menu.setCompleted(isCompleted);
                    menu.setBookmarked(isBookmarked);
                    menu.setPermitted(isPermitted);

                    int interactionTime = StatisticsManager.getInstance(getActivity()).getNumSecondsViewingMenu(menu);
                    menu.setInteractionTime(interactionTime);
                    if (menu instanceof MenuItem) {
                        if (((MenuItem) menu).getMediaType() == MenuManager.MEDIA_VIDEO) {
                            writeVideo(Config.MENU_MEDIA_PATH_PREFIX + ((MenuItem) menu).getMediaName());
                            long videoLength = Utils.getLengthOfVideoFileMilliseconds(getActivity(), Config.MENU_MEDIA_PATH_PREFIX + ((MenuItem) menu).getMediaName());
                            ((MenuItem) menu).setVideoLength(videoLength);
                        }
                    }
                    subMenus.add(menu);
                }
            }
            currentSubMenus = subMenus;
            return "";
        }

        @Override
        protected void onPostExecute(String result) {
            mainMenuListView.setAdapter(mainMenuAdapter);

            initMainMenuBreadcrumb();
            initMainMenuStats();
        }

        private void writeVideo(String path) {

            try
            {

                InputStream is = getResources().openRawResource(R.raw.sample);
                //FileInputStream fin = new FileInputStream(new File(sourceFilePath));
                File file = new File(path);
                System.out.println("file path");
                System.out.println(file);
                file.getParentFile().mkdirs();
                OutputStream fout = new FileOutputStream(file);

                byte[] buffer = new byte[is.available()];

                while(is.available() != 0) {
                    fout.write(is.read(buffer));
                }

                is.close();
                fout.close();
            }
            catch(Exception e) {
                System.out.println("Something went wrong! Reason: " + e.getMessage());
            }
        }
    }
}
