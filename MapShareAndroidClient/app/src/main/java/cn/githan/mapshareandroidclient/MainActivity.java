package cn.githan.mapshareandroidclient;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.model.LatLng;

import org.apache.mina.filter.codec.serialization.ObjectSerializationCodecFactory;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by BW on 16/5/27.
 */
public class MainActivity extends Activity {

    public static final String SERVER_ADD = "192.168.31.157";
    public static final int SERVER_PORT = 8008;
    private static final String LOG = "MainActivity";
    private static BaiduMap baiduMap;
    private MapView mapView = null;
    private LocationClient locClient;
    private LocationClientOption locOption;
    private boolean isFirstLocate = true;
    private static final int SCAN_SPAN = 20000;//定位时间间隔;
    private static List<MyLocation> clientLocations = new ArrayList<>();//储存从Server端接收到的Client定位信息（每个client唯一一个对象）;
    private static List<Marker> markers = new ArrayList<>();//储存其他client的定位图标，每个client只有唯一一个对象；
    private Marker selfMarker;//本地定位图标
    public BitmapDescriptor selfBitmap; //本地图标图片资源
    public boolean networkAvailable = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SDKInitializer.initialize(getApplicationContext());
        setContentView(R.layout.activity_main);
        //初始化界面
        initViews();
        //检查网络环境
        checkNetwork();
        //创建与服务器的连接
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                SocketClient.getInstance();
//            }
//        }).start();

    }

    /**
     * 检查网络及GPS环境
     */
    private void checkNetwork() {
        if (!NetworkUtils.isNetworkConneted(getApplicationContext())) {
            Log.d(LOG, "Network is not available");
        } else if (!NetworkUtils.isGPSOpened(getApplicationContext())) {
            Log.d(LOG, "GPS is not available");
        } else {
            Log.d(LOG, "Network & GPS is good");
            networkAvailable = true;
        }
    }

    /**
     * 初始化Views
     */
    private void initViews() {
        mapView = (MapView) findViewById(R.id.mapView);
        baiduMap = mapView.getMap();
        baiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
        baiduMap.setMyLocationEnabled(true);
        locClient = new LocationClient(getApplicationContext());
        locOption = new LocationClientOption();
        locOption.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
        locOption.setCoorType("bd09ll");
        locOption.setScanSpan(SCAN_SPAN);
        locOption.setLocationNotify(true);
        locOption.setIgnoreKillProcess(true);
        locOption.setOpenGps(true);
        locOption.setIsNeedAddress(true);
        locOption.setIsNeedLocationDescribe(true);
        locClient.setLocOption(locOption);
        locClient.registerLocationListener(listener);
        locClient.start();
        selfBitmap = BitmapDescriptorFactory.fromResource(R.drawable.icon_marka);
        startService(new Intent(MainActivity.this, MyService.class));
    }

    BDLocationListener listener = new BDLocationListener() {
        @Override
        public void onReceiveLocation(BDLocation bdLocation) {
            if (networkAvailable) {
                //将本地定位数据转换成MyLocation类；
                MyLocation location = bdLocation2MyLocation(bdLocation);
                //经度波动，模拟走路
                location = imitateLongitudeFluctuation(location);
                //本地更新定位数据
                localUpdateOverlay(location);
                //封装location
                Bundle b = new Bundle();
                b.putSerializable("location", location);
                Intent i = new Intent("cn.githan.mapshareandroidclient.LOCAL_BROADCAST");
                i.putExtra("bundle", b);
                //通过广播发送给service
                sendBroadcast(i);
            } else {
                Toast.makeText(MainActivity.this, "Cannot connect server, please check your network & GPS", Toast.LENGTH_LONG).show();
            }
        }
    };

    /**
     * 通过经度位置波动模拟走路,波动范围经度0.000001~0.000009
     *
     * @param location 定位数据
     * @return
     */
    public MyLocation imitateLongitudeFluctuation(MyLocation location) {
        double[] doubles = {0.000001, 0.000003, 0.000005, 0.000007, 0.000009};
        int index = (int) (Math.random() * doubles.length);
        BigDecimal rate = new BigDecimal(Double.toString(doubles[index]));
        BigDecimal longitude = new BigDecimal(Double.toString(location.getLongitude()));
        location.setLongitude(longitude.add(rate).doubleValue());
        return location;
    }

    /**
     * 本地更新自己的定位数据
     *
     * @param location 获取到的定位数据
     */
    public void localUpdateOverlay(MyLocation location) {
        Message m = new Message();
        Bundle b = new Bundle();
        b.putSerializable("location", location);
        m.setData(b);
        localHandler.sendMessage(m);
    }

    /**
     * 将百度地图获取到的bdLocation转换成为自定义MyLocation
     *
     * @param bdLocation 百度地图所获取到的定位信息
     * @return MyLocation 类
     */
    public MyLocation bdLocation2MyLocation(BDLocation bdLocation) {
        MyLocation location = new MyLocation();
        location.setLatitude(bdLocation.getLatitude());
        location.setLongitude(bdLocation.getLongitude());
        return location;
    }


    private Handler localHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            //如果当前定位图标存在，则删除图标，再重新添加，达到图标刷新效果。
            if (selfMarker != null) {
                selfMarker.remove();
            }
            //获取传过来的数据包
            Bundle b = msg.getData();
            MyLocation location = (MyLocation) b.getSerializable("location");
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            //如果是第一次获取定位信息，则更新地图中心点位置
            if (isFirstLocate) {
                isFirstLocate = false;
                MapStatus ms = new MapStatus.Builder().target(latLng).zoom(19).build();
                MapStatusUpdate msu = MapStatusUpdateFactory.newMapStatus(ms);
                baiduMap.animateMapStatus(msu);
            }
            //添加自己的定位图标
            OverlayOptions options = new MarkerOptions().position(latLng).icon(selfBitmap);
            selfMarker = (Marker) baiduMap.addOverlay(options);
            Log.d(LOG, "selfMarker refreshed.");
        }
    };

    public static Handler networkHandler = new Handler() {
        BitmapDescriptor netBitmap;
        LatLng ll;
        OverlayOptions options;

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Bundle b = msg.getData();
            MyLocation location = (MyLocation) b.getSerializable("location");
            if ((location.getStatus() != null) && (location.getStatus().equals("sessionClose"))) {
                //如果获取到的数据包是某客户端关闭的广播数据包，则清除地图上过期的marker
                Log.d(LOG, "Detecting client close, start clean marker");
                cleanExpiredMarker(location);
                return;
            }
            netBitmap = BitmapDescriptorFactory.fromResource(R.drawable.net_marker);
            ll = new LatLng(location.getLatitude(), location.getLongitude());
            options = new MarkerOptions().position(ll).icon(netBitmap);

            if (clientLocations.size() == 0 || clientLocations == null) {
                markers.clear();
                //新增marker
                Log.d(LOG, "This is the first run. Start adding new marker. location.getID() = " + location.getId());
                addNewMarker(location);
            } else if (isNewClient(location)) {
                //新增marker
                Log.d(LOG, "New client detected. Start adding new marker. location.getID() = " + location.getId());
                addNewMarker(location);
            } else {
                //更新marker
                Log.d(LOG, "Start updating marker.location.getID() = " + location.getId());
                updateMarker(location);
            }
        }

        private void cleanExpiredMarker(MyLocation location) {
            // TODO: 16/6/15 有待测试
            if (clientLocations != null) {
                Log.d(LOG, "Start finding marker");
                //找到过期的marker，进行删除
                for (int i = 0; i < markers.size(); i++) {
                    if (markers.get(i).getTitle().equals(String.valueOf(location.getId()))) {
                        Marker marker = markers.get(i);
                        Log.d(LOG, "Marker delete. ID: " + marker.getTitle());
                        marker.remove();
                        markers.remove(i);
                    } else {
                        Log.d(LOG, "No match marker found");
                    }
                }
                Log.d(LOG, "Start finding client");
                //找到过期的client,进行删除
                for (int i = 0; i < clientLocations.size(); i++) {
                    if (clientLocations.get(i).getId() == location.getId()) {
                        Log.d(LOG, "Client delete: " + clientLocations.get(i).getId());
                        clientLocations.remove(i);
                    } else {
                        Log.d(LOG, "No match client found");
                    }
                }
            } else {
                Log.d(LOG, "clientLocations = null , no marker clean");
            }

        }

        /**
         * 对已存在的客户端定位图标进行更新
         * @param updateLocation 定位数据
         */
        private void updateMarker(MyLocation updateLocation) {
            for (int i = 0; i < markers.size(); i++) {
                if (markers.get(i).getTitle().equals(String.valueOf(updateLocation.getId()))) {
                    //删除原来的marker
                    markers.get(i).remove();
                    //在地图上添加marker，并且保存到updateMarker
                    Marker updateMarker = (Marker) baiduMap.addOverlay(options);
                    //设置updateMarker的session ID
                    updateMarker.setTitle(String.valueOf(updateLocation.getId()));
                    //将updateMarker保存至markers中
                    markers.set(i, updateMarker);
                    Log.d(LOG, "Updating marker succeed");
                }
            }
        }

        /**
         * 在地图上新增一个客户端定位图标
         * @param location 定位数据
         */
        private void addNewMarker(MyLocation location) {
            //将接收到的location存放入clientLocation中以便管理
            clientLocations.add(location);
            //在地图上添加marker
            Marker newMarker = (Marker) baiduMap.addOverlay(options);
            newMarker.setTitle(String.valueOf(location.getId()));
            //将marker存放入markers中以便管理
            markers.add(newMarker);
            Log.d(LOG, "Adding new marker succeed");
        }

        /**
         * 判断接收到的定位数据包是否属于现有的客户端
         * @param location 定位数据
         * @return true false
         */
        private boolean isNewClient(MyLocation location) {
            for (int i = 0; i < clientLocations.size(); i++) {
                if (clientLocations.get(i).getId() == location.getId()) {
                    //不是新的客户端,将此客户端的数据更新进clientLocasions中
                    clientLocations.set(i, location);
                    return false;
                }
            }
            return true;
        }
    };

    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (locClient.isStarted()) {
            locClient.unRegisterLocationListener(listener);
            locClient.stop();
        }
        stopService(new Intent(MainActivity.this, MyService.class));
        mapView.onDestroy();
    }


    @Override
    protected void onResume() {
        super.onResume();
        // 在activity执行onResume时执行mMapView. onResume ()，实现地图生命周期管理
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 在activity执行onPause时执行mMapView. onPause ()，实现地图生命周期管理
        mapView.onPause();
    }

}
