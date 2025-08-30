package org.readutf.buildformat.plugin.commands.types;

import dev.rollczi.litecommands.argument.Argument;
import dev.rollczi.litecommands.argument.parser.ParseResult;
import dev.rollczi.litecommands.argument.resolver.ArgumentResolver;
import dev.rollczi.litecommands.invocation.Invocation;
import dev.rollczi.litecommands.suggestion.SuggestionContext;
import dev.rollczi.litecommands.suggestion.SuggestionResult;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.readutf.buildformat.plugin.formats.BuildFormatStore;

public class BuildType {

    private final @NotNull String buildType;

    public BuildType(@NotNull String buildType) {
        this.buildType = buildType;
    }

    public @NotNull String getBuildType() {
        return buildType;
    }

    public static class BuildTypesSuggester extends ArgumentResolver<CommandSender, BuildType> {

        private final BuildFormatStore buildFormatStore;

        public BuildTypesSuggester(BuildFormatStore buildFormatStore) {
            this.buildFormatStore = buildFormatStore;
        }

        @Override
        protected ParseResult<BuildType> parse(Invocation<CommandSender> invocation, Argument<BuildType> context, String argument) {
            return ParseResult.success(new BuildType(argument));
        }

        @Override
        public SuggestionResult suggest(Invocation<CommandSender> invocation, Argument<BuildType> argument, SuggestionContext context) {
            return SuggestionResult.of(buildFormatStore.getFormats());
        }
    }

}
