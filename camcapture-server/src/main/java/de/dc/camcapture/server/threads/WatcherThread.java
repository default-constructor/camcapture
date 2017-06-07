package de.dc.camcapture.server.threads;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dc.camcapture.server.model.PictureFile;
import de.dc.camcapture.server.utils.ServerUtil;

/**
 * @author Thomas Reno
 */
public class WatcherThread implements Runnable {
	
	public interface Listener {
		void onPictureDetected(PictureFile imageFile);
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
						ServerUtil.sleep(1000L);
						String filename = ((Path) event.context()).toFile().getName();
						LOG.info("Detected file {}", filename);
						listener.onPictureDetected(new PictureFile(directory, filename));
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
