package cn.githan.mapshareandroidclient;

import android.os.Bundle;
import android.os.Message;
import android.util.Log;

import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;

import static cn.githan.mapshareandroidclient.MainActivity.networkHandler;

/**
 * Created by BW on 16/6/7.
 */
public class SocketHandler extends IoHandlerAdapter {
    private static final String LOG = "MSG";
    private static SocketHandler handler = null;
    private IoSession ioSession = null;
    private MyLocation receivedLocation = null;
    private Message msg;
    private Bundle b;

    public static SocketHandler getInstance() {
        if (null == handler) {
            handler = new SocketHandler();
        }
        return handler;
    }

    public SocketHandler() {
    }

    @Override
    public void sessionCreated(IoSession session) throws Exception {
        super.sessionCreated(session);
        ioSession = session;
    }

    @Override
    public void messageReceived(IoSession session, Object message) throws Exception {
        super.messageReceived(session, message);
        //接收到数据后，将数据发送给network handler进行判断和更新
        receivedLocation = (MyLocation) message;
        b = new Bundle();
        b.putSerializable("location", receivedLocation);
        msg = new Message();
        msg.setData(b);
        networkHandler.sendMessage(msg);
    }

    @Override
    public void messageSent(IoSession session, Object message) throws Exception {
        super.messageSent(session, message);
        Log.d(LOG, "Message sent. CONTENT: " + ((MyLocation) message).toString());
    }

    @Override
    public void sessionClosed(IoSession session) throws Exception {
        super.sessionClosed(session);
        if (ioSession != null) {
            ioSession = null;
            Log.d(LOG, "Session is closed");
        }
    }

    @Override
    public void sessionOpened(IoSession session) throws Exception {
        super.sessionOpened(session);
        Log.d(LOG, "session is opened. " + " server address: " + session.getRemoteAddress());
    }

    public void writeObject(MyLocation location) {
        if (ioSession != null) {
            ioSession.write(location);
        } else {
            //Server连接不成功
            Log.d(LOG, "Session is null, nothing written.");
        }
    }
}
