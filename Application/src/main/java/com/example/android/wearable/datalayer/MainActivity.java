/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.wearable.datalayer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
//import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataApi.DataItemResult;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageApi.SendMessageResult;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Receives its own events using a listener API designed for foreground activities. Updates a data
 * item every second while it is open. Also allows user to take a photo and send that as an asset
 * to the paired wearable.
 */
public class MainActivity extends Activity implements DataApi.DataListener,
        MessageApi.MessageListener, NodeApi.NodeListener, ConnectionCallbacks,
        OnConnectionFailedListener {

    private static final String TAG = "MainActivity";

    /**
     * Request code for launching the Intent to resolve Google Play services errors.
     */
    private static final int REQUEST_RESOLVE_ERROR = 1000;

    private static final String START_ACTIVITY_PATH = "/start-activity";
    private static final String COUNT_PATH = "/count";
    private static final String IMAGE_PATH = "/image";
    private static final String IMAGE_KEY = "photo";
    private static final String COUNT_KEY = "count";

    private GoogleApiClient mGoogleApiClient;
    private boolean mResolvingError = false;
    private boolean mCameraSupported = false;

//    private ListView mDataItemList;
    private Button mSendPhotoBtn;
    private ImageView mThumbView;
    private Bitmap mImageBitmap;
    private Button mSendPhotoBtn2;
    private ImageView mThumbView2;
//    private Bitmap mImageBitmap2;
    private View mStartActivityBtn;
    private View mClearBtn;
//    private Bitmap mImageBitmap3;
    private Bitmap mImageBitmap4;

    private DataItemAdapter mDataItemListAdapter;
    private Handler mHandler;

    // Send DataItems.
    private ScheduledExecutorService mGeneratorExecutor;
    private ScheduledFuture<?> mDataItemGeneratorFuture;

    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int REQUEST_IMAGE_CAPTURE2 = 2;
    static final int REQUEST_SELECT_GALLERY = 3;

    private Resources mResources;

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        mHandler = new Handler();
        LOGD(TAG, "onCreate");
        mCameraSupported = getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
        setContentView(R.layout.main_activity);
        setupViews();

        // Stores DataItems received by the local broadcaster or from the paired watch.
        mDataItemListAdapter = new DataItemAdapter(this, android.R.layout.simple_list_item_1);
//        mDataItemList.setAdapter(mDataItemListAdapter);

        mGeneratorExecutor = new ScheduledThreadPoolExecutor(1);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mResources = getResources();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_IMAGE_CAPTURE:
                if (resultCode == RESULT_OK) {
                    Bundle extras = data.getExtras();
                    mImageBitmap = (Bitmap) extras.get("data");
                    mThumbView.setImageBitmap(mImageBitmap);
                    Log.d("width", "w:" + mThumbView.getWidth());
                    Log.d("height", "h:" + mThumbView.getHeight());
                }
                break;
            case REQUEST_IMAGE_CAPTURE2:
                if (resultCode == RESULT_OK) {
                    Bundle extras = data.getExtras();
                    Bitmap mImageBitmap2 = (Bitmap) extras.get("data");
                    mThumbView2.setImageBitmap(mImageBitmap2);
                }
                break;
            case REQUEST_SELECT_GALLERY:

                if (resultCode == RESULT_OK) {
                    try {
                        InputStream inputStream = getContentResolver().openInputStream(data.getData());
                        Bitmap mImageBitmap3 = BitmapFactory.decodeStream(inputStream);
                        mImageBitmap4 = Bitmap.createScaledBitmap(mImageBitmap3, 230, 230, false);
                        if (inputStream != null) {
                            inputStream.close();
                        }
                        mThumbView2.setImageBitmap(mImageBitmap4);
                        Log.d("width2", "w:" + mThumbView2.getWidth());
                        Log.d("height2", "h:" + mThumbView2.getHeight());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                break;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!mResolvingError) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mDataItemGeneratorFuture = mGeneratorExecutor.scheduleWithFixedDelay(
                new DataItemGenerator(), 1, 5, TimeUnit.SECONDS);
    }

    @Override
    public void onPause() {
        super.onPause();
        mDataItemGeneratorFuture.cancel(true /* mayInterruptIfRunning */);
    }

    @Override
    protected void onStop() {
        if (!mResolvingError) {
            Wearable.DataApi.removeListener(mGoogleApiClient, this);
            Wearable.MessageApi.removeListener(mGoogleApiClient, this);
            Wearable.NodeApi.removeListener(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    @Override //ConnectionCallbacks
    public void onConnected(Bundle connectionHint) {
        LOGD(TAG, "Google API Client was connected");
        mResolvingError = false;
        mStartActivityBtn.setEnabled(true);
        mClearBtn.setEnabled(true);
        mSendPhotoBtn.setEnabled(mCameraSupported);
        mSendPhotoBtn2.setEnabled(mCameraSupported);
        Wearable.DataApi.addListener(mGoogleApiClient, this);
        Wearable.MessageApi.addListener(mGoogleApiClient, this);
        Wearable.NodeApi.addListener(mGoogleApiClient, this);
    }

    @Override //ConnectionCallbacks
    public void onConnectionSuspended(int cause) {
        LOGD(TAG, "Connection to Google API client was suspended");
        mStartActivityBtn.setEnabled(false);
        mClearBtn.setEnabled(false);
        mSendPhotoBtn.setEnabled(false);
        mSendPhotoBtn2.setEnabled(false);
    }

    @Override //OnConnectionFailedListener
    public void onConnectionFailed(ConnectionResult result) {
        if (mResolvingError) {
            // Already attempting to resolve an error.
//            return;
        } else if (result.hasResolution()) {
            try {
                mResolvingError = true;
                result.startResolutionForResult(this, REQUEST_RESOLVE_ERROR);
            } catch (IntentSender.SendIntentException e) {
                // There was an error with the resolution intent. Try again.
                mGoogleApiClient.connect();
            }
        } else {
            Log.e(TAG, "Connection to Google API client has failed");
            mResolvingError = false;
            mStartActivityBtn.setEnabled(false);
            mClearBtn.setEnabled(false);
            mSendPhotoBtn.setEnabled(false);
            mSendPhotoBtn2.setEnabled(false);
            Wearable.DataApi.removeListener(mGoogleApiClient, this);
            Wearable.MessageApi.removeListener(mGoogleApiClient, this);
            Wearable.NodeApi.removeListener(mGoogleApiClient, this);
        }
    }

    @Override //DataListener
    public void onDataChanged(DataEventBuffer dataEvents) {
        LOGD(TAG, "onDataChanged: " + dataEvents);
        // Need to freeze the dataEvents so they will exist later on the UI thread
        final List<DataEvent> events = FreezableUtils.freezeIterable(dataEvents);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (DataEvent event : events) {
                    if (event.getType() == DataEvent.TYPE_CHANGED) {
                        mDataItemListAdapter.add(
                                new Event("DataItem Changed", event.getDataItem().toString()));
                    } else if (event.getType() == DataEvent.TYPE_DELETED) {
                        mDataItemListAdapter.add(
                                new Event("DataItem Deleted", event.getDataItem().toString()));
                    }
                }
            }
        });
    }

    @Override //MessageListener
    public void onMessageReceived(final MessageEvent messageEvent) {
        LOGD(TAG, "onMessageReceived() A message from watch was received:" + messageEvent
                .getRequestId() + " " + messageEvent.getPath());
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mDataItemListAdapter.add(new Event("Message from watch", messageEvent.toString()));
            }
        });

    }

    @Override //NodeListener
    public void onPeerConnected(final Node peer) {
        LOGD(TAG, "onPeerConnected: " + peer);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mDataItemListAdapter.add(new Event("Connected", peer.toString()));
            }
        });

    }

    @Override //NodeListener
    public void onPeerDisconnected(final Node peer) {
        LOGD(TAG, "onPeerDisconnected: " + peer);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mDataItemListAdapter.add(new Event("Disconnected", peer.toString()));
            }
        });
    }

    /**
     * A View Adapter for presenting the Event objects in a list
     */
    private static class DataItemAdapter extends ArrayAdapter<Event> {

        private final Context mContext;

        public DataItemAdapter(Context context, int unusedResource) {
            super(context, unusedResource);
            mContext = context;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                holder = new ViewHolder();
                LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(
                        Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(android.R.layout.two_line_list_item, null);
                convertView.setTag(holder);
                holder.text1 = (TextView) convertView.findViewById(android.R.id.text1);
                holder.text2 = (TextView) convertView.findViewById(android.R.id.text2);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            Event event = getItem(position);
            holder.text1.setText(event.title);
            holder.text2.setText(event.text);
            return convertView;
        }

        private class ViewHolder {

            TextView text1;
            TextView text2;
        }
    }

    private class Event {

        String title;
        String text;

        public Event(String title, String text) {
            this.title = title;
            this.text = text;
        }
    }

    private Collection<String> getNodes() {
        HashSet<String> results = new HashSet<>();
        NodeApi.GetConnectedNodesResult nodes =
                Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();

        for (Node node : nodes.getNodes()) {
            results.add(node.getId());
        }

        return results;
    }

    private void sendStartActivityMessage(String node) {
        Wearable.MessageApi.sendMessage(
                mGoogleApiClient, node, START_ACTIVITY_PATH, new byte[0]).setResultCallback(
                new ResultCallback<SendMessageResult>() {
                    @Override
                    public void onResult(SendMessageResult sendMessageResult) {
                        if (!sendMessageResult.getStatus().isSuccess()) {
                            Log.e(TAG, "Failed to send message with status code: "
                                    + sendMessageResult.getStatus().getStatusCode());
                        }
                    }
                }
        );
    }

    private class StartWearableActivityTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... args) {
            Collection<String> nodes = getNodes();
            for (String node : nodes) {
                sendStartActivityMessage(node);
            }
            return null;
        }
    }

    /**
     * Sends an RPC to start a fullscreen Activity on the wearable.
     */
    public void onStartWearableActivityClick(View view) {
        LOGD(TAG, "Generating RPC");

        // Trigger an AsyncTask that will query for a list of connected nodes and send a
        // "start-activity" message to each connected node.
        new StartWearableActivityTask().execute();
    }

    /**
     * Generates a DataItem based on an incrementing count.
     */
    private class DataItemGenerator implements Runnable {

        private int count = 0;

        @Override
        public void run() {
            PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(COUNT_PATH);
            putDataMapRequest.getDataMap().putInt(COUNT_KEY, count++);
            PutDataRequest request = putDataMapRequest.asPutDataRequest();

            LOGD(TAG, "Generating DataItem: " + request);
            if (!mGoogleApiClient.isConnected()) {
                return;
            }
            Wearable.DataApi.putDataItem(mGoogleApiClient, request)
                    .setResultCallback(new ResultCallback<DataItemResult>() {
                        @Override
                        public void onResult(DataItemResult dataItemResult) {
                            if (!dataItemResult.getStatus().isSuccess()) {
                                Log.e(TAG, "ERROR: failed to putDataItem, status code: "
                                        + dataItemResult.getStatus().getStatusCode());
                            }
                        }
                    });
        }
    }

    /**
     * Dispatches an {@link android.content.Intent} to take a photo. Result will be returned back
     * in onActivityResult().
     */
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

