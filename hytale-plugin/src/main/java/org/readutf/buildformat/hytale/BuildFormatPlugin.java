package org.readutf.buildformat.hytale;

import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jetbrains.annotations.NotNull;
import org.jooq.SQLDialect;
import org.readutf.buildformat.hytale.command.BuildCommand;
import org.readutf.buildformat.BuildFormatManager;
import org.readutf.buildformat.BuildManager;
import org.readutf.buildformat.format.FormatRegistry;
import org.readutf.buildformat.postgres.SQLBuildMetaStore;
import org.readutf.buildformat.hytale.requirement.collectors.text.TextInputCollector;
import org.readutf.buildformat.hytale.tools.PositionTool;
import org.readutf.buildformat.s3.S3BuildDataStore;
import org.readutf.buildformat.hytale.session.HytaleSessionManager;
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

    private final S3BuildDataStore buildDataStore = new S3BuildDataStore(this.getAwsClient(), "builds");
    private final SQLBuildMetaStore buildMetaStore = new SQLBuildMetaStore(getDatabase(), SQLDialect.POSTGRES);
    private final FormatRegistry formatRegistry = new FormatRegistry(
            BuildFormatManager.getInstance(), getDataDirectory().toFile());

    private final BuildManager buildManager = new BuildManager(buildMetaStore, buildDataStore);
    private final HytaleSessionManager spigotSessionManager = new HytaleSessionManager(buildManager);

    public BuildFormatPlugin(@NotNull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        this.getEntityStoreRegistry().registerSystem(new PositionTool.PlayerInteractionListener());
    }

    @Override
    protected void start() {
        getCommandRegistry().registerCommand(new BuildCommand(spigotSessionManager, formatRegistry));
        getEventRegistry().registerGlobal(PlayerChatEvent.class, new TextInputCollector.ChatListener());
    }

    @Override
    protected void shutdown() {}

    private @NotNull HikariDataSource getDatabase() {

        String host = System.getenv("DATABASE_HOST");
        String port = System.getenv("DATABASE_PORT");
        String database = System.getenv("DATABASE_NAME");
        String user = System.getenv("DATABASE_USER");
        String password = System.getenv("DATABASE_PASSWORD");

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

        String accessKey = System.getenv("AWS_ACCESS_KEY");
        String secretKey = System.getenv("AWS_SECRET_KEY");
        String endpoint = System.getenv("AWS_ENDPOINT");
        String region = System.getenv("AWS_REGION");

        boolean pathStyleAccessEnabled = Boolean.parseBoolean(System.getenv("AWS_PATH_STYLE_ACCESS"));
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
}
