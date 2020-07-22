package IPC;

import java.util.Base64;
import java.util.BitSet;

public class SocketData
{
    public String message;
    public String channel;
    public Exception error;
    public SocketWorker worker;

    public SocketData(String message, String channel) {
        this.channel = channel;
        this.message = message;
    }

    public static SocketData parse(String data)
    {
        String decoded = new String(Base64.getDecoder().decode(data));
        String[] parsed = decoded.split(":", 2);
        return new SocketData(parsed[1], parsed[0]);
    }

    public String serialize()
    {
        String data = channel + ":" + message;
        //return data;
        return Base64.getEncoder().encodeToString(data.getBytes());
    }

    public boolean shouldDisconnect()
    {
        return channel.equals("$DISCONNECT") && message.equals("bye");
    }

    public void attachException(Exception err)
    {
        this.error = err;
    }

    @Override
    public String toString() {
        return channel + ": " + message;
    }
}