package com.upchain.io.watcher.api;

import com.upchain.io.watcher.support.IOEvent;

public interface Notifier {

    void notify(IOEvent fileEvent) ;
}
