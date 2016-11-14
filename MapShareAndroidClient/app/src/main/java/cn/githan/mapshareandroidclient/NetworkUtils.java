package cn.githan.mapshareandroidclient;

import android.content.Context;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Created by BW on 6/14/16.
 */
public class NetworkUtils {
    /**
     * 判断网络是否连接
     *
     * @param paramContext 当前上下文
     * @return
     */
    public static boolean isNetworkConneted(Context paramContext) {
        boolean b = false;
        if (paramContext != null) {
            NetworkInfo localNetworkInfo = ((ConnectivityManager) paramContext.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
            if (localNetworkInfo != null) {
                b = localNetworkInfo.isAvailable();
            }
        }
        return b;
    }

    /**
     * 判断GPS/AGPS 是否可以用
     *
     * @param paramContext 当前上下文
     * @return
     */
    public static boolean isGPSOpened(Context paramContext) {
        LocationManager locationManager = (LocationManager) paramContext.getSystemService(Context.LOCATION_SERVICE);
        boolean gps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean agps = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if (gps || agps) {
            return true;
        }
        return false;
    }
}
