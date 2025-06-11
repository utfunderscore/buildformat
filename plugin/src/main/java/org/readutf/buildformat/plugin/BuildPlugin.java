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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

public class BuildPlugin extends JavaPlugin {


    private static final Logger log = LoggerFactory.getLogger(BuildPlugin.class);

    @Override
    public void onEnable() {

        getConfig().options().copyDefaults(true);
        saveConfig();

        HikariDataSource dataSource = getDatabase();

        S3Client awsClient = getAwsClient(
                getConfigString("AWS_ACCESS_KEY", "aws.accessKey"),
                getConfigString("AWS_SECRET_KEY", "aws.secretKey"),
                getConfigString("AWS_ENDPOINT", "aws.endpoint")
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

    private @NotNull HikariDataSource getDatabase() {
        String host = getConfigString("DATABASE_URL", "database.host");
        String port = getConfigString("DATABASE_PORT", "database.port");
        String database = getConfigString("DATABASE_NAME", "database.database");
        String user = getConfigString("DATABASE_USER", "database.user");
        String password = getConfigString("DATABASE_PASSWORD", "database.password");

        HikariConfig hikariConfig = new HikariConfig();
        String url = "jdbc:postgresql://%s:%s/%s".formatted(host, port, database);

        log.info("Connecting to database at: {}", url);
        hikariConfig.setJdbcUrl(url);
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

    public String getConfigString(String envVar, String configPath) {
        String value = System.getenv(envVar);
        if (value == null || value.isEmpty()) {
            value = getConfig().getString(configPath, "");
        }
        return value;
    }

}
