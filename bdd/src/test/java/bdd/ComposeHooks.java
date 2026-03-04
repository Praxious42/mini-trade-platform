package bdd;

import io.cucumber.java.AfterAll;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.ComposeContainer;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class ComposeHooks {
    private static final AtomicBoolean started = new AtomicBoolean(false);
    public static ComposeContainer infraCompose;
    public static ComposeContainer servicesCompose;

    // Stop once at the end of the JVM/test run
    @AfterAll
    public static void stopOnce() {
        if (!started.get()) {
            return;
        }
        log.info("[ComposeHooks] Global teardown for @system feature(s)");
        if (servicesCompose != null) {
            try {
                servicesCompose.stop();
            } catch (Exception e) {
                log.warn("[ComposeHooks] Failed to stop services compose", e);
            }
            servicesCompose = null;
        }

        if (infraCompose != null) {
            try {
                infraCompose.stop();
            } catch (Exception e) {
                log.warn("[ComposeHooks] Failed to stop infra compose", e);
            }
            infraCompose = null;
        }
        started.set(false);
    }

    // This hook runs before each scenario tagged with @system, but we only start compose once using the 'started' flag.
    @Before("@system")
    public void startOnce(Scenario scenario) throws InterruptedException {
        if (started.compareAndSet(false, true)) {
            log.info("[ComposeHooks] Starting infra and services for @system feature(s)");
            try {
                infraCompose = ComposeTestEnv.createComposeContainer();
                infraCompose.start();

                servicesCompose = ComposeTestEnv.startServicesCompose();

                // small extra wait to reduce flakiness
                Thread.sleep(10000);

                log.info("[ComposeHooks] Compose services started");
            } catch (Exception e) {
                log.error("[ComposeHooks] Failed to start compose environment", e);
                throw e;
            }
        } else {
            log.info("[ComposeHooks] Compose already started, skipping (scenario: {})", scenario.getName());
        }
    }
}
