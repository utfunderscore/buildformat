package org.readutf.buildformat.requirement;

import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;

public abstract class RequirementCollector<T> {

    private final CompletableFuture<T> resultFuture;

    public RequirementCollector() {
        this.resultFuture = new CompletableFuture<>();
    }

    protected abstract void start(Player player);

    protected abstract void cancel(Player player);

    public void complete(T result) {
        resultFuture.complete(result);
    }

    public T awaitBlocking() {
        return resultFuture.join();
    }

}
