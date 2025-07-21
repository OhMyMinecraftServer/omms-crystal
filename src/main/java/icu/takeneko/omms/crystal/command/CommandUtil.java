package icu.takeneko.omms.crystal.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;

import java.lang.reflect.Field;
import java.util.Map;

public class CommandUtil {

    @SuppressWarnings("all")
    public static <S> String unregisterCommand(LiteralArgumentBuilder<S> command, CommandDispatcher<S> dispatcher) {
        try {
            Field field = dispatcher.getClass().getDeclaredField("root");
            field.setAccessible(true);
            RootCommandNode<S> rootCommandNode = (RootCommandNode<S>) field.get(dispatcher);
            Class<?> clazz = rootCommandNode.getClass().getSuperclass();
            Field childrenField = clazz.getDeclaredField("children");
            childrenField.setAccessible(true);
            Field literalsField = clazz.getDeclaredField("literals");
            literalsField.setAccessible(true);
            Map<String, CommandNode<S>> children = (Map<String, CommandNode<S>>) childrenField.get(rootCommandNode);
            Map<String, LiteralCommandNode<S>> literals = (Map<String, LiteralCommandNode<S>>) literalsField.get(rootCommandNode);
            var node = command.build();
            if (literals.containsKey(node.getLiteral())) {
                literals.remove(node.getLiteral(), node);
            }
            if (children.containsKey(node.getLiteral())) {
                children.remove(node.getLiteral(), node);
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return e.toString();
        }
    }



}
