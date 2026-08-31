package org.alexweber.cmdtree;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.commands.synchronization.ArgumentUtils;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// Fabric API renamed its client command helper from `ClientCommandManager` to `ClientCommands`
// starting with the Fabric API build for Minecraft 26.1 - only the import differs, the call
// site (`literal(...)`) stays identical thanks to the static import.
//? if <26.1 {
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;
//?} else {
/*import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;
*///?}

/**
 * Implements {@code /cmdtree getcommands}: dumps the client-visible Brigadier command
 * tree (already filtered server-side to what the player may run) to a JSON file.
 *
 * <p>Works identically in singleplayer and on any remote server - the tree always comes
 * from the client's own network connection, never from a {@code MinecraftServer} instance.
 */
public final class GetCommandsCommand {
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private GetCommandsCommand() {
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, context) ->
                dispatcher.register(literal("cmdtree")
                        .then(literal("getcommands")
                                .executes(ctx -> run(ctx.getSource())))));
    }

    private static int run(FabricClientCommandSource source) {
        Minecraft client = Minecraft.getInstance();
        ClientPacketListener connection = client.getConnection();
        if (connection == null) {
            source.sendError(Component.literal("[CmdTree] Not connected to any server."));
            return 0;
        }

        String label;
        ServerData serverEntry = client.getCurrentServer();
        if (serverEntry != null) {
            label = serverEntry.ip;
        } else {
            IntegratedServer integratedServer = client.getSingleplayerServer();
            label = integratedServer != null ? integratedServer.getWorldData().getLevelName() : "singleplayer";
        }

        try {
            var dispatcher = connection.getCommands();
            JsonElement tree = ArgumentUtils.serializeNodeToJson(dispatcher, dispatcher.getRoot());
            Path file = writeTree(label, tree);
            source.sendFeedback(Component.literal("[CmdTree] Wrote command tree to " + file));
            return 1;
        } catch (IOException e) {
            CmdTreeMod.LOGGER.error("Failed to write command tree", e);
            source.sendError(Component.literal("[CmdTree] Failed to write command tree: " + e.getMessage()));
            return 0;
        }
    }

    private static Path writeTree(String label, JsonElement tree) throws IOException {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        Path dir = FabricLoader.getInstance().getGameDir().resolve("cmdtree");
        Files.createDirectories(dir);
        Path file = dir.resolve("cmdtree_commands_" + sanitize(label) + "_" + timestamp + ".json");
        Files.writeString(file, GSON.toJson(tree));
        return file;
    }

    private static String sanitize(String value) {
        return value.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
