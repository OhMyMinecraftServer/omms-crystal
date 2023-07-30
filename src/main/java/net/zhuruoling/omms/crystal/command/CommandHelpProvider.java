package net.zhuruoling.omms.crystal.command;


import net.kyori.adventure.text.TextComponent;

@FunctionalInterface
public interface CommandHelpProvider {
    String invoke();
}
