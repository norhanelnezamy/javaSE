/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package monitoringserver;

import java.awt.AWTException;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MouseInfo;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;

/**
 *
 * @author Nora
 */
public class ServerFrame extends javax.swing.JFrame {

    Socket socPic;
    Socket socWord;
    ObjectOutputStream oout;
    DataInputStream din;
    ServerSocket serverSocketPic;
    ServerSocket serverSocketWord;
    Vector<Socket> vSocket = new Vector<Socket>();
    Vector<Socket> vSocket1 = new Vector<Socket>();
    Thread thShareScreen;
    Thread accptClient;
    Thread sendPublic;
    Robot robot;
    BufferedImage screenCapture;
    byte[] bufferToBeSent;
    Vector<String> userName = new Vector<String>();

    /**
     * Creates new form ServerFrame
     */
    public ServerFrame() {
        initComponents();
        setLocationRelativeTo(null);
        initEventDriven();
    }

    public void initEventDriven() {

        jbtRunServer.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    serverSocketWord = new ServerSocket(33552);
                    serverSocketPic = new ServerSocket(33551);
                    jlblStatus.setText("Status: running..");
                    robot = new Robot();
                    accptClient = new Thread(new Runnable() {

                        @Override
                        public void run() {
                            while (true) {
                                try {
                                    socPic = serverSocketPic.accept();
                                    vSocket.add(socPic);
                                    socWord = serverSocketWord.accept();
                                    vSocket1.add(socWord);
                                    din = new DataInputStream(socWord.getInputStream());
                                    userName.add(din.readUTF());
                                    DefaultListModel resultList = new DefaultListModel();

                                    for (int i = 0; i < userName.size(); i++) {
                                        resultList.addElement(userName.get(i));
                                        jlsUserName.setModel(resultList);

                                    }
                                    //  System.out.println(vSocket);
                                } catch (IOException ex) {
                                    Logger.getLogger(ServerFrame.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            }

                        }
                    });
                    accptClient.start();
                    thShareScreen = new Thread(new Runnable() {

                        @Override
                        public void run() {

                            while (true) {

                                screenCapture = robot.createScreenCapture(new Rectangle(Toolkit.getDefaultToolkit().getScreenSize()));

                                try {
                                    Image cursor = ImageIO.read(new File("images/pointer.png"));
                                    int mouseX = MouseInfo.getPointerInfo().getLocation().x;
                                    int mouseY = MouseInfo.getPointerInfo().getLocation().y;

                                    // now we wanna draw the pointed in the mouse obtained coords
                                    Graphics2D graphics2D = screenCapture.createGraphics();
                                    graphics2D.drawImage(cursor, mouseX, mouseY, 30, 30, null);
                                    jlblStatus.setText("Status: capturing..");

                                    ImageIO.write(screenCapture, "GIF", new File("images/screen.jpg"));

                                    // get the image to send it
                                    FileInputStream fis = new FileInputStream("images/screen.jpg");
                                    bufferToBeSent = new byte[fis.available()];
                                    fis.read(bufferToBeSent);

                                    for (int i = 0; i < vSocket.size(); i++) {
                                        ObjectOutputStream oos = new ObjectOutputStream(vSocket.get(i).getOutputStream());
                                        oos.writeObject(bufferToBeSent);

                                        jlblStatus.setText("Status: sending screen..");
                                        oos.flush();
                                    }
                                    //      System.out.println("send pic");

                                } catch (IOException ex) {
                                    Logger.getLogger(ServerFrame.class.getName()).log(Level.SEVERE, null, ex);
                                }

                                // is NOT required.!
                                jlblShowImage.setIcon(new ImageIcon(resizeImage(screenCapture, jlblShowImage.getWidth(), jlblShowImage.getHeight())));
                                try {
                                    Thread.sleep(200);
                                } catch (InterruptedException ex) {
                                    Logger.getLogger(ServerFrame.class.getName()).log(Level.SEVERE, null, ex);
                                }

                            }
                        }
                    });
                    thShareScreen.start();

                } catch (IOException ex) {
                    Logger.getLogger(ServerFrame.class.getName()).log(Level.SEVERE, null, ex);
                } catch (AWTException ex) {
                    Logger.getLogger(ServerFrame.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
        jbtSendPublic.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                sendPublic = new Thread(new Runnable() {

                    @Override
                    public void run() {
                        try {

                            for (int i = 0; i < vSocket1.size(); i++) {
                                DataOutputStream dout = new DataOutputStream(vSocket1.get(i).getOutputStream());
                                dout.writeUTF(jtfChat.getText().toString());
                                System.out.println("Send Public");
                                dout.flush();

                            }
                            jtaChat.append("public ---> " + jtfChat.getText().toString() + "\n");
                            jtfChat.setText(null);
                        } catch (IOException ex) {
                            Logger.getLogger(ServerFrame.class.getName()).log(Level.SEVERE, null, ex);
                        }

                    }
                });

                sendPublic.start();

            }
        });
        jbtSendPrivate.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (jlsUserName.getSelectedValue() != null) {
                    int privSend = jlsUserName.getSelectedIndex();

                    try {
                        DataOutputStream dout = new DataOutputStream(vSocket1.get(privSend).getOutputStream());
                        dout.writeUTF(jtfChat.getText().toString());
                        System.out.println("Send private");
                        dout.flush();

                        jtaChat.append("private ---> " + jtfChat.getText().toString() + "\n");
                        jtfChat.setText(null);

                    } catch (IOException ex) {
                        Logger.getLogger(ServerFrame.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } else {
                    JOptionPane.showMessageDialog(null, "please,Select user");
                }
            }
        });

