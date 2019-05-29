package de.htw.ds.tcp;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import de.htw.tool.Exceptions;
import de.htw.tool.LongValidator;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

/**
 * Inner controller class for GUI mode.
 */
public class TcpMonitorController implements AutoCloseable {
	static private final Charset ASCII = Charset.forName("ASCII");
	static private final Predicate<String> PORT_VALIDATOR = new LongValidator(1, 0xffff);

	private volatile TcpMonitorServer monitorServer;
	private final ImageView startIcon, suspendIcon, resumeIcon, stopIcon, trashIcon;
	private final BorderPane rootPane;
	private final TextField servicePortField, redirectHostField, redirectPortField, errorField;
	private final TextArea requestArea, responseArea;
	private final Button startButton, stopButton, clearButton;
	private final TableView<TcpMonitorRecord> recordTable;
	private final TableColumn<TcpMonitorRecord,Long> recordIdentityColumn, recordRequestLengthColumn, recordResponseLengthColumn;
	private final TableColumn<TcpMonitorRecord,String> recordOpenTimestampColumn, recordCloseTimestampColumn;


	/**
	 * Creates a new controller instance using the given view, and initializes view callbacks.
	 * @param rootPane the root pane
	 * @param icons the icons
	 * @throws NullPointerException if any of the given arguments is {@code null}
	 */
	@SuppressWarnings("unchecked")
	public TcpMonitorController (final BorderPane rootPane, final Map<String,Image> icons) throws NullPointerException {
		this.rootPane = rootPane;
		this.startIcon = new ImageView(icons.get("start"));
		this.suspendIcon = new ImageView(icons.get("suspend"));
		this.resumeIcon = new ImageView(icons.get("resume"));
		this.stopIcon = new ImageView(icons.get("stop"));
		this.trashIcon = new ImageView(icons.get("trash"));

		final HBox controlPane = (HBox) this.rootPane.getTop();
		this.servicePortField = (TextField) controlPane.getChildren().get(1);
		this.redirectHostField = (TextField) controlPane.getChildren().get(3);
		this.redirectPortField = (TextField) controlPane.getChildren().get(5);
		this.startButton = (Button) controlPane.getChildren().get(6);
		this.stopButton = (Button) controlPane.getChildren().get(7);
		this.clearButton = (Button) controlPane.getChildren().get(8);
		final SplitPane recordPane = (SplitPane) this.rootPane.getCenter();
		this.recordTable = (TableView<TcpMonitorRecord>) recordPane.getItems().get(0);
		this.recordIdentityColumn = (TableColumn<TcpMonitorRecord,Long>) this.recordTable.getColumns().get(0);
		this.recordOpenTimestampColumn = (TableColumn<TcpMonitorRecord,String>) this.recordTable.getColumns().get(1);
		this.recordCloseTimestampColumn = (TableColumn<TcpMonitorRecord,String>) this.recordTable.getColumns().get(2);
		this.recordRequestLengthColumn = (TableColumn<TcpMonitorRecord,Long>) this.recordTable.getColumns().get(3);
		this.recordResponseLengthColumn = (TableColumn<TcpMonitorRecord,Long>) this.recordTable.getColumns().get(4);
		final SplitPane trafficPane = (SplitPane) recordPane.getItems().get(1);
		this.requestArea = (TextArea) trafficPane.getItems().get(0);
		this.responseArea = (TextArea) trafficPane.getItems().get(1);
		final HBox errorPane = (HBox) this.rootPane.getBottom();
		this.errorField = (TextField) errorPane.getChildren().get(1);

		this.startButton.setGraphic(this.startIcon);
		this.stopButton.setGraphic(this.stopIcon);
		this.clearButton.setGraphic(this.trashIcon);
		this.stopButton.setDisable(true);
		this.clearButton.setDisable(true);

		// initialize view callbacks
		this.servicePortField.textProperty().addListener((observable, oldValue, newValue) -> this.handlePortChanged(this.servicePortField));
		this.servicePortField.focusedProperty().addListener((observable, oldValue, newValue) -> this.handleFocusChanged(this.servicePortField));
		this.redirectPortField.textProperty().addListener((observable, oldValue, newValue) -> this.handlePortChanged(this.redirectPortField));
		this.redirectPortField.focusedProperty().addListener((observable, oldValue, newValue) -> this.handleFocusChanged(this.redirectPortField));
		this.startButton.setOnAction(event -> this.handleStartButtonPressed());
		this.stopButton.setOnAction(event -> this.handleStopButtonPressed());
		this.clearButton.setOnAction(event -> this.handleClearButtonPressed());
		this.recordTable.getSelectionModel().selectedItemProperty().addListener(
			(observed, oldSelection, newSelection) -> this.handleTableSelectionChanged(this.recordTable.getSelectionModel().getSelectedIndex())
		);
		this.recordIdentityColumn.setCellValueFactory(new PropertyValueFactory<TcpMonitorRecord,Long>("identity"));
		this.recordOpenTimestampColumn.setCellValueFactory(record -> new SimpleStringProperty(String.format("%tT", record.getValue().getOpenTimestamp())));
		this.recordCloseTimestampColumn.setCellValueFactory(record -> new SimpleStringProperty(String.format("%tT", record.getValue().getCloseTimestamp())));
		this.recordRequestLengthColumn.setCellValueFactory(new PropertyValueFactory<TcpMonitorRecord,Long>("requestLength"));
		this.recordResponseLengthColumn.setCellValueFactory(new PropertyValueFactory<TcpMonitorRecord,Long>("responseLength"));
	}


