package cn.githan.mapshareandroidclient;

import android.util.Log;

import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.serialization.ObjectSerializationCodecFactory;
import org.apache.mina.transport.socket.nio.NioSocketConnector;

import java.net.InetSocketAddress;

/**
 * Created by BW on 16/6/7.
 */
public class SocketClient {
    NioSocketConnector connector = new NioSocketConnector();
    DefaultIoFilterChainBuilder chain = connector.getFilterChain();
    private static SocketClient client = null;
    private static String serverAddress = "home.githan.cn";//服务器IP地址
    private static int serverPort = 8008; //服务器端口
    private static final String LOG = "MSG";

    public static SocketClient getInstance() {
        if (null == client) {
            Log.d(LOG, "New SocketClient created");
            client = new SocketClient();
        }
        return client;
    }

    public SocketClient() {
        Log.d(LOG, "Start connecting to server...");
        //添加过滤器，仅接收对象
        chain.addLast("LocationObject", new ProtocolCodecFilter(new ObjectSerializationCodecFactory()));
        //绑定handler
        connector.setHandler(SocketHandler.getInstance());
        //开始连接服务器
        connector.connect(new InetSocketAddress(serverAddress, serverPort));
    }
}
