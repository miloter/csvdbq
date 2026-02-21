package es.facite.csvdbq.core;

import java.time.LocalDate;

public class AppInfo {
	public static final String NAME = "CsvDbQ";
	public static final String VERSION = "0.1.1";
	public static final String COPYRIGHT = "\u00a9 miloter " + LocalDate.now().getYear();
	public static final String LICENSE = " Licencia MIT";
	public static final String USER_DIR = System.getProperty("user.dir");
}
