package com.cinema.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.time.Instant;
import java.util.Properties;

/** Local delivery ledger. It stores booking identifiers only, never recipients or passwords. */
@Component
public class BookingEmailDeliveryStore {
    private final Path file;

    public BookingEmailDeliveryStore(@Value("${app.booking-email.delivery-file:./var/booking-email/sent.properties}") String file) {
        this.file = Path.of(file).toAbsolutePath().normalize();
    }

    public synchronized void deliverOnce(String key, Runnable send) throws IOException {
        Path parent = file.getParent();
        Files.createDirectories(parent);
        // Also coordinate application processes that share this ledger directory.
        try (var channel = FileChannel.open(file.resolveSibling(file.getFileName() + ".lock"),
                StandardOpenOption.CREATE, StandardOpenOption.WRITE);
             var lock = channel.tryLock()) {
            if (lock == null) return; // Another worker owns delivery; the recovery job will retry.
            Properties sent = new Properties();
            if (Files.exists(file)) {
                try (var input = Files.newInputStream(file)) { sent.load(input); }
            }
            if (sent.containsKey(key)) return;
            // Allocate the temporary file before SMTP, so unwritable storage prevents delivery.
            Path temporary = Files.createTempFile(parent, "sent-", ".tmp");
            try {
                send.run(); // Failed sends never acquire a sent marker.
                sent.setProperty(key, Instant.now().toString());
                try (var output = Files.newOutputStream(temporary)) {
                    sent.store(output, "Booking payment email deliveries");
                }
                try {
                    Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException ex) {
                    Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(temporary);
            }
        }
    }
}
