package IPC;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class SocketServer
{
    private ServerSocket server;
    private SocketListener listener;

    public SocketServer()
    {
        create("127.0.0.1", 8888);
    }

    public SocketServer(String host, int port)
    {
        create(host, port);
    }

    private void create(String host, int port)
    {
        try {
            server = new ServerSocket();
            server.bind(new InetSocketAddress(host, port));
            System.out.println("Listening on " + host + ":" + port);
            listener = new SocketListener();
        } catch (Exception err)
        {
            err.printStackTrace();
        }
    }

    public void on(String event, SocketListener.Observer observer) {
        listener.register(event, observer);
    }

    public void waitForConnection()
    {
        try {
            Socket socket = server.accept();
            SocketWorker worker = new SocketWorker(socket, listener);
            worker.start();
        }
        catch (Exception err)
        {
            err.printStackTrace();
        }
    }

    public void destroy()
    {
        try
        {
            server.close();
        } catch (Exception err)
        {
            err.printStackTrace();
        }

    }
}