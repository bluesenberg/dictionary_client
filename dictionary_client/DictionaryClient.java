
import java.lang.System;
import java.io.*;
import java.net.*;
//
// This is an implementation of a simplified version of a command 
// line ftp client. The program takes no arguments.
//


public class DictionaryClient
{
    static final String DEFAULT_PORT = "2628";
    static final int MAX_LEN = 255;
    static final int PERMITTED_ARGUMENT_COUNT = 1;
    static Boolean debugOn = false;
    public static void main(String [] args)
    {
	byte cmdString[] = new byte[MAX_LEN];
	
	if (args.length == PERMITTED_ARGUMENT_COUNT) {
	    debugOn = args[0].equals("-d");
	    if (debugOn)
        {
		System.out.println("Debugging output enabled");
	    } else
        {
		System.out.println("997 Invalid command line option - Only -d is allowed");
        return;
        }
	} else if (args.length > PERMITTED_ARGUMENT_COUNT) {
	    System.out.println("996 Too many command line options - Only -d is allowed");
	    return;
	}
		
	try {
	    for (int len = 1; len > 0;) {
		System.out.print("DictionaryClient> ");
		len = System.in.read(cmdString);
            
		if (len <= 0)
        break;
        
        //Convert byte[] into a string
        String value = new String(cmdString, "UTF-8");
        //Replace \n with space
        value = value.replace("\n", " ");

        
        //split each word between space (" ") into arrays
        String[] split_string = value.split(" ");
            if(debugOn)
            {
                if(!value.contains("quit")){
                    System.out.println("--> "+value);
                }
            }
        if (split_string[0].matches("^#.*")){
        split_string[0] = "";
        }
            
            
        switch(split_string[0])
            {
                case "":
                break;
                //open SERVER PORT dict.org 2628
                case "open":
                    // check if port is not specified and if so, set input to default port.
                    if(split_string.length == 3){
                        split_string[2] = DEFAULT_PORT;
                    }
                    //length is 4 = compensate extra \n in the back of string
                    if((split_string.length != 4) && (split_string.length != 3))
                    {
                        System.out.println("901 Incorrect number of arguments.");
                        break;
                    }else if(!isInteger(split_string[2])){
                        System.out.println("902 Invalid argument.");
                    }else{
                    try (
                        //New Socket object - echoSocket: Connects to server and port.
                        Socket echoSocket = new Socket(split_string[1], Integer.parseInt(split_string[2]));
                        
                         ){
                       
                        //PrinterWriter - sends Data through socket to the Server.
                        PrintWriter out = new PrintWriter(echoSocket.getOutputStream(), true);
        
                        //BufferedReader - gets response from server
                        BufferedReader in = new BufferedReader(new InputStreamReader(echoSocket.getInputStream()));
                        //What the User Types in
                        BufferedReader stdIn = new BufferedReader( new InputStreamReader(System.in));
                        //Clear unused Connection Line of response
                        String response = in.readLine();
                        if(!response.contains("220")){
                            System.out.println("999 Processing error. Server does not contain Dict.");
                            break;
                        }
                        else if(debugOn)
                        {
                            System.out.println(response+"<-- ");
                        }
                        System.out.print("DictionaryClient> ");
                        String set_dict = "*";
                    
                        //String to store user Input
                        String userInput;
                        
                        mainloop:
                        while ((userInput = stdIn.readLine()) != null) {
                            if(debugOn)
                            {
                                if(!userInput.contains("quit"))
                                System.out.println("--> "+userInput);
                            }
                            //split each word between space (" ") into arrays
                            String[] split_input = userInput.split(" ");
                            if (split_input[0].matches("^#.*")){
                                split_input[0] = "";
                            }
                            switch(split_input[0]){

                                case "":
                                break;

                                case "close":
                                    echoSocket.close();
                                    break mainloop;
                                    
                                    
                                case "quit":
                                    System.exit(0);
                                    break;
                                    
                                    
                                case "dict":
                                    out.println("SHOW DB");
                                    print_output(echoSocket, in, true);
                                    break;
                                    
                                    
                                case "set":
                                    //Check arg length
                                    if(split_input.length != 2)
                                    {
                                        System.out.println("901 Incorrect number of arguments.");
                                        break;
                                    }
                                    //Check if dictionary exists.
                                    out.println("show info "+split_input[1]);
                                    String server_response = in.readLine();
                                    if(server_response.contains("550")){
                                        set_dict = "*";
                                        System.out.println("902 Invalid argument.");
                                        break;
                                    }
                                    else
                                    {
                                        //set dictionary
                                        set_dict = split_input[1];
                                        print_output(echoSocket, in, false);
                                    }
                                    break;
                                    
                                    
                                case "define":
                                    //Checks if # of argument is correct
                                    if(split_input.length != 2)
                                    {
                                        System.out.println("901 Incorrect number of arguments.");
                                        break;
                                    }
                                    //Check for definition - DEFINE dict word
                                    out.println("define "+set_dict+" "+split_input[1]);
                                    String define_response = in.readLine();
                                    //IF no matching word
                                    if(define_response.contains("552 no match"))
                                    {
                                        System.out.println("***No definition found***");
                                        out.println("MATCH "+set_dict+" regexp "+split_input[1]);
                                        
                                        //Try to find matching word with regexp
                                        String rematch_response = in.readLine();
                                        if(rematch_response.contains("552 no match"))
                                            {
                                            System.out.println("***No matches found***");
                                            break;
                                            }
                                        System.out.println(rematch_response);
                                    }
                                    print_output(echoSocket, in, true);
                                    break;
                                    
                                case "match":
                                    if(split_input.length != 2)
                                    {
                                        System.out.println("901 Incorrect number of arguments.");
                                        break;
                                    }
                                    //Check for Match - regexp
                                    out.println("match "+set_dict+" regexp "+split_input[1]);
                                    String match_response = in.readLine();
                                        if(match_response.contains("552 no match"))
                                        {
                                            System.out.println("*****No matching word(s)s found*****");
                                            break;
                                        }
                                    print_output(echoSocket, in, true);
                                    break;
                                
                                case "prefixmatch":
                                    if(split_input.length != 2)
                                    {
                                        System.out.println("901 Incorrect number of arguments.");
                                        break;
                                    }
                                    //Check for Match - regexp
                                    out.println("match "+set_dict+" prefix "+split_input[1]);
                                    String prefix_match_response = in.readLine();
                                    if(prefix_match_response.contains("552 no match"))
                                    {
                                        System.out.println("*****No matching word(s)s found*****");
                                        break;
                                    }
                                    print_output(echoSocket, in, true);
                                    break;
                                case "open":
                                    System.out.println("903 Supplied command not expected at this time. ");
                                    break;
                                    
                                default:
                                    System.out.println("900 Invalid command\n.");
                                    break;
                                }
                            System.out.print("DictionaryClient> ");
                            
                        }
                    }catch (UnknownHostException e) {
                        System.err.println("920 Control connection to "+split_string[1]+" on port "+split_string[2]+" failed to open.");
                    }catch (IOException e) {
                        System.err.println("925 Control connection I/O error, closing control connection.");
                    }
                    }
                    break;
               
                case "dict":
                    System.out.println("903 Supplied command not expected at this time. ");
                    break;
                case "set":
                    System.out.println("903 Supplied command not expected at this time. ");
                case "define":
                    System.out.println("903 Supplied command not expected at this time. ");
                    break;
                case "match":
                    System.out.println("903 Supplied command not expected at this time. ");
                    break;
                case "prefixmatch":
                    System.out.println("903 Supplied command not expected at this time. ");
                    break;
                case "close":
                    System.out.println("903 Supplied command not expected at this time. ");
                    break;
                case "quit":
                    System.exit(0);
                    break;
                default:
                    System.out.println("900 Invalid command.");
                    break;
            }
        
            
        // Start processing the command here.
        cmdString = new byte[MAX_LEN];
	    }
	} catch (IOException exception) {
	    System.err.println("925 Control connection I/O error, closing control connection.");
	}
}
    
    
    public static void print_output(Socket echoSocket, BufferedReader in, boolean print){
        //Check if Connection is active
        if(echoSocket.isConnected())
        {
            try{
                String s  = "";
                //Print out the response from the server
                while((s = in.readLine()) != null)
                {
                    if(s.contains("250 ok"))
                    {
                        if(debugOn)
                        {
                            System.out.println(s+"<-- ");
                        }
                        
                        break;
                    }
                    if(print)
                    {
                        System.out.println(s);
                    }
                    if(s.contains("151 "))
                    {
                        String[] dict_at = s.split("\"");
                        System.out.println("@"+dict_at[2]+"\""+dict_at[3]+"\"");
                    }
                }
            }
            catch(IOException exception) {
                System.err.println("998 Input error while reading commands, terminating.");
            }
        }
    }
    
    public static boolean isInteger(String s) {
        try {
            Integer.parseInt(s);
        } catch(NumberFormatException e) {
            return false;
        } catch(NullPointerException e) {
            return false;
        }
        // only got here if we didn't return false
        return true;
    }
    
    
}
