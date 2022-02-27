import java.time.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
public class Server {
    public static Clock clock = Clock.systemDefaultZone();
    public static long then = clock.millis();
    public static int[][] players = new int[1][6]; //x,y,proj_d,proj_x,proj_y,last_d
    public static int size_x = 40;
    public static int size_y = 40;
    public static char[][] walls = generateWalls();
    public static String console_data = "";
    public static void main() {
        for (int i = 0; i < players.length; i++) {
            players[i][0] = 1;
            players[i][1] = 1;
        }
        //System.out.println("Starting Server");
        ServerSocket[] ss = new ServerSocket[players.length];
        Socket[] s = new Socket[players.length];
        DataInputStream[] din = new DataInputStream[players.length];
        DataOutputStream[] dout=new DataOutputStream[players.length];
        BufferedReader br=new BufferedReader(new InputStreamReader(System.in));

        for (int i = 0; i < players.length; i++) {
            try {
                ss[i] = generateServerSocket(440+i);
                s[i] = establishConnection(ss[i]);
                din[i] = new DataInputStream(s[i].getInputStream());
                dout[i] = new DataOutputStream(s[i].getOutputStream());
            } catch (Exception e){
                    System.err.println(e);
            }
        }
        byte[] responses = new byte[players.length];
        String render="";
        while (console_data != "stop") {
            for (int i = 0; i < players.length; i++) {
                try {
                    responses[i]=(byte)din[i].read();
                    if ((responses[i] & (byte)0b00000001) == 1) {
                        dout[i].close();
                        s[i].close();
                        ss[i].close();
                    }
                } catch (Exception e){
                    System.err.println(e);
                }
            }
            render = gameTick(responses);
            for (int i = 0; i < players.length; i++) {
                try {
                    dout[i].writeUTF(render);
                    dout[i].flush();
                } catch (Exception e){
                    System.err.println(e);
                }
            }
        }
        for (int i = 0; i < players.length; i++) {
            try {
                dout[i].close();
                s[i].close();
                ss[i].close();
            } catch (Exception e){
                System.err.println(e);
            }
        }
    }
    public static ServerSocket generateServerSocket(int port) {
        try {
            ServerSocket ss=new ServerSocket(port);
            return ss;
        } catch (Exception e){
            System.err.println(e);
            return null;
        }
    }  
    public static String Game(int[] coords) {
        int size_x = 10;
        int size_y = 10;
        int[][] display = new int[size_x][size_y];
        for (int i = 0; i < coords.length; i+=2) {
            display[coords[i]][coords[i+1]] = 2;
        }
        String output = "";
        for (int x = 0; x < size_x; x++) {
            for (int y = 0; y < size_y; y++) {
                output += display[x][y];
            }
        }
        return output;
    }
    public static int[] ParseResponse(String response) {
        String[] coords_bad = response.split(" ");
        int[] coords = new int[coords_bad.length];
        for (int i = 0; i < coords.length; i++){
            coords[i] = Integer.parseInt(coords_bad[i]);
        }
        return coords;
    }
}  