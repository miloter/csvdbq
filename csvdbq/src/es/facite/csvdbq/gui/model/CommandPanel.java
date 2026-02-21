package es.facite.csvdbq.gui.model;

import java.awt.BorderLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.EventListenerList;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.BadLocationException;
import javax.swing.undo.UndoManager;

import es.facite.csvdbq.exception.CsvDbQException;

@SuppressWarnings("serial")
public class CommandPanel extends JPanel implements ActionListener {	
	private static final String PROMPT = "> ";

	private JTextArea txaCommand;
	private JPopupMenu pmnEdit;
	private JMenuItem mniCut;
	private JMenuItem mniCopy;
	private JMenuItem mniPaste;
	private UndoManager undoRedo;
	private EventListenerList eventListenerList;
	private CommandListener commandListener;
	private int commandStart;
	private boolean controlDown;
	private boolean keyPage;
	
	public CommandPanel() {
		this("");
	}
	
	public CommandPanel(String welcomeText) {		
		undoRedo = new UndoManager();
		eventListenerList = new EventListenerList();
		
		setLayout(new BorderLayout());
		initComponents();		
		showPrompt(welcomeText);
	}
	
	private void initComponents() {
		txaCommand = new JTextArea();
		txaCommand.setMargin(new Insets(8, 8, 8, 8));
		// Para gestionar las acciones de deshacer y rehacer
		txaCommand.getDocument().addUndoableEditListener(new TextEditUndoableEditListener());
		// Para controlar si hay texto seleccionado o no
		txaCommand.addCaretListener(new TextEditCaretListener());
		// Para gestionar los comandos en función de lo tecleado
		txaCommand.addKeyListener(new CommandPanelKeyListener());
		// Para controlar el menú contextual
		txaCommand.addMouseListener(new TextEditMouseAdapter());
		
		pmnEdit = new JPopupMenu();
		
		mniCut = new JMenuItem("Cortar");		
		mniCut.setActionCommand("cut");
		mniCut.addActionListener(this);
		pmnEdit.add(mniCut);

		mniCopy = new JMenuItem("Copiar");
		mniCopy.setActionCommand("copy");
		mniCopy.addActionListener(this);
		pmnEdit.add(mniCopy);
		
		mniPaste = new JMenuItem("Pegar");
		mniPaste.setActionCommand("paste");
		mniPaste.addActionListener(this);
		pmnEdit.add(mniPaste);

		
		add(new JScrollPane(txaCommand), BorderLayout.CENTER);
	}
	
	public void setCommandListener(CommandListener commandListener) {
		this.commandListener = commandListener;
	}	
	
	private void showPrompt() {
		showPrompt(null);
	}
	
	private void showPrompt(String previous) {
		if (previous != null) {
			txaCommand.append(previous);
		}
		txaCommand.append(System.lineSeparator());
		txaCommand.append(PROMPT);
		commandStart = textLength();
		discardAllEdits();
	}
	
	public void newPrompt() {
		txaCommand.setText(PROMPT);
		txaCommand.setSelectionStart(textLength());
		commandStart = txaCommand.getSelectionStart();
		discardAllEdits();
	}
	
	private void discardAllEdits() {
		undoRedo.discardAllEdits();
		emitUndoRedoActionPerformed();
	}
	
	private int textLength() {
		return txaCommand.getDocument().getLength();
	}

	private int getCurrentLine() {
		try {
			return txaCommand.getLineOfOffset(txaCommand.getCaretPosition());			
		} catch (BadLocationException e) {
			throw new CsvDbQException(e);
		}
	}
	
	private int getCommandStartLine() {
		try {			
			return txaCommand.getLineOfOffset(commandStart);
		} catch (BadLocationException e) {
			throw new CsvDbQException(e);
		}
	}
	
	private int getCommandEndLine() {
		try {			
			return txaCommand.getLineOfOffset(textLength());
		} catch (BadLocationException e) {
			throw new CsvDbQException(e);
		}
	}
		
	/**
	 * Corrige el punto de arranque de la línea de comandos ya que puede quedar fuera
	 * de límites a consecuencia de la ejecución de algunos comandos.
	 */
	private void correctCommandStart() {
	    // Puede haberse borrado la ventana de comandos o suprimido texto
	    if (commandStart > textLength()) {
	        commandStart = textLength();
	    }

	    // Se posicionó el cursor en un punto anterior
	    if (txaCommand.getSelectionStart() < commandStart) {
	        txaCommand.setSelectionStart(commandStart);	        
	    }
	}	
	
	private class CommandPanelKeyListener implements KeyListener {
		@Override
		public void keyTyped(KeyEvent e) { }
		
		@Override
		public void keyReleased(KeyEvent e) {
			if (!(controlDown || keyPage)) {
				correctCommandStart();
			}
		}
		
