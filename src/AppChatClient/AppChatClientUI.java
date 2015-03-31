/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package AppChatClient;

import java.io.*;
import java.net.*;
import javax.swing.*;
import java.awt.event.*;
import javax.xml.ws.WebServiceRef;
import uk.co.threeequals.webchat.Message;

public class AppChatClientUI {
    @WebServiceRef(wsdlLocation = "http://michaels-mbp:8080/WebApplication1/NewWebService?wsdl")    
    private static final int HOR_SIZE = 400;
    private static final int VER_SIZE = 200;
    private static final int VER_SIZE_TYPING = 75;
    private static final String TITLE = "Chat Client";
    
    private JFrame frame;
    private JTextArea myText;
    private static JTextArea otherText;
    private JScrollPane myTextScroll;
    private JScrollPane otherTextScroll;
    private static TextThread otherTextThread;
    private static ObjectOutputStream out;
    private uk.co.threeequals.webservice.NewWebService port;


    private void initComponents() {
    	frame = new JFrame(TITLE);
        
        /* - - - - - - Returned messages pane - - - - - - - - - */
        otherText = new JTextArea();
        otherTextScroll = new JScrollPane(otherText);
        otherText.setBackground(new java.awt.Color(200, 200, 200));
        otherTextScroll.setHorizontalScrollBarPolicy(
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        otherTextScroll.setVerticalScrollBarPolicy(
            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        otherTextScroll.setMaximumSize(
            new java.awt.Dimension(HOR_SIZE, VER_SIZE));
        otherTextScroll.setMinimumSize(
            new java.awt.Dimension(HOR_SIZE, VER_SIZE));
        otherTextScroll.setPreferredSize(new java.awt.Dimension(
		    HOR_SIZE, VER_SIZE));
        otherText.setEditable(false);
               
        frame.getContentPane().add(otherTextScroll,
            java.awt.BorderLayout.NORTH);
        
        
        /* - - - - - - Typed messages pane - - - - - - - - - */
        myText = new JTextArea();
        myTextScroll = new JScrollPane(myText);			
        myTextScroll.setHorizontalScrollBarPolicy(
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		myTextScroll.setVerticalScrollBarPolicy(
			JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		myTextScroll.setMaximumSize(
		    new java.awt.Dimension(HOR_SIZE, VER_SIZE));
		myTextScroll.setMinimumSize(new java.awt.Dimension(HOR_SIZE, VER_SIZE_TYPING));
		myTextScroll.setPreferredSize(new java.awt.Dimension(
		    HOR_SIZE, VER_SIZE_TYPING));

        myText.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyTyped(java.awt.event.KeyEvent evt) {
                textTyped(evt);
            }
        });
        frame.getContentPane().add(myTextScroll, java.awt.BorderLayout.SOUTH);
            
        //frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }
    
    public void showMessage(Message m){
        String apMesStr = "";
        
        if(m.to != null){
            apMesStr = apMesStr + "Private message from ";
        }
        if(m.from == null){
            m.from = "System";
        }
        
        apMesStr = apMesStr + m.from + ": ";
        apMesStr = apMesStr + m.body + "\n";
        otherText.append(apMesStr);
    }
    
    private void initConnection(String host){
        try {
            uk.co.threeequals.webservice.NewWebService_Service service = new uk.co.threeequals.webservice.NewWebService_Service();
            port = service.getNewWebServicePort();
        } catch (Exception ex) {
            showMessage(new Message("Failed to connect to server"));
        }
            
            //Socket mySocket = new Socket(host, 2048);
//            otherTextThread = new TextThread(this, mySocket);
//            OutputStream temp = mySocket.getOutputStream();
//            out = new ObjectOutputStream(temp);
//            otherTextThread.start();
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                  try {
                      out.writeObject(new Message("Exiting"));
                  }
                  catch (Exception ex) {
                      showMessage(new Message("Exit failed"));
                  }
                  System.exit(0);
            }
        });
    }

    private void textTyped(java.awt.event.KeyEvent evt) {
        char c = evt.getKeyChar();
        if (c == '\n'){
            JTextArea jta = (JTextArea) evt.getComponent();
            String typed = jta.getText();
            jta.setText(null);

            Message m;
            m = new Message(typed.replace("\n", ""), null, "Me");

            showMessage(m);

            uk.co.threeequals.webservice.Message message = new uk.co.threeequals.webservice.Message();
            message.setBody("My body");
            message.setFrom("Me");
            String results = port.sendMessage(message);
            System.out.println("Result = " + results);        
        }
    }
    
    
    public static void main(String[] args) {
    	if (args.length < 1) {
            System.out.println("Usage: AppChatClient host");
            return;
    	}
    	final String host = args[0];
    	javax.swing.SwingUtilities.invokeLater(new Runnable() {
            AppChatClientUI client = new AppChatClientUI();
            @Override
            public void run() {
                client.initComponents();
                client.initConnection(host);
            }
    	});
    	
    }
}

class TextThread extends Thread {

    ObjectInputStream in;
    AppChatClientUI client;
    Socket socket;

    TextThread(AppChatClientUI clientC, Socket mySocket) throws IOException{
        client = clientC;
        socket = mySocket;
    }
    
    @Override
    public void run() {
        try {    	
            in = new ObjectInputStream(socket.getInputStream());
            while (true) {
                Object message = in.readObject();
                if ((message == null) || (!(message instanceof Message))){
                    client.showMessage(new Message("Error reading from server"));
                    return;
                }
                Message m = (Message)message;
                client.showMessage(new Message(m.body));
            }
        }
        catch (IOException | ClassNotFoundException e) {
                client.showMessage(new Message("Error reading from server"));
        }
    }
}