package net.zhuruoling.omms.crystal.plugin.api;

import net.zhuruoling.omms.crystal.event.Event;
import net.zhuruoling.omms.crystal.event.EventsKt;

public class EventApi {
    public static void addEvent(String id, Event event){
        EventsKt.addEvent(id, event);
    }
}
