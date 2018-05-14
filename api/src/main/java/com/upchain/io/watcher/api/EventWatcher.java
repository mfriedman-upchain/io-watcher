package com.upchain.io.watcher.api;

import java.io.IOException;

public interface EventWatcher {

    void addNotifier(Notifier Notifier);

    void start() throws IOException;

    void close();
}
