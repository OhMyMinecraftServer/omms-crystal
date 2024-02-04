package icu.takeneko.omms.crystal.plugin.api.annotations;

import icu.takeneko.omms.crystal.event.Event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface EventHandler {
    Class<? extends Event> event();
}
