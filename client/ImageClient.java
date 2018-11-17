import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.image.*;
import javax.imageio.*;
import java.net.Socket;

/**
* A client which connects to ImageServer.java. Downloads and uploads
* images from/to the server.
* Requires arguments: host, port
*
* @author Melissa Liau
*/

public class ImageClient
{
	private Scanner socketIn = null;
	private PrintWriter socketOut = null;
	private Scanner keyboardIn = null;
	private String messageIn = null;
	public ArrayList<String> serverImageList = null;
	public ArrayList<String> imageList = null;
	public File[] fileList = null; 
	private String dirPath = null;
	private String serverHost = null;

    /**
    * Initialises ImageClient class. Opens a new socket and retrieves
    * information from the starter information from the server once
    * connected. Calls readImagesFromFile() to update image list.
    **/
	public ImageClient(String host, int port)
	{
		serverHost = host;
		dirPath = System.getProperty("user.dir");

		try
		{	
	       	Socket socket = new Socket(host, port);
			socketIn = new Scanner(socket.getInputStream());
			socketOut = new PrintWriter(socket.getOutputStream(), true);
			keyboardIn = new Scanner(System.in);
			String intro = socketIn.nextLine();
			while (true)
			{	System.out.println(intro);
				intro = socketIn.nextLine();
				if (intro.equals("stop::"))
				{	break;
				}
			}
			requestImageList();
			readImagesFromFile();
		}
		catch (IOException e)
		{	
			e.printStackTrace();
		}
			
	}

    /**
    * Requests the list of images from the server and prints to 
    * terminal the list of images as well as assigns variable
    * serverImageList to list.
    **/
	public void requestImageList()
	{
		socketOut.println("requestlist::");
		int length = Integer.parseInt(socketIn.nextLine());
		this.serverImageList = new ArrayList<String>(length);
		this.messageIn = socketIn.nextLine();
		System.out.println("  Available images to download:");
		while (!this.messageIn.equals("stop::"))
			{
				System.out.println("    - " + this.messageIn);
				this.serverImageList.add(this.messageIn);
				this.messageIn = socketIn.nextLine();
			}
		System.out.println("");
	}

    /**
    * Reads the list of images in the /images folder of the client
    * and assigns variable imageList to list, before printing out
    * list of images to terminal.
    **/
	private void readImagesFromFile()
	{
		File dir = new File(this.dirPath + "/images");
		this.fileList = dir.listFiles();
		this.imageList = new ArrayList<String>(this.fileList.length);
		System.out.println("Finding files in /images directory...");
		boolean found = false;
		for (File file : this.fileList)	
		{	if (file.isFile())
			{	System.out.println(" Found: " + file.getName());
				found = true;
				this.imageList.add(file.getName());
			}
		}
		if (!found)
		{
			System.out.println("No files found!");
		}
		System.out.println("");
	}

    /**
    * Reads input from terminal, which should be one of the following:
    * requesting::[filename], sending::[filename], requestlist::
    * Calls appropriate method if input is valid. Does not send
    * query to server if e.g. file does not exist.
    **/
	private void queryHandler()
	{
		String messageOut;
		while ((messageOut = keyboardIn.nextLine()) != null)
		{	
			String fileName = "";
			boolean error = true;
			if (messageOut.contains("::"))
			{
				int separatorLocation = messageOut.lastIndexOf("::");
				if (!messageOut.endsWith("::"))
				{
					fileName = messageOut.substring(separatorLocation+2);
				}
			
				if (messageOut.equals("requestlist::"))
				{
					error = false;
					System.out.println("[CLIENT OUTPUT] " + messageOut + "\n");
					System.out.println("  Available images to download:");
					requestImageList();
				}
				
				if (messageOut.startsWith("requesting::") && serverImageList.contains(fileName))
				{
					error = false;
					System.out.println("[CLIENT OUTPUT] " + messageOut);
					downloadFromServer(fileName);
				}
				
				if (messageOut.startsWith("sending::") && imageList.contains(fileName))
				{
					error = false;
					System.out.println("Not implemented yet");
					sendToServer(fileName);
				}
			}

			if (error)
			{
				System.out.println("\n  Invalid requests. Please enter one of the following:");
				System.out.println("   * requesting::[filename] ");
				System.out.println("   * sending::[filename]");
				System.out.println("   * requestlist::");
			}
		}
	}

    /**
    * Sends file of name fileName to server. Prints out what is 
    * being done to terminal during process.
    **/
	public void sendToServer(String fileName)
	{
		socketOut.println("sending::" + fileName);
		try
		{
			int dot = fileName.lastIndexOf(".");
			String fileType = "";
			if (dot > 0)
			{	fileType = fileName.substring(dot+1);
			}
			System.out.println("Detected file type: " + fileType);
			System.out.println("Reading file: /images/" + fileName);
			BufferedImage img = ImageIO.read(new File(this.dirPath + "/images/" + fileName));
			System.out.println("Converting image to byte array output stream.");
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write(img, fileType, baos);
			baos.flush();
			byte[] bytes = baos.toByteArray();
			baos.close();
			System.out.println("Byte array of length " + bytes.length + " created.");

			System.out.println("Opening new socket to connect to server.");
			Socket soc = new Socket(this.serverHost, 4000);
			System.out.println("Opening streams with server.");
			OutputStream outStream = soc.getOutputStream();
			DataOutputStream dos = new DataOutputStream(outStream);

			System.out.println("Writing to server stream.");
			dos.writeInt(bytes.length);
			dos.write(bytes, 0, bytes.length);
			System.out.println("Closing streams/socket.");
			dos.close();
			outStream.close();
			soc.close();
		}

		catch (Exception e)
		{	System.out.println("Exception: " + e.getMessage());
			e.printStackTrace();
		}
	}

    /**
    * Downloads file of name fileName from server. Prints out what is 
    * being done to terminal during process.
    **/
	public void downloadFromServer(String fileName)
	{
		socketOut.println("requesting::" + fileName);
		try
		{
			System.out.println("Opening new socket for server to connect...");
			ServerSocket server = new ServerSocket(4000);
			Socket socket = server.accept();
			System.out.println(" Accepted connection with server.\nRetrieving input stream...");
			InputStream inStream = socket.getInputStream();
			DataInputStream dis = new DataInputStream(inStream);

			int dataLength = dis.readInt();
			byte[] data = new byte[dataLength];
			dis.readFully(data);
			dis.close();
			inStream.close();
			System.out.println(" Finished receiving input stream.\nConverting to file...");

			InputStream bais = new ByteArrayInputStream(data);
			String filePath = this.dirPath + "/images/" + fileName;
			OutputStream toFile = new FileOutputStream(filePath);
			byte[] buffer = new byte[1024];
			int bytesRead = 0;
			while ((bytesRead = bais.read(buffer)) != -1)
			{	System.out.println(" Bytes read of length: " + bytesRead);
				toFile.write(buffer, 0, bytesRead);
			}
			bais.close();
			toFile.flush();
			toFile.close();
			server.close();
			System.out.println(" ...Finished!\n");

			readImagesFromFile();
		}

		catch (Exception e)
		{	System.out.println("Exception: " + e.getMessage());
			e.printStackTrace();
		}
	}

    /**
    * If run from terminal, automatically connects to localhost
    * for bug testing, then runs queryHandler to read terminal
    * inputs.
    **/
	public static void main(String[] args)
	{
		String host = "127.0.0.1"; 				 //args[0]
		int port = 5000;
		ImageClient ic = new ImageClient(host, port);
		ic.queryHandler();
	}

}