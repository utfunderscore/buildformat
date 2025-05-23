package org.readutf.buildformat.plugin.marker;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer;
import org.bukkit.Location;
import org.enginehub.linbus.tree.LinCompoundTag;
import org.enginehub.linbus.tree.LinListTag;
import org.enginehub.linbus.tree.LinStringTag;
import org.enginehub.linbus.tree.LinTag;
import org.enginehub.linbus.tree.LinTagType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.readutf.buildformat.common.markers.Marker;
import org.readutf.buildformat.common.markers.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MarkerScanner {

    private static final Logger logger = LoggerFactory.getLogger(MarkerScanner.class);

    public static @NotNull List<Marker> scan(@NotNull Clipboard clipboard) {

        long start = System.currentTimeMillis();
        List<Marker> markers = StreamSupport.stream(clipboard.spliterator(), false).map(blockVector3 -> {
            BaseBlock block = clipboard.getFullBlock(blockVector3);
            LinCompoundTag nbt = block.getNbt();
            if (nbt == null) return null;

            LinCompoundTag frontText = getTag(nbt, "front_text", LinTagType.compoundTag());
            LinCompoundTag backText = getTag(nbt, "back_text", LinTagType.compoundTag());
            if (frontText == null || backText == null) {
                logger.debug("No front text or back text found");
                return null;
            }

            LinListTag<@NotNull LinStringTag> frontMessagesTag = getListTag(frontText, "messages", LinTagType.stringTag());
            LinListTag<@NotNull LinStringTag> backMessagesTag = getListTag(backText, "messages", LinTagType.stringTag());
            if (frontMessagesTag == null || backMessagesTag == null) {
                logger.debug("No front messages or back messages found");
                return null;
            }
            List<Component> front = frontMessagesTag.value().stream().map(LinStringTag::value).map(Component::text).map(comp -> (Component) comp).toList();
            List<Component> back = backMessagesTag.value().stream().map(LinStringTag::value).map(Component::text).map(comp -> (Component) comp).toList();

            BlockVector3 relative = blockVector3.subtract(BlockVector3.at(0, 0, 0));

            Position position = new Position(
                    relative.x(),
                    relative.y(),
                    relative.z()
            );

            logger.info("Position: {}", position);

            Marker frontMarker = toMarker(position, front);
            Marker backMarker = toMarker(position, back);
            if(frontMarker != null) {
                return frontMarker;
            } else return backMarker;
        }).filter(Objects::nonNull).toList();

        long after = System.currentTimeMillis();

        logger.info("Scanning {} markers in {} ms", markers.size(), (after - start));

        return markers;
    }

    private @Nullable
    static <T extends LinTag<?>> T getTag(LinCompoundTag nbt, String name, LinTagType<T> type) {
        try {
            return nbt.getTag(name, type);
        } catch (Exception e) {
            return null;
        }
    }

    private static <T extends LinTag<?>> @Nullable LinListTag<T> getListTag(
            LinCompoundTag tag, String name, LinTagType<T> elementType
    ) {
        try {
            return tag.getListTag(name, elementType);
        } catch (Exception e) {
            return null;
        }
    }


    private static boolean isMarker(List<Component> lines) {
        if (lines.isEmpty() || !(lines.getFirst() instanceof net.kyori.adventure.text.TextComponent text)) return false;
        String content = text.content();
        return content.toLowerCase().contains("marker");
    }

    private static @Nullable Marker toMarker(Position position, List<Component> lines) {
        if (lines.size() < 2 || !(lines.get(1) instanceof net.kyori.adventure.text.TextComponent nameText)) return null;
        String name = nameText.content();

        String offsetLine = "";
        if (lines.size() > 2 && lines.get(2) instanceof net.kyori.adventure.text.TextComponent offsetText) {
            offsetLine = offsetText.content();
        }

        Position offset = getOffset(offsetLine);
        if (offset == null) offset = new Position(0.0, 0.0, 0.0);

        return new Marker(
                name,
                position,
                offset
        );
    }

    private static @Nullable Position getOffset(String offsetLine) {
        String[] parts = offsetLine.split(" ");
        if (parts.length != 3) return null;
        try {
            double x = Double.parseDouble(parts[0]);
            double y = Double.parseDouble(parts[1]);
            double z = Double.parseDouble(parts[2]);
            return new Position(x, y, z);
        } catch (NumberFormatException e) {
            return null;
        }
    }

}
