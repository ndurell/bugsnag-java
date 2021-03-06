package com.bugsnag;

import com.bugsnag.http.NetworkException;
import java.net.Proxy;

public class Client {
    protected Configuration config = new Configuration();
    protected Diagnostics diagnostics = new Diagnostics(config);
    protected NotificationWorker notificationWorker;

    public Client(String apiKey) {
        this(apiKey, true);
    }

    public Client(String apiKey, boolean installHandler) {
        if(apiKey == null) {
            throw new RuntimeException("You must provide a Bugsnag API key");
        }
        config.apiKey = apiKey;

        // Install a default exception handler with this client
        if(installHandler) {
            ExceptionHandler.install(this);
        }

        notificationWorker = new NotificationWorker(config);
    }

    public void setContext(String context) {
        config.context.setLocked(context);
    }

    /**
    * @deprecated  Replaced by {@link #setUser(String, String, String)}}
    */
    public void setUserId(String id) {
        config.setUser(id, null, null);
    }

    public void setUser(String id, String email, String name) {
        config.setUser(id, email, name);
    }

    public void setReleaseStage(String releaseStage) {
        config.releaseStage.setLocked(releaseStage);
    }

    public void setNotifyReleaseStages(String... notifyReleaseStages) {
        config.setNotifyReleaseStages(notifyReleaseStages);
    }

    public void setAutoNotify(boolean autoNotify) {
        config.setAutoNotify(autoNotify);
    }

    public void setUseSSL(boolean useSSL) {
        config.setUseSSL(useSSL);
    }

    public boolean getUseSSL() {
        return config.useSSL;
    }

    public void setEndpoint(String endpoint) {
        config.setEndpoint(endpoint);
    }

    public void setFilters(String... filters) {
        config.setFilters(filters);
    }

    public void setProjectPackages(String... projectPackages) {
        config.setProjectPackages(projectPackages);
    }

    public void setOsVersion(String osVersion) {
        config.osVersion.setLocked(osVersion);
    }

    public void setAppVersion(String appVersion) {
        config.appVersion.setLocked(appVersion);
    }

    public void setNotifierName(String notifierName) {
        config.setNotifierName(notifierName);
    }

    public void setNotifierVersion(String notifierVersion) {
        config.setNotifierVersion(notifierVersion);
    }

    public void setNotifierUrl(String notifierUrl) {
        config.setNotifierUrl(notifierUrl);
    }

    public void setProxy(Proxy proxy) {
        config.setProxy(proxy);
    }

    public void setIgnoreClasses(String... ignoreClasses) {
        config.setIgnoreClasses(ignoreClasses);
    }

    public void setLogger(Logger logger) {
        config.setLogger(logger);
    }

    public void setSendThreads(boolean sendThreads) {
        config.setSendThreads(sendThreads);
    }

    public void setAsynchronousNotification(boolean asynchronousNotification) {
        config.setAsynchronousNotification(asynchronousNotification);
    }

    public void addBeforeNotify(BeforeNotify beforeNotify) {
        config.addBeforeNotify(beforeNotify);
    }

    public void notify(Error error) {
        if (error == null || error.getException() == null) {
            config.logger.warn("Report not sent to Bugsnag, Throwable is null");
            return;
        }
        if (!config.shouldNotify() ||
            error.shouldIgnore() ||
            !beforeNotify(error)) return;

        Notification notif = new Notification(config, error);
        if (config.asynchronousNotification) {
            notificationWorker.notifyAsync(notif);
        } else {
            notif.deliver();
        }
    }

    public void notify(Throwable e, String severity, MetaData metaData) {
        Error error = new Error(e, severity, metaData, config, diagnostics);
        notify(error);
    }

    public void notify(Throwable e, MetaData metaData) {
        notify(e, null, metaData);
    }

    public void notify(Throwable e, String severity) {
        notify(e, severity, null);
    }

    public void notify(Throwable e) {
        notify(e, null, null);
    }

    public void autoNotify(Throwable e) {
        if(config.autoNotify) {
            notify(e, "error");
        }
    }

    public void addToTab(String tab, String key, Object value) {
        config.addToTab(tab, key, value);
    }

    public void clearTab(String tab) {
        config.clearTab(tab);
    }

    public void trackUser() {
        try {
            Metrics metrics = new Metrics(config, diagnostics);
            metrics.deliver();
        } catch (NetworkException ex) {
            config.logger.warn("Error sending metrics to Bugsnag", ex);
        }
    }

    protected boolean beforeNotify(Error error) {
        for (BeforeNotify beforeNotify : config.beforeNotify) {
            try {
                if (!beforeNotify.run(error)) {
                    return false;
                }
            } catch (Throwable ex) {
                config.logger.warn("BeforeNotify threw an Exception", ex);
            }
        }

        // By default, allow the error to be sent if there were no objections
        return true;
    }

    // Factory methods so we don't have to expose the Configuration class
    public Notification createNotification() {
        return new Notification(config);
    }

    public Notification createNotification(Error error) {
        return new Notification(config, error);
    }

    public Metrics createMetrics() {
        return new Metrics(config, diagnostics);
    }

    public Error createError(Throwable e, String severity, MetaData metaData) {
        return new Error(e, severity, metaData, config, diagnostics);
    }
}
