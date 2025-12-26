package org.bhel.hrm.payroll;

import org.bhel.hrm.common.config.Configuration;
import org.bhel.hrm.common.utils.CryptoUtils;
import org.bhel.hrm.common.utils.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PayrollServerLauncher {
    private static final Logger logger = LoggerFactory.getLogger(PayrollServerLauncher.class);

    public static void main(String[] args) {
        Configuration configuration = new Configuration();
        SslContextFactory sslContextFactory = new SslContextFactory(configuration);
        CryptoUtils cryptoUtils = new CryptoUtils(configuration);

        PayrollServer server = new PayrollServer(configuration, sslContextFactory, cryptoUtils);

        addShutdownHook(server);

        server.start();
    }

    /**
     * Adds a shutdown hook to gracefully unbind the payroll server on JVM shutdown.
     */
    private static void addShutdownHook(PayrollServer server) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutdown signal received, cleaning up...");
            server.shutdown();
        }));
    }

}
