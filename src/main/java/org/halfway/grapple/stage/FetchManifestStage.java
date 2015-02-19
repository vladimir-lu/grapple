package org.halfway.grapple.stage;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Range;
import org.halfway.grapple.impl.*;
import org.halfway.grapple.model.GrappleFatalException;
import org.halfway.grapple.model.HttpRuntimeException;
import org.halfway.grapple.model.IORuntimeException;
import org.halfway.grapple.model.configuration.Configuration;
import org.halfway.grapple.model.configuration.JvmApplicationTarget;
import org.halfway.grapple.model.configuration.LaunchTarget;
import org.halfway.grapple.model.configuration.StandaloneApplicationTarget;
import org.halfway.grapple.model.manifest.GrappleManifest;
import org.halfway.grapple.model.manifest.JvmManifest;
import org.halfway.grapple.model.manifest.StandaloneManifest;
import org.halfway.grapple.util.FileIO;
import org.halfway.grapple.util.SingletonByteSink;
import org.halfway.grapple.util.UrlDownloader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Stage to fetch the manifest(s) for an application from the list of base urls provided in the
 * {@link org.halfway.grapple.model.configuration.Configuration}. The manifests are <strong>not</strong> written to disk
 * at this point.
 * <p/>
 * One of the things the configuration fetch does is to make sure we are only using one base url
 */
public class FetchManifestStage implements LauncherStage {
    private static final Logger logger = Logger.getLogger(FetchManifestStage.class.getName());
    private static final Range<Integer> PROGRESS_RANGE = Range.closed(2, 5);

    private final Configuration configuration;
    private final UrlDownloader urlDownloader;

    public FetchManifestStage(final Configuration configuration, final UrlDownloader urlDownloader) {
        this.configuration = configuration;
        this.urlDownloader = urlDownloader;
    }

    @Override
    public Range<Integer> progressRange() {
        return PROGRESS_RANGE;
    }


    private byte[] httpGetManifestBytes(final URL url) {
        logger.info("Trying to fetch manifest from " + url);
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
        final int responseCode = urlDownloader.httpGet(url, new SingletonByteSink(outputStream),
                new SingletonByteSink(errorStream).asCharSink(Charset.defaultCharset()));
        if (responseCode != HttpURLConnection.HTTP_OK) {
            logger.fine("Got " + responseCode + " for manifest download url " + url);
            if (logger.isLoggable(Level.FINEST)) {
                final String errorBody = new String(errorStream.toByteArray(), Charset.defaultCharset());
                logger.log(Level.FINEST, errorBody);
            }
            throw new HttpRuntimeException("Error trying to download manifest file", responseCode);
        }
        return outputStream.toByteArray();
    }

    private Map.Entry<URL, Properties> httpGetManifestPropertiesFrom(final ImmutableList<URL> baseUrlList) {
        for (final URL baseUrl : baseUrlList) {
            final URL url = urlDownloader.combinePath(baseUrl, GrapplePropertiesManifest.PROPERTIES_FILE);
            try {
                final byte[] manifestBytes = httpGetManifestBytes(url);

                final Properties properties = new Properties();
                try {
                    properties.load(new ByteArrayInputStream(manifestBytes));
                } catch (IOException e) {
                    throw new IORuntimeException("Unable to parse properties file", e);
                }
                return Maps.immutableEntry(baseUrl, properties);
            } catch (final HttpRuntimeException e) {
                logger.log(Level.WARNING, "Unable to download manifest from url " + url, e);
            } catch (final IORuntimeException e) {
                logger.log(Level.SEVERE, "Unknown IO error during http manifest download from " + url, e);
            }
        }
        throw new GrappleFatalException("Unable to download manifest from any URLs");
    }

    private Map.Entry<URL, Properties> fileGetManifestPropertiesFrom(final File contentRoot) {
        final File manifestFile = new File(contentRoot, GrapplePropertiesManifest.PROPERTIES_FILE);
        final URL manifestFileUrl = FileIO.toUrl(manifestFile);
        logger.info("Trying to read manifest properties from file " + manifestFile);
        if (!manifestFile.isFile()) {
            throw new GrappleFatalException("Offline mode enabled but no manifest file at " + manifestFile);
        }
        final Properties properties = FileIO.readProperties(manifestFile);
        return Maps.immutableEntry(manifestFileUrl, properties);
    }

