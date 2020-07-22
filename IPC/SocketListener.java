package IPC;

import java.util.ArrayList;
import java.util.List;

public class SocketListener {
    private static class ObserverRegistration {
        public Observer observer;
        public String event;
        public ObserverRegistration(String event, Observer observer){
            this.observer = observer;
            this.event = event;
        };
    }

    private final List<ObserverRegistration> listeners = new ArrayList<>();

    public interface Observer{
        void update (SocketData data);
    }

    public void notify(SocketData data)
    {
        listeners.forEach(listener -> {
            if (listener.event.equals("$MESSAGE") || listener.event.equals(data.channel)) {
                if (data != null)
                {
                    listener.observer.update(data);
                }
            }
        });
    }

    public void register(String event, Observer observer)
    {
        listeners.add(new ObserverRegistration(event, observer));
    }
}
