package de.htw.ds.sync;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;


public final class FileCopyMultiThreaded {
	
	static public void main (final String[] args) throws IOException {
		
		final Sender sender = new Sender();
		final Receiver receiver = new Receiver();
		
		sender.pos.connect(receiver.pis);
		
		final Thread senderThread = new Thread( sender , "my-thread1");
		final Thread receiverThread = new Thread( receiver , "my-thread2");
		
		senderThread.start();
		receiverThread.run();
		
		System.out.println("done.");
	} 	
}

class Sender extends PipedOutputStream implements Runnable {

	public PipedOutputStream pos = new PipedOutputStream();
	
	@SuppressWarnings("finally")
	@Override
	public void run() {
		
		while(true) {
			try {
				File file = new File("C:\\Users\\carro\\OneDrive\\Desktop\\uni\\SoSe19\\Verteilte Systeme\\distributed systems\\test\\test.txt");
				FileReader fr = new FileReader(file);
				while (fr.ready()) {
					pos.write( fr.read() );
				}
				fr.close();
				pos.close();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				break;
			}
		}
	}
	
}

class Receiver extends PipedInputStream implements Runnable {
	
	public PipedInputStream pis = new PipedInputStream();
 
	@SuppressWarnings("finally")
	@Override
	public void run() {
		while (true) {
			try {
				File copy = new File("C:\\Users\\carro\\OneDrive\\Desktop\\uni\\SoSe19\\Verteilte Systeme\\distributed systems\\test\\copy.txt");
				FileWriter fw = new FileWriter(copy);
				while (pis.available()>0) {
					fw.write(pis.read());
					fw.flush();
				}
				fw.close();
				pis.close();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				break;
			}
		}
		
	}
	
}
