package com.kinetiqa.glacier.ui;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;

import com.kinetiqa.glacier.R;
import com.kinetiqa.glacier.core.Config;
import com.kinetiqa.glacier.core.PointsManager;
import com.kinetiqa.glacier.core.StatisticsManager;
import com.kinetiqa.glacier.database.DatabaseHelper;
import com.kinetiqa.glacier.dialogs.DialogInfo;
import com.kinetiqa.glacier.dialogs.DialogStopwatch;
import com.kinetiqa.glacier.menu.Menu;
import com.kinetiqa.glacier.menu.MenuFolder;
import com.kinetiqa.glacier.menu.MenuItem;
import com.kinetiqa.glacier.menu.MenuManager;
import com.kinetiqa.glacier.ui.components.MediaVideoView;


public class MediaVideo extends Activity implements MediaViewer {

    private MediaController controller;
    private MediaVideoView videoView;
    private MenuItem video;
    private DialogStopwatch dialogStopwatch;
    private long timePracticed = 0;

    private ImageButton favButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.media_video);
        Intent intent = getIntent();
        String id = intent.getExtras().getString("id");
        Menu video = MenuManager.getInstance().findMenu(id);
        if (video == null) {
            finish();
            return;
        }

        if (video instanceof MenuFolder) {
            finish();
            return;
        }

        this.video = (MenuItem) video;

        initTitle();
        initButtons();
        initVideoPlayer();
        StatisticsManager.getInstance(getApplicationContext()).beginTiming();
    }

    @Override
    protected void onResume() {
        super.onResume();
        StatisticsManager.getInstance(getApplicationContext()).endPause();
        if (dialogStopwatch != null) {
            dialogStopwatch.resumeTimer();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        StatisticsManager.getInstance(getApplicationContext()).beginPause(false);
        if (dialogStopwatch != null) {
            dialogStopwatch.pauseTimer();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        finishEvent();
    }

    private void finishEvent() {
        if (video == null) {
            return;
        }
        if (timePracticed < 0) {
            timePracticed = 0;
        }
        long totalTimeSeconds = StatisticsManager.getInstance(getApplicationContext()).finishTiming();
        PointsManager.getInstance(getApplicationContext()).addPoints(totalTimeSeconds);

        DatabaseHelper.getInstance(getApplicationContext()).setCompleted(
                video.getID());
        DatabaseHelper.getInstance(getApplicationContext()).addStatisticsTime(
                video.getID(), video.getMediaType(),
                (int) totalTimeSeconds, 1);
        DatabaseHelper.getInstance(getApplicationContext()).addStatisticsBreakdownTime(
                video.getID(), video.getMediaType(),
                (int) totalTimeSeconds, (int) timePracticed);

        finish();
    }

    private void initTitle() {
        TextView title = (TextView) findViewById(R.id.vp_title);
        title.setText(video.getTitle());
    }

    private void initButtons() {
        // Back button transitions between different menu layers
        Button backButton = (Button) findViewById(R.id.vp_back_btn);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finishEvent();
            }
        });

        ImageButton infoButton = (ImageButton) findViewById(R.id.vp_info);
        infoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DialogInfo dialogInfo = new DialogInfo(MediaVideo.this, video.getTitle(), video.getDesc());
                dialogInfo.show();
            }
        });

        ImageButton practiceButton = (ImageButton) findViewById(R.id.vp_practice);
        if (video.isActivity()) {
            practiceButton.setVisibility(View.VISIBLE);
            practiceButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    StatisticsManager.getInstance(getApplicationContext()).beginActivityTiming();
                    dialogStopwatch = new DialogStopwatch(MediaVideo.this, video.getID());
                    dialogStopwatch.show();
                    dialogStopwatch.startTimer();

                    dialogStopwatch.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialogInterface) {
                            timePracticed = StatisticsManager.getInstance(getApplicationContext()).finishActivityTiming();
                            dialogStopwatch.stopTimer();
                        }
                    });
                }
            });
        } else {
            practiceButton.setVisibility(View.GONE);
        }

        boolean isBookmarked = DatabaseHelper.getInstance(getApplicationContext()).isBookmarked(video.getID());
        favButton = (ImageButton) findViewById(R.id.vp_favourite);
        favButton.setVisibility(View.VISIBLE);
        if (isBookmarked) {
            favButton.setImageResource(R.drawable.ic_action_important);
        } else {
            favButton.setImageResource(R.drawable.ic_action_not_important);
        }
        favButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean isBookmarked = DatabaseHelper.getInstance(getApplicationContext()).isBookmarked(video.getID());
                if (isBookmarked) {
                    DatabaseHelper.getInstance(getApplicationContext()).removeBookmark(video.getID());
                    favButton.setImageResource(R.drawable.ic_action_not_important);
                    Toast.makeText(getApplicationContext(), "Favourite removed", Toast.LENGTH_LONG).show();
                } else {
                    DatabaseHelper.getInstance(getApplicationContext()).addBookmark(
                            video.getID(), video.getMediaName());
                    favButton.setImageResource(R.drawable.ic_action_important);
                    Toast.makeText(getApplicationContext(), "Favourite added", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void initVideoPlayer() {
        videoView = (MediaVideoView) findViewById(R.id.vp_video);
        videoView.setVideoPath(Config.MENU_MEDIA_PATH_PREFIX + video.getMediaName());
        videoView.requestFocus();

        controller = new MediaController(this);
        controller.setAnchorView(videoView);
        controller.setMediaPlayer(videoView);

        videoView.setMediaController(controller);
        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                videoView.start();
                videoView.requestFocus();
                controller.show(0);
            }
        });
        videoView.setPlayPauseListener(new MediaVideoView.PlayPauseListener() {
            @Override
            public void onPlay() {
                StatisticsManager.getInstance(getApplicationContext()).endPause();
            }

            @Override
            public void onPause() {
                StatisticsManager.getInstance(getApplicationContext()).beginPause(false);
            }
        });

    }
}
