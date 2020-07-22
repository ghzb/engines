package IPC;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;

public class SocketWorker extends Thread
{
    Socket socket;
    SocketListener listener;
    BufferedInputStream input;
    public BufferedOutputStream output;

    //int counter = 0;

    public SocketWorker(Socket socket, SocketListener listener)
    {
        try {
            this.socket = socket;
            this.listener = listener;
            this.input = new BufferedInputStream(socket.getInputStream());
            this.output = new BufferedOutputStream(socket.getOutputStream());
        } catch (Exception err)
        {
            err.printStackTrace();
        }
    }

    private SocketData makeData(String message, String channel, Exception err){
        SocketData data = new SocketData(message, channel);
        data.worker = this;
        if (err != null)
        {
            data.error = err;
        }
        return data;
    }

    private SocketData makeData(String message, String channel){
        return makeData(message, channel, null);
    }


    public void send(String channel, String message)
    {
        try {
            if (message == null)
            {
                message = "$NULL";
            }
            SocketData data = new SocketData(message, channel);
            byte[] out = (data.serialize() + "\n").getBytes("UTF-8");
            output.write(out);
            output.flush();
        } catch (SocketException ignore) {}
        catch (Exception err)
        {
            err.printStackTrace();
        }

    }

    public void run()
    {
        try {

            BufferedReader reader = new BufferedReader(new InputStreamReader(input));

            SocketData data = makeData(null, "$CONNECT");
            send("$CONNECT", null);
            listener.notify(data);

            while (true)
            {
                String encoding = reader.readLine();
                if (encoding == null) {
                    break;
                }
                data = SocketData.parse(encoding);
                data.worker = this;
                if (!data.shouldDisconnect())
                {
                    listener.notify(data);
                }
            }
        }
        catch (SocketException err) {
            listener.notify(makeData(null, "$DISCONNECT"));
        }
        catch (Exception err)
        {
            listener.notify(makeData(null, "$ISSUE", err));
        }
        finally {
            try {
                input.close();
            } catch (Exception ignored){}
            try {
                output.close();
            } catch (Exception ignored){}
            try {
                socket.close();
            } catch (Exception ignored){}
        }
        return;
    }

    public static class DisconnectException extends NullPointerException {
        @Override
        public String getMessage()
        {
            return "User disconnected";
        }
    }
}
