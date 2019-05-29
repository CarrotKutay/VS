package de.htw.ds.sync;

import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import de.htw.tool.Copyright;
import de.htw.tool.IOStreams;


/**
 * Demonstrates child process fork-join using interruptibe blocking. Note that any remaining active
 * child processes should be stopped when ending resynchronization prematurely (by exception)!
 */
@Copyright(year=2008, holders="Sascha Baumeister")
public final class ResyncProcessByBlocking {

	/**
	 * Application entry point. The single parameters must be a command line suitable to start a
	 * program/process.
	 * @param args the arguments
	 * @throws IndexOutOfBoundsException if no argument is passed
	 * @throws IOException if there's an I/O related problem
	 * @throws InterruptedException 
	 */
	static public void main (final String[] args) throws IOException, InterruptedException {
		System.out.println("Starting child process... ");
		final Process childProcess = Runtime.getRuntime().exec(args[0]);

		System.out.println("Connecting child process I/O streams with current Java process... ");
		redirectSystemStreams(childProcess);
		final long timestamp = System.currentTimeMillis();

		System.out.println("Resynchronising child process... ");
		try {
			childProcess.waitFor();
		} catch (final Throwable exception) {
			childProcess.destroyForcibly();
			throw exception;
		}

		System.out.format("Child process ended with exit code %s after running %sms.\n", childProcess.exitValue(), System.currentTimeMillis() - timestamp);
	}


	/**
	 * Redirects the system I/O streams of the given child process to this process.
	 * @param childProcess the child process
	 */
	static private void redirectSystemStreams (final Process childProcess) {
		final Runnable systemInputTransporter = () -> {
			try {
				IOStreams.copy(System.in, new PrintStream(childProcess.getOutputStream()), 0x10);
			} catch (final IOException exception) {
				throw new UncheckedIOException(exception);
			}
		};
		final Runnable systemOutputTransporter = () -> {
			try {
				IOStreams.copy(childProcess.getInputStream(), System.out, 0x10);
			} catch (final IOException exception) {
				throw new UncheckedIOException(exception);
			}
		};
		final Runnable systemErrorTransporter = () -> {
			try {
				IOStreams.copy(childProcess.getErrorStream(), System.err, 0x10);
			} catch (final IOException exception) {
				throw new UncheckedIOException(exception);
			}
		};

		// System.in transporter must be started as a daemon thread, otherwise read-block prevents termination!
		final Thread systemInputThread = new Thread(systemInputTransporter, "sysin-transporter");
		systemInputThread.setDaemon(true);
		systemInputThread.start();
		new Thread(systemOutputTransporter, "sysout-transporter").start();
		new Thread(systemErrorTransporter, "syserr-transporter").start();
	}
}