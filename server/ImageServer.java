import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;

/**
* Creates a server for ImageClient.java to connect to. Works with 
* ClientHandler.java to handle all server to/from client functions. 
* Has a connection limit of 10 clients.
* Requires: /ClientHandler.java
*
* @author Melissa Liau
*/

public class ImageServer
{
	private ServerSocket serverSocket = null;
	private Integer threadLimit = null;

    /**
    * Passes argument upperThreadLimit as a global variable to be used
    * in ServerConnectLoop.
    * Opens a new socket the start the server on and if successful,
    * starts a thread to run the server on.
    **/
	public ImageServer(int port, int upperThreadLimit)
	{
		threadLimit = upperThreadLimit;
		try
		{	serverSocket = new ServerSocket(port);
		}
		catch (IOException e)
		{	e.printStackTrace();
		}

		Thread sclThread = new Thread(new ServerConnectLoop());
		sclThread.start();
	}

    /**
    * Creates a thread pool with the thread limit as the value of
    * threadLimit.
    * Listens for connections. Once a connection is accepted, runs 
    * connection on a new thread and prints out to terminal that a 
    * client has connected.
    **/
	public class ServerConnectLoop implements Runnable
	{	
		ThreadPoolExecutor pool = (ThreadPoolExecutor) Executors.newFixedThreadPool(threadLimit);

		public void run()
		{	
			try
			{	while (true)
				{	Socket clientSocket = serverSocket.accept();
					ClientHandler c = new ClientHandler(clientSocket);
					pool.execute(c);
					System.out.println("[SERVER] Client connected. " + pool.getActiveCount() + " client(s) connected.");
				}
			}
			catch (IOException e)
			{	e.printStackTrace();
			}
		}
	}

    /**
    * If run from terminal, automatically opens server on port 5000,
    * with thread limit of 10.
    **/
	public static void main(String[] args)
	{	ImageServer s = new ImageServer(5000, 5);
	}
}