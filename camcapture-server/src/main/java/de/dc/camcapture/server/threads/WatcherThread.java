package de.dc.camcapture.server.threads;

import static de.dc.camcapture.server.utils.ServerUtil.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dc.camcapture.model.Snapshot;

/**
 * @author Thomas Reno
 */
public class WatcherThread implements Runnable {

	interface Listener {
		void onSnapshotDetected(Snapshot snapshot);
	}

	private static final Logger LOG = LoggerFactory.getLogger(WatcherThread.class);

	@Override
	public void run() {
		try {
			WatchKey watchKey = Paths.get(directory).register(watcherService, StandardWatchEventKinds.ENTRY_CREATE);
			while (true) {
				for (WatchEvent<?> event : watchKey.pollEvents()) {
					if (StandardWatchEventKinds.ENTRY_CREATE.equals(event.kind())) {
						// Waiting 1 sec so the file can be finished written
						sleep(1000L);
						File file = ((Path) event.context()).toFile();
						String filename = file.getName();
						File snapshotFile = new File(directory, filename);
						LOG.info("Snapshot detected {}", filename);
						try (FileInputStream fis = new FileInputStream(snapshotFile)) {
							Date createdAt = new Date(snapshotFile.lastModified());
							byte[] buffer = new byte[fis.available()];
							fis.read(buffer);
							Snapshot snapshot = new Snapshot(filename, buffer, createdAt);
							listener.onSnapshotDetected(snapshot);
						}
					}
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private Listener listener;

	private final String directory;
	private final WatchService watcherService;

	public WatcherThread(String directory) {
		this.directory = directory;
		try {
			watcherService = FileSystems.getDefault().newWatchService();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void addListener(Listener listener) {
		this.listener = listener;
	}
}
