package com.cinema.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class BookingEmailDeliveryStoreTest {
    @TempDir Path temp;

    @Test void failedSendDoesNotCreateASentMarker() throws Exception {
        String file = temp.resolve("sent.properties").toString();
        var store = new BookingEmailDeliveryStore(file);
        assertThrows(IllegalStateException.class, () -> store.deliverOnce("42:BK42", () -> {
            throw new IllegalStateException("SMTP failed");
        }));
        var count = new AtomicInteger();
        new BookingEmailDeliveryStore(file).deliverOnce("42:BK42", count::incrementAndGet);
        new BookingEmailDeliveryStore(file).deliverOnce("42:BK42", count::incrementAndGet);
        assertEquals(1, count.get());
    }

    @Test void concurrentWorkersAndRestartDoNotDuplicateSuccessfulDelivery() throws Exception {
        String file = temp.resolve("sent.properties").toString();
        var store = new BookingEmailDeliveryStore(file);
        var count = new AtomicInteger();
        var executor = Executors.newFixedThreadPool(4);
        try {
            var tasks = new java.util.ArrayList<Future<?>>();
            for (int i = 0; i < 8; i++) tasks.add(executor.submit(() -> {
                try { store.deliverOnce("42:BK42", count::incrementAndGet); }
                catch (java.io.IOException ex) { throw new RuntimeException(ex); }
            }));
            for (var task : tasks) task.get(5, TimeUnit.SECONDS);
        } finally { executor.shutdownNow(); }
        new BookingEmailDeliveryStore(file).deliverOnce("42:BK42", count::incrementAndGet);
        assertEquals(1, count.get());
        assertTrue(Files.readString(Path.of(file)).contains("BK42"));
    }

    @Test void unwritableLedgerDoesNotSend() throws Exception {
        Path parentIsFile = temp.resolve("not-a-directory");
        Files.writeString(parentIsFile, "fixture");
        var count = new AtomicInteger();
        var store = new BookingEmailDeliveryStore(parentIsFile.resolve("sent.properties").toString());
        assertThrows(java.io.IOException.class, () -> store.deliverOnce("42:BK42", count::incrementAndGet));
        assertEquals(0, count.get());
    }
}
