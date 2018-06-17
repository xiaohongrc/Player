package com.hongenit.Aplay;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.MessageQueue.IdleHandler;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.hongenit.Aplay.base.BaseFragmentActivity;
import com.hongenit.Aplay.fragment.LocalFragment;

import io.vov.vitamio.MediaPlayer;
import io.vov.vitamio.MediaPlayer.OnCompletionListener;
import io.vov.vitamio.MediaPlayer.OnErrorListener;
import io.vov.vitamio.MediaPlayer.OnPreparedListener;
import io.vov.vitamio.Vitamio;
import io.vov.vitamio.widget.VideoView;

public class PlayActivity extends BaseFragmentActivity implements OnClickListener {
    private VideoView mVideoView = null;
    //	private LinearLayout ll_playlist;
//    private ListView list;
    private TextView no_video_play;
    private Intent mIntent;
    private Uri uri;
    private boolean isFromFile = false;
    private View mControlView = null;
    private PopupWindow mControlerPopupWindow = null;
    private static int screenWidth = 0;
    private static int screenHeight = 0;
    private TextView durationTextView = null;
    private TextView playedTextView = null;
    private GestureDetector mGestureDetector = null;
    private Button mPlay = null;
    private SeekBar seekBar = null;
    private final static int HIDE_CONTROLLER = 1;
    private final static int PAUSE = 3;
    private final static int PROGRESS_CHANGED = 0;
    private float startX;
    private static long CLICK_INTERVAL = 200;
    private long lastTimeOnSingleTapConfirmed;
    private long lastTimemPlayListener;
    private boolean isControllerShow = true;
    private final static int TIME = 5000;
    private PausePlayerReceiver mPausePlayerReceiver;
    private boolean isChangedVideo = false;
    private int playedTime;


    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {

                case PROGRESS_CHANGED:
                    // 得到当前播放位置
                    int i = (int) mVideoView.getCurrentPosition();
                    // 更新播放进度
                    seekBar.setProgress(i);
                    i /= 1000;
                    int minute = i / 60;
                    int hour = minute / 60;
                    int second = i % 60;
                    minute %= 60;
                    if (hour > 0) {
                        playedTextView.setText(String.format("%02d:%02d:%02d", hour, minute, second));
                    } else {
                        playedTextView.setText(String.format("%02d:%02d", minute, second));
                    }

                    sendEmptyMessageDelayed(PROGRESS_CHANGED, 1000);
                    break;

                case HIDE_CONTROLLER:
                    hideController();
                    break;

                case PAUSE:
                    if (mVideoView != null) {
                        mVideoView.pause();
                    }

                    break;
            }

