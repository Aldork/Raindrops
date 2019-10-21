package com.kinetiqa.glacier.utils;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;

import com.kinetiqa.glacier.R;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Tom on 2014-11-02.
 */
public class Utils {

    public static String generateAlphaNumeric(int len) {
        String ALPHA_NUM = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        StringBuffer sb = new StringBuffer(len);
        for (int i = 0; i < len; i++) {
            int ndx = (int) (Math.random() * ALPHA_NUM.length());
            sb.append(ALPHA_NUM.charAt(ndx));
        }
        return sb.toString();
    }

    /**
     * @return True if there's an active wifi or data connection; False
     *         otherwise
     */
    public static boolean isConnectedToInternet(Context c) {

        boolean haveConnectedWifi = false;
        boolean haveConnectedMobile = false;

        ConnectivityManager cm = (ConnectivityManager) c
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo[] netInfo = cm.getAllNetworkInfo();
        for (NetworkInfo ni : netInfo) {
            if (ni.getTypeName().equalsIgnoreCase("WIFI"))
                if (ni.isConnected())
                    haveConnectedWifi = true;
            if (ni.getTypeName().equalsIgnoreCase("MOBILE"))
                if (ni.isConnected())
                    haveConnectedMobile = true;
        }

        return haveConnectedWifi || haveConnectedMobile;
    }

    public static long getLengthOfVideoFileMilliseconds(Context c, String path) {


        Pattern pattern = Pattern.compile("([^\\/]+$)");

        Matcher matcher = pattern.matcher(path);

        if(matcher.find())
        {
            String videoNameWithExtension = matcher.group(1);

            String videoName = videoNameWithExtension.replaceAll("\\.(.*)", "");

            Uri uri = Uri.parse("android.resource://" + c.getPackageName() + "/raw/" + videoName);

            MediaPlayer mp = MediaPlayer.create(c, uri);
            int duration = mp.getDuration();
            mp.release();
            return duration;
        }

        return 0;
    }

}
