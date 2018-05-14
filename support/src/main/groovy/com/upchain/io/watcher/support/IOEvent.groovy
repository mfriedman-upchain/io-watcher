package com.upchain.io.watcher.support

import groovy.transform.Canonical
import groovy.transform.builder.Builder

import java.nio.file.Path

@Canonical
@Builder
class IOEvent {
    String eventType
    Path path
}
