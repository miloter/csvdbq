package es.facite.csvdbq.gui.model;

import java.awt.BorderLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;

@SuppressWarnings("serial")
public class AppStatusBar extends JPanel {
	private JLabel lblStatus;
	
	public AppStatusBar() {
		setLayout(new BorderLayout());
		initComponents();
	}
	
	public void setText(String text) {
		lblStatus.setText(text);
	}

	private void initComponents() {
		lblStatus = new JLabel();
		add(lblStatus, BorderLayout.CENTER);
	}	
}
