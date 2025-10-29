package org.readutf.buildformat;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.rollczi.litecommands.bukkit.LiteBukkitFactory;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jooq.SQLDialect;
import org.readutf.buildformat.commands.BuildCommand;
import org.readutf.buildformat.format.FormatRegistry;
import org.readutf.buildformat.postgres.SQLBuildMetaStore;
import org.readutf.buildformat.requirement.SessionManager;
import org.readutf.buildformat.requirement.collectors.text.BuildNameCollector;
import org.readutf.buildformat.requirement.collectors.text.TextInputCollector;
import org.readutf.buildformat.s3.S3BuildDataStore;
import org.readutf.buildformat.tools.ClickableManager;
import org.readutf.buildformat.tools.PositionTool;
import org.readutf.buildformat.tools.RegionSelectionTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

public class BuildFormatPlugin extends JavaPlugin {

    private static final Logger log = LoggerFactory.getLogger(BuildFormatPlugin.class);

    @Override
    public void onEnable() {

        System.setProperty("org.jooq.no-logo", "true");


        getConfig().options().copyDefaults(true);
        saveDefaultConfig();

        S3BuildDataStore buildDataStore = new S3BuildDataStore(this.getAwsClient(), "builds");
        SQLBuildMetaStore buildMetaStore = new SQLBuildMetaStore(getDatabase(), SQLDialect.POSTGRES);
        SessionManager sessionManager = new SessionManager(new BuildManager(buildMetaStore, buildDataStore));
        FormatRegistry formatRegistry = new FormatRegistry(BuildFormatManager.getInstance(), getDataFolder());

        LiteBukkitFactory.builder(this)
                .commands(new BuildCommand(sessionManager, formatRegistry))
                .build();

        Bukkit.getPluginManager().registerEvents(new RegionSelectionTool(), this);
        Bukkit.getPluginManager().registerEvents(new PositionTool(), this);
        Bukkit.getPluginManager().registerEvents(new ClickableManager(), this);
        Bukkit.getPluginManager().registerEvents(new TextInputCollector.ChatListener(), this);
        Bukkit.getPluginManager().registerEvents(new BuildNameCollector.ChatListener(), this);

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

        S3Configuration serviceConfiguration = S3Configuration.builder()
                .pathStyleAccessEnabled(pathStyleAccessEnabled)
                .build();

        S3AsyncClientBuilder builder = S3AsyncClient.builder()
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .serviceConfiguration(serviceConfiguration)
                .region(Region.US_EAST_1);

        if (endpoint != null) {
            builder.endpointOverride(URI.create(endpoint));
        }
        return builder.build();
    }

    public @Nullable String getConfigValue(String envKey, String confKey) {
        String env = System.getenv(envKey);
        if (env != null && !env.isEmpty()) return env;
        System.out.println("Config " + confKey + " loaded from config file.");
        return getConfig().getString(confKey, null);
    }
}
