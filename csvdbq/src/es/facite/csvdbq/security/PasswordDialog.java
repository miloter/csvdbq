package es.facite.csvdbq.security;

import java.awt.Component;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.WindowConstants;

public class PasswordDialog {
	private final static String TITLE_DEFAULT = "Solicitud de contraseña";
	private final static boolean ALWAYS_ON_TOP_DEFAULT = false;
	
	/**
	 * Muestra un cuadro de diálogo de solicitud de contraseña centrado en la pantalla.
	 *
	 * @return La contraseña en claro o null si se canceló.
	 */
	public static String showPasswordDialog() {
		return showPasswordDialog(null, "", TITLE_DEFAULT, ALWAYS_ON_TOP_DEFAULT);
	}
	
	/**
	 * Muestra un cuadro de diálogo de solicitud de contraseña
	 *
	 * @param parentComponent Componente padre de este cuadro de diálogo. El cuadro de diálogo
	 * aparecerá centrado sobre el padre. Si el padre es <code>null</code> aparecerá centrado
	 * en la pantalla.
	 * @return La contraseña en claro o null si se canceló.
	 */
	public static String showPasswordDialog(Component parentComponent) {
		return showPasswordDialog(parentComponent, "", TITLE_DEFAULT, ALWAYS_ON_TOP_DEFAULT);
	}
	
	/**
	 * Muestra un cuadro de diálogo de solicitud de contraseña centrado en la pantalla.
	 *
	 * @param alwaysOnTop Si es <code>true</code> el cuadro de diálogo se muestra siempre al frente.
	 * @return La contraseña en claro o null si se canceló.
	 */
	public static String showPasswordDialog(boolean alwaysOnTop) {		
		return showPasswordDialog(null, "", TITLE_DEFAULT, alwaysOnTop);
	}
	
	/**
	 * Muestra un cuadro de diálogo de solicitud de contraseña
	 *
	 * @param parentComponent Componente padre de este cuadro de diálogo. El cuadro de diálogo
	 * aparecerá centrado sobre el padre. Si el padre es <code>null</code> aparecerá centrado
	 * en la pantalla.
	 * @param alwaysOnTop Si es <code>true</code> el cuadro de diálogo se muestra siempre al frente.
	 * @return La contraseña en claro o null si se canceló.
	 */
	public static String showPasswordDialog(Component parentComponent, boolean alwaysOnTop) {
		return showPasswordDialog(parentComponent, "", TITLE_DEFAULT, alwaysOnTop);
	}	
	
	/**
	 * Muestra un cuadro de diálogo de solicitud de contraseña centrado en la pantalla.
	 *
	 * @param title Título del cuadro de diálogo.
	 * @param alwaysOnTop Si es <code>true</code> el cuadro de diálogo se muestra siempre al frente.
	 * @return La contraseña en claro o null si se canceló.
	 */
	public static String showPasswordDialog(String title, boolean alwaysOnTop) {
		return showPasswordDialog(null, "", title, alwaysOnTop);
	}
	
	/**
	 * Muestra un cuadro de diálogo de solicitud de contraseña centrado en la pantalla.
	 *
	 * @param message  Texto inicial que aparecerá como contraseña.
	 * @param title Título del cuadro de diálogo.
	 * @param alwaysOnTop Si es <code>true</code> el cuadro de diálogo se muestra siempre al frente.
	 * @return La contraseña en claro o null si se canceló.
	 */
	public static String showPasswordDialog(String message,
			String title, boolean alwaysOnTop) {
		return showPasswordDialog(null, message, title, alwaysOnTop);
	}			

	/**
	 * Muestra un cuadro de diálogo de solicitud de contraseña
	 *
	 * @param parentComponent Componente padre de este cuadro de diálogo. El cuadro de diálogo
	 * aparecerá centrado sobre el padre. Si el padre es <code>null</code> aparecerá centrado
	 * en la pantalla.
	 * @param message  Texto inicial que aparecerá como contraseña.
	 * @param title Título del cuadro de diálogo.
	 * @param alwaysOnTop Si es <code>true</code> el cuadro de diálogo se muestra siempre al frente.
	 * @return La contraseña en claro o null si se canceló.
	 */
	public static String showPasswordDialog(Component parentComponent, String message,
			String title, boolean alwaysOnTop) {		
		final JPasswordField pf = new JPasswordField();
		pf.setText(message);
		// Create OptionPane & Dialog
		JOptionPane pane = new JOptionPane(pf, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
		JDialog dialog = pane.createDialog(parentComponent, title);

		dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

		// Add a listener to the dialog to request focus of Password Field
		dialog.addComponentListener(new ComponentListener() {
			@Override
			public void componentShown(ComponentEvent e) {
				pf.requestFocusInWindow();
			}

			@Override
			public void componentHidden(ComponentEvent e) {
				dialog.dispose();
			}

			@Override
			public void componentResized(ComponentEvent e) {
			}

			@Override
			public void componentMoved(ComponentEvent e) {
			}
		});

		if (alwaysOnTop) {
			dialog.setAlwaysOnTop(true);
		}

		dialog.setVisible(true);

		if (pane.getValue() == null || (int) pane.getValue() == JOptionPane.CANCEL_OPTION || (int) pane.getValue() == JOptionPane.CLOSED_OPTION) {
			return null;
		} else {
			return new String(pf.getPassword());
		}
	}
}
