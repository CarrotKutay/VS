package de.htw.ds.sync;

public class ThreadDemo {

	public static void main (String[] args) {
		// nützliche Kürzel in Eclipse:
		// - F3: open editor for marked type
		// - F4: open hierarchy for marked type
		// - ctrl-shift-T: open type search
		// - ctrl-S: Save (& Compile)

		Runnable worker = () -> {
			try {
				Thread.currentThread().join();
			} catch (Exception e) {
				throw new AssertionError(e);
			}
		};

		int count = 0;
		try {
			while (true) {
				Thread t100 = new Thread(worker);
				t100.setDaemon(true);
				t100.start();

				count += 1;
			}
		} catch (final OutOfMemoryError e) {
			System.out.format("bye bye main-thread after %s threads!", count);
		}
	}
}