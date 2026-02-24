package es.facite.csvdbq.gui.view;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import es.facite.csvdbq.core.AppInfo;
import es.facite.csvdbq.core.CsvRow;
import es.facite.csvdbq.core.CsvRows;
import es.facite.csvdbq.core.TokensCsv;
import es.facite.csvdbq.exception.CsvDbQException;
import es.facite.csvdbq.gui.model.AppMenuBar;
import es.facite.csvdbq.gui.model.AppStatusBar;
import es.facite.csvdbq.gui.model.CommandListener;
import es.facite.csvdbq.gui.model.CommandPanel;
import es.facite.csvdbq.qlcsv.CommandExecutor;
import es.facite.csvdbq.qlcsv.CsvConfig;
import es.facite.csvdbq.qlcsv.Delete;
import es.facite.csvdbq.qlcsv.Insert;
import es.facite.csvdbq.qlcsv.Select;
import es.facite.csvdbq.util.Resource;

@SuppressWarnings("serial")
public class App extends JFrame {
	private static final int MAX_LEN_HISTORY_COMMAND = 1024;
	private static final String USER_DIR = System.getProperty("user.dir");
	private static final CsvConfig CONFIG_COMMAND_HISTORY = new CsvConfig(USER_DIR, TokensCsv.DEFAULT_QUOTES,
			TokensCsv.DEFAULT_SEP, true);
	private static final String TABLE_NAME_COMMAND_HISTORY = "command_history";

	private static CsvConfig configCurrent = new CsvConfig(USER_DIR, TokensCsv.DEFAULT_QUOTES, TokensCsv.DEFAULT_SEP,
			TokensCsv.DEFAULT_DS_POINT);

	private List<String> commands;
	private int iCommand;
	private CommandPanel commandPanel;	
	private CommandExecutor commandExecutor;	
	private AppMenuBar appMenuBar;
	private AppStatusBar statusPanel;
	private ToolsActionListener toolsActionListener;
	

