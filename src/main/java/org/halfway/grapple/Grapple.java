package org.halfway.grapple;

import com.google.common.base.Verify;
import com.google.common.collect.ImmutableList;
import org.halfway.grapple.gui.BacktraceWindow;
import org.halfway.grapple.gui.GrappleGuiApi;
import org.halfway.grapple.gui.GrappleGuiApiImpl;
import org.halfway.grapple.gui.ProgressWindow;
import org.halfway.grapple.impl.ConfigurationFactory;
import org.halfway.grapple.impl.RuntimeContext;
import org.halfway.grapple.model.GrappleFatalException;
import org.halfway.grapple.model.configuration.Configuration;
import org.halfway.grapple.model.manifest.ManifestVerificationException;
import org.halfway.grapple.stage.*;
import org.halfway.grapple.util.Logging;
import org.halfway.grapple.util.ScrollbackHandler;
import org.halfway.grapple.util.UrlDownloader;

import javax.swing.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Grapple is a launcher for applications that downloads the required resources over HTTP
 * <p/>
 * It can be used in conjunction with JWS or as a standalone launcher.
 */
public class Grapple {
    private final static Logger logger = Logger.getLogger(Grapple.class.getSimpleName());

    private final Configuration configuration;
    private final ScrollbackHandler scrollbackHandler;
    private final ImmutableList<LauncherStage> stages;

    private GrappleGuiApi guiApi = null;
    private RuntimeContext context = null;

    public Grapple(final ScrollbackHandler scrollbackHandler, final Configuration configuration) {
        Verify.verifyNotNull(scrollbackHandler, "scrollback handler must not be null");
        Verify.verifyNotNull(configuration, "configuration must not be null");

        this.scrollbackHandler = scrollbackHandler;
        this.configuration = configuration;
        logger.info("Using " + configuration);
        this.stages = ImmutableList.of(
                new PrepareApplicationDirectoriesStage(configuration),
                new FetchManifestStage(configuration, new UrlDownloader()),
                new OfflineVerifyStage(configuration),
                new UpdateStage(configuration, new UrlDownloader()),
                new WriteManifestStage(),
                new FinalStage(configuration));
    }

    /**
     * Entry point to Grapple. Configuration is currently managed via system properties
     * {@link org.halfway.grapple.impl.ConfigurationFactory#fromSystemProperties()}
     *
     * @param args Arguments passed to the JVM upon launch
     */
    public static void main(String[] args) {
        ScrollbackHandler scrollbackHandler = new ScrollbackHandler(ConfigurationFactory.getBacktraceScrollback());
        Logging.initialize(scrollbackHandler);
        Configuration configuration = ConfigurationFactory.fromSystemProperties();
        new Grapple(scrollbackHandler, configuration).begin();
    }

    private void setUpNativeLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (final UnsupportedLookAndFeelException e) {
            logger.log(Level.SEVERE, "Unable to set system look and feel because it is unsupported", e);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setUpGuiApi() {
        ProgressWindow progressWindow = new ProgressWindow();
        progressWindow.pack();
        BacktraceWindow backtraceWindow = new BacktraceWindow(true);
        backtraceWindow.pack();
        guiApi = new GrappleGuiApiImpl(configuration, progressWindow, backtraceWindow);
    }

    private void setUpRuntimeContext() {
        Verify.verifyNotNull(guiApi, "guiApi must not be null");
        context = new RuntimeContext(guiApi);
    }

    private void makeProgress() {
        guiApi.showProgressWindow();
    }

    private void stagedLaunch() {
        try {
            for (final LauncherStage stage : stages) {
                stage.burn(context);
            }
            guiApi.waitForProgressWindowClose();
        } catch (final ManifestVerificationException e) {
            logger.log(Level.SEVERE, "Manifest failed to verify", e);
            guiApi.displayBacktrace(e.getMessage(), scrollbackHandler.getScrollback());
        } catch (final GrappleFatalException e) {
            logger.log(Level.SEVERE, "Fatal exception occurred", e);
            guiApi.displayBacktrace(e.getMessage(), scrollbackHandler.getScrollback());
        } catch (final Throwable t) {
            logger.log(Level.SEVERE, "BUG: unknown error occurred", t);
            guiApi.displayBacktrace("Unknown error", scrollbackHandler.getScrollback());
        }
    }

    /**
     * Let the grapple begin!
     */
    public void begin() {
        setUpNativeLookAndFeel();
        setUpGuiApi();
        setUpRuntimeContext();
        makeProgress();
        stagedLaunch();
    }

}
