package com.upchain.io.watcher.api;

import com.google.common.collect.Lists;
import com.upchain.io.watcher.support.IOEvent;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.nio.file.StandardWatchEventKinds.*;

public class EventWatcherImpl implements EventWatcher {

    private final Path dirToWatch;
    private final WatchService watchService;
    private  final ExecutorService executor;
    private List<Notifier> notifiers = Lists.newArrayList();
    private final WatchKeyRegistry watchKeyRegistry;

    @SuppressWarnings("WeakerAccess")
    static final WatchEvent.Kind[] eventKinds = new WatchEvent.Kind[]{ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY};

    EventWatcherImpl(Path dirToWatch) throws IOException {
        this.dirToWatch = dirToWatch;
        watchService = FileSystems.getDefault().newWatchService();
        executor = Executors.newSingleThreadExecutor();
        watchKeyRegistry = new WatchKeyRegistry(watchService, eventKinds);
    }

    @Override
    public void addNotifier(Notifier Notifier) {
        notifiers.add(Notifier);
    }



    @Override
    public void start() {

        register(dirToWatch);

        executor.submit(() -> {

            while (true) {

                try {
                    WatchKey key = watchService.take();


                    List<WatchEvent<?>> events = key.pollEvents();

                    for(WatchEvent watchEvent : events) {
                        System.out.println(" a watch evt: " +watchEvent);


                        @SuppressWarnings("unchecked")
                        Path eventPath = ((WatchEvent<Path>) watchEvent).context();

                        Path absPath = resolveAbsolutePath(watchKeyRegistry.getPathForKey(key), eventPath);

                        // todo possibly a race condition here; the file/dir might get deleted before we access it. However, in this case, `isDirectory` will return false, and it will no longer exist, so it probably doesn't in fact matter.
                        if (absPath.toFile().isDirectory()) {
                            register(absPath);
                        }

                        doNotify(key, watchEvent);
                    }

//                    events.forEach(watchEvent -> {
//
//                        @SuppressWarnings("unchecked")
//                        Path eventPath = ((WatchEvent<Path>) watchEvent).context();
//
//                        Path absPath = resolveAbsolutePath(watchKeyRegistry.getPathForKey(key), eventPath);
//
//                        // todo possibly a race condition here; the file/dir might get deleted before we access it. However, in this case, `isDirectory` will return false, and it will no longer exist, so it probably doesn't in fact matter.
//                        if (absPath.toFile().isDirectory()) {
//                            register(absPath);
//                        }
//
//                        doNotify(key, watchEvent);
//
//                    });

                    if (!key.reset()) {
                        // object no longer registered
                        break;
                    }


                } catch (InterruptedException e) {
                    // todo what should happen to this exception?
                    e.printStackTrace();
                }
            }
        });


    }

    @Override
    public void close() {

        executor.shutdown();

        try {
            watchService.close();
        } catch (IOException e) {

            // todo should this ex just be ignored?

            throw new EventWatcherException("Error while closing the event watcher. ", e);
        }
    }

    private Path resolveAbsolutePath(Path parent, Path eventPath) {
        return parent.resolve(eventPath);
    }

    private void register(Path path) {
        watchKeyRegistry.register(path);
    }

    private void doNotify(WatchKey key, WatchEvent<?> watchEvent) {

        Path parentDir = watchKeyRegistry.getPathForKey(key);

        Path eventPath = (Path) watchEvent.context();

        Path absPath = resolveAbsolutePath(parentDir, eventPath);

        IOEvent evt = IOEvent.builder()
                .path(absPath)
                .eventType(watchEvent.kind().name())
                .build();

        for(Notifier notifier : notifiers) {
            notifier.notify(evt);
        }

//        notifiers.forEach(notifier -> {
//            //noinspection unchecked
//            notifier.notify(evt);
//        });
    }

}
