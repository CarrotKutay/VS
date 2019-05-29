package de.htw.ds.sync;

import de.htw.tool.Copyright;


/**
 * This facade provides demo runnable instances featuring equivalent functionality, but created using various means. It
 * demonstrates the brevity advantages of lambda expressions over anonymous classes over external, inner or local classes, at
 * least as long as the implemented functionality can be expressed briefly.
 */
@Copyright(year = 2015, holders = "Sascha Baumeister")
public final class DemoRunnables {

	/**
	 * Named runnable class that prints it's text property.
	 */
	static public class NamedRunnable implements Runnable {
		private final String text;

		public NamedRunnable (final String text) throws NullPointerException {
			if (text == null) throw new NullPointerException();

			this.text = text;
		}

		public void run () {
			System.out.println(this.text);
		}
	}



	/**
	 * Returns a new instance of the named runnable class. Note that the coding effort is 10+3 = 13 lines of code.
	 * @param text the text to the printed by the runnable
	 * @return the runnable
	 */
	static public Runnable newNamedClassRunnable (final String text) {
		return new NamedRunnable(text);
	}


	/**
	 * Returns a new instance of an anonymous runnable class. Note that the coding effort is 5 lines of code, i.e. around 38% of
	 * the named class case.
	 * @param text the text to the printed by the runnable
	 * @return the runnable
	 */
	static public Runnable newAnonymousClassRunnable (final String text) {
		return new Runnable() {
			public void run () {
				System.out.println(text);
			}
		};
	}


	/**
	 * Returns a new instance of a lambda expression. Note that the coding effort is 1 lines of code, i.e. 20% of the anonymous
	 * class case, and around 7.6% of the named class case. Additionally, lambda expressions do not define "this", therefore an
	 * outer instance context remains accessible within.
	 * @param text the text to the printed by the runnable
	 * @return the runnable
	 */
	static public Runnable newLambdaRunnable (final String text) {
		return () -> System.out.println(text);
	}



	/**
	 * Prevents external instantiation.
	 */
	private DemoRunnables () {}
}