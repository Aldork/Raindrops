package com.kinetiqa.glacier.ui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import android.app.Activity;
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
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.kinetiqa.glacier.R;
import com.kinetiqa.glacier.core.Config;
import com.kinetiqa.glacier.database.DatabaseHelper;
import com.kinetiqa.glacier.menu.MenuItem;
import com.kinetiqa.glacier.menu.MenuXMLSetupParser;

/**
 * Run initially when no data has been downloaded to the app yet
 * 
 * @author Tom
 * 
 */
public class Setup extends Activity {

	private SharedPreferences sharedPreferences;
	private File rootDir;

	// Android Components
	private TextView feedbackTextView;
	private EditText setupAddressEditText;
	private Button continueButton;
	private ProgressDialog progressDialog;

	// Setup Fields
	private String protocol;
	private String setupAddress;

	@Override
	public void onCreate(Bundle savedInstanceState) {

		String sampleXML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
				"<menu id=\"root\" requires=\"\" name=\"Main\" desc=\"\">\n" +
					"<menu id=\"151sd5\" name=\"Safety\" requires=\"\" desc=\"\">\n" +
					"    <item id=\"40j03G\" type=\"1\" activity=\"true\" path=\"sample.mp4\" requires=\"\" desc=\"\">Safety Item 1</item>\n" +
					"    <item id=\"40j03H\" type=\"1\" activity=\"true\" path=\"sample.mp4\" requires=\"\" desc=\"\">Safety Item 2</item>\n" +
					"</menu>\n" +
					"<menu id=\"151sd6\" name=\"Test\" requires=\"151sd5\" desc=\"\">\n" +
					"    <item id=\"40j03I\" type=\"1\" activity=\"true\" path=\"sample.mp4\" requires=\"\" desc=\"\">Test Item 1</item>\n" +
					"</menu>\n" +
				"</menu>\n";

		byte b[]=sampleXML.getBytes();

		OutputStream output = null;
		rootDir = new File(Config.CONTENT_DIR_PATH);
		rootDir.mkdirs();
		File outputFile = new File(rootDir, "menu.xml");
		try {
			output = new FileOutputStream(outputFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		try {
			output.write(b);
		} catch (IOException e) {
			e.printStackTrace();
		}

		Log.i("MyActivity", "Hellloooo");

		super.onCreate(savedInstanceState);
		setContentView(R.layout.setup);

		sharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(Setup.this);
		rootDir = new File(Config.CONTENT_DIR_PATH);
		rootDir.mkdirs();

		Intent i = new Intent(getApplicationContext(), Home.class);
		startActivity(i);
		finish();
	}

	@Override
	public void onResume() {
		super.onResume();
		// TODO check update status
	}

	private class MenuStructureDownloaderTask extends
			AsyncTask<String, Integer, String> {

		private Context context;

		public MenuStructureDownloaderTask(Context context) {
			this.context = context;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			progressDialog.setMessage("Please Wait. Downloading Menu...");
			progressDialog.show();
		}

		@Override
		protected String doInBackground(String... sUrl) {
			// take CPU lock to prevent CPU from going off if the user
			// presses the power button during download
			PowerManager pm = (PowerManager) context
					.getSystemService(Context.POWER_SERVICE);
			PowerManager.WakeLock wl = pm.newWakeLock(
					PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());
			wl.acquire();

			try {
				InputStream input = null;
				OutputStream output = null;
				HttpURLConnection connection = null;
				try {
					URL url = new URL(sUrl[0]);
					connection = (HttpURLConnection) url.openConnection();
					connection.connect();

					// expect HTTP 200 OK, so we don't mistakenly save error
					// report
					// instead of the file
					if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
						return "Server returned HTTP "
								+ connection.getResponseCode() + " "
								+ connection.getResponseMessage();

					// this will be useful to display download percentage
					// might be -1: server did not report the length
					int fileLength = connection.getContentLength();

					// download the file
					input = connection.getInputStream();

					File outputFile = new File(rootDir, "menu.xml");
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
						// publishing the progress....
						if (fileLength > 0) // only if total length is known
							publishProgress((int) (total * 100 / fileLength));
						output.write(data, 0, count);
					}
				} catch (Exception e) {
					System.out.println(e.toString());
				} finally {
					try {
						if (output != null)
							output.close();
						if (input != null)
							input.close();
					} catch (IOException ignored) {
						System.out.println(ignored.toString());
					}

					if (connection != null)
						connection.disconnect();
				}
			} finally {
				wl.release();
			}

			// Parses menu XML file and establishes media files to download
			System.setProperty("org.xml.sax.driver",
					"org.xmlpull.v1.sax2.Driver");
			try {
				XMLReader reader = XMLReaderFactory.createXMLReader();
				reader.setContentHandler(new MenuXMLSetupParser(
						Setup.this));
				File xmlMenu = new File(rootDir, "menu.xml");
				InputStream inputStream = new FileInputStream(xmlMenu);

				Reader reader_input = new InputStreamReader(inputStream,
						"UTF-8");
				InputSource is = new InputSource(reader_input);
				reader.parse(is);

			} catch (SAXException e) {
				System.out.println(e.toString());
			} catch (IOException e) {
				System.out.println(e.toString());
				// TODO : deal with exceptions in a better way
			}

			return null;
		}

		@Override
		protected void onProgressUpdate(Integer... progress) {
			super.onProgressUpdate(progress);
			// if we get here, length is known, now set indeterminate to false
			progressDialog.setIndeterminate(false);
			progressDialog.setMax(100);
			progressDialog.setProgress(progress[0]);
		}

		@Override
		protected void onPostExecute(String result) {
			progressDialog.dismiss();
			if (result != null) {
				feedbackTextView.setText("Download error. Please try again.");
				System.out.println(result);
			} else {
				sharedPreferences
						.edit()
						.putString("address",
								setupAddressEditText.getText().toString())
						.commit();

				ContentDownloaderTask contentDownloader = new ContentDownloaderTask();
				contentDownloader.execute();
			}
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

			// take CPU lock to prevent CPU from going off if the user
			// presses the power button during download
			PowerManager pm = (PowerManager) context
					.getSystemService(Context.POWER_SERVICE);
			PowerManager.WakeLock wl = pm.newWakeLock(
					PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());
			wl.acquire();

			try {
				InputStream input = null;
				OutputStream output = null;
				HttpURLConnection connection = null;

				for (int i = 0; i < items.size(); i++) {
					try {
						URL url = new URL(protocol + "://" + setupAddress + "/content/media/"
								+ items.get(i).getMediaName());
						System.out.println("Fetching from: " + protocol + "://" + setupAddress + "/content/media/"
								+ items.get(i).getMediaName());
						connection = (HttpURLConnection) url.openConnection();
						connection.connect();

						// expect HTTP 200 OK, so we don't mistakenly save error
						// report
						// instead of the file
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
						System.out.println("Writing to: " + mediaDir + "/" + items.get(i).getMediaName());
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
						
						DatabaseHelper.getInstance(Setup.this).setDownloaded(items.get(i).getID());
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
			progressDialog.setMessage("Downloading Components " + currentItemProcessed + "/" + items.size());
			progressDialog.setMax(100);
			progressDialog.setProgress(progress[0]);
		}

		@Override
		protected void onPostExecute(String result) {
			progressDialog.dismiss();
			if (result != null) {
				feedbackTextView.setText("Download error. Please try again.");
				System.out.println(result);
			} else {
				finish();
			}
		}
	}
}
