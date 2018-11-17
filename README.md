# L2ImageClientServer
Computer Science degree - Level 2 Networks basic image client and server.  
  
This program consists of the following classes: 
* `ImageServer` - This runs the server on the local machine.
* `ClientHandler` - This contains the protocol to handle clients as they connect to the server.
* `ImageClient` - This connects the client to the server if it is running. By default, is hardcoded to look for server on the local machine.
* `ImageClientUI` - This runs a Java Swing based GUI for client-side usability.
   
## Instructions (for Windows) 
* Open two terminals: one in the `/client/` folder, another in the `/server/` folder.
* Compile in both using `javac *.java`
* To run the server, type: `java ImageServer`
* To run the client:
  * For Java Swing GUI, type: `java ImageClientUI`
  * For terminal-based UI, type: `java ImageClient`
  
