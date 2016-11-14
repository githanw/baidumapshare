package cn.githan.mapshareandroidclient;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;

import static cn.githan.mapshareandroidclient.MainActivity.networkHandler;

public class MyService extends Service {
    private static final String LOG = "MyService";
    static Socket socket;
    MyLocation receivedLocation;
    static OutputStream os;
    static InputStream is;
    Bundle bundle;
    Message msg;

    public MyService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(LOG, "MyService is started");
        new Thread(new Runnable() {
            @Override
            public void run() {
                // TODO: 16/6/16 建立Socket连接
                connectServer();
            }
        }).start();
        return super.onStartCommand(intent, flags, startId);
    }

    private void connectServer() {
        try {
            socket = new Socket(MainActivity.SERVER_ADD, MainActivity.SERVER_PORT);
            Log.d(LOG,"socket is good");
            os = socket.getOutputStream();
            is =socket.getInputStream();
            readLocation(socket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readLocation(Socket socket) {
        //test
//        BufferedInputStream bufis;
//        try {
//            bufis = new BufferedInputStream(socket.getInputStream());
//            while (true){
//                int read = 0;
//                byte[] bytes = new byte[bufis.available()];
//                bufis.read(bytes,0,bytes.length);
//                System.out.println("读取完成，准备解析：");
//                Object o  = byteToObject(bytes);
//                if (o instanceof MyLocation){
//                    System.out.println("解析成功："+o.toString());
//                }else {
//                    System.out.println("解析失败。");
//                }
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        //InputStream
        try {
            while (true) {
                int count = 0;
                byte[] b;
                while (count == 0) {
                    count = is.available();
                }
                b = new byte[count];
                int readCount = 0; // 已经成功读取的字节的个数
                while (readCount < count) {
                    readCount += is.read(b, readCount, count - readCount);
                }
                //将byte[]转换成MyLocation对象
                receivedLocation = (MyLocation) byteToObject(b);
                Log.d(LOG,"Received location: " + receivedLocation.toString());
                //将MyLocation对象更新在地图上
                bundle = new Bundle();
                msg = new Message();
                bundle.putSerializable("location", receivedLocation);
                msg.setData(bundle);
                networkHandler.sendMessage(msg);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        //ObjectInputStream
//        try {
//            ois = new ObjectInputStream(socket.getInputStream());
//            MyLocation location ;
//            while (true){
//                location = (MyLocation) ois.readObject();
//                if (location!=null){
//                    System.out.println("a object read: "+location.toString());
//                }
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        } catch (ClassNotFoundException e) {
//            e.printStackTrace();
//        }


//        try {
//            ois = new ObjectInputStream(socket.getInputStream());
//            receivedLocation = (MyLocation) ois.readObject();
//        } catch (IOException e) {
//            e.printStackTrace();
//        } catch (ClassNotFoundException e) {
//            e.printStackTrace();
//        }
//        Bundle b = new Bundle();
//        b.putSerializable("location", receivedLocation);
//        Message msg = new Message();
//        msg.setData(b);
//        networkHandler.sendMessage(msg);


    }

    private static void writeLocation(MyLocation location) {
//        if (socket.isConnected()&&(!socket.isClosed())) {
//            try {
//                oos = new ObjectOutputStream(socket.getOutputStream());
//                oos.writeObject(location);
//                oos.flush();
//                Log.d(LOG, "Message sent. CONTENT: " + (location.toString()));
//            } catch (IOException e) {
//                Log.d(LOG,"Error in writing location");
//                e.printStackTrace();
//            }
//        } else {
//            //Server连接不成功
//            Log.d(LOG, "Session is null, nothing written.");
//        }

        byte[] bytes = objectToByte(location);
        try {
            os.write(bytes);
            os.flush();
            Log.d(LOG, "Message sent. CONTENT: " + (location.toString()));
        } catch (IOException e) {
            e.printStackTrace();
        }

        //ObjectOutputStream
//        try {
//            oos.writeObject(location);
//            oos.flush();
//            oos.reset();
//            System.out.println("writing succeed");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }


    }


    // 网上抄来的，将 int 转成字节
    public static byte[] i2b(int i) {
        return new byte[]{(byte) ((i >> 24) & 0xFF),
                (byte) ((i >> 16) & 0xFF), (byte) ((i >> 8) & 0xFF),
                (byte) (i & 0xFF)};
    }

    public static int b2i(byte[] b) {
        int value = 0;
        for (int i = 0; i < 4; i++) {
            int shift = (4 - 1 - i) * 8;
            value += (b[i] & 0x000000FF) << shift;
        }
        return value;
    }

    public static byte[] objectToByte(Object obj) {
        byte[] bytes = new byte[1024];
        try {
            // object to bytearray
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            ObjectOutputStream oo = new ObjectOutputStream(bo);
            oo.writeObject(obj);

            bytes = bo.toByteArray();

            bo.close();
            oo.close();
        } catch (Exception e) {
            System.out.println("translation" + e.getMessage());
            e.printStackTrace();
        }
        return (bytes);
    }

    private static Object byteToObject(byte[] bytes) {
        java.lang.Object obj = new java.lang.Object();
        try {
            // bytearray to object
            ByteArrayInputStream bi = new ByteArrayInputStream(bytes);
            ObjectInputStream oi = new ObjectInputStream(bi);

            obj = oi.readObject();

            bi.close();
            oi.close();
        } catch (Exception e) {
            System.out.println("translation" + e.getMessage());
            e.printStackTrace();
        }
        return obj;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            if (is!=null){
                is.close();
            }
            if (os!=null){
                os.close();
            }

            if (socket != null || socket.isConnected()) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static class MyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            //接收到广播之后，将定位数据传到服务器
            Bundle b = intent.getBundleExtra("bundle");
            MyLocation myLocation = (MyLocation) b.getSerializable("location");
            if (socket != null && socket.isConnected()) {
                writeLocation(myLocation);
            }else {
                Log.d(LOG,"Socket is null, nothing written");
            }
        }
    }

}
