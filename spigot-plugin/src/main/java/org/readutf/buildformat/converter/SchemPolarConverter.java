package org.readutf.buildformat.converter;

import net.hollowcube.polar.PolarLoader;
import net.hollowcube.polar.PolarWorld;
import net.hollowcube.schem.Schematic;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.LightingChunk;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

public class SchemPolarConverter {

    private static final MinecraftServer server = MinecraftServer.init();

    static {
        server.start(new InetSocketAddress(0));
    }

    public static byte @NotNull [] convert(@NotNull Schematic schematic) throws Exception {
        File temporaryFile = File.createTempFile("polar-", ".schem");
        InstanceContainer instance = MinecraftServer.getInstanceManager().createInstanceContainer(new PolarLoader(temporaryFile.toPath(), new PolarWorld()));

        try {
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

            latch.await();

            LightingChunk.relight(
                    instance, futures.stream().map(CompletableFuture::join).toList());

            instance.saveChunksToStorage().join();

            return Files.readAllBytes(temporaryFile.toPath());
        } finally {
            MinecraftServer.getInstanceManager().unregisterInstance(instance);
        }
    }
}
