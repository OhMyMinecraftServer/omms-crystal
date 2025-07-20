package icu.takeneko.omms.crystal.util;

import icu.takeneko.omms.crystal.parser.MinecraftParser;
import icu.takeneko.omms.crystal.plugin.PluginInitializer;
import icu.takeneko.omms.crystal.plugin.api.annotations.EventHandler;
import icu.takeneko.omms.crystal.plugin.api.annotations.Parser;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

public class PluginUtil {

    @NotNull
    @Contract(pure = true)
    public static HashMap<String, MinecraftParser> getPluginDeclaredParser(@NotNull Class<? extends PluginInitializer> clazz, PluginInitializer main) {
        HashMap<String, MinecraftParser> minecraftParserHashMap = new HashMap<>();
        for (Class<?> declaredClass : clazz.getDeclaredClasses()) {
            var annotation = declaredClass.getAnnotation(Parser.class);
            if (annotation != null) {
                try {
                    var constructor = declaredClass.getConstructor(clazz);
                    constructor.setAccessible(true);
                    minecraftParserHashMap.put(annotation.name(), (MinecraftParser) constructor.newInstance(main));
                } catch (NoSuchMethodException | InstantiationException | IllegalAccessException |
                         InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        return minecraftParserHashMap;
    }
}
