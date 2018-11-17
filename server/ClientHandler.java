import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.awt.image.*;
import javax.imageio.*;
import java.net.Socket;
import java.text.*;

/**
* Client handler - a protocol to handle queries between the client
* and the server. Designed to work with ImageClient.java.
* Requires: /ImageServer.java
*           /images/[various images]
*
* @author Melissa Liau
*/

public class ClientHandler implements Runnable
{
	private Scanner reader = null;
	private PrintWriter writer = null;
	private PrintWriter logWriter = null;
	public Boolean nseeStatus = false;
	private String message = null;
	public ArrayList<String> imageList = null;
	public File[] fileList = null; 
	private String dirPath = null;
	private InetAddress clientAddress = null;
	private Calendar calendar = null;

    /**
    * Initialises the ClientHandler class.
    **/
	public ClientHandler(Socket client)
	{
		// Gets the client's inet address
		clientAddress = client.getInetAddress();
		// Gets the directory the server is run from in order to read
		// image files.
		dirPath = System.getProperty("user.dir");
		try
		{	
			// Creates input and output streams with client.
			reader = new Scanner(client.getInputStream());
			writer = new PrintWriter(client.getOutputStream(), true);	
			Send("\n  Please enter one of the following:");
			Send("   * requesting::[filename] ");
			Send("   * sending::[filename]");
			Send("   * requestlist:: \n");
			Send("stop::");
			System.out.println("");
		}
		catch (IOException e)
		{	e.printStackTrace();
		}
		writeToLog("client connected", "");
	}

    /**
    * Sends message through output stream to clilent and prints
    * message to terminal.
    **/
	private void Send(String message)
	{
		this.writer.println(message);
		System.out.println("[SERVER OUTPUT] " + message);
	}

	/**
	* Appends to serverlog.txt in the format:
	* date:time:ipAddress:request
	**/
	private void writeToLog(String functionRequested, String filename)
	{
		File log = new File("serverlog.txt");

		// If serverlog.txt does not exist, create empty txt file.
		if (!log.exists())
		{
			try
			{	PrintWriter writer = new PrintWriter("serverlog.txt", "UTF-8");
				writer.write("");
				writer.close();
			}
			catch (Exception e)
			{	e.printStackTrace();
			}
		}
		// Create a new Date object and read date and time from it as strings.
		Date currentDateTime = new Date();
		DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
		DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
		String request = functionRequested + " " + filename;
		String date = (String)dateFormat.format(currentDateTime);
		String time = (String)timeFormat.format(currentDateTime);
		String toLog = date + " : " + time + " : " + this.clientAddress + " : " + request + "\n";
		// Write to serverlog.txt
		try
		{	Files.write((Paths.get("serverlog.txt")), toLog.getBytes(), StandardOpenOption.APPEND);
			System.out.println("Logged event.")
		}
		catch (IOException e)
		{	e.printStackTrace();
		}
	}

    /**
    * Reads input from client, which should be one of the following:
    * requesting::[filename], sending::[filename], requestlist::
    * Calls appropriate method if input is valid.
    **/
	private void handleQueries()
	{	
		String fileName = "";
		// Retrieve filename from terminal query.
		int separatorLocation = this.message.lastIndexOf("::");
		if ((separatorLocation > 0) && this.message.contains("::"))
		{	if (!this.message.endsWith("::"))
			{
				fileName = this.message.substring(separatorLocation+2);
			}
		}
		// Reacts accordingly.
		if (this.message.startsWith("requesting::"))
		{	UploadToUser(fileName);
		}
		else if (this.message.startsWith("sending::"))
		{	DownloadFromUser(fileName);
		}
		else if (this.message.startsWith("requestlist::"))
		{	SendImageList();
		}
		else
		{	Send("Invalid query: " + this.message);
		}
	}

    /**
    * Updates own file list and prints out filenames to terminal.
    **/
	private void ReadImagesFromFile()
	{
		System.out.println("Updating own list...");
		File dir = new File(this.dirPath + "/images");
		this.fileList = dir.listFiles();
		this.imageList = new ArrayList<String>(this.fileList.length);
		for (File file : this.fileList)
		{	if (file.isFile())
			{	System.out.println(" adding " + file.getName());
				// adds each filename to imageList
				this.imageList.add(file.getName());
			}
		}
		System.out.println("");
	}

    /**
    * Sends file of name fileName to client. Prints out what is 
    * being done to terminal during process.
    **/
	public void UploadToUser(String fileName)
	{
		try
		{
			int dotLocation = fileName.lastIndexOf(".");
			String fileType = "";
			if (dotLocation > 0)
			{	fileType = fileName.substring(dotLocation+1);
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

			System.out.println("Opening new socket to connect to user.");
			Socket soc = new Socket(this.clientAddress, 4000);
			System.out.println("Opening streams with user.");
			OutputStream outStream = soc.getOutputStream();
			DataOutputStream dos = new DataOutputStream(outStream);

			System.out.println("Writing to user stream.");
			dos.writeInt(bytes.length);
			dos.write(bytes, 0, bytes.length);
			System.out.println("Closing streams/socket.");
			dos.close();
			outStream.close();
			soc.close();
			writeToLog("sent image", fileName);
		}

		catch (Exception e)
		{	e.printStackTrace();
		}
	}

    /**
    * Downloads file of name fileName from client. Prints out what is 
    * being done to terminal during process.
    **/
	public void DownloadFromUser(String fileName)
	{
		try
		{
			System.out.println("Opening new socket for client to connect...");
			ServerSocket server = new ServerSocket(4000);
			Socket socket = server.accept();
			System.out.println(" Accepted connection with client.\nRetrieving input stream...");
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
			int bytesRead;
			while ((bytesRead = bais.read(buffer)) != -1)
			{	System.out.println(" Bytes read of length: " + bytesRead);
				toFile.write(buffer, 0, bytesRead);
			}
			bais.close();
			toFile.flush();
			toFile.close();
			server.close();
			System.out.println(" ...Finished!\n");
			writeToLog("received image", fileName);

			ReadImagesFromFile();
		}

		catch (Exception e)
		{	e.printStackTrace();
		}
	}

    /**
    * Sends list of images on server to client.
    **/
	public void SendImageList()
	{
		ReadImagesFromFile();
		Send(Integer.toString(imageList.size()));
		for (int i = 0; i < this.imageList.size(); i++)
		{	Send(imageList.get(i));
		}
		Send("stop::");
	}

    /**
    * Runs the client handler and listens for messages from client. If
    * message stream is broken, stops reading, breaking the thread.
    **/
	public void run()
	{
		while (true)
		{	try
			{	
				this.message = reader.nextLine();
				while (this.message != null)
				{	
					System.out.println("Server read: "+ this.message + "\n");
					handleQueries();
					System.out.println("Listening for message...");
					this.message = reader.nextLine();
				}
			}
			catch (NoSuchElementException nsee)
			{	this.nseeStatus = true;
				reader.close();
				writer.close();
				System.out.println("Client disconnected.");
				writeToLog("client disconnected", "");
				break;
			}
		}
	}

}