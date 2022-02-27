import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import java.awt.FlowLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.event.KeyEvent;
public class MyClient {
    public static byte player_movement = 0;
    public static void main() {
        //yoinked Key listener code
        KeyListener listener = new KeyListener() {
            @Override
            public void keyPressed(KeyEvent event) {
                printEventInfo("Key Pressed", event);
            }
            @Override
            public void keyReleased(KeyEvent event) {
                //Nothing lol
            }
            @Override
            public void keyTyped(KeyEvent event) {
                //Still nothing lol
            }
            //Keylogger
            private void printEventInfo(String str, KeyEvent e) {
                int code = e.getKeyCode();
                String key = KeyEvent.getKeyText(code);
                if (key == "Up" || key == "w") {
                    player_movement |= 0b10000000;
                } else if (key == "Down" || key == "s") {
                    player_movement |= 0b01000000;
                } else if (key == "Left" || key == "a") {
                    player_movement |= 0b00100000;
                } else if (key == "Right" || key == "d") {
                    player_movement |= 0b00010000;
                } else if (key == "Space") {
                    player_movement |= 0b00001000;
                } else if (key == "Escape") {
                    player_movement |= 0b00000001;
                }

            }
        };
        //Create keylogger
        JFrame frame = new JFrame("Key Listener");
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout());
        Container contentPane = frame.getContentPane();
        JLabel label = new JLabel("<html>Connecting...</html>");
        panel.add(label);
        frame.add(panel);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JTextField textField = new JTextField();
        textField.addKeyListener(listener);
        contentPane.add(textField, BorderLayout.NORTH);
        frame.pack();
        frame.setSize(800,800);
        frame.setVisible(true);
        try {
            //Handshake
            Socket handshake_s = new Socket("192.168.106.74",420);
            DataInputStream handshake_din = new DataInputStream(handshake_s.getInputStream());
            //Recieve initial config form server
            int size_x = handshake_din.readInt();
            int size_y = handshake_din.readInt();

            char[][] walls = new char[size_x][size_y];
            String walls_string = handshake_din.readUTF();
            for (int x = 0; x < size_x; x++) {
                for (int y = 0; y < size_y; y++) {
                    walls[x][y] = walls_string.charAt(x*size_y + y);
                }
            }
            //Connect to socket
            Socket s = new Socket("192.168.106.74",421);
            DataInputStream din=new DataInputStream(s.getInputStream());
            DataOutputStream dout=new DataOutputStream(s.getOutputStream());
            BufferedReader br=new BufferedReader(new InputStreamReader(System.in));
            //Murder handshake
            handshake_din.close();
            handshake_s.close();
            //Main loop
            byte request = 0;
            String response = "";
            while(!response.equals("0")){
                request = player_movement;
                player_movement = 0;
                dout.write(request);
                dout.flush();
                //System.out.println("Packet Sent");
                response = din.readUTF();
                if (!response.equals("0")) {
                    Render(response, frame, panel, label);
                }
            }
            //Graceful shutdown
            dout.close();
            s.close();
            if (response.equals("0")) {
                label.setText("<html><pre>Server closed</pre></html>");
                panel.invalidate();
                panel.validate();
            } else {
                label.setText("<html><pre>Disconnected</pre></html>");
                panel.invalidate();
                panel.validate();
            }
        } catch (Exception e){
            label.setText("<html><pre>Disconnected\n\nError code: " + e + "</pre></html>");
            panel.invalidate();
            panel.validate();
        }
    }
    public static void Render(String input, JFrame frame, JPanel panel, JLabel label) {
        //Hardcoded level size
        int size_x = 40;
        int size_y = 40;
        char[][] data = new char[size_x][size_y];
        //Render world
        String render = "";
        for (int x = 0; x < size_x; x++) {
            for (int y = 0; y < size_y; y++) {
                data[x][y] = input.charAt(x * size_x + y);
                if (data[x][y] == 'e') {
                    render += "  ";
                } else if (data[x][y] == 'w') {
                    render += "WW";
                } else if (data[x][y] == 'v') {
                    render += "||";
                } else if (data[x][y] == 'h') {
                    render += "==";
                } else {
                    render += ("P" + data[x][y]);
                }
            }
            render += "<br>";
        }
        label.setText("<html><pre>" + render + "</pre></html>");
        //panel.remove(label);
        //panel.add(label);
        panel.invalidate();
        panel.validate();
        //frame.invalidate();
        //frame.validate();
    }

}