//    private void dispatchTakePictureIntent2() {
//        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
//            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE2);
//        }
//    }

    /**
     * ファイル選択のテスト
     */

    private void selectPictureIntent() {
        Intent selectPictureIntent = new Intent();
        selectPictureIntent.setType("image/*");
        selectPictureIntent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(selectPictureIntent, REQUEST_SELECT_GALLERY);
    }


    /**
     * Builds an {@link com.google.android.gms.wearable.Asset} from a bitmap. The image that we get
     * back from the camera in "data" is a thumbnail size. Typically, your image should not exceed
     * 320x320 and if you want to have zoom and parallax effect in your app, limit the size of your
     * image to 640x400. Resize your image before transferring to your wearable device.
     */
    private static Asset toAsset(Bitmap bitmap) {
        ByteArrayOutputStream byteStream = null;
        try {
            byteStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteStream);
            return Asset.createFromBytes(byteStream.toByteArray());
        } finally {
            if (null != byteStream) {
                try {
                    byteStream.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    /**
     * Sends the asset that was created form the photo we took by adding it to the Data Item store.
     */
    private void sendPhoto(Asset asset) {
        PutDataMapRequest dataMap = PutDataMapRequest.create(IMAGE_PATH);
        dataMap.getDataMap().putAsset(IMAGE_KEY, asset);
        dataMap.getDataMap().putLong("time", new Date().getTime());
        PutDataRequest request = dataMap.asPutDataRequest();
        Wearable.DataApi.putDataItem(mGoogleApiClient, request)
                .setResultCallback(new ResultCallback<DataItemResult>() {
                    @Override
                    public void onResult(DataItemResult dataItemResult) {
                        LOGD(TAG, "Sending image was successful: " + dataItemResult.getStatus()
                                .isSuccess());
                    }
                });

    }

    public void onTakePhotoClick(View view) {
        switch (view.getId()) {
            case R.id.takePhoto:
                dispatchTakePictureIntent();
                break;
            case R.id.takePhoto2:
//                dispatchTakePictureIntent2();
                selectPictureIntent();
                break;
        }
    }

    public void onClearClick(View view) {
        Bitmap icBitmap = BitmapFactory.decodeResource(mResources, R.drawable.ic_content_picture);
        mThumbView.setImageBitmap(icBitmap);
        mThumbView2.setImageBitmap(icBitmap);
    }

    public void onSendPhotoClick(View view) {
        switch (view.getId()) {
            case R.id.sendPhoto:
                if (null != mImageBitmap && mGoogleApiClient.isConnected()) {
                    sendPhoto(toAsset(mImageBitmap));
                }
                break;
//            case R.id.sendPhoto2:
//                if (null != mImageBitmap2 && mGoogleApiClient.isConnected()) {
//                    sendPhoto(toAsset(mImageBitmap2));
//                }
            case R.id.sendPhoto2:
                if (null != mImageBitmap4 && mGoogleApiClient.isConnected()) {
                    sendPhoto(toAsset(mImageBitmap4));
                }
                break;
        }

    }

    /**
     * Sets up UI components and their callback handlers.
     */
    private void setupViews() {
        mSendPhotoBtn = (Button) findViewById(R.id.sendPhoto);
        mThumbView = (ImageView) findViewById(R.id.imageView);
        mSendPhotoBtn2 = (Button) findViewById(R.id.sendPhoto2);
        mThumbView2 = (ImageView) findViewById(R.id.imageView2);
//        mDataItemList = (ListView) findViewById(R.id.data_item_list);
        mStartActivityBtn = findViewById(R.id.start_wearable_activity);
        mClearBtn = findViewById(R.id.clear_button);
    }

    /**
     * As simple wrapper around Log.d
     */
    private static void LOGD(final String tag, String message) {
        if (Log.isLoggable(tag, Log.DEBUG)) {
            Log.d(tag, message);
        }
    }

}
