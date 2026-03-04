package bdd;

import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitStrategy;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ComposeTestEnv {
    private static final File INFRA_COMPOSE = new File("../infra/docker/docker-compose.yml");
    private static final File INFRA_SERVICES_COMPOSE = new File("../infra/docker/docker-compose.services.yml");

    public static ComposeContainer createComposeContainer() {
        File composeFile = prepareComposeFile(INFRA_COMPOSE);
        return new ComposeContainer(composeFile)
            .withExposedService("broker", 9092, Wait.forLogMessage(".*Kafka Server started.*\\n", 1))
            .withExposedService("portfolio-init-runner", 9092, Wait.forLogMessage(".*Runner finished.*\\n", 1))
            .withExposedService("order-init-runner", 9092, Wait.forLogMessage(".*Runner finished.*\\n", 1))
            .withExposedService("kafka-init", 9092, Wait.forLogMessage(".*All requested topics processed.*\\n", 1))
            .withExposedService("mintrade-order", 5432, Wait.forLogMessage(".*database system is ready to accept connections.*\\n", 1))
            .withExposedService("mintrade-portfolio", 5432, Wait.forLogMessage(".*database system is ready to accept connections.*\\n", 1));
    }

    public static ComposeContainer createServicesComposeContainer() {
        File composeFile = prepareComposeFile(INFRA_SERVICES_COMPOSE);
        WaitStrategy springBootStarted = Wait.forLogMessage(".*Started Application in.*\\n", 1);
        return new ComposeContainer(composeFile)
            .withExposedService("order-service", 8081, springBootStarted)
            .withExposedService("execution-service", 8082, springBootStarted)
            .withExposedService("portfolio-service", 8083, springBootStarted);
    }

    // Start services compose and return the running ComposeContainer
    public static ComposeContainer startServicesCompose() {
        ComposeContainer container = createServicesComposeContainer();
        container.start();
        return container;
    }

    private static File prepareComposeFile(File composeSource) {
        try {
            // read original compose file
            String content = Files.readString(composeSource.toPath());

            // remove container_name lines
            String cleaned = content.replaceAll("(?m)^\\s*container_name\\s*:\\s*.*\\r?\\n", "");

            // Convert relative host volume mounts (./...) into absolute paths so Docker won't create host directories
            Path infraDir = composeSource.toPath().getParent().toAbsolutePath().normalize();
            String infraAbs = infraDir.toString().replace("\\", "/"); // use forward slashes for compose compatibility on Windows

            Pattern p = Pattern.compile("(?m)^(\\s*-\\s*)\\./([^:\\r\\n]+)");
            Matcher m = p.matcher(cleaned);
            StringBuffer sb = new StringBuffer();
            while (m.find()) {
                String prefix = m.group(1);
                String relPath = m.group(2);
                String replacement = prefix + infraAbs + "/" + relPath;
                m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            }
            m.appendTail(sb);
            cleaned = sb.toString();

            // write cleaned compose to a temp file so Testcontainers points at it
            Path tmp = Files.createTempFile("docker-compose-abs-", ".yml");
            Files.writeString(tmp, cleaned);
            tmp.toFile().deleteOnExit();
            return tmp.toFile();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