        jbtExit.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });
    }

    private java.awt.Image resizeImage(java.awt.Image img, int w, int h) {
        BufferedImage resizedImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = resizedImage.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(img, 0, 0, w, h, null);
        g2.dispose();

        return resizedImage;

    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jButton1 = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        jbtRunServer = new javax.swing.JButton();
        jlblStatus = new javax.swing.JLabel();
        jbtExit = new javax.swing.JButton();
        jbtRecord = new javax.swing.JButton();
        jlblShowImage = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jtaChat = new javax.swing.JTextArea();
        jbtSendPublic = new javax.swing.JButton();
        jtfChat = new javax.swing.JTextField();
        jbtSendPrivate = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        jlsUserName = new javax.swing.JList();

        jButton1.setText("jButton1");

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanel1.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        jbtRunServer.setText("Run");

        jlblStatus.setText("status:");

        jbtExit.setText("Exit");

        jbtRecord.setText("Record:");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jbtRunServer, javax.swing.GroupLayout.PREFERRED_SIZE, 179, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jlblStatus))
                .addGap(42, 42, 42)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jbtExit, javax.swing.GroupLayout.PREFERRED_SIZE, 125, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jbtRecord, javax.swing.GroupLayout.PREFERRED_SIZE, 136, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jbtRunServer)
                    .addComponent(jbtExit))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jlblStatus)
                    .addComponent(jbtRecord))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jtaChat.setColumns(20);
        jtaChat.setRows(5);
        jScrollPane1.setViewportView(jtaChat);

        jbtSendPublic.setText("Send public");

        jtfChat.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jtfChatActionPerformed(evt);
            }
        });

        jbtSendPrivate.setText("Send private");

        jScrollPane2.setViewportView(jlsUserName);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jlblShowImage, javax.swing.GroupLayout.PREFERRED_SIZE, 744, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jbtSendPublic, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jbtSendPrivate, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jtfChat)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 260, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 128, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jScrollPane2)
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 276, Short.MAX_VALUE))
                        .addGap(18, 18, 18)
                        .addComponent(jtfChat, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jbtSendPublic)
                            .addComponent(jbtSendPrivate))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(34, 34, 34)
                        .addComponent(jlblShowImage, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jtfChatActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jtfChatActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jtfChatActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(ServerFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(ServerFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(ServerFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(ServerFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new ServerFrame().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JButton jbtExit;
    private javax.swing.JButton jbtRecord;
    private javax.swing.JButton jbtRunServer;
    private javax.swing.JButton jbtSendPrivate;
    private javax.swing.JButton jbtSendPublic;
    private javax.swing.JLabel jlblShowImage;
    private javax.swing.JLabel jlblStatus;
    private javax.swing.JList jlsUserName;
    private javax.swing.JTextArea jtaChat;
    private javax.swing.JTextField jtfChat;
    // End of variables declaration//GEN-END:variables

}
