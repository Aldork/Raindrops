package com.kinetiqa.glacier.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.MediaController;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.kinetiqa.glacier.R;
import com.kinetiqa.glacier.core.Config;
import com.kinetiqa.glacier.core.StatisticsManager;
import com.kinetiqa.glacier.database.DatabaseHelper;
import com.kinetiqa.glacier.messaging.Message;
import com.kinetiqa.glacier.ui.components.MediaVideoView;
import com.kinetiqa.glacier.utils.TimeConversion;

import java.io.IOException;


public class MessageViewer extends Activity {

    private Message message;

    private MediaVideoView videoView;
    private MediaController mediaController;

    private MediaPlayer audioPlayer;
    private Handler seekHandler = new Handler();

    private SeekBar playbackSeekBar;
    private TextView playbackTimerTextView;
    private Button playButton;
    private Button pauseButton;

    private Dialog deleteConfirmDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.message_viewer);
        Intent intent = getIntent();
        String id = intent.getExtras().getString("id");
        boolean isInbox = intent.getExtras().getBoolean("isInbox");
        Message message;
        if (isInbox) {
            message = DatabaseHelper.getInstance(this).getInboxMessage(id);
        } else {
            message = DatabaseHelper.getInstance(this).getOutboxMessage(id);
        }
        if (message == null) {
            finish();
            return;
        }
        this.message = message;

        initTitle();
        initMessageBody();
        initButtons();
        if (message.getType() == Message.TYPE_VIDEO) {
            initVideoPlayer();
        } else if (message.getType() == Message.TYPE_AUDIO) {
            initAudioPlayer();
        }

    }

    private void initTitle() {
        TextView title = (TextView) findViewById(R.id.mv_title);
        title.setText(message.getTitle());
    }

    private void initMessageBody() {
        TextView title = (TextView) findViewById(R.id.mv_body);
        title.setText(message.getDescription());
    }

    private void initButtons() {
        Button backButton = (Button) findViewById(R.id.mv_back_btn);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        ImageButton trashButton = (ImageButton) findViewById(R.id.mv_trash_btn);
        trashButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openDeleteAllMessageConfirm();
            }
        });
    }

    public void openDeleteAllMessageConfirm() {
        deleteConfirmDialog = new Dialog(MessageViewer.this,
                android.R.style.Theme_Translucent);
        deleteConfirmDialog.getWindow().setBackgroundDrawableResource(R.color.transparent);
        deleteConfirmDialog.getWindow().getAttributes().windowAnimations = R.style.DialogFadeAnimation;
        deleteConfirmDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        deleteConfirmDialog.setCancelable(true);
        deleteConfirmDialog.setContentView(R.layout.dialog_confirm);

        TextView msgTitle = (TextView) deleteConfirmDialog.findViewById(R.id.confirm_title);
        msgTitle.setText("Delete");

        TextView msgBody = (TextView) deleteConfirmDialog.findViewById(R.id.confirm_desc);
        msgBody.setText("Are you sure you want to delete this message?");

        Button yesButton = (Button) deleteConfirmDialog
                .findViewById(R.id.confirm_okay);
        yesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (message.isInbox()) {
                    DatabaseHelper.getInstance(getApplicationContext()).deleteInboxMsg(message.getId());
                } else {
                    DatabaseHelper.getInstance(getApplicationContext()).deleteOutboxMsg(message.getId());
                }
                Toast.makeText(getApplicationContext(), "Message Deleted", Toast.LENGTH_LONG).show();
                deleteConfirmDialog.dismiss();
                finish();
            }
        });

        Button noButton = (Button) deleteConfirmDialog.findViewById(R.id.confirm_cancel);
        noButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                deleteConfirmDialog.dismiss();
            }
        });

        deleteConfirmDialog.show();
    }

    private void initVideoPlayer() {
        mediaController = new MediaController(this) {
            @Override
            public void hide() {
                // Always show: so we do nothing
                return;
            }
        };

        videoView = (MediaVideoView) findViewById(R.id.mv_video);
        videoView.setVisibility(View.VISIBLE);
        videoView.setVideoPath(Config.MESSAGE_MEDIA_PATH_PREFIX + message.getAttachmentName());
        videoView.setMediaController(mediaController);
        mediaController.setAnchorView(videoView);
        videoView.requestFocus();
        videoView.start();
        videoView.setPlayPauseListener(new MediaVideoView.PlayPauseListener() {
            @Override
            public void onPlay() {
                StatisticsManager.getInstance(Home.h).endPause();
            }

            @Override
            public void onPause() {
                StatisticsManager.getInstance(Home.h).beginPause(false);
            }
        });
        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            public void onPrepared(MediaPlayer mp) {
                mediaController.show(0);
            }
        });
    }

    private void initAudioPlayer() {
        playbackTimerTextView = (TextView) findViewById(R.id.mv_audio_time);
        playbackTimerTextView.setVisibility(View.VISIBLE);

        playbackSeekBar = (SeekBar) findViewById(R.id.mv_seek);
        playbackSeekBar.setVisibility(View.VISIBLE);
        playbackSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (audioPlayer != null && fromUser) {
                    audioPlayer.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        playButton = (Button) findViewById(R.id.mv_audio_play);
        playButton.setVisibility(View.VISIBLE);
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playButton.setVisibility(View.GONE);
                pauseButton.setVisibility(View.VISIBLE);

                if (audioPlayer == null) {
                    initAudioPlayback();
                }
                audioPlayer.start();
            }
        });

        pauseButton = (Button) findViewById(R.id.mv_audio_pause);
        pauseButton.setVisibility(View.GONE);
        pauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playButton.setVisibility(View.VISIBLE);
                pauseButton.setVisibility(View.GONE);
                audioPlayer.pause();
            }
        });

        initAudioPlayback();
    }

    private void initAudioPlayback() {
        playbackSeekBar.setVisibility(View.VISIBLE);
        pauseButton.setVisibility(View.VISIBLE);

        audioPlayer = new MediaPlayer();
        audioPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

            @Override
            public void onCompletion(MediaPlayer mp) {
                playButton.setVisibility(View.VISIBLE);
                pauseButton.setVisibility(View.GONE);
                audioPlayer.seekTo(0);
            }

        });

        try {
            audioPlayer.setDataSource(Config.MESSAGE_MEDIA_PATH_PREFIX + message.getAttachmentName());
            audioPlayer.prepare();

            playbackSeekBar.setMax(audioPlayer.getDuration());
            playbackSeekBar.setProgress(audioPlayer.getCurrentPosition());
            seekHandler.postDelayed(seekBarRunnable, 100);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    Runnable seekBarRunnable = new Runnable() {
        @Override
        public void run() {
            playbackSeekBar.setProgress(audioPlayer.getCurrentPosition());
            playbackTimerTextView.setText(TimeConversion.convertMillisecondsToTime(audioPlayer.getCurrentPosition()) + "/" + TimeConversion.convertMillisecondsToTime(audioPlayer.getDuration()));
            seekHandler.postDelayed(seekBarRunnable, 100);
        }
    };

}
