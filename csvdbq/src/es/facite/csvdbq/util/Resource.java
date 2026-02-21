package es.facite.csvdbq.util;

import java.awt.Image;
import java.awt.Toolkit;
import java.net.URL;

import javax.swing.Icon;
import javax.swing.ImageIcon;

public class Resource {
	private static final String RESOURCES_PATH = "/es/facite/csvdbq/gui/resource";
	
	public static Image imageOfResource(String name) {
		return Toolkit.getDefaultToolkit().createImage(urlOfResource(name));
	}
	
	public static Icon iconOfResource(String name) {		
		return new ImageIcon(urlOfResource(name));
	}
	
	private static URL urlOfResource(String name) {
		return Resource.class.getResource(RESOURCES_PATH + "/" + name);
	}
}
