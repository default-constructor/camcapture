package de.dc.camcapture.model;

import static de.dc.camcapture.server.utils.ServerUtil.*;

import java.io.Serializable;
import java.util.Date;

/**
 * @author Thomas Reno
 */
public class Snapshot implements Serializable {

	private static final long serialVersionUID = -8904972727928023384L;

	private static final String PATTERN_FILENAME = "yyyyMMdd_HHmmss";

	private final String filename;

	private final byte[] data;

	private final Date createdAt;

	public Snapshot(String filename, byte[] data) {
		this.filename = filename;
		this.data = data;
		String name = filename.substring(0, filename.indexOf("."));
		createdAt = getDateTime(name, PATTERN_FILENAME);
	}

	public String getFilename() {
		return filename;
	}

	public byte[] getData() {
		return data;
	}

	public Date getCreatedAt() {
		return createdAt;
	}
}