            super.handleMessage(msg);
        }
    };
    private TextView cur_play_time;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!Vitamio.isInitialized(this))
            return;
        setContentView(R.layout.video_view);
        mVideoView = findViewById(R.id.vv);
        no_video_play = findViewById(R.id.no_video_play);
        cur_play_time = findViewById(R.id.cur_play_time);

        getPlayData();

        initControlView();

        initVideoView();

        startPlay();

    }

    private void startPlay() {
        if (uri != null && mVideoView != null) {
            mVideoView.stopPlayback();
            mVideoView = findViewById(R.id.vv);
            mVideoView.setVideoURI(uri);
            mVideoView.start();
            no_video_play.setVisibility(View.GONE);
        } else {
            no_video_play.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        getScreenSize();
        if (isControllerShow) {
            cancelDelayHide();
            hideController();
            showController();
            hideControllerDelay();
        }

        super.onConfigurationChanged(newConfig);
    }

    /**
     * 获得手机或模拟器屏蔽高和宽，并计算好控制面板的高度
     */
    @SuppressWarnings("deprecation")
    private void getScreenSize() {
        Display display = getWindowManager().getDefaultDisplay();
        screenHeight = display.getHeight();
        screenWidth = display.getWidth();
    }

    private void getPlayData() {
        mIntent = getIntent();
        if (mIntent != null) {
            String uriExtra = mIntent.getStringExtra(LocalFragment.VIDEOURI);
            if (uriExtra != null) {
                uri = Uri.parse(uriExtra);
                isFromFile = false;
            } else {
                // 本地文件夹发起播放
                uri = mIntent.getData();
                isFromFile = true;
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void initControlView() {
        // 加载控制视频界面。
        mControlView = getLayoutInflater().inflate(R.layout.controler_view, null);

        // PopupWindow 作为一种用户提醒 而且其开销也比Activity要小
        mControlerPopupWindow = new PopupWindow(mControlView);

        // 加载控制面板
        Looper.myQueue().addIdleHandler(new IdleHandler() {

            @Override
            public boolean queueIdle() {

                if (mControlerPopupWindow != null && mVideoView.isShown()) {
                    mControlerPopupWindow.showAtLocation(mVideoView, Gravity.BOTTOM, 0, 0);
                    mControlerPopupWindow.update(0, 0, screenWidth, LayoutParams.WRAP_CONTENT);
                }

                return false;
            }
        });

        // 显示视频总长度时间
        durationTextView = mControlView.findViewById(R.id.duration);
        // 显示视频播放了多少时间
        playedTextView = mControlView.findViewById(R.id.has_played);

        // 播放视频
        mPlay = mControlView.findViewById(R.id.control_play_state);


        mPlay.setOnClickListener(mPlayListener);


        getScreenSize();

        // 播放进度条,可以拖动
        seekBar = mControlView.findViewById(R.id.seekbar);

        seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            // 当拖动发生改变的时候执行
            @Override
            public void onProgressChanged(SeekBar seekbar, int progress, boolean fromUser) {

                if (fromUser) {
                    mVideoView.seekTo(progress);
                    System.out.println("--------------progress---" + progress);
                }
            }

            // 当拖动刚触动的时候执行
            @Override
            public void onStartTrackingTouch(SeekBar arg0) {
                mHandler.removeMessages(HIDE_CONTROLLER);
            }

            // 当拖动刚触动的时候执行
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mHandler.sendEmptyMessageDelayed(HIDE_CONTROLLER, TIME);
            }
        });

        mGestureDetector = new GestureDetector(new SimpleOnGestureListener() {

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {

                long time = System.currentTimeMillis();
                if (time - lastTimeOnSingleTapConfirmed < CLICK_INTERVAL) {
                    return true;
                }
                lastTimeOnSingleTapConfirmed = time;

                if (!isControllerShow && mVideoView.isPlaying()) {
                    showController();
                    hideControllerDelay();
                } else {
                    cancelDelayHide();
                    hideController();
                }

                return true;
            }

        });
    }

    private View.OnClickListener mPlayListener = new View.OnClickListener() {
        public void onClick(View v) {

            switch (v.getId()) {
                case R.id.control_play_state:

                    // 防止频繁点击导致错误
                    long time = System.currentTimeMillis();
                    if (time - lastTimemPlayListener < CLICK_INTERVAL) {
                        return;
                    }
                    lastTimemPlayListener = time;

                    cancelDelayHide();

                    if (mVideoView.isPlaying()) {
                        mVideoView.pause();
                        mPlay.setBackgroundResource(R.drawable.bg_status_pause);
                    } else {
                        mVideoView.start();
                        mPlay.setBackgroundResource(R.drawable.bg_status_play);
                        hideControllerDelay();
                    }
                    break;

            }

        }
    };

    private float curX;
    private String lastTimeStr = "";

    private void initVideoView() {

        /*
         * 监听播放出错时的处理
         */
        mVideoView.setOnErrorListener(new OnErrorListener() {

            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {

                if (mVideoView != null) {
                    mVideoView.stopPlayback();
                }

                Toast.makeText(PlayActivity.this, "播放出错了", Toast.LENGTH_SHORT).show();
                return true;

            }

        });

        mVideoView.setOnPreparedListener(new OnPreparedListener() {

            @Override
            public void onPrepared(MediaPlayer arg0) {

                if (isControllerShow) {
                    showController();
                }

                int i = (int) mVideoView.getDuration();
                Log.d("onCompletion", "" + i);
                // 视频文件和seekBar关联：视频有多长那么seekBar拖动也在这个范围内
                seekBar.setMax(i);

                i /= 1000;
                int minute = i / 60;
                int hour = minute / 60;
                int second = i % 60;
                minute %= 60;
                if (hour > 0) {
                    durationTextView.setText(String.format("%02d:%02d:%02d", hour, minute, second));
                } else {
                    durationTextView.setText(String.format("%02d:%02d", minute, second));
                }

                mVideoView.start();

                hideControllerDelay();

                mHandler.sendEmptyMessage(PROGRESS_CHANGED);
            }
        });

        mVideoView.setOnCompletionListener(new OnCompletionListener() {

            @Override
            public void onCompletion(MediaPlayer arg0) {
                // 显示播放列表
                finish();

            }
        });
    }

    /**
     * 发送信息，告诉控制面板在多少秒后隐藏。
     */
    private void hideControllerDelay() {
        mHandler.sendEmptyMessageDelayed(HIDE_CONTROLLER, TIME);
    }

    /**
     * 隐藏控制面板
     */
    private void hideController() {

        // 隐藏控制面板
        if (mControlerPopupWindow.isShowing()) {
            mControlerPopupWindow.update(0, 0, 0, 0);
            isControllerShow = false;
        }

    }

    /**
     * 删除隐藏面板消息
     */
    private void cancelDelayHide() {
        mHandler.removeMessages(HIDE_CONTROLLER);
    }

    /**
     * 显示控制面板
     */
    private void showController() {
        mControlerPopupWindow.update(0, 0, screenWidth, LayoutParams.WRAP_CONTENT);
        isControllerShow = true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        // 非常关键
        boolean result = mGestureDetector.onTouchEvent(event);

        if (!result) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startX = event.getRawX();


                    break;
                case MotionEvent.ACTION_MOVE:
                    curX = event.getRawX();
                    int x0 = (int) ((curX - startX) * 20 / screenWidth);
                    long time = mVideoView.getCurrentPosition() + x0 * 2000;
                    String timeStr = milliTimeToStr(time);
                    if (!TextUtils.equals(lastTimeStr, timeStr) && x0 > 1) {
                        cur_play_time.setVisibility(View.VISIBLE);
                        cur_play_time.setText(timeStr);
                        lastTimeStr = timeStr;
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    endX = event.getRawX();
                    dx = endX - startX;
                    int x = (int) (dx * 20 / screenWidth);
                    if (x != 0) {
                        long seekTime = mVideoView.getCurrentPosition() + x * 3000;
                        mVideoView.seekTo(seekTime);
                    }
                    cur_play_time.setVisibility(View.GONE);
                    break;

                default:
                    break;
            }
            result = super.onTouchEvent(event);
        }

        return result;
    }

    /**
     * 将毫秒时间值转化为时：分：秒格式
     */
    public String milliTimeToStr(long milliTime) {
        milliTime /= 1000;
        long minute = milliTime / 60;
        long hour = minute / 60;
        long second = milliTime % 60;
        minute %= 60;
        if (hour > 0) {
            return String.format("%02d:%02d:%02d", hour, minute, second);
        } else {
            return String.format("%02d:%02d", minute, second);
        }
    }

    private float endX;

    private float dx;


    @Override
    public void onClick(View v) {

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && isFromFile || keyCode == KeyEvent.KEYCODE_HOME) {
            Intent intent = new Intent(this, MainActivity.class);
            this.startActivity(intent);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onResume() {

        if (!isChangedVideo) {
            mVideoView.seekTo(playedTime);
            mVideoView.start();
        } else {
            isChangedVideo = false;
        }
        if (mVideoView.isPlaying()) {
            hideControllerDelay();
        }
        // adView.requestFreshAd();
        Log.d("REQUEST", "NEW AD !");
        IntentFilter filter = new IntentFilter("com.hongenit.VideoPlayerActivity.pause");
        mPausePlayerReceiver = new PausePlayerReceiver();
        registerReceiver(mPausePlayerReceiver, filter);

        // 设置为横屏
        if (getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }

        hideControllerDelay();

        super.onResume();
    }

    @Override
    protected void onDestroy() {
        if (mControlerPopupWindow != null && mControlerPopupWindow.isShowing()) {
            mControlerPopupWindow.dismiss();
        }

        mHandler.removeMessages(PROGRESS_CHANGED);
        mHandler.removeMessages(HIDE_CONTROLLER);
        if (mVideoView != null && mVideoView.isPlaying()) {
            mVideoView.stopPlayback();
        }
//        if (GloableParams.CURRENT_PLAYLIST_URIS != null) {
//            GloableParams.CURRENT_PLAYLIST_URIS.clear();
//            System.out.println("------------playActivity--onDestroy--");
//        }
        unregisterReceiver(mPausePlayerReceiver);
        super.onDestroy();
    }

    public class PausePlayerReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.hongenit.VideoPlayerActivity.pause".equals(intent.getAction())) {
                mHandler.sendEmptyMessageDelayed(PAUSE, 950);
            }
        }
    }

    @Override
    protected void onPause() {
        playedTime = (int) mVideoView.getCurrentPosition();
        mVideoView.pause();
        mHandler.sendEmptyMessage(PAUSE);
        super.onPause();
    }

}
