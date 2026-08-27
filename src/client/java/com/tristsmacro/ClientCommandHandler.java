package com.tristsmacro;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.ChatFormatting;

import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;
import java.util.List;

public class ClientCommandHandler {

    private static final Path MACRO_FOLDER = Paths.get(
        Minecraft.getInstance().gameDirectory.getPath(),
        "macro_mod_macros"
    );

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {

        dispatcher.register(
            ClientCommands.literal("macro")

                .then(ClientCommands.literal("create")
                    .then(ClientCommands.argument("name", StringArgumentType.word())
                        .executes(ctx -> {
                            String name = StringArgumentType.getString(ctx, "name");
                            return createMacroFile(name, ctx.getSource());
                        })
                    )
                )

                .then(ClientCommands.literal("execute")
                    .then(ClientCommands.argument("name", StringArgumentType.word())
                        .suggests(MACRO_NAME_SUGGESTIONS)
                        .executes(ctx -> {
                            String name = StringArgumentType.getString(ctx, "name");
                            return executeMacro(name, ctx.getSource());
                        })
                    )
                )

                .then(ClientCommands.literal("delete")
                    .then(ClientCommands.argument("name", StringArgumentType.word())
                        .suggests(MACRO_NAME_SUGGESTIONS)
                        .executes(ctx -> {
                            String name = StringArgumentType.getString(ctx, "name");
                            return deleteMacro(name, ctx.getSource());
                        })
                    )
                )

                .then(ClientCommands.literal("openfolder")
                    .executes(ctx -> {
                        sendFolderLink(ctx.getSource());
                        return 1;
                    })
                )
        );
    }

    private static int createMacroFile(
        String name,
        FabricClientCommandSource source
    ) {
        try {
            if (!Files.exists(MACRO_FOLDER)) {
                Files.createDirectories(MACRO_FOLDER);
            }

            Path file = MACRO_FOLDER.resolve(name + ".macro");

            if (Files.exists(file)) {
                source.sendFeedback(
                    Component.literal("Macro already exists: " + name)
                );
                return 0;
            }

            Files.createFile(file);

            source.sendFeedback(
                Component.literal("Created macro: " + name)
            );

            return 1;

        } catch (IOException e) {

            source.sendFeedback(
                Component.literal(
                    "Error creating macro: " + e.getMessage()
                )
            );

            return 0;
        }
    }

    private static final SuggestionProvider<FabricClientCommandSource>
        MACRO_NAME_SUGGESTIONS = (context, builder) -> {

        try {
            if (Files.exists(MACRO_FOLDER)) {

                Files.list(MACRO_FOLDER)
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".macro"))
                    .forEach(path -> {

                        String fileName =
                            path.getFileName().toString();

                        String macroName =
                            fileName.substring(
                                0,
                                fileName.length() - 6
                            );

                        builder.suggest(macroName);
                    });
            }

        } catch (IOException ignored) {
        }

        return builder.buildFuture();
    };

    private static void sendFolderLink(
        FabricClientCommandSource source
    ) {
        Path folderPath = MACRO_FOLDER.toAbsolutePath();

        String uri =
            "file:///" +
            folderPath.toString()
                .replace("\\", "/");

        Component message =
            Component.literal("Open macro folder")
                .setStyle(
                    Style.EMPTY
                        .withColor(ChatFormatting.BLUE)
                        .withUnderlined(true)
                        .withClickEvent(
                            new net.minecraft.network.chat.ClickEvent.OpenUrl(
                                java.net.URI.create(uri)
                            )
                        )
                );

        source.sendFeedback(message);
    }

    private static int executeMacro(
        String name,
        FabricClientCommandSource source
    ) {
        try {

            Path file =
                MACRO_FOLDER.resolve(name + ".macro");

            if (!Files.exists(file)) {

                source.sendFeedback(
                    Component.literal(
                        "Macro not found: " + name
                    )
                );

                return 0;
            }

            List<String> lines =
                Files.readAllLines(file);

            Minecraft client =
                Minecraft.getInstance();

            if (client.player == null) {
                source.sendFeedback(
                    Component.literal(
                        "You must be in a world to execute a macro."
                    )
                );

                return 0;
            }

            for (String line : lines) {

                line = line.trim();

                if (line.isEmpty()) {
                    continue;
                }

                if (line.startsWith("/")) {

                    client.player.connection.sendCommand(
                        line.substring(1)
                    );

                } else {

                    client.player.connection.sendChat(
                        line
                    );
                }
            }

            source.sendFeedback(
                Component.literal(
                    "Executed macro: " + name
                )
            );

            return 1;

        } catch (IOException e) {

            source.sendFeedback(
                Component.literal(
                    "Error executing macro: " +
                    e.getMessage()
                )
            );

            return 0;
        }
    }

    private static int deleteMacro(
        String name,
        FabricClientCommandSource source
    ) {

        Path file =
            MACRO_FOLDER.resolve(name + ".macro");

        try {

            if (Files.exists(file)) {

                Files.delete(file);

                source.sendFeedback(
                    Component.literal(
                        "Deleted macro: " + name
                    )
                );

                return 1;

            } else {

                source.sendFeedback(
                    Component.literal(
                        "Macro not found: " + name
                    )
                );

                return 0;
            }

        } catch (IOException e) {

            source.sendFeedback(
                Component.literal(
                    "Error deleting macro: " +
                    e.getMessage()
                )
            );

            return 0;
        }
    }
}