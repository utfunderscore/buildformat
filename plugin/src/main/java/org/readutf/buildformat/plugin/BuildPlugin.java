package org.readutf.buildformat.plugin;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.rollczi.litecommands.bukkit.LiteBukkitFactory;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.readutf.buildformat.common.exception.BuildFormatException;
import org.readutf.buildformat.common.meta.BuildMetaStore;
import org.readutf.buildformat.common.schematic.BuildSchematicStore;
import org.readutf.buildformat.plugin.commands.BuildCommand;
import org.readutf.buildformat.plugin.commands.types.BuildType;
import org.readutf.buildformat.plugin.commands.types.ExampleInvalidUsageHandler;
import org.readutf.buildformat.plugin.formats.BuildFormatCache;
import org.readutf.buildformat.s3.S3BuildSchematicStore;
import org.readutf.buildstore.PostgresDatabaseManager;
import org.readutf.buildstore.PostgresMetaStore;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

public class BuildPlugin extends JavaPlugin {


    @Override
    public void onEnable() {

        getConfig().options().copyDefaults(true);
        saveConfig();

        HikariDataSource dataSource = getDatabase(
                getConfig().getString("database.host", "localhost"),
                getConfig().getString("database.port", "5432"),
                getConfig().getString("database.database", "buildformat"),
                getConfig().getString("database.user", ""),
                getConfig().getString("database.password", "")
        );

        S3Client awsClient = getAwsClient(
                getConfig().getString("aws.accessKey", ""),
                getConfig().getString("aws.secretKey", ""),
                getConfig().getString("aws.endpoint", "")
        );

        String bucket = getConfig().getString("aws.bucket", "buildformat");

        PostgresDatabaseManager databaseManager = new PostgresDatabaseManager(dataSource);
        BuildMetaStore buildMetaStore = new PostgresMetaStore(databaseManager);
        BuildSchematicStore buildSchematicStore = new S3BuildSchematicStore(awsClient, bucket);

        try {
            BuildFormatCache buildFormatCache = new BuildFormatCache(new File(getDataFolder(), "formats"));

            LiteBukkitFactory.builder(this)
                    .invalidUsage(new ExampleInvalidUsageHandler())
                    .argument(BuildType.class, new BuildType.BuildTypesSuggester(buildFormatCache))
                    .commands(new BuildCommand(buildMetaStore, buildSchematicStore, buildFormatCache))
                    .build();

        } catch (BuildFormatException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private @NotNull HikariDataSource getDatabase(
            @NotNull String host,
            @NotNull String port,
            @NotNull String database,
            @NotNull String user,
            @NotNull String password
    ) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:postgresql://%s:%s/%s".formatted(host, port, database));
        if (!password.isEmpty() && !user.isEmpty()) {
            hikariConfig.setPassword(password);
            hikariConfig.setUsername(user);
        }
        hikariConfig.setDriverClassName("org.postgresql.Driver");
        hikariConfig.setMaximumPoolSize(16);
        return new HikariDataSource(hikariConfig);
    }

    public S3Client getAwsClient(@NotNull String accessKey, @NotNull String secretKey, @NotNull String endpoint) {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(
                accessKey,
                secretKey
        );

        S3Configuration serviceConfiguration = S3Configuration.builder()
                .pathStyleAccessEnabled(true)
                .build();

        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .region(Region.of("auto"))
                .serviceConfiguration(serviceConfiguration)
                .build();
    }

}
