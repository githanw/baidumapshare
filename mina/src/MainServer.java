import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.filter.stream.StreamWriteFilter;
import org.apache.mina.transport.socket.SocketAcceptor;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * Created by BW on 16/4/10.
 */
public class MainServer {
    private static MainServer mainServer = null;
    private SocketAcceptor socketAcceptor = new NioSocketAcceptor();
    private DefaultIoFilterChainBuilder chain = socketAcceptor.getFilterChain();
    private final int BIND_PORT = 8008;

    public static MainServer getInstance() {
        if (null == mainServer) {
            mainServer = new MainServer();
        }
        return mainServer;
    }

    private MainServer() {
        chain.addLast("location", new StreamWriteFilter());
        socketAcceptor.setHandler(ServerHandler.getInstance());
        try {
            socketAcceptor.bind(new InetSocketAddress(BIND_PORT));
            socketAcceptor.getSessionConfig().setReceiveBufferSize(1024);
            socketAcceptor.getSessionConfig().setIdleTime(IdleStatus.BOTH_IDLE, 10);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        MainServer.getInstance();
    }
}
