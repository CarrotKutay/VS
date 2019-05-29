package de.htw.ds.sync;

public class VariableDemo {

	public static void main (final String[] args) {
		final Thread worker = new Thread(() -> System.out.println("Aloa!"));

		boolean asynchronous = true;
		asynchronous = Boolean.parseBoolean(args[0]);
		if (asynchronous)
			worker.start();		// führt den Code in neuem Thread asynchron aus
		else
			worker.run();		// führt den Code des threads synchron im main-Thread aus!
	}
}
