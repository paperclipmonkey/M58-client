/*
 * @author Michael Waterworth
 * Adapted from code written by Neal Harman
 */
package AppChatClient;

import java.io.*;
import javax.swing.*;
import java.awt.event.*;
import javax.swing.text.DefaultCaret;
import javax.xml.ws.WebServiceRef;

/**
 * UI for client connecting to AppChat Server
 * @author michaelwaterworth
 */
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
    private ChatSvr port;
    private String uuid;//Unique User ID - based on UUID v4


    /**
     * Initialize UI components
     */
    private void initComponents() {
    	frame = new JFrame(TITLE);
        
        /* - - - - - - Returned messages pane - - - - - - - - - */
        otherText = new JTextArea();
        
        // Set to continue to scroll as new messages come in
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
            java.awt.BorderLayout.NORTH);//Push to the top
        
        
        /* - - - - - - Message entry pane - - - - - - - - - */
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
        frame.pack();
        frame.setVisible(true);
    }
    
    /**
     * Show a message in the UI
     * @param m 
     */
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
    
    /**
     * Set up initial connection with endpoint
     */
    private void initConnection(){
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
            AppChatClient.Message k = new Message();
            k.setBody("Failed to connect to server");
            showMessage(k);
        }
            

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    Message k = new AppChatClient.Message();
                    k.setBody("Exiting");
                    showMessage(k);
                }
                catch (Exception ex) {
                    Message k = new AppChatClient.Message();
                    k.setBody("Exit failed");
                    showMessage(k);
                }
                System.exit(0);
            }
        });
    }

    /**
     * Event handler for keypress. Wait for /n character and then send string off to remote
     * @param evt Keyevent that triggered event
     */
    private void textTyped(java.awt.event.KeyEvent evt) {
        char c = evt.getKeyChar();
        if (c == '\n'){
            JTextArea jta = (JTextArea) evt.getComponent();
            String typed = jta.getText();
            jta.setText(null);
            
            typed = typed.replace("\n", "");
            //Don't send empty strings
            if(typed != null && !typed.isEmpty()){
                AppChatClient.Message m = new Message();
                m.setBody(typed);
                m.setFrom("Me");
                showMessage(m);
                port.sendMessage(typed, uuid);
            }
        }
    }
    
    /**
     * Main for application
     * @param args - Empty
     */
    public static void main(String[] args) {
    	javax.swing.SwingUtilities.invokeLater(new Runnable() {
            AppChatClientUI client = new AppChatClientUI();
            @Override
            public void run() {
                client.initComponents();
                client.initConnection();
            }
    	});
    }
}

/**
 * Thread to get new text from remote
 * @author michaelwaterworth
 */
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
    
    /**
     * Run method for thread
     */
    @Override
    public void run() {
        try {
            while(true){
                Message m = chatSvr.getMessage(uuid);
                if(m!=null){
                    client.showMessage(m);
                }
                sleep(100);
            }
        } catch (Exception e) {
                Message m = new Message();
                m.setBody("Error reading from server");
                m.setFrom("Me");
                client.showMessage(m);
        }
    }
}

