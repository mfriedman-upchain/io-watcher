package com.upchain.io.watcher.api;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.sun.nio.file.SensitivityWatchEventModifier;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;

public class WatchKeyRegistry {

    private final Map<WatchKey, Path> watchKeys = Maps.newHashMap();
    private final WatchService watchService;
    private final WatchEvent.Kind[] eventKinds;

    WatchKeyRegistry(WatchService watchService, WatchEvent.Kind[] eventKinds) {
        this.watchService = watchService;
        this.eventKinds = eventKinds;
    }

    void register(Path path) {
        try {
            Files.walkFileTree(path, makeFileVisitor());
        } catch (IOException e) {
            throw new EventWatcherException(String.format("Failed to register directory: %s", path), e);
        }
    }

    private Map<WatchKey, Path> getWatchKeys() {
        return ImmutableMap.copyOf(watchKeys);
    }

    Path getPathForKey(WatchKey key) {
        return watchKeys.get(key);
    }

    private FileVisitor<Path> makeFileVisitor() {

        return new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {

                /*
                 * NOTE: In general we should not use any com.sun.* libraries but atm we don't have a choice
                 * as the class type is hardcoded into oracle's WatchService implementation.
                 */
                WatchKey watchKey = dir.register(watchService, eventKinds, SensitivityWatchEventModifier.HIGH);
                watchKeys.put(watchKey, dir);
                return super.preVisitDirectory(dir, attrs);
            }
        };
    }
}
