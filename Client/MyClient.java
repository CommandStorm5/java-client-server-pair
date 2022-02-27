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
    public static int player_x = 0;
    public static int player_y = 0;
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
                if (key == "Right" && player_y < 9) {
                    player_y += 1;
                } else if (key == "Left" && player_y > 0) {
                    player_y -= 1;
                } else if (key == "Down" && player_x < 9) {
                    player_x += 1;
                } else if (key == "Up" && player_x > 0) {
                    player_x -= 1;
                } else if (key == "Escape") {
                    player_x = 100;
                    player_y = 100;
                }
                
            }
        };
        JTextField textField = new JTextField();
        textField.addKeyListener(listener);
        contentPane.add(textField, BorderLayout.NORTH);
        frame.pack();
        frame.setVisible(true);
        
        Scanner stdin = new Scanner(System.in);
        try {      
            Socket s = new Socket("localhost",505);
            DataInputStream din=new DataInputStream(s.getInputStream());  
            DataOutputStream dout=new DataOutputStream(s.getOutputStream());  
            BufferedReader br=new BufferedReader(new InputStreamReader(System.in));
            String request = "", response = "";
            while(!response.equals("stop")){
                request = player_x + " " + player_y;
                dout.writeUTF(request);
                dout.flush();
                response = din.readUTF();
                if (!response.equals("stop")) {
                    Render(response);
                }
            }
            dout.close();
            s.close();
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
