package es.facite.csvdbq.gui.model;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.event.EventListenerList;

@SuppressWarnings("serial")
public class AppMenuBar extends JMenuBar implements ActionListener {	
	private EventListenerList eventListenerList;
	
	private JMenu mnuFile;
	private JMenuItem mniExit;
	
	private JMenu mnuEdit;
	private JMenuItem mniCut;
	private JMenuItem mniCopy;
	private JMenuItem mniPaste;
	private JMenuItem mniUndo;
	private JMenuItem mniRedo;	
	
	private JMenu mnuHelp;
	private JMenuItem mniHelpInWeb;
		
	public AppMenuBar() {		
		eventListenerList = new EventListenerList();		
		
		mnuFile = new JMenu("Archivo");
		mnuFile.setMnemonic('A');
				
		mniExit = new JMenuItem("Salir");
		mniExit.setMnemonic('S');
		mniExit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F4, InputEvent.ALT_DOWN_MASK));
		mniExit.setActionCommand("exit");
		mniExit.addActionListener(this);
		mnuFile.add(mniExit);
		
		add(mnuFile);
		
		mnuEdit = new JMenu("Edición");
		mnuEdit.setMnemonic('E');
		
		var mniClearCommandHistory = new JMenuItem("Borrar historial de comandos");
		mniClearCommandHistory.setActionCommand("clear-command-history");
		mniClearCommandHistory.addActionListener(this);
		mnuEdit.add(mniClearCommandHistory);

		var mniClearCommandWindow = new JMenuItem("Borrar ventana de comandos");
		mniClearCommandWindow.setMnemonic(KeyEvent.VK_B);
		mniClearCommandWindow.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, KeyEvent.CTRL_DOWN_MASK));
		mniClearCommandWindow.setActionCommand("clear-command-window");
		mniClearCommandWindow.addActionListener(this);
		mnuEdit.add(mniClearCommandWindow);
		mnuEdit.addSeparator();
		
		mniCut = new JMenuItem("Cortar");
		mniCut.setMnemonic('T');
		mniCut.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_DOWN_MASK));
		mniCut.setActionCommand("cut");
		mniCut.addActionListener(this);
		mnuEdit.add(mniCut);
		
		mniCopy = new JMenuItem("Copiar");
		mniCopy.setMnemonic('C');
		mniCopy.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK));
		mniCopy.setActionCommand("copy");
		mniCopy.addActionListener(this);
		mnuEdit.add(mniCopy);
		
		mniPaste = new JMenuItem("Pegar");
		mniPaste.setMnemonic('P');
		mniPaste.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK));
		mniPaste.setActionCommand("paste");
		mniPaste.addActionListener(this);
		mnuEdit.add(mniPaste);
		mnuEdit.addSeparator(); 
		
		mniUndo = new JMenuItem("Deshacer");
		mniUndo.setMnemonic('D');
		mniUndo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK));
		mniUndo.setActionCommand("undo");
		mniUndo.addActionListener(this);
		mnuEdit.add(mniUndo);
		
		mniRedo = new JMenuItem("Rehacer");
		mniRedo.setMnemonic('R');
		mniRedo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK));
		mniRedo.setActionCommand("redo");
		mniRedo.addActionListener(this);
		mnuEdit.add(mniRedo);
		
		add(mnuEdit);				
				
		mnuHelp = new JMenu("Ayuda");
		mnuHelp.setMnemonic('Y');
		mniHelpInWeb = new JMenuItem("Web");
		mniHelpInWeb.setAccelerator(KeyStroke.getKeyStroke("F1"));
		mniHelpInWeb.setActionCommand("help-web");
		mniHelpInWeb.addActionListener(this);
		mnuHelp.add(mniHelpInWeb);
		
		add(mnuHelp);
	}
	
	public void addActionListener(ActionListener actionListener) {
		eventListenerList.add(ActionListener.class, actionListener);
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		for (var listener : eventListenerList.getListeners(ActionListener.class)) {			
			listener.actionPerformed(e);
		}
	}	
	
	public void actionPerformed(String actionCommand) {
		switch (actionCommand) {
		case "can-cut":
			mniCut.setEnabled(true);
			break;
		case "can-paste":
			mniPaste.setEnabled(true);
			break;
		case "can-redo":
			mniRedo.setEnabled(true);
			break;
		case "can-undo":
			mniUndo.setEnabled(true);
			break;
		case "cannot-cut":
			mniCut.setEnabled(false);
			break;
		case "cannot-paste":
			mniPaste.setEnabled(false);
			break;
		case "cannot-redo":
			mniRedo.setEnabled(false);
			break;
		case "cannot-undo":
			mniUndo.setEnabled(false);
			break;
		case "selected-text-on":
			mniCopy.setEnabled(true);
			mniCut.setEnabled(true);
			break;
		case "selected-text-off":
			mniCopy.setEnabled(false);
			mniCut.setEnabled(false);
			break;			
		}		
	}
}
