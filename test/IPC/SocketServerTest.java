package test.IPC;

import IPC.SocketData;
import IPC.SocketServer;
import org.junit.Test;

public class SocketServerTest {

    private static void onMessage (SocketData socket) {
        if (socket.message == null)
        {
            return;
        }
        if (socket.message.equals("getLoop"))
        {
            for(int i = 0; i<=10000;i++)
            {
                socket.worker.send("$MESSAGE", String.valueOf(i));
            }
        } else {
            socket.worker.send(socket.channel, socket.message);
        }
    }

    private static void onDisconnect (SocketData socket) {
        System.out.println("DISCONNECT");
    }

    private static void onConnect (SocketData socket) {
        System.out.println("CONNECT");
        socket.worker.send("$UPDATE", "asdfasdf");
    }

    private static void onIssue (SocketData socket) {
        socket.error.printStackTrace();
    }

    @Test
    public void SocketServer () {
        SocketServer server = new SocketServer();
        int numberOfConnections = 1;
        server.on("$CONNECT", SocketServerTest::onConnect);
        server.on("$MESSAGE", SocketServerTest::onMessage);
        server.on("$DISCONNECT", SocketServerTest::onDisconnect);
        server.on("$ISSUE", SocketServerTest::onIssue);

        for (int i = 0; i < numberOfConnections; i ++)
        {
            server.waitForConnection();
        }
        while (true){}
    }
}
