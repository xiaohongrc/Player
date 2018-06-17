package com.hongenit.Aplay.fragment;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.hongenit.Aplay.GloableParams;
import com.hongenit.Aplay.PlayActivity;
import com.hongenit.Aplay.R;
import com.hongenit.Aplay.base.BaseFragment;
import com.hongenit.Aplay.list.DisplayListAdapter;
import com.hongenit.Aplay.list.ImageManager;
import com.hongenit.Aplay.list.VideoList;
import com.hongenit.Aplay.list.VideoObject;
import com.hongenit.Aplay.utils.LogUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;


/**
 * Created by hongenit on 2016/12/9.
 * desc:
 */

@SuppressLint("NewApi")
public class LocalFragment extends BaseFragment implements ListView.OnScrollListener {

    private static final String CALLER_VIDEOPLAYER = "VIDEOPLAYER";
    private static final String CALLER_CAMERA = "CAMERA";
    private static final int PROCESS_DIALOG_START_KEY = 0;
    private static final int PROCESS_MEDIA_SCANNING_KEY = 1;

    private static final String CAMERAFOLDER_SDCARD_PATH = "/mnt/sdcard/Camera/Videos";
    public static final String VIDEOURI = "VIDEOURI";
    private static LocalFragment mInstance;

    private enum ListEnum {
        NormalVideo, CameraVideo;
    }

    ;

    private static final int LIST_STATE_IDLE = 0;
    private static final int LIST_STATE_BUSY = 1;
    private static final int LIST_STATE_REQUEST_REFRESH = 2;
    private static final int APPSTATE_FIRST_START = 0;
    private static final int APPSTATE_INITIALIZED = 1;
    private static final int APPSTATE_FINISHED = 2;

    public class VideoWorkItem {
        public VideoObject object;
        public long dataModified = 0;
        public String datapath;
        public String name;
        public String duration;
        public String size;
        public boolean isHighlight = false;
        public int lastPos = 0;
    }

    public class ListLastPosition {
        public int normalVideo = 0;
        public int cameraVideo = 0;
    }

    private int mAppState;
    private boolean mRequest_stop_thread;
    private boolean mFinishScanning;
    private int mCurrentListState;
    private String mCaller;
    private ListLastPosition listLastPosition = new ListLastPosition();
    private VideoWorkItem mLastPlayedItem;
    private ListView listview;
    private DisplayListAdapter mListAdapter;
    private static Display mDisplay;
    private AlertDialog mCurrrentActiveDialog;
    private VideoList mAllImages;
    private List<VideoWorkItem> mAllVideoList = new ArrayList<VideoWorkItem>();
    private List<VideoWorkItem> mNormalVideoList = new ArrayList<VideoWorkItem>();
    private List<VideoWorkItem> mCameraList = new ArrayList<VideoWorkItem>();
    private List<VideoWorkItem> mActiveList;

    private Hashtable<Integer, Bitmap> mThumbHash = new Hashtable<Integer, Bitmap>();
    private Bitmap mDefaultBitmap;

    private Thread mLoadingThread = null;

    private TextView mNoFileTextView;
    private String TAG = "";


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        initialize();
        mLoadingThread = createLoadingThread();
        mLoadingThread.start();

