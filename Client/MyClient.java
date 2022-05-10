import java.awt.*;
import java.awt.event.*;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import java.time.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.awt.event.KeyEvent;
public class MyClient {
    public static byte player_movement = 0;
    public static void main(String[] args) {
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
        Draw draw = new Draw(0,0);
        frame.add(draw);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JTextField textField = new JTextField();
        textField.addKeyListener(listener);
        contentPane.add(textField, BorderLayout.NORTH);
        frame.pack();
        frame.setSize(800,800);
        frame.setVisible(true);
        try {
            String serverIP = "89.212.118.128";
            //Handshake
            Socket handshake_s = new Socket(serverIP,25552);
            DataInputStream handshake_din = new DataInputStream(handshake_s.getInputStream());
            //Recieve initial config form server
            int size_x = handshake_din.readInt();
            int size_y = handshake_din.readInt();
            draw.setX(size_x);
            draw.setY(size_y);

            char[][] walls = new char[size_x][size_y];
            draw.setWalls(walls);
            String walls_string = handshake_din.readUTF();
            for (int x = 0; x < size_x; x++) {
                for (int y = 0; y < size_y; y++) {
                    walls[x][y] = walls_string.charAt(x*size_y + y);
                }
            }
            //Connect to socket
            Socket s = new Socket(serverIP,25553);
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
                    Render(size_x, size_y, walls, response, panel, frame, draw);
                }
            }
            //Graceful shutdown
            dout.close();
            s.close();
            if ((request & 0b00000001) == 0 && response.equals("0")) {
                draw.setText("Server closed", "", 1000);
                draw.repaint();
                wait(1000);
                frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
            } else {
                draw.setText("Disconnected", "", 1000);
                draw.repaint();
                wait(1000);
                frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
            }
        } catch (Exception e){
            //System.err.println(Arrays.toString(e.getStackTrace()));
            draw.setText("Disconnected; Error code:", e.toString(), 1000);
            draw.repaint();
        }
    }

    public static void wait(int milis) {
      try {
          TimeUnit.MILLISECONDS.sleep(milis);
      } catch (Exception e) {
          System.err.println(e);
      }
    }

    public static void Render(int size_x, int size_y, char[][] walls, String input, JPanel panel, JFrame frame, Draw draw) {
        char[][] data = new char[size_x][size_y];
        //Parse objects
        int playerID = -1;
        String[] objects = input.split("-", 0);
        for (int i = 0; i < objects.length; i++) {
            String[] params = objects[i].split(",", 0);
            if (params[0].equals("P") || params[0].equals("B")) {
                data[Integer.valueOf(params[1])][Integer.valueOf(params[2])] = params[3].charAt(0);
            }
            if (params[0].equals("ID")) {
                playerID = Integer.valueOf(params[1]);
            }
        }
        draw.setID(playerID);
        draw.setData(data);
        draw.repaint();
    }

}

class Draw extends JPanel {
    Clock clock = Clock.systemDefaultZone();
    Font font = new Font("Dialog", Font.PLAIN, 20);
    Font smallFont = new Font("Dialog", Font.PLAIN, 10);
    int size_q = 20;
    int size_r = 10;
    int size_x, size_y;
    long textTime = 0;
    String text = "";
    String subtext = "";
    int playerID;
    char[][] data;
    char[][] walls;
    //data inputs
    public Draw(int size_x, int size_y) {
        this.size_x = size_x;
        this.size_y = size_y;
    }
    void setText(String text, String subtext, int time) {this.text = text; this.subtext = subtext; this.textTime = clock.millis() + time;}
    void setX(int size_x) {this.size_x = size_x;}
    void setY(int size_y) {this.size_y = size_y;}
    void setData(char[][] data) {this.data = data;}
    void setWalls(char[][] walls) {this.walls = walls;}
    void setID(int playerID) {this.playerID = playerID;}


    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (data != null) {
            //draw all the things
            for (int x = 0; x < size_x; x++) {
                for (int y = 0; y < size_y; y++) {
                    if (walls[x][y] == 'w') {
                        g.setColor(Color.BLACK);
                        g.fillRect(y*size_q, x*size_q, size_q, size_q);
                    } else if (data[x][y] == 'v') {
                        g.setColor(Color.GRAY);
                        g.fillOval((y*size_q)  + (size_r/2), x*size_q, size_r, size_q);
                    } else if (data[x][y] == 'h') {
                        g.setColor(Color.GRAY);
                        g.fillOval(y*size_q, (x*size_q)  + (size_r/2), size_q, size_r);
                    } else if (data[x][y] != 0) {
                        if (data[x][y] == Character.forDigit(playerID,10)) {
                            g.setColor(Color.GREEN);
                        } else {
                              g.setColor(Color.RED);
                        }

                        g.fillOval(y*size_q, x*size_q, size_q, size_q);
                        g.setColor(Color.BLACK);
                        g.drawString("P"+data[x][y], y*size_q + size_q/6, x*size_q + size_q/4*3);
                    } else {
                        g.setColor(Color.WHITE);
                    }
                }
            }
        }
        if (textTime > clock.millis()) {
            g.setColor(Color.WHITE);
            g.fillRect(0,0,size_y*size_q,size_x*size_q);
            g.setColor(Color.BLACK);
            g.setFont(font);
            g.drawString(text, 100, 100);
            g.setFont(smallFont);
            g.drawString(subtext, 100, 200);
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(800, 800);
    }
}
