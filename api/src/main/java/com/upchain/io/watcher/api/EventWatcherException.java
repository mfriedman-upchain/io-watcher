package com.upchain.io.watcher.api;

public class EventWatcherException extends RuntimeException {
    public EventWatcherException(String msg, Exception e) {
        super(msg,e );
    }
}
