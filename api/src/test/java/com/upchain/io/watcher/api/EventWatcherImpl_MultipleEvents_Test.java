package com.upchain.io.watcher.api;

import com.google.common.collect.Lists;
import com.upchain.io.watcher.support.IOEvent;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;


@RunWith(MockitoJUnitRunner.class)
public class EventWatcherImpl_MultipleEvents_Test {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private CountDownLatch latch;

    @Test
    public void testMultipleEventsArePublished() throws Exception {

        File willBeDeleted = folder.newFile();

        latch = new CountDownLatch(2);

        EventWatcher watcher = new EventWatcherImpl(folder.getRoot().toPath());

        List<IOEvent> eventList = Lists.newArrayList();

        watcher.addNotifier(e -> {
            eventList.add(e);
            latch.countDown();
        });

        watcher.start();

        Files.delete(willBeDeleted.toPath());
        Path fubar = folder.getRoot().toPath().resolve("fubar");

        Files.createFile(fubar);

        latch.await(5000, TimeUnit.MILLISECONDS);

        assertThat(eventList.size(), equalTo(2));


        IOEvent expectedDeletedEvent = IOEvent.builder().eventType("ENTRY_DELETE").path(willBeDeleted.toPath()).build();

        assertThat(
                eventList.stream().filter(e -> e.getEventType().equals("ENTRY_DELETE")).findAny().get(),
                equalTo(expectedDeletedEvent)
        );

        IOEvent expectedCreatedFileEvent = IOEvent.builder().eventType("ENTRY_CREATE").path(fubar).build();

        assertThat(
                eventList.stream().filter(e -> e.getEventType().equals("ENTRY_CREATE")).findAny().get(),
                equalTo(expectedCreatedFileEvent)
        );

    }
}