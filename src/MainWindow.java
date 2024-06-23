import org.apache.groovy.parser.antlr4.util.StringUtils;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MainWindow extends JFrame {
    private JButton installButton;
    private JPanel MainPanel;
    private JLabel ipLabel;
    private JTextField ipTextField;
    private JLabel appNameLabel;
    private JTextField appNameTextField;
    private JLabel pathLabel;
    private JTextField scriptPathextField;
    private JButton aboutButton;

    private static final String FILE_NAME = "cache.knc";

    public MainWindow() {
        setContentPane(MainPanel);
        setTitle("Install App Script on Kinoma Device");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(400, 300);
        setLocationRelativeTo(null);
        setVisible(true);
        aboutButton.setBorder(new RoundedBorder(1));
        VariableHolder holder;
        //load text fields from cache
        if (fileExists(FILE_NAME)) {
            holder = loadVariablesFromFile(FILE_NAME);
            if (holder != null) {
                ipTextField.setText(holder.getIp());
                appNameTextField.setText(holder.getAppName());
                scriptPathextField.setText(holder.getPath());
            }
        }

        installButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String ip = ipTextField.getText();
                String appName = appNameTextField.getText();
                String path = scriptPathextField.getText();
                if (StringUtils.isEmpty(ip)) {
                    JOptionPane.showMessageDialog(MainWindow.this, "IP Address is required!");
                } else if (StringUtils.isEmpty(appName)) {
                    JOptionPane.showMessageDialog(MainWindow.this, "App Name is required!");
                } else if (StringUtils.isEmpty(path)) {
                    JOptionPane.showMessageDialog(MainWindow.this, "Script path is required!");
                } else {
                    //save text filed to cache
                    VariableHolder holder = new VariableHolder("", "", "");
                    try {
                        FileObserver fileObserver = new FileObserver(FILE_NAME);
                        holder.addObserver(fileObserver);
                        holder.updateVariables(ip, appName, path);
                        fileObserver.close();
                    } catch (IOException ie) {
                        JOptionPane.showMessageDialog( MainWindow.this, ie);
                    }
                    try {
                        appName = appName.replace(" ", "-");
                        ip = ip.replace(" ", "");
                        var code = SendHttpRequest.sendRequest("OPTIONS", "http://" + ip +":10000/disconnect",
                                "application/javascript", null, null);
                        if (code == 200) {
                            code = SendHttpRequest.sendRequest("POST", "http://" + ip +":10000/disconnect",
                                    "application/javascript", null, null);
                            if (code == 200) {
                                code = SendHttpRequest.sendRequest("OPTIONS", "http://" + ip +":10000/upload?path=applications" +
                                        "/" + appName +"/application.xml&temporary=false", "application/javascript", null, null);
                                if (code == 200) {
                                    var body = "<?xml version=\"1.0\" encoding=\"utf-8\"?><application " +
                                            "xmlns=\"http://www.kinoma.com/kpr/application/1\" id=\"" + appName +"\" " +
                                            "program=\"src/main\" title=\"" + appName +"\"></application>";
                                    code = SendHttpRequest.sendRequest("PUT", "http://" + ip +":10000/upload?path=" +
                                            "applications/" + appName +"/application.xml&temporary=false", "application/javascript", null, body);
                                    if (code == 200) {
                                        File folder = new File(path);
                                        // Check if the given path is a directory
                                        if (!folder.exists() || !folder.isDirectory()) {
                                            JOptionPane.showMessageDialog(MainWindow.this, "Path: " + path + " is not a folder!");
                                        } else {
                                            try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(path), "*.js")) {
                                                for (Path filePath : directoryStream) {
                                                    code = SendHttpRequest.sendRequest("OPTIONS", "http://" + ip +":10000/" +
                                                                    "upload?path=applications/" + appName +"/src/" + filePath.getFileName() +"&temporary=false",
                                                            "application/javascript", null, null);
                                                    if (code == 200) {
                                                        if (filePath.getFileName().toString().endsWith(".js")) {
                                                            code = SendHttpRequest.sendRequest("PUT", "http://" + ip +":10000/" +
                                                                            "upload?path=applications/" + appName +"/src/" + filePath.getFileName() +"&temporary=false",
                                                                    "application/javascript", filePath.toString(), null);
                                                            if (code == 200) {
                                                                code = SendHttpRequest.sendRequest("OPTIONS", "http://" + ip +":10000/launch?" +
                                                                        "id=" + appName +"&file=main.js", "application/javascript", null, null);
                                                                if (code == 200) {
                                                                    String launchBody = "{\n" +
                                                                            "    \"debug\": false,\n" +
                                                                            "    \"breakOnExceptions\": false,\n" +
                                                                            "    \"temporary\": false,\n" +
                                                                            "    \"application\": {\n" +
                                                                            "        \"id\": \"" + appName +"\",\n" +
                                                                            "        \"app\": \"applications/" + appName +"\"\n" +
                                                                            "    }\n" +
                                                                            "}";
                                                                    SendHttpRequest.sendRequest("POST", "http://" + ip +":10000/launch?" +
                                                                            "id=" + appName +"&file=main.js", "application/javascript", null, launchBody);
                                                                }
                                                            }
                                                        } else {
                                                            JOptionPane.showMessageDialog(MainWindow.this, "Not a javascript file!");
                                                        }
                                                    }
                                                }
                                            } catch (IOException ex) {
                                                JOptionPane.showMessageDialog( MainWindow.this, ex);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog( MainWindow.this, ex);
                    }
                }
            }
        });
        aboutButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(MainWindow.this, "This program is created for installing Kinoma JavaScript applications using simple GUI.\n " +
                        "If Kinoma Create device is not upgraded to the latest software then it will have issues with installing apps via Kinoma Code,\n" +
                        " this allows installing apps on devices with old software, but works only for JavaScript, no images." +
                        "\n Author: Mykola Gutsaliuk\n Version 1.0.1\n 2024");
            }
        });
    }

    public static void main(String[] args) {
        new MainWindow();
    }

    // Method to check if a file exists
    private static boolean fileExists(String fileName) {
        File file = new File(fileName);
        return file.exists();
    }

    // Method to load variables from a file
    private static VariableHolder loadVariablesFromFile(String fileName) {
        VariableHolder holder = null;
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String ip = null;
            String appName = null;
            String path = null;

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("ip=")) {
                    ip = line.substring("ip=".length());
                } else if (line.startsWith("appName=")) {
                    appName = line.substring("appName=".length());
                } else if (line.startsWith("path=")) {
                    path = line.substring("path=".length());
                }
            }

            holder = new VariableHolder(ip, appName, path);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return holder;
    }
}