	/**
	 * Closes this controller's resources.
	 */
	public void close () {
		try { this.monitorServer.close(); } catch (final Exception exception) {}
		this.monitorServer = null;
	}


	/**
	 * Event handler for refocusing due to invalid content.
	 * @param source a node whose focus just changed
	 */
	protected void handleFocusChanged (final Node node) {
		if (!node.isFocused() && node.getStyleClass().contains("invalid")) {
			Platform.runLater(() -> node.requestFocus());
		}
	}


	/**
	 * Event handler for port field validation.
	 * @param node a node whose text just changed
	 */
	protected void handlePortChanged (final TextInputControl node) {
		final boolean valid = PORT_VALIDATOR.test(node.getText());
		if (valid) {
			node.getStyleClass().remove("invalid");
		} else if (!node.getStyleClass().contains("invalid")) {
			node.getStyleClass().add("invalid");
		}
		this.startButton.setDisable(!valid);
	}


	/**
	 * Event handler for the start button.
	 */
	protected void handleStartButtonPressed () {
		this.errorField.setText("");
		try {
			if (this.monitorServer == null) {
				final int servicePort = Integer.parseInt(this.servicePortField.getText());
				final String redirectHostName = this.redirectHostField.getText();
				final int redirectHostPort = Integer.parseInt(this.redirectPortField.getText());
				final InetSocketAddress redirectHostAddress = new InetSocketAddress(redirectHostName, redirectHostPort);
				final Consumer<TcpMonitorRecord> recordConsumer = record -> this.handleRecordCreated(record);
				final Consumer<Throwable> exceptionConsumer = exception -> this.handleExceptionCatched(exception);

				this.monitorServer = new TcpMonitorServer(servicePort, redirectHostAddress, recordConsumer, exceptionConsumer);
				this.stopButton.setDisable(false);
				new Thread(this.monitorServer, "tcp-acceptor").start();
			}

			final boolean active = this.startButton.getGraphic() == this.suspendIcon;
			this.startButton.getTooltip().setText(active ? "resume" : "suspend");
			this.startButton.setGraphic(active ? this.resumeIcon : this.suspendIcon);
		} catch (final Exception exception) {
			this.errorField.setText(errorMessage(exception));
		}
	}


	/**
	 * Closes and discards this pane's TCP monitor, and sets the activity state to inactive.
	 */
	protected void handleStopButtonPressed () {
		try { this.close(); } catch (final Exception exception) {}

		this.startButton.getTooltip().setText("start");
		this.startButton.setGraphic(this.startIcon);
		this.stopButton.setDisable(true);
		this.errorField.setText("");
	}


	/**
	 * Event handler for the clear button.
	 */
	protected void handleClearButtonPressed () {
		this.errorField.setText("");
		this.recordTable.getItems().clear();
		this.clearButton.setDisable(true);
	}


	/**
	 * Event handler for the list selector.
	 * @param rowIndex the selected row index
	 */
	protected void handleTableSelectionChanged (final int rowIndex) {
		if (rowIndex == -1) {
			this.requestArea.setText("");
			this.responseArea.setText("");
		} else {
			final TcpMonitorRecord record = this.recordTable.getItems().get(rowIndex);
			this.requestArea.setText(new String(record.getRequestData(), ASCII));
			this.responseArea.setText(new String(record.getResponseData(), ASCII));
		}
	}


	/**
	 * {@inheritDoc}
	 */
	protected void handleExceptionCatched (final Throwable exception) {
		if (this.startButton.getGraphic() == this.suspendIcon) {
			this.errorField.setText(errorMessage(exception));
		}
	}


	/**
	 * {@inheritDoc}
	 */
	protected void handleRecordCreated (final TcpMonitorRecord record) {
		if (this.startButton.getGraphic() == this.suspendIcon) {
			this.recordTable.getItems().add(record);
			this.clearButton.setDisable(false);
			this.errorField.setText("");
		}
	}


	/**
	 * Returns a formatted error message for the given exception, or an empty string for none.
	 * @param exception the (optional) exception
	 */
	static private String errorMessage (final Throwable exception) {
		if (exception == null) return "";
		final Throwable rootCause = Exceptions.rootCause(exception);
		return String.format("%s: %s", rootCause.getClass().getSimpleName(), rootCause.getMessage());
	}
}
