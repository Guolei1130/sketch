/*
 * Copyright (C) 2013 Peng fei Pan <sky@xiaopan.me>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.xiaopan.sketchsample.fragment;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import me.xiaopan.assemblyadapter.AssemblyRecyclerAdapter;
import me.xiaopan.sketch.util.SketchUtils;
import me.xiaopan.sketchsample.BaseFragment;
import me.xiaopan.sketchsample.BindContentView;
import me.xiaopan.sketchsample.R;
import me.xiaopan.sketchsample.activity.ApplyBackgroundCallback;
import me.xiaopan.sketchsample.adapter.itemfactory.MyVideoItemFactory;
import me.xiaopan.sketchsample.bean.VideoItem;
import me.xiaopan.sketchsample.util.ScrollingPauseLoadManager;
import me.xiaopan.sketchsample.util.VideoThumbnailPreprocessor;

@BindContentView(R.layout.fragment_recycler)
public class MyVideoFragment extends BaseFragment implements SwipeRefreshLayout.OnRefreshListener {
    @BindView(R.id.refresh_recyclerFragment)
    SwipeRefreshLayout refreshLayout;

    @BindView(R.id.recycler_recyclerFragment_content)
    RecyclerView recyclerView;

    private AssemblyRecyclerAdapter adapter;

    private ApplyBackgroundCallback applyBackgroundCallback;
    private String backgroundImageUri;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof ApplyBackgroundCallback) {
            applyBackgroundCallback = (ApplyBackgroundCallback) activity;
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        refreshLayout.setOnRefreshListener(this);
        recyclerView.setOnScrollListener(new ScrollingPauseLoadManager(view.getContext()));

        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        int padding = SketchUtils.dp2px(getActivity(), 2);
        recyclerView.setPadding(padding, padding, padding, padding);
        recyclerView.setClipToPadding(false);

        if (adapter != null) {
            recyclerView.setAdapter(adapter);
            recyclerView.scheduleLayoutAnimation();
        } else {
            refreshLayout.post(new Runnable() {
                @Override
                public void run() {
                    refreshLayout.setRefreshing(true);
                    onRefresh();
                }
            });
        }
    }

    @Override
    public void onRefresh() {
        if (getActivity() != null) {
            new LoadVideoListTask(getActivity().getBaseContext()).execute();
        }
    }

    @Override
    protected void onUserVisibleChanged(boolean isVisibleToUser) {
        if (applyBackgroundCallback != null && isVisibleToUser) {
            changeBackground(backgroundImageUri);
        }
    }

    private void changeBackground(String imageUri) {
        this.backgroundImageUri = imageUri;
        if (applyBackgroundCallback != null) {
            applyBackgroundCallback.onApplyBackground(backgroundImageUri);
        }
    }

    private class LoadVideoListTask extends AsyncTask<Void, Integer, List<VideoItem>> {
        private Context context;

        private LoadVideoListTask(Context context) {
            this.context = context;
        }

        @Override
        protected List<VideoItem> doInBackground(Void[] params) {
            Cursor cursor = context.getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    new String[]{
                            MediaStore.Video.Media.TITLE,
                            MediaStore.Video.Media.DATA,
                            MediaStore.Video.Media.SIZE,
                            MediaStore.Video.Media.DURATION,
                            MediaStore.Video.Media.DATE_ADDED,
                    },
                    null,
                    null,
                    MediaStore.Images.Media.DATE_ADDED + " DESC");
            if (cursor == null) {
                return null;
            }

            List<VideoItem> imagePathList = new ArrayList<VideoItem>(cursor.getCount());
            while (cursor.moveToNext()) {
                VideoItem video = new VideoItem();
                video.title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE));
                video.path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA));
                video.size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE));
                video.duration = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION));
                video.date = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED));
                imagePathList.add(video);
            }
            cursor.close();
            return imagePathList;
        }

        @Override
        protected void onPostExecute(List<VideoItem> imageUriList) {
            if (getActivity() == null) {
                return;
            }

            AssemblyRecyclerAdapter adapter = new AssemblyRecyclerAdapter(imageUriList);
            adapter.addItemFactory(new MyVideoItemFactory());
            recyclerView.setAdapter(adapter);
            recyclerView.scheduleLayoutAnimation();
            MyVideoFragment.this.adapter = adapter;
            refreshLayout.setRefreshing(false);
            if (imageUriList != null && imageUriList.size() > 0) {
                changeBackground(VideoThumbnailPreprocessor.createUri(imageUriList.get(0).path));
            }
        }
    }
}