package net.zhuruoling.omms.crystal.util;

import net.zhuruoling.omms.crystal.parser.MinecraftParser;
import net.zhuruoling.omms.crystal.plugin.PluginInitializer;
import net.zhuruoling.omms.crystal.plugin.api.annotations.EventHandler;
import net.zhuruoling.omms.crystal.plugin.api.annotations.Parser;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

public class PluginUtil {
    @NotNull
    public static HashMap<String, Method> getPluginDeclaredEventHandlerMethod(@NotNull Class<? extends PluginInitializer> clazz) {
        var map = new HashMap<String, Method>();
        for (Method declaredMethod : clazz.getDeclaredMethods()) {
            var annotation = declaredMethod.getAnnotation(EventHandler.class);
            if (annotation != null) {
                String e = annotation.event();
                map.put(e, declaredMethod);
            }
        }
        return map;
    }

    @NotNull
    @Contract(pure = true)
    public static HashMap<String, MinecraftParser> getPluginDeclaredParser(@NotNull Class<? extends PluginInitializer> clazz, PluginInitializer main) {
        HashMap<String,MinecraftParser> minecraftParserHashMap = new HashMap<>();
        for (Class<?> declaredClass : clazz.getDeclaredClasses()) {
            var annotation = declaredClass.getAnnotation(Parser.class);
            if (annotation!= null){
                try {
                    var constructor = declaredClass.getConstructor(clazz);
                    constructor.setAccessible(true);
                    minecraftParserHashMap.put(annotation.name(), (MinecraftParser) constructor.newInstance(main));
                }catch (NoSuchMethodException | InstantiationException | IllegalAccessException |
                        InvocationTargetException e){
                    throw new RuntimeException(e);
                }
            }
        }

        return minecraftParserHashMap;
    }
}
