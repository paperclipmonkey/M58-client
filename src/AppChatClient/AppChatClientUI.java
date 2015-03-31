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
import javax.swing.text.DefaultCaret;
import javax.xml.ws.WebServiceRef;
import uk.co.threeequals.webchat.Message;

public class AppChatClientUI {
    @WebServiceRef(wsdlLocation = "http://localhost:8080/ChatServer/ChatSvr?wsdl")    
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
    private ChatSvr port;
    private String uuid;


    private void initComponents() {
    	frame = new JFrame(TITLE);
        
        /* - - - - - - Returned messages pane - - - - - - - - - */
        otherText = new JTextArea();
        
        DefaultCaret caret = (DefaultCaret) otherText.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE); 
        
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
    
    public void showMessage(AppChatClient.Message m){
        Message cnvr;
        cnvr = new Message(
                m.body,
                m.to,
                m.from
        );
        showMessage(cnvr);
    }
    
    
    public void showMessage(Message m){
        String apMesStr = "";
        
        if(m.to != null && !"".equals(m.to)){
            apMesStr = apMesStr + "Private message from ";
        }
        
        if(m.from == null || "".equals(m.from)){
            m.from = "System";
        }
        
        apMesStr = apMesStr + m.from + ": ";
        apMesStr = apMesStr + m.body + "\n";
        otherText.append(apMesStr);
    }
    
    private void initConnection(String host){
        try {
            ChatSvr_Service service = new ChatSvr_Service();
            port = service.getChatSvrPort();
            
            //Get 'session' string from server
            uuid = port.join();
            System.out.println("Connected with UUID: " + uuid);
            
            otherTextThread = new TextThread(this, port, uuid);
//            OutputStream temp = mySocket.getOutputStream();
//            out = new ObjectOutputStream(temp);
            otherTextThread.start();
            
        } catch (Exception ex) {
            System.out.println(ex.toString());
            showMessage(new Message("Failed to connect to server"));
        }
            

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
            
            typed = typed.replace("\n", "");
            //Don't send empty strings
            if(typed != null && !typed.isEmpty()){
                Message m;
                m = new Message(typed, null, "Me");
                showMessage(m);
                port.sendMessage(typed, uuid);
            }
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
    ChatSvr chatSvr;
    String uuid;
    
    TextThread(AppChatClientUI clientC, ChatSvr myChatSvr, String myUuid) throws IOException{
        client = clientC;
        chatSvr = myChatSvr;
        uuid = myUuid;
    }
    
    @Override
    public void run() {
        try {
            while(true){
                AppChatClient.Message m = chatSvr.getMessage(uuid);
                if(m!=null){
                    client.showMessage(m);
                }
                sleep(100);
            }
        } catch (Exception e) {
                client.showMessage(new Message("Error reading from server"));
        }
    }
}

