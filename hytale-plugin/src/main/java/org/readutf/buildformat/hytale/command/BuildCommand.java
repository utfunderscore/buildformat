package org.readutf.buildformat.hytale.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;
import org.readutf.buildformat.hytale.session.HytaleSessionManager;
import org.readutf.buildformat.BuildDataType;
import org.readutf.buildformat.format.FormatRegistry;
import org.readutf.buildformat.hytale.utils.PlatformUtils;
import org.readutf.buildformat.requirement.Requirement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class BuildCommand extends AbstractCommandCollection {

    private static final Logger log = LoggerFactory.getLogger(BuildCommand.class);
    private final HytaleSessionManager sessionManager;
    private final FormatRegistry formatRegistry;

    public RequiredArg<String> format = withRequiredArg("format", "Build format", ArgTypes.STRING);

    public BuildCommand(HytaleSessionManager sessionManager, FormatRegistry formatRegistry) {
        super("build", "build");
        this.sessionManager = sessionManager;
        this.formatRegistry = formatRegistry;
        addSubCommand(new SaveCommand());
        addSubCommand(new UploadCommand());
    }

    public class UploadCommand extends AbstractPlayerCommand {

        public UploadCommand() {
            super("upload", "Upload the build to a remote server");
        }

        @Override
        protected void execute(@NotNull CommandContext commandContext, @NotNull Store<EntityStore> store, @NotNull Ref<EntityStore> ref, @NotNull PlayerRef playerRef, @NotNull World world) {


            CompletableFuture<Void> future = sessionManager.save(playerRef);
            playerRef.sendMessage(Message.raw("Build is now uploading...").color(Color.YELLOW));
            future.thenRun(() -> {
                playerRef.sendMessage(Message.raw("Build uploaded.").color(Color.GREEN));
            }).exceptionally(throwable -> {
                playerRef.sendMessage(Message.raw("Upload the build failed: " + throwable.getMessage()).color(Color.RED));
                return null;
            });
        }
    }

    public class SaveCommand extends AbstractPlayerCommand {

        private final RequiredArg<String> formatArg = this.withRequiredArg("format", "The build format type", ArgTypes.STRING);

        public SaveCommand() {
            super("save", "Save the build and upload it");
        }

        @Override
        protected void execute(@NotNull CommandContext commandContext, @NotNull Store<EntityStore> store, @NotNull Ref<EntityStore> ref, @NotNull PlayerRef playerRef, @NotNull World world) {
            String format = this.formatArg.get(commandContext);

            PlatformUtils.getWorld(playerRef).execute(() -> {
                try {
                    Map<String, List<Requirement>> formats = formatRegistry.readFormats();
                    List<Requirement> requirements = formats.get(format);
                    if (requirements == null) {
                        playerRef.sendMessage(Message.raw("Format not found").color(Color.RED));
                        return;
                    }

                    sessionManager.startInputSession(playerRef, BuildDataType.POLAR_WORLD, format, requirements);
                } catch (Exception e) {
                    log.error("Failed to start build session", e);
                    playerRef.sendMessage(Message.raw("Error starting build session: " + e.getMessage())
                            .color(Color.RED));
                }
            });
        }
    }

}
