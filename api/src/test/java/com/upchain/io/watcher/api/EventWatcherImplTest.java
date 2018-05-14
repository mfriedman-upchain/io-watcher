package com.upchain.io.watcher.api;

import com.upchain.io.watcher.support.IOEvent;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.powermock.reflect.Whitebox;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchKey;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;


@RunWith(MockitoJUnitRunner.class)
//@IfProfileValue(name = "test-groups", values = { "unit-tests", "integration-tests" })

public class EventWatcherImplTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private CountDownLatch latch;
    private IOEvent[] recordedEvent;
    private EventWatcher watcher;

    @Before
    public void setup() throws Exception {

        latch = new CountDownLatch(1);

        watcher = new EventWatcherImpl(folder.getRoot().toPath());

        recordedEvent = new IOEvent[1];

        watcher.addNotifier(e -> {
            recordedEvent[0] = e;
            latch.countDown();
        });
    }

    @After
    public void tearDown() {
        watcher.close();
    }


    @Test
    public void testExistingNestedDirsAreRegistered() throws Exception {

        File dir2 = folder.newFolder();
        File dir3 = Files.createDirectory(dir2.toPath().resolve("fubar")).toFile();

        watcher.start();

        Path createdFile = Files.createFile(dir3.toPath().resolve("baz"));

        latch.await(5000, TimeUnit.MILLISECONDS);

        IOEvent event = recordedEvent[0];

        assertThat(event, notNullValue());
        assertThat(event.getPath(), equalTo(createdFile));
    }


    @Test
    public void testCreateDirectoryEvent_DirsAreRegistered() throws Exception {

        watcher.start();

        File createdDir = folder.newFolder();

        latch.await(5000, TimeUnit.MILLISECONDS);

        IOEvent event = recordedEvent[0];

        assertThat(event, notNullValue());
        assertThat(event.getPath(), equalTo(createdDir.toPath()));
        assertThat(event.getEventType(), equalTo("ENTRY_CREATE"));

        WatchKeyRegistry watchKeyRegistry = Whitebox.getInternalState(watcher, WatchKeyRegistry.class);
        Map<WatchKey, Path> watchKeys = Whitebox.invokeMethod(watchKeyRegistry, "getWatchKeys");

        assertThat(watchKeys.size(), equalTo(2));

        boolean keyCreatedForNewDir = watchKeys.containsValue(createdDir.toPath());

        assertThat(keyCreatedForNewDir, equalTo(true));
    }


    @Test
    public void testCreatedFileEvent() throws Exception {

        watcher.start();

        File createdFile = folder.newFile();

        latch.await(5000, TimeUnit.MILLISECONDS);

        IOEvent event = recordedEvent[0];

        assertThat(event, notNullValue());
        assertThat(event.getPath(), equalTo(createdFile.toPath()));
        assertThat(event.getPath().toFile().isFile(), equalTo(true));
        assertThat(event.getEventType(), equalTo("ENTRY_CREATE"));
    }

    @Test
    public void testDeletedFileEvent() throws Exception {

        File willBeDeleted = folder.newFile();

        watcher.start();

        Files.delete(willBeDeleted.toPath());

        latch.await(5000, TimeUnit.MILLISECONDS);

        IOEvent event = recordedEvent[0];

        assertThat(event, notNullValue());
        assertThat(event.getPath(), equalTo(willBeDeleted.toPath()));
        assertThat(event.getEventType(), equalTo("ENTRY_DELETE"));
    }

}