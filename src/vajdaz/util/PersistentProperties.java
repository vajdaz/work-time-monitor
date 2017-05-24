package vajdaz.util;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class PersistentProperties extends Properties {
	private static final long serialVersionUID = -5663271800140136783L;
	private String sFileName = null;

	public PersistentProperties(String sFileName) {
		this.sFileName = sFileName;
		load();
	}

	public PersistentProperties(Properties props, String sFileName) {
		super(props);
		this.sFileName = sFileName;
		load();
	}

	public synchronized Object setPropertyPersistent(String key, String value) {
		Object retVal = super.setProperty(key, value);
		save();
		return retVal;
	}

	protected synchronized void load() {
		try {
			java.io.FileInputStream in = new java.io.FileInputStream(sFileName);
			loadFromXML(in);
			in.close();
		} catch (IOException e) {
			System.out.println("Warning: IO error in PersistntProperties::load().");
		}
	}

	public void refresh() {
		load();
	}

	protected synchronized void save() {
		try {
			FileOutputStream out = new FileOutputStream(sFileName);
			storeToXML(out, null);
			out.close();
		} catch (IOException e) {
			System.out.println("Warning: IO error in PersistntProperties::save().");
		}
	}
}
