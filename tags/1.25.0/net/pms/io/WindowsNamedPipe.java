/*
 * PS3 Media Server, for streaming any medias to your PS3.
 * Copyright (C) 2008  A.Brochard
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; version 2
 * of the License only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.pms.io;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;

import net.pms.PMS;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.StdCallLibrary;

public class WindowsNamedPipe extends Thread implements ProcessWrapper {

	private static final int BUFSIZE = 500000;

	public interface Kernel32 extends StdCallLibrary {
		Kernel32 INSTANCE = (Kernel32) Native.loadLibrary("kernel32",
				Kernel32.class);
		Kernel32 SYNC_INSTANCE = (Kernel32) Native
				.synchronizedLibrary(INSTANCE);

		class SECURITY_ATTRIBUTES extends Structure {
			public int nLength = size();
			public Pointer lpSecurityDescriptor;
			public boolean bInheritHandle;
		}

		public static class LPOVERLAPPED extends Structure {

		}

		Pointer CreateNamedPipeA(String lpName, int dwOpenMode, int dwPipeMode,
				int nMaxInstances, int nOutBufferSize, int nInBufferSize,
				int nDefaultTimeOut, SECURITY_ATTRIBUTES lpSecurityAttributes);

		boolean ConnectNamedPipe(Pointer handle, LPOVERLAPPED overlapped);

		boolean DisconnectNamedPipe(Pointer handle);

		boolean FlushFileBuffers(Pointer handle);

		boolean CloseHandle(Pointer handle);

		boolean ReadFile(Pointer hFile, Pointer lpBuffer,
				int nNumberOfBytesToRead, IntByReference lpNumberOfBytesRead,
				LPOVERLAPPED lpOverlapped);

		boolean WriteFile(Pointer hFile, Pointer lpBuffer,
				int nNumberOfBytesToRead, IntByReference lpNumberOfBytesRead,
				LPOVERLAPPED lpOverlapped);
	}

	public static void main(String[] args) throws Exception {

	}

	private String name;
	private boolean in;
	private boolean forcereconnect;
	private Pointer handle1;
	private Pointer handle2;
	private OutputStream writable;
	private InputStream readable;
	private Thread forced;
	private boolean b2; 
	private FileOutputStream debug;
	public static boolean loop = true;
	private BufferedOutputFile directBuffer;

	public WindowsNamedPipe(String n, boolean forcereconnect, boolean in, OutputParams params) {
		this.name = n;
		this.name = "\\\\.\\pipe\\" + this.name; 
		this.in = in;
		this.forcereconnect = forcereconnect;
		PMS.info("Creating pipe " + this.name);
		try {
			if (PMS.get().isWindows()) {
				handle1 = Kernel32.INSTANCE.CreateNamedPipeA(this.name, 3, 0, 255,
						BUFSIZE, BUFSIZE, 0, null);
				if (forcereconnect) {
					handle2 = Kernel32.INSTANCE.CreateNamedPipeA(this.name, 3, 0, 255,
							BUFSIZE, BUFSIZE, 0, null);
				}
				if (params != null)
					directBuffer = new BufferedOutputFile(params);
				else {
					writable = new PipedOutputStream();
					readable = new PipedInputStream((PipedOutputStream) writable, BUFSIZE);
				}
				start();
				if (forcereconnect) {
					forced = new Thread(new Runnable() {
						public void run() {
							b2 = Kernel32.INSTANCE.ConnectNamedPipe(handle2, null);
						}
					});
					forced.start();
				}
				//debug = new FileOutputStream(n);
				//Thread.sleep(150);
			}
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}

	public void run() {

		PMS.info("Waiting for pipe connection " + this.name);
		boolean b1 = Kernel32.INSTANCE.ConnectNamedPipe(handle1, null);

		if (forcereconnect) {
			while (forced.isAlive()) {
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {}
			}
			PMS.info("Forced reconnection of " + name + " with result : " + b2);
			//Kernel32.INSTANCE.DisconnectNamedPipe(handle1);
			//Kernel32.INSTANCE.CloseHandle(handle1);
			handle1 = handle2;
			
		}

		PMS.info("Result of " + this.name + " : " + b1);

		try {
			if (b1) {
				if (in) {
					IntByReference ibr = new IntByReference();
					int count = 0;
					//byte buffer [] = new byte [BUFSIZE];
					Memory buffer = new Memory(BUFSIZE);
					while (loop) {
						// ByteBuffer buffer = ByteBuffer.wrap(b);
						boolean fSuccess = Kernel32.INSTANCE.ReadFile(handle1,
								buffer, BUFSIZE, ibr, null);
						int cbBytesRead = ibr.getValue();
						if (cbBytesRead == -1) {
							if (directBuffer != null)
								directBuffer.close();
							if (writable != null)
								writable.close();
							if (debug != null)
								debug.close();
							break;
						}
						count++;
						if (directBuffer != null)
							directBuffer.write(buffer.getByteArray(0, cbBytesRead));
						if (writable != null)
							writable.write(buffer.getByteArray(0, cbBytesRead));
						if (debug != null)
							debug.write(buffer.getByteArray(0, cbBytesRead));
									
						if (!fSuccess || cbBytesRead == 0) {
							if (directBuffer != null)
								directBuffer.close();
							if (writable != null)
								writable.close();
							if (debug != null)
								debug.close();
							break;
						}

					}

				} else {
					byte b[] = new byte[BUFSIZE];
					IntByReference ibw = new IntByReference();
					int count = 0;
					Memory buffer = new Memory(BUFSIZE);
					while (loop) {
						int cbBytesRead = readable.read(b);
						if (cbBytesRead == -1) {
							/*buffer.write(0, b, 0, BUFSIZE);
							Kernel32.INSTANCE.WriteFile(handle1,
									buffer, BUFSIZE, ibw, null);
							*/
							readable.close();
							if (debug != null)
								debug.close();
							break;
						}
						buffer.write(0, b, 0, cbBytesRead);
						boolean fSuccess = Kernel32.INSTANCE.WriteFile(handle1,
								buffer, cbBytesRead, ibw, null);
						int cbWritten = ibw.getValue();
						//PMS.minimal(name + "fSuccess" + fSuccess + " cbWritten: " + cbWritten);
						if (debug != null)
							debug.write(buffer.getByteArray(0, cbBytesRead));
						count++;
						if (!fSuccess || cbWritten == 0) {
							readable.close();
							if (debug != null)
								debug.close();
							break;
						}

					}
				}
			}
		} catch (IOException e) {
			PMS.info("Error: " + e.getMessage());
		}

		if (!in) {
			
			PMS.info("Disconnected pipe: " + name);
			Kernel32.INSTANCE.FlushFileBuffers(handle1);
			Kernel32.INSTANCE.DisconnectNamedPipe(handle1);
			//Kernel32.INSTANCE.CloseHandle(handle1);
			
		} else
			Kernel32.INSTANCE.CloseHandle(handle1);

	}

	public String getPipeName() {
		return name;
	}

	public OutputStream getWritable() {
		return writable;
	}

	public InputStream getReadable() {
		return readable;
	}
	
	public BufferedOutputFile getDirectBuffer() {
		return directBuffer;
	}

	@Override
	public InputStream getInputStream(long seek) throws IOException {
		return null;
	}

	@Override
	public ArrayList<String> getResults() {
		return null;
	}

	@Override
	public boolean isDestroyed() {
		return !isAlive();
	}

	@Override
	public void runInNewThread() {
		
	}

	@Override
	public boolean isReadyToStop() {
		return false;
	}

	@Override
	public void setReadyToStop(boolean nullable) {

	}

	@Override
	public void stopProcess() {
		interrupt();
	}

}
