package vajdaz;

import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import vajdaz.util.HelperFunctions;
import vajdaz.util.PersistentProperties;

public class WorkTimeMonitor {
	private static final String CONFIG_FILENAME = "vajdaz.WorktimeMonitor.config";
	private Image image1 = null;
	private Image image2 = null;
	private Image image3 = null;
	private TrayIcon trayIcon = null;
	private PersistentProperties props = new WorkTimeMonitorProperties(CONFIG_FILENAME);
	private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

	public WorkTimeMonitor() {
		if (!SystemTray.isSupported()) {
			throw new RuntimeException("Error: Systray not supported.");
		}
		try {
			image1 = javax.imageio.ImageIO.read(WorkTimeMonitor.class.getClassLoader().getResource("ampel_1.png"));
			image2 = javax.imageio.ImageIO.read(WorkTimeMonitor.class.getClassLoader().getResource("ampel_2.png"));
			image3 = javax.imageio.ImageIO.read(WorkTimeMonitor.class.getClassLoader().getResource("ampel_3.png"));
			trayIcon = new TrayIcon(image1, "", createMenu());
			trayIcon.setImageAutoSize(true);
			SystemTray.getSystemTray().add(trayIcon);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private PopupMenu createMenu() {
		PopupMenu popup = new PopupMenu();
		MenuItem menuItem = new MenuItem("Beenden");
		menuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				try {
					SystemTray.getSystemTray().remove(trayIcon);
					executor.shutdown();
					executor.awaitTermination(60, TimeUnit.SECONDS);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				System.exit(0);
			}
		});
		popup.add(menuItem);
		menuItem = new MenuItem("Einstellungen neu laden");
		menuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				props.refresh();
				WorkTimeMonitor.this.updateTrayIcon();
			}
		});
		popup.add(menuItem);
		menuItem = new MenuItem("Konfigurationsdatei öffnen");
		menuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				try {
					Process proc = Runtime.getRuntime()
							.exec(props.getProperty(WorkTimeMonitorProperties.EDITOR) + " " + CONFIG_FILENAME);
					proc.waitFor();
					if (proc.exitValue() == 0) {
						props.refresh();
						WorkTimeMonitor.this.updateTrayIcon();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		popup.add(menuItem);
		popup.add(menuItem);
		menuItem = new MenuItem("Pausenzeit zurücksetzen");
		menuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				WorkTimeMonitor.this.resetBreakTime();
				WorkTimeMonitor.this.updateTrayIcon();
			}
		});
		popup.add(menuItem);
		return popup;
	}

	private boolean didThisRanToday() {
		SimpleDateFormat sdf = new SimpleDateFormat();
		String sLastRun = props.getProperty("startTime", "");
		if (sLastRun.length() == 0) {
			return false;
		}
		Calendar calLastRun = Calendar.getInstance();
		try {
			calLastRun.setTime(sdf.parse(sLastRun));
		} catch (java.text.ParseException e) {
			System.out.println("Warning: Parse error in Properties.");
			return false;
		}
		Calendar calNow = Calendar.getInstance();
		return (calNow.get(1) == calLastRun.get(1)) && (calNow.get(2) == calLastRun.get(2))
				&& (calNow.get(5) == calLastRun.get(5));
	}

	private void startup() {
		if (!didThisRanToday()) {
			resetStartTime();
			resetBreakTime();
		}
		executor.scheduleAtFixedRate(new Runnable() {
			public void run() {
				WorkTimeMonitor.this.updateTrayIcon();
			}
		}, 0, 60000, TimeUnit.MILLISECONDS);
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				try {
					executor.shutdown();
					executor.awaitTermination(60, TimeUnit.SECONDS);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});
	}

	private long resetStartTime() {
		Date now = new Date();
		props.setPropertyPersistent("startTime", new SimpleDateFormat().format(now));
		return now.getTime();
	}

	private long resetBreakTime() {
		Date now = new Date();
		Calendar cal = Calendar.getInstance();
		cal.setTime(now);
		cal.add(10, 1);
		props.setPropertyPersistent("breakTime", new SimpleDateFormat().format(cal.getTime()));
		return cal.getTimeInMillis();
	}

	private boolean moveBreakTime() {
		Date now = new Date();
		Date breakTime = null;
		try {
			breakTime = new SimpleDateFormat().parse(props.getProperty("breakTime"));
		} catch (Exception e) {
			resetBreakTime();
			return false;
		}
		if (now.getTime() > breakTime.getTime()) {
			Calendar cal = Calendar.getInstance();
			cal.setTime(now);
			cal.add(10, 1);
			props.setPropertyPersistent("breakTime", new SimpleDateFormat().format(cal.getTime()));
			return true;
		}
		return false;
	}

	private void updateTrayIcon() {
		Date startTime = null;
		try {
			startTime = new SimpleDateFormat().parse(props.getProperty("startTime"));
		} catch (java.text.ParseException e) {
			throw new RuntimeException(e);
		}
		Date now = new Date();
		long grossWorkitmeDone = (now.getTime() - startTime.getTime()) / 1000;
		long lunchime = (long) (3600.0 * Double.parseDouble(props.getProperty("lunchTimeSpan", "1")));
		long netWorktimeDone = grossWorkitmeDone - lunchime;
		long netWorktime = (long) (3600.0 * Double.parseDouble(props.getProperty("worktimeHours", "8")));

		if ((netWorktimeDone <= netWorktime - 3600) && (trayIcon.getImage() != image1)) {
			trayIcon.setImage(image1);
		} else if ((netWorktimeDone > netWorktime - 3600) && (netWorktimeDone <= netWorktime)
				&& (trayIcon.getImage() != image2)) {
			trayIcon.setImage(image2);
			trayIcon.displayMessage("Worktime Monitor", "Noch eine Stunde", TrayIcon.MessageType.INFO);
			Toolkit.getDefaultToolkit().beep();
		} else if ((netWorktimeDone > netWorktime) && (trayIcon.getImage() != image3)) {
			trayIcon.setImage(image3);
			trayIcon.displayMessage("Worktime Monitor", "Go, go, go!", TrayIcon.MessageType.INFO);
			Toolkit.getDefaultToolkit().beep();
		}

		boolean breakTimeNotification = false;
		if (moveBreakTime()) {
			breakTimeNotification = true;
		}
		long nextBreakInSeconds = 0;
		try {
			nextBreakInSeconds = new SimpleDateFormat().parse(props.getProperty("breakTime")).getTime() / 1000;
		} catch (Exception e) {
			nextBreakInSeconds = resetBreakTime() / 1000;
		}

		long[] split = HelperFunctions.splitTimeDiff(netWorktimeDone);
		String sTimeAll = String.format("Netto Arbeit: " + (split[0] < 0 ? "-" : "") + "%d:%02d",
				new Object[] { Long.valueOf(split[2]), Long.valueOf(split[3]) });
		split = HelperFunctions.splitTimeDiff(netWorktime - netWorktimeDone);
		String sTimeRemaining = String.format((split[0] < 0 ? "Überzug " : "Restzeit: ") + "%d:%02d",
				new Object[] { Long.valueOf(split[2]), Long.valueOf(split[3]) });
		split = HelperFunctions.splitTimeDiff(nextBreakInSeconds - now.getTime() / 1000);
		String sNextBreak = String.format(split[0] < 0 ? "Nächste Pause: -" : "Nächste Pause: %d:%02d",
				new Object[] { Long.valueOf(split[2]), Long.valueOf(split[3]) });
		split = HelperFunctions.splitTimeDiff(lunchime);
		String sLunchTime = String.format("Mittagspause: %d:%02d",
				new Object[] { Long.valueOf(split[2]), Long.valueOf(split[3]) });

		if (breakTimeNotification) {
			trayIcon.displayMessage("Worktime Monitor", "Pause!", TrayIcon.MessageType.INFO);
			Toolkit.getDefaultToolkit().beep();
		}
		trayIcon.setToolTip(sTimeAll + "\n" + sTimeRemaining + "\n" + sNextBreak + "\n" + sLunchTime);
	}

	public static void main(String[] args) {
		WorkTimeMonitor gadget = new WorkTimeMonitor();
		gadget.startup();
	}
}
