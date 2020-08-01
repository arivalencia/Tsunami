
package com.example.tsunami;

public class Event {
    public final String title;
    public final long time;
    public final int tsunamiAlert;

    public Event(String eventTitle, long eventTime, int eventTsunamiAlert) {
        title = eventTitle;
        time = eventTime;
        tsunamiAlert = eventTsunamiAlert;
    }
}

