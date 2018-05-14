package com.upchain.io.watcher.api;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.upchain.io.watcher.support.IOEvent;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.powermock.reflect.Whitebox;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class EventWatcherImpl_WithMockFS_Test {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private CountDownLatch latch;
    private IOEvent[] recordedEvent ;



    @Mock
    WatchService watchService;

    @Mock
    WatchKeyRegistry watchKeyRegistry;

    Path parentPath;

    EventWatcherImpl eventWatcher;


    @Before
    public void setup() throws Exception {

        parentPath = new File("/parentDir").toPath();

        latch = new CountDownLatch(1);

        eventWatcher = new EventWatcherImpl(parentPath);

        Whitebox.setInternalState(eventWatcher, WatchKeyRegistry.class, watchKeyRegistry);
        Whitebox.setInternalState(eventWatcher, WatchService.class, watchService);

        recordedEvent = new IOEvent[1];

        eventWatcher.addNotifier(e -> {
            recordedEvent[0] = e;
            latch.countDown();
        });
    }

    @After
    public void tearDown() {
        eventWatcher.close();
    }

    @Test
    public void testCreateFileEventHappens() throws Exception {

        Path createdPath = new File("created").toPath();

        WatchKey watchKey = mock(WatchKey.class);

        when(watchService.take()) .thenReturn(watchKey);

        WatchEvent createdEvent = mock(WatchEvent.class);

        when(createdEvent.kind()).thenReturn(ENTRY_CREATE);

        @SuppressWarnings("unchecked")
        List<WatchEvent<?>> watchEvents = Lists.newArrayList(createdEvent);

        when(watchKey.pollEvents()).thenReturn(watchEvents);

        when(createdEvent.context()).thenReturn(createdPath);

        when(watchKeyRegistry.getPathForKey(watchKey)).thenReturn(parentPath);

        Whitebox.setInternalState(watchKeyRegistry, Map.class, ImmutableMap.of(watchKey, parentPath));

        eventWatcher.start();


        latch.await(5000, TimeUnit.MILLISECONDS);

        System.out.println("recordedEvent = " + recordedEvent[0]);

        System.out.println("watchEvents = " + watchEvents);


        IOEvent ioEvent = recordedEvent[0];

        assertThat(ioEvent, notNullValue());
        assertThat(ioEvent.getPath(), equalTo(parentPath.resolve(createdPath)));







    }





//    @Test
//    public void testExistingNestedDirsAreRegistered() throws Exception {
//
//        File dir2 = folder.newFolder();
//        File dir3 = Files.createDirectory(dir2.toPath().resolve("fubar")).toFile();
//
//        watcher.start();
//
//        Path createdFile = Files.createFile(dir3.toPath().resolve("baz"));
//
//        latch.await(5000, TimeUnit.MILLISECONDS);
//
//        IOEvent event = recordedEvent[0];
//
//        assertThat(event, notNullValue());
//        assertThat(event.getPath(), equalTo(createdFile));
//    }


}