    private Map.Entry<URL, Properties> getOrReadManifest(final ImmutableList<URL> baseUrlList, final File contentRoot) {
        if (configuration.isOfflineMode()) {
            return fileGetManifestPropertiesFrom(contentRoot);
        } else {
            return httpGetManifestPropertiesFrom(baseUrlList);
        }
    }

    private Map.Entry<URL, StandaloneManifest> fetchStandaloneManifest(
            final RuntimeContext context, final ImmutableList<URL> baseUrlList, final File contentRoot) {
        final Map.Entry<URL, Properties> entry = getOrReadManifest(baseUrlList, contentRoot);
        final GrappleManifest manifest = GrapplePropertiesManifest.
                fromPropertiesMap()
                .properties(entry.getValue())
                .build();
        if (!(manifest instanceof StandaloneManifest)) {
            throw new GrappleFatalException("Manifest at url " + entry.getKey() + " must be a standalone manifest type");
        }
        return Maps.immutableEntry(entry.getKey(), (StandaloneManifest) manifest);
    }

    private Map.Entry<URL, JvmManifest> fetchJvmManifest(final RuntimeContext context, final ImmutableList<URL> baseUrlList,
                                                         File contentRoot) {
        final Map.Entry<URL, Properties> entry = getOrReadManifest(baseUrlList, contentRoot);
        final GrappleManifest manifest = GrapplePropertiesManifest
                .fromPropertiesMap()
                .properties(entry.getValue())
                .build();
        if (!(manifest instanceof JvmManifest)) {
            throw new GrappleFatalException("Manifest at url " + entry.getKey() + " must be a jvm manifest type");
        }
        return Maps.immutableEntry(entry.getKey(), (JvmManifest) manifest);
    }

    private JvmApplicationWithManifests fetchJvmApplicationManifest(final RuntimeContext context, final JvmApplicationTarget target) {
        final Map.Entry<URL, StandaloneManifest> standaloneManifestEntry = fetchStandaloneManifest(context, target.getBaseUrlList(), target.getContentRoot());
        final Map.Entry<URL, JvmManifest> jvmManifest = fetchJvmManifest(context, target.getJvmBaseUrlList(), target.getJvmContentRoot());
        context.setBaseUrlMap(ImmutableMap.of(
                target.getContentRoot(), standaloneManifestEntry.getKey(),
                target.getJvmContentRoot(), jvmManifest.getKey()
        ));
        return new JvmApplicationWithManifests(target, standaloneManifestEntry.getValue(), jvmManifest.getValue());
    }

    private StandaloneApplicationWithManifests fetchStandaloneApplicationManifest(final RuntimeContext context, final StandaloneApplicationTarget target) {
        final Map.Entry<URL, StandaloneManifest> standaloneManifestEntry = fetchStandaloneManifest(context, target.getBaseUrlList(), target.getContentRoot());
        context.setBaseUrlMap(ImmutableMap.of(target.getContentRoot(), standaloneManifestEntry.getKey()));
        return new StandaloneApplicationWithManifests(target, standaloneManifestEntry.getValue());
    }

    private TargetWithManifests<? extends LaunchTarget> fetchManifest(final RuntimeContext context) {
        if (configuration.getLaunchTarget() instanceof JvmApplicationTarget) {
            return fetchJvmApplicationManifest(context, (JvmApplicationTarget) configuration.getLaunchTarget());
        } else if (configuration.getLaunchTarget() instanceof StandaloneApplicationTarget) {
            return fetchStandaloneApplicationManifest(context, (StandaloneApplicationTarget) configuration.getLaunchTarget());
        } else {
            throw new UnsupportedOperationException("Unknown launch target class " + configuration.getLaunchTarget().getClass());
        }
    }

    @Override
    public void burn(RuntimeContext context) {
        context.getGuiApi().notifyProgress(progressRange().lowerEndpoint(), Optional.of("Fetching manifests..."));
        final TargetWithManifests<? extends LaunchTarget> targetWithManifests = fetchManifest(context);
        context.setTargetWithManifest(targetWithManifests);
        context.getGuiApi().notifyProgress(progressRange().upperEndpoint(), Optional.<String>absent());
    }
}
