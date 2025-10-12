package org.readutf.buildformat.plugin.formats.impl;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import net.hollowcube.polar.PolarLoader;
import net.hollowcube.polar.PolarWorld;
import net.hollowcube.schem.Schematic;
import net.hollowcube.schem.reader.SpongeSchematicReader;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.LightingChunk;
import org.readutf.buildformat.plugin.formats.SchematicFormat;

import java.io.File;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

public class PolarFormat implements SchematicFormat {
    @Override
    public byte[] convert(Clipboard clipboard) throws Exception {
        byte[] convert = new SpongeV3Format().convert(clipboard);

        MinecraftServer server = MinecraftServer.init();
        server.start(new InetSocketAddress("localhost", 0));

        Schematic schematic = new SpongeSchematicReader().read(convert);

        File temporaryFile = File.createTempFile("polar-", ".schem");
        InstanceContainer instance = MinecraftServer.getInstanceManager().createInstanceContainer();
        instance.setChunkSupplier(LightingChunk::new);

        Point size = schematic.size();
        Point offset = schematic.offset();

        ArrayList<CompletableFuture<Chunk>> futures = new ArrayList<>();
        for (int i = 0; i < (size.blockX() >> 4) + 1; i++) {
            for (int j = 0; j < (size.blockZ() >> 4) + 1; j++) {
                futures.add(instance.loadChunk(i, j));
            }
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        CountDownLatch latch = new CountDownLatch(1);
        Point inv = offset.mul(-1);
        schematic.createBatch().apply(instance, new Pos(inv), latch::countDown);

        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        LightingChunk.relight(
                instance, futures.stream().map(CompletableFuture::join).toList());


        instance.setChunkLoader(new PolarLoader(temporaryFile.toPath(), new PolarWorld()));
        instance.saveChunksToStorage().join();


        MinecraftServer.stopCleanly();

        return Files.readAllBytes(temporaryFile.toPath());
    }
}
