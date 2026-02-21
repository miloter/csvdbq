package es.facite.csvdbq.gui.model;

import java.util.EventListener;

public interface CommandListener extends EventListener {
	String commandTyped(String command);
	String previousCommand();
	String nextCommand();
}
