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
    public static int[] parseResponse(String response) {
        String[] inputs_bad = response.split(" ");
        int[] inputs = new int[inputs_bad.length];
        for (int i = 0; i < inputs.length; i++){
            inputs[i] = Integer.parseInt(inputs_bad[i]);
        }
        return inputs;
    }
    public static void runMovement(int[] inputs) {
        for (int i = 0; i < inputs.length; i++) {
            if ((inputs[i] & 0b10000000) != 0 && players[i][0] > 0 && walls[players[i][0]-1][players[i][1]] != 'w') {
                players[i][0] -= 1;
                players[i][5] = 1;
            } else if ((inputs[i] & 0b01000000) != 0 && players[i][0] < size_x-1 && walls[players[i][0]+1][players[i][1]] != 'w') {
                players[i][0] += 1;
                players[i][5] = 3;
            } else if ((inputs[i] & 0b00100000) != 0 && players[i][1] > 0 && walls[players[i][0]][players[i][1]-1] != 'w') {
                players[i][1] -= 1;
                players[i][5] = 2;
            } else if ((inputs[i] & 0b00010000) != 0 && players[i][1] < size_x-1 && walls[players[i][0]][players[i][1]+1] != 'w') {
                players[i][1] += 1;
                players[i][5] = 4;
            }
            if ((inputs[i] & 0b00001000) != 0 && players[i][2] == 0) {
                players[i][2] = players[i][5];
                players[i][3] = players[i][0];
                players[i][4] = players[i][1];
            }
            if (walls[players[i][3]][players[i][4]] == 'w') {
                players[i][2] = 0;
            }
        }
        if (clock.millis() > then) {
            then += 20;
            for (int i = 0; i < players.length; i++) {
                if (players[i][2] == 1) {
                    players[i][3] -= 1;
                } else if (players[i][2] == 2) {
                    players[i][4] -= 1;
                } else if (players[i][2] == 3) {
                    players[i][3] += 1;
                } else if (players[i][2] == 4) {
                    players[i][4] += 1;
                }
                for (int j = 0; j < players.length; j++) {
                    if (i != j && players[i][3] == players[j][0] && players[i][4] == players[j][1]) {
                        players[j][0] = 0;
                        players[j][1] = 0;
                    }
                }
            }
        }
    }
}  
