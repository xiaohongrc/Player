package com.hongenit.Aplay.list;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.hongenit.Aplay.R;
import com.hongenit.Aplay.fragment.LocalFragment.PublicTools;
import com.hongenit.Aplay.fragment.LocalFragment.VideoWorkItem;

public class DisplayListAdapter extends BaseAdapter {
  	@SuppressWarnings("unused")
    private static final String TAG = "DisplayListAdapter";

    private List<VideoWorkItem> mItemList = new ArrayList<VideoWorkItem>();
    private List<ViewHolder> mViewHolderList = new ArrayList<ViewHolder>();
    private LayoutInflater mInflater;
    private Drawable mDefaultDrawable;
    private Bitmap mDefaultBitmap = null;
    private Hashtable<Integer, Bitmap> mThumbHash = null;
    public DisplayListAdapter(Context context) {
        mDefaultDrawable = context.getResources().getDrawable(R.drawable.defualt_room_avatar);
        mInflater = LayoutInflater.from(context);
    }

    public void setListItems(List<VideoWorkItem> list) {
        mItemList = list;
    }

    public int getCount() {
        return mItemList.size();
    }

    public Object getItem(int position) {
        return mItemList.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    public void destroy() {
//        Object[] objects = mViewHolderList.toArray();
//        for (int i = 0; i < objects.length; i++) {
//            ViewHolder holder = (ViewHolder) objects[i];
//
//            if (holder.mBitmap != null)
//                holder.mBitmap.recycle();
//        }
    }

    @SuppressWarnings("unchecked")
    public Object[] getHolderObjects() {
        Object[] objs = mViewHolderList.toArray();

        if (objs != null)
            Arrays.sort(objs, new SortArray());
        return objs;
    }

    public void sendRefreshMessage(ViewHolder holder) {
        Message msg = new Message();
        msg.obj = (Object) holder;        mHandler.sendMessage(msg);
    }

    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            ((ViewHolder) msg.obj).refreshThumbnail();
        }
    };

    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.list_block, null);
            holder = new ViewHolder(convertView);
            holder.refresh(position);
            convertView.setTag(holder);

            mViewHolderList.add(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
            holder.refresh(position);
        }

        return convertView;
    }

    public class ViewHolder {

        private static final int HModeTitleLength = 60;
        private static final int VModeTitleLength = 22;
        int COLOR_BLACK = Color.argb(255, 85, 86, 90);
        public VideoWorkItem mItem;
        public Bitmap mBitmap;
        public boolean mUseDefault;
        public boolean mIsHighLighted;
        public int mPosition;
        private ImageView mThumbnail;
        private TextView mName;
        private TextView mDuration;
        private TextView mSize;

        public ViewHolder(View convertView) {
            mThumbnail = (ImageView)convertView.findViewById(R.id.block_thumbnail);
            mName = (TextView)convertView.findViewById(R.id.block_name);
            mDuration = (TextView)convertView.findViewById(R.id.block_duration);
            mSize = (TextView)convertView.findViewById(R.id.block_size);

        }

        @SuppressWarnings("deprecation")
		public void refresh(int pos) {
            mPosition = pos;
            mItem = mItemList.get(pos);

            if (PublicTools.THUMBNAIL_PREPARED == mItem.object.getThumbnailState()) {
//                if (mBitmap != null)
//                    mBitmap.recycle();

                mBitmap = mItem.object.miniThumbBitmap(false, DisplayListAdapter.this.mThumbHash, mDefaultBitmap);
                if (!mBitmap.isRecycled()) {
                    mThumbnail.setImageBitmap(mBitmap);
                    mUseDefault = false;
                } else {
                    mThumbnail.setImageDrawable(mDefaultDrawable);
                    mUseDefault = true;
                }
            } else {
            	mThumbnail.setImageDrawable(mDefaultDrawable);
                mUseDefault = true;
            }

            String displayName = (PublicTools.isLandscape() ?
                    PublicTools.cutString(mItem.name, HModeTitleLength)
                    : PublicTools.cutString(mItem.name, VModeTitleLength) );

            mName.setText(displayName);
            mDuration.setText(mItem.duration);
            mSize.setText(mItem.size);

            Log.v("WUYA", "DisplayListAdapter L183 VideoWorkItem.isHighlight is ???? == "
                    + mItem.isHighlight);
            if (mItem.isHighlight) {
                mIsHighLighted = true;
            } else {
                if (mIsHighLighted) {
                }
                mIsHighLighted = false;
            }
        }

        @SuppressWarnings("deprecation")
		public void refreshThumbnail() {
            if ((PublicTools.THUMBNAIL_PREPARED == mItem.object.getThumbnailState()
                 || mBitmap != null) && !mBitmap.isRecycled()) {
                mThumbnail.setImageBitmap(mBitmap);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public class SortArray implements Comparator {
        public int compare(Object obj1, Object obj2) {
            if (obj1 == null || obj1 == null)
                return 0;
            ViewHolder v1 = (ViewHolder) obj1;
            ViewHolder v2 = (ViewHolder) obj2;
            return (v1.mPosition < v2.mPosition ? -1 : (v1.mPosition == v2.mPosition ? 0 : 1));
        }
    }
    public void setThumbHashtable(Hashtable<Integer, Bitmap> ht, Bitmap defaultThumb) {
        this.mThumbHash = ht;
        this.mDefaultBitmap = defaultThumb;
    }
}
