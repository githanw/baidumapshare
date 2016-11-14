package cn.githan.mapshareandroidclient;

import android.os.Bundle;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import static cn.githan.mapshareandroidclient.MainActivity.networkHandler;

/**
 * Created by BW on 16/6/16.
 */
public class SocketUtil implements Runnable {
    private static final String LOG = "MSG";
    static Socket socket;
    static ObjectInputStream ois;
    static ObjectOutputStream oos;
    MyLocation receivedLocation;

    @Override
    public void run() {
        try {
            socket = new Socket(MainActivity.SERVER_ADD, MainActivity.SERVER_PORT);
            ois = new ObjectInputStream(socket.getInputStream());
            oos = new ObjectOutputStream(socket.getOutputStream());
            readLocation(ois);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readLocation(ObjectInputStream objectInputStream) {
        try {
            receivedLocation = (MyLocation) objectInputStream.readObject();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        Bundle b = new Bundle();
        b.putSerializable("location", receivedLocation);
        Message msg = new Message();
        msg.setData(b);
        networkHandler.sendMessage(msg);
    }

    private static void writeLocation(MyLocation location){
        if (socket != null){
            try {
                oos.writeObject(location);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else {
            //Server连接不成功
            Log.d(LOG, "Session is null, nothing written.");
        }
    }
}
