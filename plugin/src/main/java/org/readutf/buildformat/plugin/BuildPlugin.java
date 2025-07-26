package org.readutf.buildformat.plugin;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.rollczi.litecommands.bukkit.LiteBukkitFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.readutf.buildformat.common.exception.BuildFormatException;
import org.readutf.buildformat.common.meta.BuildMetaStore;
import org.readutf.buildformat.common.schematic.BuildSchematicStore;
import org.readutf.buildformat.plugin.commands.BuildCommand;
import org.readutf.buildformat.plugin.commands.types.BuildType;
import org.readutf.buildformat.plugin.commands.types.ExampleInvalidUsageHandler;
import org.readutf.buildformat.plugin.formats.BuildFormatCache;
import org.readutf.buildformat.s3.S3BuildSchematicStore;
import org.readutf.buildformat.sql.SQLMetaStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.*;

public class BuildPlugin extends JavaPlugin {

    private static final Logger log = LoggerFactory.getLogger(BuildPlugin.class);

    @Override
    public void onEnable() {

        getConfig().options().copyDefaults(true);
        saveConfig();

        String bucket = getConfigValue("AWS_BUCKET", "aws.bucket");

        HikariDataSource dataSource = getDatabase();
        S3AsyncClient awsClient = getAwsClient();

        BuildMetaStore buildMetaStore = SQLMetaStore.createMetaStore(dataSource);
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
        String host = getConfigValue("DATABASE_HOST", "database.host");
        String port = getConfigValue("DATABASE_PORT", "database.port");
        String database = getConfigValue("DATABASE_NAME", "database.database");
        String user = getConfigValue("DATABASE_USER", "database.user");
        String password = getConfigValue("DATABASE_PASSWORD", "database.password");

        HikariConfig hikariConfig = new HikariConfig();
        String url = "jdbc:postgresql://%s:%s/%s".formatted(host, port, database);

        log.info("Connecting to database at: {}", url);
        hikariConfig.setJdbcUrl(url);
        if (password != null && user != null) {
            hikariConfig.setPassword(password);
            hikariConfig.setUsername(user);
        }
        hikariConfig.setDriverClassName("org.postgresql.Driver");
        hikariConfig.setMaximumPoolSize(16);
        return new HikariDataSource(hikariConfig);
    }

    public S3AsyncClient getAwsClient() {

        String accessKey = getConfigValue("AWS_ACCESS_KEY", "aws.accessKey");
        String secretKey = getConfigValue("AWS_SECRET_KEY", "aws.secretKey");
        String endpoint = getConfigValue("AWS_ENDPOINT", "aws.endpoint");
        String region = getConfigValue("AWS_REGION", "aws.region");

        System.out.println("Using AWS credentials: " + accessKey + ", endpoint: " + endpoint + ", region: " + region);

        boolean pathStyleAccessEnabled = getConfig().getBoolean("aws.pathStyleAccessEnabled", true);
        if (accessKey == null || secretKey == null) {
            throw new RuntimeException("AWS credentials are not set in the configuration.");
        }

        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);

        S3Configuration serviceConfiguration =
                S3Configuration.builder().pathStyleAccessEnabled(pathStyleAccessEnabled).build();

        S3AsyncClientBuilder builder = S3AsyncClient.builder()
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .serviceConfiguration(serviceConfiguration)
                .region(Region.US_EAST_1);


        if(endpoint != null) {
            builder.endpointOverride(URI.create(endpoint));
        }
        return builder.build();
    }

    public @Nullable String getConfigValue(String envKey, String confKey) {
        String env = System.getenv(envKey);
        if (env != null && !env.isEmpty()) return env;
        return getConfig().getString(confKey, null);
    }
}