		@Override
		public void keyPressed(KeyEvent e) {
			String command;
			String result;
						
			controlDown = e.isControlDown();
			if (controlDown) return;
			
			keyPage = e.getKeyCode() == KeyEvent.VK_PAGE_DOWN || e.getKeyCode() == KeyEvent.VK_PAGE_UP;			
			if (!keyPage) {
				correctCommandStart();
			}

			try {
				switch (e.getKeyCode()) {
				case KeyEvent.VK_ENTER:
					e.consume();
					if (e.isShiftDown()) {
						txaCommand.replaceSelection(System.lineSeparator());
						return;
					}
					command = txaCommand.getText(commandStart, textLength() - commandStart);
					if (command.trim().length() > 0) {						
						if (commandListener != null) {
							result = commandListener.commandTyped(command);									
							txaCommand.append(System.lineSeparator());
							showPrompt(result);
						} else {
							showPrompt();
						}						
					} else {
						showPrompt();
					}
					break;
				case KeyEvent.VK_ESCAPE:
					txaCommand.replaceRange("", commandStart, textLength());
					break;
				case KeyEvent.VK_BACK_SPACE:
					if (txaCommand.getSelectionStart() == commandStart &&
						txaCommand.getSelectionEnd() == txaCommand.getSelectionStart())  {
						e.consume();
					}
					break;
				case KeyEvent.VK_UP:
					if (getCurrentLine() > getCommandStartLine()) return;
					if (commandListener != null) {
						command = commandListener.previousCommand();
						if (command != null) {
							txaCommand.replaceRange(command, commandStart, textLength());	
							e.consume();
						}
					}
					break;
				case KeyEvent.VK_DOWN:
					if (getCurrentLine() < getCommandEndLine()) return;
					if (commandListener != null) {
						command = commandListener.nextCommand();
						if (command != null) {
							txaCommand.replaceRange(command, commandStart, textLength());
							e.consume();
						}
					}
					break;				
				}				
			} catch (BadLocationException ex) {
				throw new CsvDbQException(ex);
			}
		}
	}
	
	@Override
	public void requestFocus() {
		txaCommand.requestFocus();
	}

	public void copy() {
		txaCommand.copy();
	}

	public void paste() {
		txaCommand.paste();
	}

	public void cut() {
		txaCommand.cut();
	}


	@Override
	public void actionPerformed(ActionEvent e) {
		switch (e.getActionCommand()) {
		case "cut":
			cut();
			break;
		case "copy":
			copy();
			break;
		case "paste":
			paste();
			break;
		}
	}	
	
	public void actionPerformed(String actionCommand) {
		switch (actionCommand) {
		case "undo":
			if (undoRedo.canUndo()) {
				undoRedo.undo();
			}			
			break;
		case "redo":
			if (undoRedo.canRedo()) {
				undoRedo.redo();
			}
			break;		
		}	
		emitUndoRedoActionPerformed();
	}
	
	public void emitUndoRedoActionPerformed() {
		for (var listener : eventListenerList.getListeners(ActionListener.class)) {
			listener.actionPerformed(new ActionEvent(this, -1, undoRedo.canUndo() ?
					"can-undo" : "cannot-undo"));
			listener.actionPerformed(new ActionEvent(this, -1, undoRedo.canRedo() ?
					"can-redo" : "cannot-redo"));
		}
	}
	
	public void addActionListener(ActionListener actionListener) {
		eventListenerList.add(ActionListener.class, actionListener);
		emitUndoRedoActionPerformed();
	}
	
	private class TextEditCaretListener implements CaretListener {
		@Override
		public void caretUpdate(CaretEvent e) {						
			boolean isSelectedText = txaCommand.getSelectedText() != null;
			
			for (var listener : eventListenerList.getListeners(ActionListener.class)) {								
				listener.actionPerformed(new ActionEvent(e, -1,
						isSelectedText ?
						"selected-text-on" : "selected-text-off"));
				listener.actionPerformed(new ActionEvent(e, -1,
						isSelectedText && txaCommand.getSelectionStart() >= commandStart ?
						"can-cut" : "cannot-cut"));
				listener.actionPerformed(new ActionEvent(e, -1,
						txaCommand.getSelectionStart() >= commandStart ?
							"can-paste" : "cannot-paste"));
			}
		}
	}	
	
	private class TextEditUndoableEditListener implements UndoableEditListener {
		@Override
		public void undoableEditHappened(UndoableEditEvent e) {
			undoRedo.addEdit(e.getEdit());
			emitUndoRedoActionPerformed();
		}		
	}
	
	private class TextEditMouseAdapter extends MouseAdapter {
		@Override
		public void mousePressed(MouseEvent e) {
			if (e.getButton() != MouseEvent.BUTTON3) return;
			
			boolean isSelectedText = txaCommand.getSelectedText() != null;
			mniCut.setEnabled(isSelectedText && txaCommand.getSelectionStart() >= commandStart);
			mniCopy.setEnabled(isSelectedText);
			mniPaste.setEnabled(txaCommand.getSelectionStart() >= commandStart);
			pmnEdit.show(txaCommand, e.getX(), e.getY());
		}
	}
}
