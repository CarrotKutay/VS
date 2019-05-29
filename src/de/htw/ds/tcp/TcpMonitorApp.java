package de.htw.ds.tcp;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import de.htw.tool.Copyright;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;


/**
 * This class models a TCP monitor application that can work in both command and GUI mode. TCP
 * monitors are servers that redirect all incoming client connections towards another host, while
 * logging all traffic.
 */
@Copyright(year=2012, holders="Sascha Baumeister")
public class TcpMonitorApp extends Application {
	static private final String PACKAGE_PATH = TcpMonitorApp.class.getPackage().getName().replace('.', '/');

	/**
	 * Application entry point. Note that passing no arguments starts the application in GUI mode,
	 * while passing three starts it in command mode.
	 * @param args the runtime arguments: either none, or a service port, redirect address, and
	 *        context path
	 * @throws NullPointerException if any of the given arguments is {@code null}
	 * @throws IllegalArgumentException if there are three arguments while representing an invalid
	 *         service port, redirect address, or context path
	 * @throws IOException if there is an I/O related problem
	 */
	static public void main (final String[] args) throws IOException {
		launch(args);
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public void start (final Stage window) throws IOException {
		final BorderPane rootPane = newRootPane();
		final Map<String, Image> icons = new HashMap<>();
		for (final String name : new String[] { "start", "suspend", "resume", "stop", "trash" }) {
			icons.put(name, newIcon(name));
		}

		final TcpMonitorController controller = new TcpMonitorController(rootPane, icons);
		final Image icon = new Image(PACKAGE_PATH + "/tcp-monitor.png");
		final Scene sceneGraph = new Scene(rootPane, 640, 480);
		sceneGraph.getStylesheets().add(PACKAGE_PATH + "/tcp-monitor.css");

		window.setOnCloseRequest(event -> controller.close());
		window.setScene(sceneGraph);
		window.setTitle("TCP Monitor");
		window.getIcons().add(icon);
		window.show();
	}


	/**
	 * Returns a new root pane.
	 * @return the root pane created
	 * @throws IOException if there is an I/O related problem
	 */
	static private BorderPane newRootPane () throws IOException {
		try (InputStream byteSource = Thread.currentThread().getContextClassLoader().getResourceAsStream(PACKAGE_PATH + "/tcp-monitor.fxml")) {
			return new FXMLLoader().load(byteSource);
		}
	}


	/**
	 * Returns a new icon image.
	 * @param name the icon name
	 * @return the icon created
	 * @throws NullPointerException if the given argument is {@code null}
	 * @throws IOException if there is an I/O related problem
	 */
	static private Image newIcon (final String name) throws NullPointerException, IOException {
		try (InputStream byteSource = Thread.currentThread().getContextClassLoader().getResourceAsStream(PACKAGE_PATH + "/" + name + "-icon.gif")) {
			return new Image(byteSource);
		}
	}
}