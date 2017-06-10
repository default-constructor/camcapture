package de.dc.camcapture.model;

import java.io.Serializable;
import java.util.Date;

/**
 * @author Thomas Reno
 */
public class Snapshot implements Serializable {

	private static final long serialVersionUID = -8904972727928023384L;

	private final String filename;
	private final byte[] data;
	private final Date createdAt;

	public Snapshot(String filename, byte[] data, Date createdAt) {
		this.filename = filename;
		this.data = data;
		this.createdAt = createdAt;
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
