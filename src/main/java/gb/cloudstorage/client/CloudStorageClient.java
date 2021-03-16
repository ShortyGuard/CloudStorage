package gb.cloudstorage.client;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CloudStorageClient extends JFrame {
    private final Socket socket;
    private final DataInputStream in;
    private final DataOutputStream out;
    private JList<String> clientList;
    private JList<String> serverList;

    public CloudStorageClient() throws IOException {
        socket = new Socket("localhost", 1234);
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());
        runClient();
    }

    private void runClient() {
        JFrame frame = new JFrame("Cloud Storage");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 700);
        frame.setLocationRelativeTo(null);

        JMenuBar menuBar = new JMenuBar();
        frame.setJMenuBar(menuBar);

        JMenu fileMenu = new JMenu("File");
        menuBar.add(fileMenu);

        JMenu helpMenu = new JMenu("Help");
        menuBar.add(helpMenu);

        // разделим экран на серверную и локальную часть
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.5d);

        JPanel clientPanel = new JPanel();
        clientPanel.setLayout(new BorderLayout());
        clientPanel.add(BorderLayout.NORTH, new JLabel("Локальные файлы:"));
        splitPane.add(clientPanel);
        JPanel serverPanel = new JPanel();
        serverPanel.setLayout(new BorderLayout());
        serverPanel.add(BorderLayout.NORTH, new JLabel("Файлы в облаке"));
        splitPane.add(serverPanel);


        // создание списка файлов сервера
        clientList = new JList<>();
        // TODO: 16.03.2021 переделать на множественный выбор, если будет время
        clientList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        clientList.setModel(new DefaultListModel<>());
        clientPanel.add(BorderLayout.CENTER, new JScrollPane(clientList));

        clientList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                JList list = (JList)evt.getSource();
                if (evt.getClickCount() == 2) {
                    // Double-click detected
                    System.out.println(clientList.getSelectedValue());
                }
            }
        });

        clientList.addKeyListener(new KeyAdapter(){
            public void keyPressed(KeyEvent e){
                if (e.getKeyCode() == KeyEvent.VK_ENTER){
                    System.out.println(clientList.getSelectedValue());
                }
            }
        });

        // создание списка файлов сервера
        serverList = new JList<>();
        // TODO: 16.03.2021 переделать на множественный выбор, если будет время
        serverList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        serverList.setModel(new DefaultListModel<>());
        serverPanel.add(BorderLayout.CENTER, new JScrollPane(serverList));

        JTextArea ta = new JTextArea();
        // TODO: 02.03.2021
        // list file - JList
        JButton uploadButton = new JButton("Upload");

        frame.getContentPane().add(BorderLayout.NORTH, ta);
        frame.getContentPane().add(BorderLayout.CENTER, splitPane);
        frame.getContentPane().add(BorderLayout.SOUTH, uploadButton);

        fillLocalList((DefaultListModel<String>) clientList.getModel());
        fillServerList((DefaultListModel<String>) serverList.getModel());


        frame.setVisible(true);

        uploadButton.addActionListener(a -> {
            System.out.println(sendFile(ta.getText()));
        });
    }

    private void fillLocalList(DefaultListModel<String> clientListModel) {
        clientListModel.clear();
        // добавим root и parent директории
        clientListModel.addElement(".");
        clientListModel.addElement("..");

        File file = new File("client");
        File[] files = file.listFiles();
        for (File f : files) {
            clientListModel.addElement(f.getName());
        }
    }

    private void fillServerList(DefaultListModel<String> serverListModel) {
        java.util.List<String> list = downloadFileList();
        serverListModel.clear();
        // добавим root и parent директории
        serverListModel.addElement(".");
        serverListModel.addElement("..");

        for (String filename : list) {
            serverListModel.addElement(filename);
        }
    }

    private List<String> downloadFileList() {
        List<String> list = new ArrayList<>();
        try {
            StringBuilder sb = new StringBuilder();
            out.write("list-files".getBytes(StandardCharsets.UTF_8));
            while (true) {
                byte[] buffer = new byte[512];
                int size = in.read(buffer);
                sb.append(new String(buffer, 0, size));
                if (sb.toString().endsWith("end")) {
                    break;
                }
            }
            String fileString = sb.substring(0, sb.toString().length() - 4);
            list = Arrays.asList(fileString.split("\n"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return list;
    }

    private String sendFile(String filename) {
        try {
            File file = new File("client" + File.separator + filename);
            if (file.exists()) {
                out.writeUTF("upload");
                out.writeUTF(filename);
                long length = file.length();
                out.writeLong(length);
                FileInputStream fis = new FileInputStream(file);
                int read = 0;
                byte[] buffer = new byte[256];
                while ((read = fis.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                out.flush();
                String status = in.readUTF();
                return status;
            } else {
                return "File is not exists";
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "Something error";
    }

    public static void main(String[] args) throws IOException {
        new CloudStorageClient();
    }
}
