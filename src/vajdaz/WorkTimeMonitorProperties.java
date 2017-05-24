package vajdaz;

import vajdaz.util.PersistentProperties;

public class WorkTimeMonitorProperties extends PersistentProperties {
	public static final String START_TIME = "startTime";
	public static final String BREAK_TIME = "breakTime";
	public static final String BREAK_TIME_SPAN = "breakTimeSpan";
	public static final String LUNCH_TIME_SPAN = "lunchTimeSpan";
	public static final String WORKTIME_HOURS = "worktimeHours";
	public static final String EDITOR = "editor";
	private static final long serialVersionUID = -1429104126216722722L;

	public WorkTimeMonitorProperties(String sFileName) {
		super(sFileName);
		checkAndSetInitValues();
	}

	public WorkTimeMonitorProperties(java.util.Properties props, String sFileName) {
		super(props, sFileName);
		checkAndSetInitValues();
	}
	
	private boolean setDefaultValue(String key, String defaultValue) {
		if (!containsKey(key)) {
			setProperty(key, defaultValue);
			return true;
		}
		return false;
	}

	private void checkAndSetInitValues() {
		boolean dirty = false;
		dirty |= setDefaultValue(START_TIME, "");
		dirty |= setDefaultValue(BREAK_TIME, "");
		dirty |= setDefaultValue(BREAK_TIME_SPAN, "1");
		dirty |= setDefaultValue(LUNCH_TIME_SPAN, "0.75");
		dirty |= setDefaultValue(WORKTIME_HOURS, "8");
		dirty |= setDefaultValue(EDITOR, "notepad.exe");
		if (dirty) {
			save();
		}
	}
}