        System.out.println("-----------------localfragment---onActivityCreated");
        super.onActivityCreated(savedInstanceState);
    }



    public static LocalFragment newInstance(){
        if (mInstance == null){
            mInstance =  new LocalFragment();
        }
        return  mInstance;
    }

    @Override
    protected View inflaterView() {
        refreshLastest(true);

        return LayoutInflater.from(getActivity()).inflate(R.layout.fragment_local, null);
    }

    /**
     * 初始化布局
     */
    private void initialize() {
        Log.v(TAG, "VideoPlayerActivity  initialize");
        // setTitle("");

        mAppState = APPSTATE_FIRST_START;
        mCaller = CALLER_VIDEOPLAYER;
        mFinishScanning = false;
        // 加载布局
        // setContentView(R.layout.play_list_activity);

        mNoFileTextView = (TextView) getActivity().findViewById(R.id.playListView);
        listview = (ListView) getActivity().findViewById(R.id.list);
        // 设置监听点击某一条
        // listview.setOnItemClickListener(this);

        // 设置监听滑动
        listview.setOnScrollListener(this);

        // 设置单击事件
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String uri = GloableParams.CURRENT_PLAYLIST_URIS.get(position);
                Intent intent = new Intent();
                intent.setClass(getContext(), PlayActivity.class);
                intent.putExtra(VIDEOURI, uri);

                getContext().startActivity(intent);
            }
        });

        mDisplay = getActivity().getWindow().getWindowManager().getDefaultDisplay();

        String caller = getActivity().getIntent().getStringExtra("Caller");
        if (caller != null) {
            mCaller = caller;
        }

        IntentFilter iFilter = new IntentFilter();
        // 广播行动：用户已表示希望删除的外部存储介质。应用程序应该在挂载点关闭所有已打开的文件时，他们收到这个意图。弹出媒体的挂载点的路径包含在Intent.mData领域。
        iFilter.addAction(Intent.ACTION_MEDIA_EJECT);
        // 移除SDCard
        iFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        iFilter.addAction(Intent.ACTION_MEDIA_SCANNER_STARTED);
        // 扫描完成
        iFilter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
        iFilter.addDataScheme("file");
        getActivity().registerReceiver(mBroadcastReceiver, iFilter);
        mThumbHash.clear();
        mDefaultBitmap = BitmapFactory.decodeResource(this.getResources(), R.drawable.defualt_room_avatar);
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        boolean mountState = false;

        @Override
        public void onReceive(Context context, Intent intent) {
            if (mAppState == APPSTATE_FINISHED) {
                return;
            }
            String action = intent.getAction();
            Log.v(TAG, "BroadcastReceiver action : " + action);
            // action.equals(Intent.ACTION_MEDIA_MOUNTED)

            if (action.equals(Intent.ACTION_MEDIA_EJECT)) {
                if (!mountState) {
                    Log.v(TAG, "BroadcastReceiver sdcard ejected/mounted");
                    if (mAppState == APPSTATE_INITIALIZED) {
                        uninitialize();
                    }
                    mountState = true;
                }
            } else if (action.equals(Intent.ACTION_MEDIA_SCANNER_STARTED)) {
                Log.v(TAG, "BroadcastReceiver start scan media");
                // if (mountState) {
                // showDialog(PROCESS_DIALOG_SCAN_KEY);
                // }
            } else if (action.equals(Intent.ACTION_MEDIA_SCANNER_FINISHED)) {
                if (isSDcardEjected() && mAppState != APPSTATE_FINISHED) {
                    Log.v(TAG, "BroadcastReceiver stop scan media");
                    if (mAppState == APPSTATE_FIRST_START) {
                        getActivity().showDialog(PROCESS_DIALOG_START_KEY);
                        createLoadingThread().start();
                    } else {
                        getActivity().removeDialog(PROCESS_MEDIA_SCANNING_KEY);
                        refreshLastest(true);
                    }
                    mountState = false;
                    mFinishScanning = true;
                }
            }
        }
    };
    private static final int REFRESH = 1;
    private Handler mRefreshHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == REFRESH) {
                Log.d(TAG, "handleMessage()===============receive REFRESH message+++++++++++");
                // if (mSongsAdapter != null) {
                // mSongsAdapter.notifyDataSetChanged();
                // }
                // refresh();
                showEmptyView();
            }
        }
    };

    public void showEmptyView() {
        if (mAllImages != null) {
            int totalNum = mAllImages.getCount();
            if (totalNum == 0) {
                setEmptyView(true);
            } else {
                setEmptyView(false);
            }
        }

    }

    public void refreshLastest(boolean isRefreshData) {
        if (isRefreshData) {
            getVideoData();
        }
        if (mActiveList == mNormalVideoList) {
            refreshList(ListEnum.NormalVideo);
        } else if (mActiveList == mCameraList) {
            refreshList(ListEnum.CameraVideo);
        }
        // if (isRefreshData) {
        // Toast.makeText(getActivity(), getString(R.string.list_refresh),
        // 1500).show();
        // }
    }

    private void refreshList(ListEnum list) {
        int lastPos = listview.getFirstVisiblePosition();

        if (mActiveList == mNormalVideoList) {
            listLastPosition.normalVideo = lastPos;
        } else if (mActiveList == mCameraList) {
            listLastPosition.cameraVideo = lastPos;
        }
        if (list.equals(ListEnum.NormalVideo)) {
            mActiveList = mNormalVideoList;
            lastPos = listLastPosition.normalVideo;
        } else if (list.equals(ListEnum.CameraVideo)) {
            mActiveList = mCameraList;
            lastPos = listLastPosition.cameraVideo;
        }

        if (mListAdapter != null) {
            mListAdapter.destroy();
        }

        mListAdapter = new DisplayListAdapter(getActivity());
        mListAdapter.setThumbHashtable(mThumbHash, mDefaultBitmap);
        mListAdapter.setListItems(mActiveList);

        listview.setAdapter(mListAdapter);
        listview.setSelection(lastPos);

        mCurrentListState = LIST_STATE_REQUEST_REFRESH;
    }

    private VideoList allImages() {
        mAllImages = null;
        return ImageManager.instance().allImages(getActivity(), getActivity().getContentResolver(), ImageManager.INCLUDE_VIDEOS, ImageManager.SORT_ASCENDING);
    }

    public void getVideoData() {
        Log.v(TAG, "getVideoData()");
        // String lastVideo = mManagePreference.getValue("lastVideo");
        // int lastPos = mManagePreference.getInt("lastPos");

        // Log.e(TAG, "mManagerPreference.getInt('lastPos') === " + lastPos);

        mAllVideoList.clear();
        mNormalVideoList.clear();
        mCameraList.clear();

        mAllImages = allImages(); // Video List

        if (mAllImages != null) {
            int totalNum = mAllImages.getCount();
            for (int i = 0; i < totalNum; i++) {
                VideoObject image = mAllImages.getImageAt(i);

                VideoWorkItem videoDisplayObj = new VideoWorkItem();
                videoDisplayObj.object = image;
                videoDisplayObj.name = image.getTitle();
                videoDisplayObj.duration = getString(R.string.duration_tag) + " " + image.getDuration();
                videoDisplayObj.size = image.getSize();
                videoDisplayObj.datapath = image.getMediapath();

                long bucketId = image.getBucketId();

                if (PublicTools.getBucketId(CAMERAFOLDER_SDCARD_PATH) == bucketId) {
                    videoDisplayObj.dataModified = image.getDateModified();
                    mCameraList.add(videoDisplayObj);
                } else {
                    mNormalVideoList.add(videoDisplayObj);
                }

                mAllVideoList.add(videoDisplayObj);
            }
            mRefreshHandler.sendEmptyMessage(REFRESH);

            Log.v(TAG, "LoadDataThread  totalNum : " + totalNum);
        }
    }

    public String getEmptyString() {
        return getResources().getString(R.string.no_video_file);
    }

    public void setEmptyView(boolean show) {
        if (show) {
            mNoFileTextView.setText(getEmptyString());
            mNoFileTextView.setVisibility(View.VISIBLE);
        } else {
            mNoFileTextView.setText(getEmptyString());
            mNoFileTextView.setVisibility(View.GONE);
        }
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Log.v(TAG, "LoadDataThread  handleMessage APPSTATE_FIRST_START");
            mAppState = APPSTATE_INITIALIZED;

            if (mCaller.equals(CALLER_CAMERA)) {
                mActiveList = mCameraList;
            } else {
                mActiveList = mNormalVideoList;
                refreshLastest(false);
            }
            getActivity().removeDialog(PROCESS_DIALOG_START_KEY);
            checkListScanning();
            setPlayList();
        }
    };

    public void checkListScanning() {
        if (PublicTools.isMediaScannerScanning(getActivity().getContentResolver()) && !mFinishScanning) {
            getActivity().showDialog(PROCESS_MEDIA_SCANNING_KEY);
        }
    }

    private Thread createLoadingThread() {
        return new Thread(new Runnable() {
            private static final int STATE_STOP = 0;
            private static final int STATE_IDLE = 1;
            private static final int STATE_TERMINATE = 2;
            private int workStatus;
            private int currentPos;
            private int maxPos;
            private Object[] items;

            public void run() {
                Log.v(TAG, "LoadDataThread  run");
                mRequest_stop_thread = false;

                getVideoData();
                mHandler.sendMessage(mHandler.obtainMessage());

                init();
                loadThumbnails();
            }

            private void init() {
                mCurrentListState = LIST_STATE_IDLE;
                workStatus = STATE_STOP;

                items = mAllVideoList.toArray();
                maxPos = items.length;
                currentPos = 0;

                Log.v("LoadDataThread", "maxPos : " + maxPos);
            }

            private void loadThumbnails() {
                while (workStatus != STATE_TERMINATE) {
                    switch (workStatus) {
                        case STATE_STOP:
                            workStatus = onStop();
                            break;
                        case STATE_IDLE:
                            workStatus = onIdle();
                            break;
                        default:
                            break;
                    }
                }
                Log.v("LoadDataThread", "STATE_TERMINATE!!!");
            }

            private int onIdle() {
                Log.v(TAG, "createLoadingThread : onIdle");

                while (true) {
                    if (mRequest_stop_thread || (currentPos == maxPos)) {
                        return STATE_TERMINATE;
                    }
                    if (mCurrentListState == LIST_STATE_REQUEST_REFRESH) {
                        mCurrentListState = LIST_STATE_IDLE;
                        return STATE_STOP;
                    }

                    PublicTools.sleep(PublicTools.LONG_INTERVAL);
                }
            }

            private int onStop() {
                if (mRequest_stop_thread) {
                    return STATE_TERMINATE;
                }
                if (mActiveList == null || listview == null) {
                    PublicTools.sleep(PublicTools.SHORT_INTERVAL);
                    return STATE_STOP;
                }
                if (mActiveList.isEmpty()) {
                    return STATE_IDLE;
                }
                if (-1 == listview.getLastVisiblePosition()) {
                    PublicTools.sleep(PublicTools.SHORT_INTERVAL);
                    return STATE_STOP;
                }

                Log.v(TAG, "createLoadingThread : onStop");

                Object[] viewHolders = mListAdapter.getHolderObjects();
                int count = viewHolders.length;
                for (int i = 0; i < count; i++) {
                    if (mCurrentListState == LIST_STATE_BUSY) {
                        return STATE_IDLE;
                    } else if (mCurrentListState == LIST_STATE_REQUEST_REFRESH) {
                        mCurrentListState = LIST_STATE_IDLE;
                        return STATE_STOP;
                    }
                    RefreshThumbnail((DisplayListAdapter.ViewHolder) viewHolders[i]);
                    PublicTools.sleep(PublicTools.MINI_INTERVAL);
                }

                PublicTools.sleep(PublicTools.MIDDLE_INTERVAL);

                if (count < mListAdapter.getHolderObjects().length) {
                    return STATE_STOP;
                }
                if (mCurrentListState == LIST_STATE_IDLE) {
                    return STATE_IDLE;
                } else {
                    mCurrentListState = LIST_STATE_IDLE;
                    return STATE_STOP;
                }
            }

            private void RefreshThumbnail(DisplayListAdapter.ViewHolder holder) {
                if (holder == null) {
                    return;
                }
                if (!holder.mUseDefault || holder.mItem == null || PublicTools.THUMBNAIL_CORRUPTED == holder.mItem.object.getThumbnailState()) {
                    return;
                }
                // if (holder.mBitmap != null && !holder.mBitmap.isRecycled()) {
                // holder.mBitmap.recycle();
                // }
                holder.mBitmap = holder.mItem.object.miniThumbBitmap(false, mThumbHash, mDefaultBitmap);
                if (PublicTools.THUMBNAIL_PREPARED == holder.mItem.object.getThumbnailState()) {
                    mListAdapter.sendRefreshMessage(holder);
                    holder.mUseDefault = false;
                } else {
                    holder.mUseDefault = true;
                }
            }
        });
    }

    public static class PublicTools {
        public static final int THUMBNAIL_PREPARED = 1;
        public static final int THUMBNAIL_EMPTY = 0;
        public static final int THUMBNAIL_CORRUPTED = -1;
        public static final int MINI_INTERVAL = 50;
        public static final int SHORT_INTERVAL = 150;
        public static final int MIDDLE_INTERVAL = 300;
        public static final int LONG_INTERVAL = 600;
        public static final int LONG_LONG_INTERVAL = 6000;
        private static final int FILENAMELENGTH = 80;

        public static long getBucketId(String path) {
            return path.toLowerCase().hashCode();
        }

        public static String cutString(String origin, int length) {
            char[] c = origin.toCharArray();
            int len = 0;
            int strEnd = 0;
            for (int i = 0; i < c.length; i++) {
                strEnd++;
                len = (c[i] / 0x80 == 0) ? (len + 1) : (len + 2);
                if (len > length || (len == length && i != (c.length - 1))) {
                    origin = origin.substring(0, strEnd) + "...";
                    break;
                }
            }
            return origin;
        }

        public static String replaceFilename(String filepath, String name) {
            String newPath = "";
            int lastSlash = filepath.lastIndexOf('/');
            if (lastSlash >= 0) {
                lastSlash++;
                if (lastSlash < filepath.length()) {
                    newPath = filepath.substring(0, lastSlash);
                }
            }
            newPath = newPath + name;
            int lastDot = filepath.lastIndexOf('.');
            if (lastDot > 0) {
                newPath = newPath + filepath.substring(lastDot, filepath.length());
            }
            return newPath;
        }

        public static boolean isFilenameIllegal(String filename) {
            return (filename.length() <= FILENAMELENGTH);
        }

        public static boolean isLandscape() {
            // Log.v(TAG,"isLandscape : "+ mDisplay.getOrientation());
            return (Surface.ROTATION_90 == mDisplay.getOrientation());

        }

        public static boolean isFileExist(String filepath) {
            File file = new File(filepath);
            return file.exists();
        }

        public static void sleep(int interval) {
            try {
                Thread.sleep(interval);
            } catch (Exception e) {
            }
        }


        public static Cursor query(ContentResolver resolver, Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
            try {
                if (resolver == null) {
                    return null;
                }
                return resolver.query(uri, projection, selection, selectionArgs, sortOrder);
            } catch (UnsupportedOperationException ex) {
                return null;
            }
        }

        public static boolean isMediaScannerScanning(ContentResolver cr) {
            boolean result = false;
            Cursor cursor = query(cr, MediaStore.getMediaScannerUri(), new String[]{MediaStore.MEDIA_SCANNER_VOLUME}, null, null, null);
            if (cursor != null) {
                if (cursor.getCount() == 1) {
                    cursor.moveToFirst();
                    result = "external".equals(cursor.getString(0));
                }
                cursor.close();
            }

            return result;
        }

        public static boolean isVideoStreaming(Uri uri) {
            return ("http".equalsIgnoreCase(uri.getScheme()) || "rtsp".equalsIgnoreCase(uri.getScheme()));
        }
    }

    // help functions
    private void uninitialize() {
        Log.v(TAG, "uninitialize");
        Toast.makeText(getActivity(), getString(R.string.sd_not_insert), Toast.LENGTH_SHORT).show();
        if (mAllImages != null) {
            mAllImages.onDestory();
        }
        if (mCurrrentActiveDialog != null) {
            if (mCurrrentActiveDialog.isShowing()) {
                mCurrrentActiveDialog.dismiss();
            }
        }
        listLastPosition.cameraVideo = 0;
        listLastPosition.normalVideo = 0;
        mAllImages = null;
        mAllVideoList.clear();
        mNormalVideoList.clear();
        mCameraList.clear();
        if (GloableParams.CURRENT_PLAYLIST_URIS != null)
            GloableParams.CURRENT_PLAYLIST_URIS.clear();
        if (mLastPlayedItem != null) {
            mLastPlayedItem.object = null;
            mLastPlayedItem.isHighlight = false;
            mLastPlayedItem.lastPos = 0;
        }
        refreshLastest(false);
    }

    /**
     * SDcard是否存在
     *
     * @return
     */
    private boolean isSDcardEjected() {
        boolean isSdcard_ok = false;
        String status = Environment.getExternalStorageState();
        LogUtil.d(TAG, "status : " + status);

        if (status.equals(Environment.MEDIA_MOUNTED)) {
            isSdcard_ok = true;
        }

        if (!isSdcard_ok) {
            Toast.makeText(getActivity(), getString(R.string.sd_not_insert), Toast.LENGTH_SHORT).show();
        }

        return isSdcard_ok;
    }

    @Override
    public void onDestroy() {
        Log.v(TAG, "call onDestroy");

        mRequest_stop_thread = true;
        mAppState = APPSTATE_FINISHED;

        if (mListAdapter != null) {
            mListAdapter.destroy();
        }
        if (mBroadcastReceiver != null) {
            getActivity().unregisterReceiver(mBroadcastReceiver);
            mBroadcastReceiver = null;
        }
        if (mAllImages != null) {
            mAllImages.onDestory();
        }
        Enumeration<Bitmap> e = mThumbHash.elements();
        while (e.hasMoreElements()) {
            Bitmap tmp = e.nextElement();
            if (!tmp.isRecycled()) {
                tmp.recycle();
            }
        }
        mThumbHash.clear();
        if (mLoadingThread != null) {
        }
        super.onDestroy();
    }

    @Override
    public void onResume() {
        mRefreshHandler.sendEmptyMessage(REFRESH);
        super.onResume();
        Log.v(TAG, "onResume()");
    }

    private long lastTimeonItemClick;
    private static long CLICK_INTERVAL = 800;

    private void setPlayList() {
        if (GloableParams.CURRENT_PLAYLIST_URIS != null)
            GloableParams.CURRENT_PLAYLIST_URIS.clear();
        GloableParams.CURRENT_PLAYLIST_URIS = new ArrayList<String>();
        int i = 0;
        while (i < mActiveList.size()) {
            String path = ((VideoWorkItem) mActiveList.get(i)).datapath;
            GloableParams.CURRENT_PLAYLIST_URIS.add(path);
            // Log.v(TAG, "video id : " + idList[i]);
            i++;
        }
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        switch (scrollState) {
            case AbsListView.OnScrollListener.SCROLL_STATE_IDLE:
                mCurrentListState = LIST_STATE_REQUEST_REFRESH;
                break;
            case AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL:
                break;
            case AbsListView.OnScrollListener.SCROLL_STATE_FLING:
                mCurrentListState = LIST_STATE_BUSY;
                break;
        }
    }

}
