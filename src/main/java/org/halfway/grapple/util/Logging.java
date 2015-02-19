package org.halfway.grapple.util;

import org.halfway.grapple.model.IORuntimeException;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.*;

public class Logging {
    private static final Logger logger = Logger.getLogger(Logging.class.getName());
    private static final String LOGGING_PROPERTIES = "logging.properties";

    private Logging() {
        // no-op
    }

    public static void initialize() {
        try {
            LogManager.getLogManager().readConfiguration(Thread.currentThread().getContextClassLoader().
                    getResourceAsStream(LOGGING_PROPERTIES));
        } catch (final IOException e) {
            throw new IORuntimeException("Error during logger initialization", e);
        }

        logger.finest("Logging initialized");
        // FIXME - make this dependent upon some sort of logging property, but for now..
    }

    public static void initialize(final ScrollbackHandler scrollbackHandler) {
        initialize();
        // FIXME !
        scrollbackHandler.setLevel(Level.FINE);

        final Logger rootLogger = LogManager.getLogManager().getLogger("");
        scrollbackHandler.setFormatter(new Formatter() {
            @Override
            public String format(final LogRecord record) {
                // FIXME not nice
                final StringWriter stringWriter = new StringWriter();
                final PrintWriter writer = new PrintWriter(stringWriter);

                writer.write(formatMessage(record));
                if (record.getThrown() != null) {
                    writer.write("\n");
                    record.getThrown().printStackTrace(writer);
                }
                writer.close();
                return stringWriter.getBuffer().toString();
            }
        });
        rootLogger.addHandler(scrollbackHandler);
    }

}
