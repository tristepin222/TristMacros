package com.tristsmacro;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;


import java.nio.file.Path;

import net.minecraft.util.Util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class ClientCommandHandler {

    private static final Path MACRO_FOLDER = Paths.get(MinecraftClient.getInstance().runDirectory.getPath(), "macro_mod_macros");

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {

        dispatcher.register(ClientCommandManager.literal("macro")
            .then(ClientCommandManager.literal("create")
                .then(ClientCommandManager.argument("name", StringArgumentType.word())
                    .executes(ctx -> {
                        String name = StringArgumentType.getString(ctx, "name");
                        return createMacroFile(name, ctx.getSource());
                    })
                )
            )
            .then(ClientCommandManager.literal("execute")
                .then(ClientCommandManager.argument("name", StringArgumentType.word())
                    .suggests(MACRO_NAME_SUGGESTIONS)
                    .executes(ctx -> {
                        String name = StringArgumentType.getString(ctx, "name");
                        return executeMacro(name, ctx.getSource());
                    })
                )
            )
            .then(ClientCommandManager.literal("delete")
                .then(ClientCommandManager.argument("name", StringArgumentType.word())
                    .suggests(MACRO_NAME_SUGGESTIONS) 
                    .executes(ctx -> {
                        String name = StringArgumentType.getString(ctx, "name");
                        return deleteMacro(name, ctx.getSource());
                    })
                )
            )
            .then(ClientCommandManager.literal("openfolder")
                .executes(ctx -> {
                    openMacroFolder(ctx.getSource());
                    return 1;
                })
            )
        );
    }

    private static int openMacroFolder(FabricClientCommandSource source) {
        try {
            if (!Files.exists(MACRO_FOLDER)) {
                Files.createDirectories(MACRO_FOLDER);
            }

            try {
                Util.getOperatingSystem().open(MACRO_FOLDER.toAbsolutePath().toUri());
                return 1;
            } catch (Exception e) {
                source.getPlayer().sendMessage(Text.literal("Error when opening folder: " + e.getMessage()), false);
            }

            // Zweiter Versuch: Desktop API
            try {
                if (java.awt.Desktop.isDesktopSupported()) {
                    java.awt.Desktop.getDesktop().open(MACRO_FOLDER.toFile());
                    return 1;
                }
            } catch (Exception e) {
                source.getPlayer().sendMessage(Text.literal("Desktop API doesnt work, try last alternative..."), false);
            }

            // Dritter Versuch: Betriebssystemspezifischer Befehl
            String os = System.getProperty("os.name").toLowerCase();
            ProcessBuilder pb;

            if (os.contains("linux")) {
                pb = new ProcessBuilder("xdg-open", MACRO_FOLDER.toString());
            } else if (os.contains("mac")) {
                pb = new ProcessBuilder("open", MACRO_FOLDER.toString());
            } else if (os.contains("windows")) {
                pb = new ProcessBuilder("explorer", MACRO_FOLDER.toString());
            } else {
                source.getPlayer().sendMessage(Text.literal("OS could not be detected."), false);
                return 0;
            }

            pb.start();
            return 1;

        } catch (Exception e) {
            source.getPlayer().sendMessage(Text.literal("Error when opening macro folder: " + e.getMessage()), false);
            return 0;
        }
    }

    private static int createMacroFile(String name, FabricClientCommandSource source) {
        try {
            if (!Files.exists(MACRO_FOLDER)) Files.createDirectories(MACRO_FOLDER);
            Path file = MACRO_FOLDER.resolve(name + ".macro");
            if (Files.exists(file)) {
                source.getPlayer().sendMessage(Text.literal("Macro already exists: " + name), false);
                return 0;
            }
            Files.createFile(file);
            source.getPlayer().sendMessage(Text.literal("Created macro: " + name), false);
            return 1;
        } catch (IOException e) {
            source.getPlayer().sendMessage(Text.literal("Error creating macro: " + e.getMessage()), false);
            return 0;
        }
    }
    private static final SuggestionProvider<FabricClientCommandSource> MACRO_NAME_SUGGESTIONS = (context, builder) -> {
        try {
            if (Files.exists(MACRO_FOLDER)) {
                Files.list(MACRO_FOLDER)
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".macro"))
                    .forEach(path -> {
                        String fileName = path.getFileName().toString();
                        String macroName = fileName.substring(0, fileName.length() - 6); // strip ".macro"
                        builder.suggest(macroName);
                    });
            }
        } catch (IOException e) {
            // Could log or ignore
        }
        return builder.buildFuture();
    };

    private static int executeMacro(String name, FabricClientCommandSource source) {
        try {
            Path file = MACRO_FOLDER.resolve(name + ".macro");
            if (!Files.exists(file)) {
                source.getPlayer().sendMessage(Text.literal("Macro not found: " + name), false);
                return 0;
            }
            List<String> lines = Files.readAllLines(file);
            for (String line : lines) {
                if (!line.trim().isEmpty()) {
                    if (line.startsWith("/")) {
                        // Send command to server
                        MinecraftClient.getInstance().player.networkHandler.sendChatCommand(line.substring(1));
                    } else {
                        // Send message to server
                        MinecraftClient.getInstance().player.networkHandler.sendChatMessage(line);
                    }
                }
            }
            source.getPlayer().sendMessage(Text.literal("Executed macro: " + name), false);
            return 1;
        } catch (IOException e) {
            source.getPlayer().sendMessage(Text.literal("Error executing macro: " + e.getMessage()), false);
            return 0;
        }
    }

    private static int deleteMacro(String name, FabricClientCommandSource source) {
        Path file = MACRO_FOLDER.resolve(name + ".macro");
        try {
            if (Files.exists(file)) {
                Files.delete(file);
                source.getPlayer().sendMessage(Text.literal("Deleted macro: " + name), false);
                return 1;
            } else {
                source.getPlayer().sendMessage(Text.literal("Macro not found: " + name), false);
                return 0;
            }
        } catch (IOException e) {
            source.getPlayer().sendMessage(Text.literal("Error deleting macro: " + e.getMessage()), false);
            return 0;
        }
    }
}
