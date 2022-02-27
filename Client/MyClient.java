import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JFrame;
import javax.swing.JTextField;

import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.event.KeyEvent;
public class MyClient {
    public static byte player_movement = 0;
    public static void main() {
        //yoinked Key listener code
        JFrame frame = new JFrame("Key Listener");
        Container contentPane = frame.getContentPane();
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
        JTextField textField = new JTextField();
        textField.addKeyListener(listener);
        contentPane.add(textField, BorderLayout.NORTH);
        frame.pack();
        frame.setVisible(true);

        try {
            Socket handshake_s = new Socket("192.168.106.74",420);
            DataInputStream handshake_din = new DataInputStream(handshake_s.getInputStream());
            int portNumber = handshake_din.readInt();
            System.out.println(portNumber);
            Socket s = new Socket("192.168.106.74",portNumber);
            DataInputStream din=new DataInputStream(s.getInputStream());
            DataOutputStream dout=new DataOutputStream(s.getOutputStream());
            BufferedReader br=new BufferedReader(new InputStreamReader(System.in));
            handshake_din.close();
            handshake_s.close();
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
                    Render(response);
                }
            }
            //Graceful shutdown
            dout.close();
            s.close();
            System.out.println('\u000C');
            if (response.equals("0")) {
                System.out.println("Server closed");
            } else {
                System.out.println("Disconnected");
            }
        } catch (Exception e){
            System.err.println(e);
        }
    }
    public static void Render(String input) {
        System.out.println('\u000C');
        int size_x = 40;
        int size_y = 40;
        char[][] data = new char[size_x][size_y];
        for (int x = 0; x < size_x; x++) {
            for (int y = 0; y < size_y; y++) {
                data[x][y] = input.charAt(x * size_x + y);
                if (data[x][y] == 'e') {
                    System.out.print("  ");
                } else if (data[x][y] == 'w') {
                    System.out.print("WW");
                } else if (data[x][y] == 'v') {
                    System.out.print("||");
                } else if (data[x][y] == 'h') {
                    System.out.print("==");
                } else {
                    System.out.print("P" + data[x][y]);
                }
            }
            System.out.println("");
        }
    }

}
