package de.dc.camcapture.model;

import java.io.Serializable;
import java.util.Date;

import static de.dc.camcapture.client.utils.ClientUtil.*;

/**
 * @author Thomas Reno
 */
public class Snapshot implements Serializable {

	private static final long serialVersionUID = -8904972727928023384L;

	private byte[] data;

	private final String filename;

	private final Date createdAt;

	public Snapshot(String filename) {
		this.filename = filename;
		String name = filename.substring(0, filename.indexOf("."));
		createdAt = getDateTime(name, "yyyyMMdd_HHmmss");
	}

	public String getFilename() {
		return filename;
	}

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}

	public Date getCreatedAt() {
		return createdAt;
	}
}
