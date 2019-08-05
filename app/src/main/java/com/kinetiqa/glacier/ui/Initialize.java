package com.kinetiqa.glacier.ui;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.preference.PreferenceManager;

import com.kinetiqa.glacier.R;
import com.kinetiqa.glacier.core.Config;
import com.kinetiqa.glacier.database.DatabaseHelper;
import com.kinetiqa.glacier.dialogs.DialogLogin;
import com.kinetiqa.glacier.menu.MenuItem;

public class Initialize extends Activity {

	private ProgressDialog progressDialog;
	private SharedPreferences sharedPreferences;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		System.out.println("Saved instance");
		System.out.println(savedInstanceState);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.loading);
		sharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(Initialize.this);
	}

	@Override
	public void onResume() {
		super.onResume();

		int databaseStatus = DatabaseHelper
				.getInstance(getApplicationContext()).databaseStatus();
		switch (databaseStatus) {
		case DatabaseHelper.DATABASE_READY:
			// Everything is in order.
			// TODO: Check database updates in the future
			Intent i = new Intent(getApplicationContext(), Home.class);
			startActivity(i);
			finish();
			break;
		case DatabaseHelper.DATABASE_ITEMS_NOT_DOWNLOADED:
			// Continue to download database components
			ContentDownloaderTask contentDownloader = new ContentDownloaderTask();
			contentDownloader.execute();
			break;
		case DatabaseHelper.DATABASE_EMPTY:
			// First startup - opens setup page
			Intent s = new Intent(getApplicationContext(), Setup.class);
			startActivity(s);
			break;
		default:
			new AlertDialog.Builder(this)
					.setTitle("Corrupted Database")
					.setMessage(
							"The database has been corrupted - please reinstall the application or contact a system administrator.")
					.setPositiveButton("Okay",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									finish();
								}
							}).show();
		}

	}

	private class ContentDownloaderTask extends
			AsyncTask<Void, Integer, String> {

		private Context context;
		private List<MenuItem> items;
		private Integer currentItemProcessed = 1;
		private File sdCard;

		public ContentDownloaderTask() {
			this.context = getApplicationContext();
			sdCard = Environment.getExternalStorageDirectory();
			progressDialog = new ProgressDialog(Initialize.this);
			progressDialog.setIndeterminate(true);
			progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			progressDialog.setCancelable(true);
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			progressDialog.setMessage("Please Wait...");
			progressDialog.show();
		}

		@Override
		protected String doInBackground(Void... voids) {
			items = DatabaseHelper.getInstance(getApplicationContext())
					.getItemsNotDownloaded();

			// take CPU lock to prevent CPU from going off if the user presses the power button during download
			PowerManager pm = (PowerManager) context
					.getSystemService(Context.POWER_SERVICE);
			PowerManager.WakeLock wl = pm.newWakeLock(
					PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());
			wl.acquire();

			try {
				InputStream input = null;
				OutputStream output = null;
				HttpURLConnection connection = null;
				String protocol = sharedPreferences.getString("url_protocol",
						"http");
				String setupAddress = sharedPreferences.getString("address",
						null);
				if (setupAddress == null || setupAddress.equals("")) {
					return null;
				}

				for (int i = 0; i < items.size(); i++) {
					try {
						URL url = new URL(protocol + "://" + setupAddress
								+ "/content/media/" + items.get(i).getMediaName());
						System.out.println("Fetching from: http://"
								+ setupAddress + "/content/media/"
								+ items.get(i).getMediaName());
						connection = (HttpURLConnection) url.openConnection();
						connection.connect();

						// expect HTTP 200 OK, so we don't mistakenly save error report instead of the file
						if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
							return "Server returned HTTP "
									+ connection.getResponseCode() + " "
									+ connection.getResponseMessage();

						int fileLength = connection.getContentLength();
						// download the file
						input = connection.getInputStream();

						File mediaDir = new File(Config.MENU_MEDIA_PATH_PREFIX);
						mediaDir.mkdirs();
						File outputFile = new File(mediaDir, items.get(i).getMediaName());
						System.out.println("Writing to: " + mediaDir + "/"
								+ items.get(i).getMediaName());
						try {
							outputFile.createNewFile();
						} catch (IOException e) {
							// Not much we can do if we can't write
							return e.toString();
						}

						output = new FileOutputStream(outputFile);

						byte data[] = new byte[4096];
						long total = 0;
						int count;
						while ((count = input.read(data)) != -1) {
							// allow canceling with back button
							if (isCancelled())
								return null;
							total += count;
							if (fileLength > 0) // only if total length is known
								publishProgress((int) (total * 100 / fileLength));
							output.write(data, 0, count);
						}

						DatabaseHelper.getInstance(Initialize.this)
								.setDownloaded(items.get(i).getID());
						publishProgress((int) (i * 100 / items.size()));
						currentItemProcessed++;
					} catch (Exception e) {
						return e.toString();
					} finally {
						try {
							if (output != null)
								output.close();
							if (input != null)
								input.close();
						} catch (IOException ignored) {
						}

						if (connection != null)
							connection.disconnect();
					}
				}
			} finally {
				wl.release();
			}
			return null;
		}

		@Override
		protected void onProgressUpdate(Integer... progress) {
			super.onProgressUpdate(progress);
			// if we get here, length is known, now set indeterminate to false
			progressDialog.setIndeterminate(false);
			progressDialog.setMessage("Downloading Components "
					+ currentItemProcessed + "/" + items.size());
			progressDialog.setMax(100);
			progressDialog.setProgress(progress[0]);
		}

		@Override
		protected void onPostExecute(String result) {
			progressDialog.dismiss();
			Intent i = new Intent(getApplicationContext(), Home.class);
			startActivity(i);
			finish();
		}
	}
}
