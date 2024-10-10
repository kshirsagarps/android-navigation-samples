/*
 * Copyright 2024 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.navigationapidemo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.widget.Toast;
import androidx.core.content.ContextCompat;
import java.util.concurrent.atomic.AtomicBoolean;

/** Class that contains logic to interact with the Event Track broadcast system in NavSDK. */
final class EventTrackBroadcastManager {
  interface OnEventTrackDumpListener {
    /**
     * Called when an event track dump has been successfully collected.
     *
     * @param url - the file path where the event track has been stored in the device.
     */
    void onEventTrackDump(String url);
  }

  private static final String ACTION_EVENT_TRACK_HANDSHAKE_NAV =
      "com.google.android.libraries.navigation.EVENT_TRACK_HANDSHAKE_NAV";

  private static final String ACTION_EVENT_TRACK_DUMP_COLLECTED =
      "com.google.android.libraries.navigation.EVENT_TRACK_DUMP_COLLECTED";

  private static final String ACTION_EVENT_TRACK_DUMP_COLLECTED_FAILURE =
      "com.google.android.libraries.navigation.EVENT_TRACK_DUMP_COLLECTED_FAILURE";

  private static final String ACTION_EVENT_TRACK_HANDSHAKE_DRIVER =
      "com.google.android.libraries.navigation.EVENT_TRACK_HANDSHAKE_DRIVER";

  private static final String ACTION_EVENT_TRACK_START_RECORDING =
      "com.google.android.libraries.navigation.EVENT_TRACK_START_RECORDING";

  private static final String ACTION_EVENT_TRACK_STOP_RECORDING =
      "com.google.android.libraries.navigation.EVENT_TRACK_STOP_RECORDING";

  private static final String ACTION_EVENT_TRACK_COLLECT_DUMP =
      "com.google.android.libraries.navigation.EVENT_TRACK_COLLECT_DUMP";

  private static final String FILE_PATH_EXTRA = "filePath";

  private final AtomicBoolean isRunning = new AtomicBoolean(false);

  private final Context appContext;
  private final OnEventTrackDumpListener onEventTrackDumpListener;

  private final BroadcastReceiver broadcastReceiver =
      new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
          String intentAction = intent.getAction();

          switch (intentAction) {
            case ACTION_EVENT_TRACK_HANDSHAKE_NAV:
              Log.d("EventTrackBroadcastManager", "Handshake established");
              Toast.makeText(appContext, "Handshake established", Toast.LENGTH_SHORT).show();
              break;

            case ACTION_EVENT_TRACK_DUMP_COLLECTED:
              String url = intent.getStringExtra(FILE_PATH_EXTRA);
              onEventTrackDumpListener.onEventTrackDump(url);

              Log.d("EventTrackBroadcastManager", "Received event track dump from " + url);
              Toast.makeText(appContext, "Received event track dump", Toast.LENGTH_SHORT).show();

              break;

            case ACTION_EVENT_TRACK_DUMP_COLLECTED_FAILURE:
              Log.d("EventTrackBroadcastManager", "Failed to collect event track");
              Toast.makeText(appContext, "Failed to collect event track", Toast.LENGTH_SHORT).show();
              break;

            default:
              break;
          }
        }
      };

  EventTrackBroadcastManager(Context context, OnEventTrackDumpListener onEventTrackDumpListener) {
    appContext = context.getApplicationContext();
    this.onEventTrackDumpListener = onEventTrackDumpListener;
  }

  /** Register the broadcast receiver to start receiving messages from the NavSDK system. */
  void startReceiver() {
    if (isRunning.compareAndSet(false, true)) {

      IntentFilter intentFilter = new IntentFilter();

      intentFilter.addAction(ACTION_EVENT_TRACK_HANDSHAKE_NAV);
      intentFilter.addAction(ACTION_EVENT_TRACK_DUMP_COLLECTED);
      intentFilter.addAction(ACTION_EVENT_TRACK_DUMP_COLLECTED_FAILURE);

      ContextCompat.registerReceiver(
          appContext, broadcastReceiver, intentFilter, ContextCompat.RECEIVER_NOT_EXPORTED);

      appContext.sendBroadcast(getIntentWithPackageName(ACTION_EVENT_TRACK_HANDSHAKE_DRIVER));
    }
  }

  /** Unregister the broadcast receiver to the NavSDK system. */
  void stopReceiver() {
    if (isRunning.compareAndSet(true, false)) {
      try {
        appContext.unregisterReceiver(broadcastReceiver);
      } catch (IllegalArgumentException e) {
        // This only occurs when the receiver has already been unregistered or if it never was.
        Log.d("EventTrackBroadcastManager", "Failed to unregister receiver");
      }
    }
  }

  /** Request NavSDK to start recording event track logs. */
  void startRecording() {
    appContext.sendBroadcast(getIntentWithPackageName(ACTION_EVENT_TRACK_START_RECORDING));
  }

  /** Request NavSDK to stop recording event track logs. */
  void stopRecording() {
    appContext.sendBroadcast(getIntentWithPackageName(ACTION_EVENT_TRACK_STOP_RECORDING));
  }

  /** Requests NavSDK to collect an event track dump and store it in the device. */
  void collectDump() {
    appContext.sendBroadcast(getIntentWithPackageName(ACTION_EVENT_TRACK_COLLECT_DUMP));
  }

  private Intent getIntentWithPackageName(String intentAction) {
    Intent intent = new Intent(intentAction);
    intent.setPackage(appContext.getPackageName());
    return intent;
  }
}