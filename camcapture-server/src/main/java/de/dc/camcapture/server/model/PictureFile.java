package de.dc.camcapture.server.model;

import static de.dc.camcapture.server.utils.ServerUtil.*;

import java.io.File;

public class PictureFile extends File {

	private static final long serialVersionUID = -8904972727928023384L;

	private final String directory;
	private final String filename;
	private final long createdAt;

	public PictureFile(String directory, String filename) {
		super(directory, filename);
		this.directory = directory;
		this.filename = filename;
		String name = filename.substring(0, filename.indexOf("."));
		createdAt = getMillis(getDateTime(name, "yyyyMMdd_HHmmss"));
	}

	public String getDirectory() {
		return directory;
	}

	public String getFilename() {
		return filename;
	}

	public long getCreatedAt() {
		return createdAt;
	}
}
