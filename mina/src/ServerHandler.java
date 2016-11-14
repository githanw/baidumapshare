import cn.githan.mapshareandroidclient.MyLocation;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by BW on 16/6/7.
 */
public class ServerHandler extends IoFilterAdapter implements IoHandler {
    private static ServerHandler handler = null;
    private List<IoSession> allSessions = new ArrayList<>();
    private MyLocation location = null;
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置日期格式

    public static ServerHandler getInstance() {
        if (null == handler) {
            handler = new ServerHandler();
        }
        return handler;
    }

    public ServerHandler() {
    }

    @Override
    public void sessionCreated(IoSession session) throws Exception {
        System.out.println("===========================");
        System.out.println("MSG: Session created. ID:" + session.getId() + " STATUS: success. TIME: " + sdf.format(new Date()));
        System.out.println("Current session list: ");
        allSessions.add(session);
        for (int i = 0; i < allSessions.size(); i++) {
            System.out.println("ID:" + allSessions.get(i).getId() + " IP:" + allSessions.get(i).getRemoteAddress());
        }
        System.out.println("===========================");
    }

    @Override
    public void sessionIdle(NextFilter nextFilter, IoSession session, IdleStatus status) throws Exception {
        System.out.println("session on idle. ID: " + session.getId());
        if (status == IdleStatus.BOTH_IDLE) {
            System.out.println("session forced closed. ID: " + session.getId());
            allSessions.remove(session);
            session.closeNow();
        }
    }

    @Override
    public void sessionOpened(IoSession ioSession) throws Exception {

    }

    @Override
    public void messageReceived(IoSession session, Object message) throws Exception {
        System.out.println("message received");
        if (message instanceof IoBuffer) {
            //将message解析成MyLocation对象;
            IoBuffer buffer = (IoBuffer) message;
            Object o = IoBuffer2Object(buffer);
            if (o instanceof MyLocation) {
                //将sessionID封装进对象中
                MyLocation location = (MyLocation) o;
                location.setId(session.getId());
                //将对象解析成ioBuffer,再发送给其他客户端
                writeSessions(session, "Normal update", object2IoBuffer(location));
            } else {
                System.out.println("Message is not a MyLocation instance");
            }
        }
    }

    @Override
    public void sessionClosed(IoSession session) throws Exception {
        //向所有其他客户端发送一个关闭客户端的数据包MyLocation
        location = new MyLocation();
        location.setStatus("sessionClose");
        location.setId(session.getId());
        writeSessions(session, "Session closed", object2IoBuffer(location));
        System.out.println("MSG: A session close. ID:" + session.getId() + " " + session.getRemoteAddress() + " TIME: " + sdf.format(new Date()));
        allSessions.remove(session);
    }

    /**
     * 将object转换成为IoBuffer
     *
     * @param o object
     * @return iobuffer
     */
    public IoBuffer object2IoBuffer(Object o) {
        byte[] bytes = objectToByte(o);
        IoBuffer ioBuffer = IoBuffer.allocate(bytes.length);
        ioBuffer.put(bytes, 0, bytes.length);
        ioBuffer.flip();
        return ioBuffer;
    }

    /**
     * 将IoBuffer转换成为Object
     *
     * @param buffer Iobuffer
     * @return Object
     */
    public Object IoBuffer2Object(IoBuffer buffer) {
        byte[] bytes = new byte[buffer.limit()];
        buffer.get(bytes);
        return byteToObject(bytes);
    }

    /**
     * 除了当前session, 向所有其他session发送数据
     *
     * @param session 当前session
     */
    private void writeSessions(IoSession session, String str, IoBuffer o) {
        for (IoSession ioSession : allSessions) {
            if (ioSession != session) {
                System.out.println("Start writing to session(ID:" + ioSession.getId() + ")  Status: " + str);
                //发送location到其他客户端
                ioSession.write(o);
            }
        }
    }

    @Override
    public void sessionIdle(IoSession ioSession, IdleStatus idleStatus) throws Exception {

    }

    @Override
    public void exceptionCaught(IoSession ioSession, Throwable throwable) throws Exception {

    }

    @Override
    public void messageSent(IoSession session, Object message) throws Exception {
    }

    @Override
    public void inputClosed(IoSession ioSession) throws Exception {

    }

    /**
     * 将Object转换成为byte[]
     *
     * @param obj Object
     * @return byte[]
     */
    public byte[] objectToByte(Object obj) {
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

    /**
     * 将byte[] 转换成为Object
     *
     * @param bytes byte[]
     * @return Object
     */
    private Object byteToObject(byte[] bytes) {
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
}

