package com.upchain.io.watcher.api;

import com.sun.nio.file.SensitivityWatchEventModifier;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.powermock.reflect.Whitebox;

import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class WatchKeyRegistryTest {

    private WatchKeyRegistry watchKeyRegistry;

    @Mock
    private
    WatchService watchService;

    @Before
    public void setup() {
        watchKeyRegistry = new WatchKeyRegistry(watchService, EventWatcherImpl.eventKinds);
    }

    @Test
    public void testMakeFileVisitor() throws Exception {

        FileVisitor<Path> fileVisitor = Whitebox.invokeMethod(watchKeyRegistry, "makeFileVisitor");

        Path dir = Mockito.mock(Path.class);
        BasicFileAttributes basicFileAttributes = Mockito.mock(BasicFileAttributes.class);
        WatchKey watchKey = Mockito.mock(WatchKey.class);

        when(dir.register(watchService, EventWatcherImpl.eventKinds, SensitivityWatchEventModifier.HIGH))
                .thenReturn(watchKey);

        fileVisitor.preVisitDirectory(dir, basicFileAttributes);

        verify(dir).register(watchService, EventWatcherImpl.eventKinds, SensitivityWatchEventModifier.HIGH);

        Map<WatchKey, Path> watchKeys = Whitebox.invokeMethod(watchKeyRegistry, "getWatchKeys");

        assertThat(watchKeys.size(), equalTo(1));
        assertThat(watchKeys.containsKey(watchKey), is(true));
        assertThat(fileVisitor, instanceOf(SimpleFileVisitor.class));

        Path p = watchKeyRegistry.getPathForKey(watchKey);
        assertThat(p, equalTo(dir));
    }
}