	public App() {
		commands = new ArrayList<>();
		commandExecutor = new CommandExecutor();
		// Si no existe el historial de comandos lo crea
		final File fileCommandHisory = new File(USER_DIR,
				TABLE_NAME_COMMAND_HISTORY + CsvConfig.TABLE_SUFFIX); 
		if (!fileCommandHisory.exists()) {
			commandExecutor.execute("create table " + TABLE_NAME_COMMAND_HISTORY +
					"(commands)", CONFIG_COMMAND_HISTORY);
		}
		// Cargamos el historial de comandos
		onClearCommandHistory(new Select(CONFIG_COMMAND_HISTORY)
				.execute("SELECT commands FROM " + TABLE_NAME_COMMAND_HISTORY).getRows());

		setTitle(AppInfo.NAME + " - " + AppInfo.VERSION);
		setIconImage(Resource.imageOfResource("app.gif"));
		setLayout(new BorderLayout());
		setMinimumSize(new Dimension(640, 480));
		setSize(new Dimension(800, 600));
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);		
		initComponents();
		setJMenuBar(appMenuBar);
		setVisible(true);
	}
	
	public static void main(String[] args) {	
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());			
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
				| UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}
		SwingUtilities.invokeLater(() -> new App());		
	}

	private void initComponents() {
		toolsActionListener = new ToolsActionListener();
		
		appMenuBar = new AppMenuBar();
		appMenuBar.addActionListener(toolsActionListener);			
		
		commandPanel = new CommandPanel(AppInfo.NAME + " - " + AppInfo.VERSION + " - " +
				AppInfo.COPYRIGHT + " - " + AppInfo.LICENSE);
		commandPanel.addActionListener(toolsActionListener);
		commandPanel.setCommandListener(new AppCommandListener());		
		add(commandPanel, BorderLayout.CENTER);
		
		statusPanel = new AppStatusBar();
		add(statusPanel, BorderLayout.SOUTH);
		onUpdateStatusPanel();
		
		addWindowListener(new AppWindowAdapter());
	}

	private void onClearCommandHistory() {
		onClearCommandHistory(new CsvRows());
	}

	private void onClearCommandHistory(CsvRows rows) {
		if (rows.size() == 0) {
			rows = new CsvRows();
			// Agrega una entrada con un espacio en blanco (para que el final del CSV lo
			// reconozca)
			rows.add(CsvRow.create(CONFIG_COMMAND_HISTORY, " "));
		}

		commands.clear();
		for (var row : rows) {
			// Obtenemos la cadena que representa sin el formato de almacenamiento
			commands.add(row.get(0).toString());
		}
		iCommand = commands.size() - 1;
	}

	private void onWindowClosing() {
		// Guarda los comandos
		new Delete(CONFIG_COMMAND_HISTORY).execute("DELETE FROM " + TABLE_NAME_COMMAND_HISTORY);
		new Insert(CONFIG_COMMAND_HISTORY)
				.execute("INSERT INTO " + TABLE_NAME_COMMAND_HISTORY + "(commands) VALUES " + String.join(", ",
						commands.stream().map(c -> "('" + reverseCommand(c) + "')").collect(Collectors.toList())));

		System.exit(0);
	}
	
	private void onUpdateStatusPanel() {
		var text =
			"<html>" +
				"<body style='margin: 5px; font-family: consolas; font-size: 10px;'>" +
					"Entrecomillado: <b color='#0000ff'>" + configCurrent.getQuotes() + "</b> | " +
					"Separador de campos: <b color='#0000ff'>" + configCurrent.getSep() + "</b> | " +
					"Separador decimal: <b color='#0000ff'>" +
						(configCurrent.isDecimalSeparatorPoint() ? '.' : ',') + "</b> | " +
					"Cifrado activado: <b color='#0000ff'>" +
						(configCurrent.getCipher().isEncrypted() ? "Si" : "No") + "</b>" +
					"<hr>" +
					"Directorio de trabajo: <b color='#0000ff'>" + configCurrent.getDbDir() + "</b>" +
				"</body>" +
			"</html>";
		
		statusPanel.setText(text);
	}
	
	/**
	 * Procesa un comando para que pueda ser almacenado y luego recuperarse de forma
	 * idéntica al original. Para conseguirlo se escapan las comillas simples (') y los
	 * caracteres de nueva línea.
	 * Si las comillas ya están escapadas, les agrega una nueva barra de escape previa:
	 * 'hola que tal \'están todos\' hoy' => \'hola que tal \\\'están todos\\\' hoy\'.
	 * Nótese que la cadena final al interprertarse por Java quedará como:
	 * 'hola que tal \'están todos\' hoy', ya que la pieza \' se sutituye por ', y la
	 * pieza \\\' por \' (\\ => \ y \' por '). Para las nuevas líneas, la cadena:
	 * hola
	 * que
	 * tal
	 * quedaría en un sistema Windows como:
	 * hola\r\nque\r\ntal
	 * 
	 * @param command
	 * @return
	 */
	private String reverseCommand(String command) {        
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < command.length()) {
        	final char ch = command.charAt(i);
        	
            if (ch == '\'') {
            	sb.append('\\');                        
                if (i > 0 && command.charAt(i - 1) == '\\') {                	                
                	sb.append('\\');
                }                
                sb.append('\'');
            } else if (ch == '\r' || ch == '\n') {
            	if (ch == '\r') {
            		sb.append('\\');            
            		sb.append('r');
            	}
            	sb.append('\\');
            	sb.append('n');
            	if (ch == '\r' && i < (command.length() - 1) && command.charAt(i + 1) == '\n' ) {
            		i++;
            	}           
            } else {
                sb.append(ch);
            }
            i++;
        }
        return sb.toString();
    }

	private void addCommand(String command) {		
		// Si se ha superado el número máximo de comandos elminina el más antiguo
		if (commands.size() == MAX_LEN_HISTORY_COMMAND) {
			commands.remove(0);
		}

		// Existe el comando?
		iCommand = commands.indexOf(command);
		if (iCommand >= 0) {
			// Quita el comando de la posición elegida
			commands.remove(iCommand);
		}
		// Pone el comando al final
		commands.set(commands.size() - 1, command);

		// Agrega una entrada con un espacio en blanco (para que el final del CSV lo
		// reconozca)
		commands.add(" ");

		// Apunta siempre a una entrada en blanco
		iCommand = commands.size() - 1;
	}

	private String executeCommand(String command) {
		final long tStart = System.currentTimeMillis();
		String result;

		try {
			result = commandExecutor.execute(command, configCurrent);
			result += System.lineSeparator() + "Comando ejecutado en " + (System.currentTimeMillis() - tStart) + " ms.";
			onUpdateStatusPanel();
		} catch (CsvDbQException e) {
			result = e.getMessage();
		} catch (Exception e) {
			result = e.getMessage();
		}

		return result;
	}
	
	private class AppWindowAdapter extends WindowAdapter {
		@Override
		public void windowClosing(WindowEvent e) {
			onWindowClosing();
		}
		
		@Override
		public void windowOpened(WindowEvent e) {
			commandPanel.requestFocus();
		}
	}
	
	private class ToolsActionListener implements ActionListener {		
		@Override
		public void actionPerformed(ActionEvent e) {
			switch(e.getActionCommand()) {
			case "can-cut":
			case "can-paste":
			case "can-redo":
			case "can-undo":
			case "cannot-cut":
			case "cannot-paste":
			case "cannot-redo":
			case "cannot-undo":
				appMenuBar.actionPerformed(e.getActionCommand());
				break;
			case "clear-command-history":
				final int option = JOptionPane.showConfirmDialog(App.this,
						"¿Está seguro que desea borrar el historial de comandos?",
						getTitle(), JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
				if (option == JOptionPane.OK_OPTION) {
					onClearCommandHistory();
				}
				break;
			case "clear-command-window":
				commandPanel.newPrompt();
				break;
			case "copy":
				commandPanel.copy();
				break;
			case "cut":
				commandPanel.cut();
				break;
			case "exit":
				onWindowClosing();;
				break;	
			case "help-web":
				try {
					Desktop.getDesktop().browse(new URI("https://assets.facite.es/csvdbq"));
				} catch (Exception ex) {					
					ex.printStackTrace();
					JOptionPane.showMessageDialog(App.this, "Error al abrir la ayuda web: " +
					ex.getMessage(), getTitle(), JOptionPane.WARNING_MESSAGE);
				}
				break;
			case "paste":
				commandPanel.paste();
				break;
			case "redo":
			case "undo":
				commandPanel.actionPerformed(e.getActionCommand());
				break;
			case "selected-text-on":
			case "selected-text-off":
				appMenuBar.actionPerformed(e.getActionCommand());				
				break;
			}			
		}		
	}
	
	private class AppCommandListener implements CommandListener {
		@Override
		public String previousCommand() {
			if (iCommand > 0) {
				iCommand--;
				return commands.get(iCommand);
			}
			return null;
		}

		@Override
		public String nextCommand() {
			if (iCommand < (commands.size() - 1)) {
				iCommand++;
				return commands.get(iCommand);
			}
			return null;
		}

		@Override
		public String commandTyped(String command) {
			addCommand(command);
			return executeCommand(command);
		}
	}
}
