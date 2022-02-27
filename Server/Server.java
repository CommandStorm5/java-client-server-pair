import java.time.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
public class Server {
    public static Clock clock = Clock.systemDefaultZone();
    public static long then = clock.millis();
    public static List<List<Integer>> players = new ArrayList<List<Integer>>(); //x,y,proj_d,proj_x,proj_y,last_d
    public static int player_vars = 6;
    public static int size_x = 40;
    public static int size_y = 40;
    public static char[][] walls = generateWalls();
    public static int await_client = 0;
    public static List<ServerSocket> ss = new ArrayList<ServerSocket>();
    public static List<Socket> s = new ArrayList<Socket>();
    public static List<DataInputStream> din = new ArrayList<DataInputStream>();
    public static List<DataOutputStream> dout = new ArrayList<DataOutputStream>();
    public static BufferedReader br=new BufferedReader(new InputStreamReader(System.in));
    public static List<Integer> ports = new ArrayList<Integer>();
    //public static String console_data = "";
    public static void main() {
        //for (int i = 0; i < 6; i++) {
        //    players.add(new ArrayList<Integer>());
        //}

        // for (int i = 0; i < players.get(0).size(); i++) {
            // players.get(i).set(0, 1);
            // players.get(i).set(0, 1);
        // }
        //System.out.println("Starting Server");


        // for (int i = 0; i < players.get(0).size(); i++) {
            // try {
                // ss.get(i) = generateServerSocket(440+i);
                // s.get(i) = establishConnection(ss[i]);
                // din.get(i) = new DataInputStream(s[i].getInputStream());
                // dout.set(i, new DataOutputStream(s.get(i).getOutputStream()));
            // } catch (Exception e){
                    // System.err.println(e);
            // }
        // }
        // FIX THIS

        String render="";
        int await = 1;
        String console_data = "";
        while (!console_data.equals("stop")) {
            CompletableFuture<String> future = CompletableFuture.supplyAsync(new Supplier<String>() {
                @Override
                public String get() {
                    Scanner s = new Scanner(System.in);
                    return s.nextLine();
                }
            });
            await = 1;
            while (await == 1) {
                for (int i = 0; i < players.length; i++) {
                    if (s.size() == 0) {
                        System.out.println("Waiting for initial");
                        while (!await_new_client.isDone()) {

                        }
                        System.out.println("Initial recieved");
                    }
                }
                while (players.size() == 0) {
                    System.out.println("Waiting for players");
                }
                byte[] responses = new byte[players.size()];
                System.out.println(players.size());
                for (int i = 0; i < players.size(); i++) {
                    try {
                        responses[i]=(byte)din.get(i).read();
                        System.out.println("Response: " + i);
                        System.out.println(responses[i]);
                        if ((responses[i] & (byte)0b00000001) == 1) {
                            disconnect(i);
                        }
                    } catch (Exception e){
                        System.err.println("Read: " + e);
                        disconnect(i);
                    }

                }
                System.out.println("Responses L: " + responses.length);
                if (responses.length > 0) {
                    render = gameTick(responses);

                    for (int i = 0; i < players.size(); i++) {
                        System.out.println("Writing: " + i);
                        try {
                            dout.get(i).writeUTF(render);
                            dout.get(i).flush();
                        } catch (Exception e){
                            System.err.println("Write: " + e);
                        }
                    }
                }
                if (console_command.isDone()) {
                    await = 0;
                }
                try {
                    TimeUnit.MILLISECONDS.sleep(30);
                } catch (Exception e) {
                    System.err.println(e);
                }
            }

            try {
                console_data = console_command.get();
                console_data = console_data.trim();
                console(console_data);
            } catch (Exception e){
                System.err.println(e);
            }
        }
        for (int i = 0; i < players.size(); i++) {
            gracefulDisconnect(i);
        }
    }
    public static void gracefulDisconnect(int n) {
        try {
            dout.get(n).writeUTF("0");
            dout.get(n).flush();
            TimeUnit.MILLISECONDS.sleep(50);
            dout.get(n).close();
            s.get(n).close();
            ss.get(n).close();
        } catch (Exception e) {
            System.err.println("gracefulDisconnect: " + e);
        }
    }
    public static void disconnect(int n) {
        System.out.println("Removing: " + n);
        players.remove(n);
        ports.remove(n);
        dout.remove(n);
        din.remove(n);
        s.remove(n);
        ss.remove(n);
    }
    public static void console(String data) {
        if (data.equals("reset")) {
            for (int i = 0; i < players.size(); i++) {
                players.get(i).set(0, 1);
                players.get(i).set(1, 1);
            }
        }
        if (data.equals("respawn")) {
            Scanner s = new Scanner(System.in);
            int i = s.nextInt();
            if (players.get(i).get(0) == 0 && players.get(i).get(1) == 0) {
                players.get(i).set(0, 1);
                players.get(i).set(1, 1);
            }
        }
        if (data.equals("respawn all")) {
            for (int i = 0; i < players.size(); i++) {
                if (players.get(i).get(0) == 0 && players.get(i).get(1) == 0) {
                    players.get(i).set(0, 1);
                    players.get(i).set(1, 1);
                }
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
    public static Socket establishConnection(ServerSocket ss) {
        try {
            Socket s = ss.accept();
            return s;
        } catch (Exception e){
            System.err.println(e);
            return null;
        }
    }
    public static char[][] generateWalls() {
        char[][] walls = new char[size_x][size_y];
        for (int x = 0; x < size_x; x++) {
            for (int y = 0; y < size_y; y++) {
                if (x == 0 || x == size_x-1) { //gen bounds
                    walls[x][y] = 'w';
                }
                if (y == 0 || y == size_y-1) {
                    walls[x][y] = 'w';
                }
                if (y == 3 && x > 3 && x < size_x-4) {
                    walls[x][y] = 'w';
                }
                if (y == size_y-4 && x > 3 && x < size_x-4) {
                    walls[x][y] = 'w';
                }
                if (x == 8 && y > 8 && y < size_y-9) {
                    walls[x][y] = 'w';
                }
                if (x == size_x-9 && y > 8 && y < size_y-9) {
                    walls[x][y] = 'w';
                }
                if (y == 13 && x > 13 && x < size_x-14) {
                    walls[x][y] = 'w';
                }
                if (y == size_y-14 && x > 13 && x < size_x-14) {
                    walls[x][y] = 'w';
                }
            }
        }
        return walls;
    }
    public static String gameTick(byte[] data_array) {
        char[][] display = new char[size_x][size_y];
        //String data = "";
        //for (int i = 0; i < players.get(0).size(); i++) {
        //    data = data_array[i] + " ";
        //}
        //int[] inputs = parseResponse(data);
        int[] inputs = new int[players.size()];
        System.out.println(data_array.length);
        System.out.println(inputs.length);
        //System.out.println(data);
        for (int i = 0; i < players.size(); i++) {
            inputs[i] = data_array[i];
        }
        runMovement(inputs);
        for (int x = 0; x < size_x; x++) {
            for (int y = 0; y < size_y; y++) {
                if (walls[x][y] == 'w') {
                    display[x][y] = 'w';
                } else {
                    display[x][y] = 'e';
                }
            }
        }
        for (int i = players.size()-1; i >= 0; i--) {
            display[players.get(i).get(0)][players.get(i).get(1)] = (char)(i + 48);
            if (players.get(i).get(2) == 1 || players.get(i).get(2) == 3) {
                display[players.get(i).get(3)][players.get(i).get(4)] = 'v';
            } else if (players.get(i).get(2) == 2 || players.get(i).get(2) == 4) {
                display[players.get(i).get(3)][players.get(i).get(4)] = 'h';
            }
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
            if ((inputs[i] & 0b10000000) != 0 && players.get(i).get(0) > 0 && walls[players.get(i).get(0)-1][players.get(i).get(1)] != 'w') {
                players.get(i).set(0, players.get(i).get(0) - 1);
                players.get(i).set(5, 1);
            } else if ((inputs[i] & 0b01000000) != 0 && players.get(i).get(0) < size_x-1 && walls[players.get(i).get(0)+1][players.get(i).get(1)] != 'w') {
                players.get(i).set(0, players.get(i).get(0) + 1);
                players.get(i).set(5, 3);
            } else if ((inputs[i] & 0b00100000) != 0 && players.get(i).get(1) > 0 && walls[players.get(i).get(0)][players.get(i).get(1)-1] != 'w') {
                players.get(i).set(1, players.get(i).get(1) - 1);
                players.get(i).set(5, 2);
            } else if ((inputs[i] & 0b00010000) != 0 && players.get(i).get(1) < size_x-1 && walls[players.get(i).get(0)][players.get(i).get(1)+1] != 'w') {
                players.get(i).set(1, players.get(i).get(1) + 1);
                players.get(i).set(5, 4);
            }
            if ((inputs[i] & 0b00001000) != 0 && players.get(i).get(2) == 0) {
                players.get(i).set(2, players.get(i).get(5));
                players.get(i).set(3, players.get(i).get(0));
                players.get(i).set(4, players.get(i).get(1));
            }
            if (walls[players.get(i).get(3)][players.get(i).get(4)] == 'w') {
                players.get(i).set(2, 0);
            }
        }
        if (clock.millis() > then) {
            then += 20;
            for (int i = 0; i < players.size(); i++) {
                if (players.get(i).get(2) == 1) {
                    players.get(i).set(3, players.get(i).get(3) - 1);
                } else if (players.get(i).get(2) == 2) {
                    players.get(i).set(4, players.get(i).get(4) - 1);
                } else if (players.get(i).get(2) == 3) {
                    players.get(i).set(3, players.get(i).get(3) + 1);
                } else if (players.get(i).get(2) == 4) {
                    players.get(i).set(4, players.get(i).get(4) + 1);
                }
                for (int j = 0; j < players.size(); j++) {
                    if (i != j && players.get(i).get(3) == players.get(j).get(0) && players.get(i).get(4) == players.get(j).get(1)) {
                        players.get(j).set(0, 0);
                        players.get(j).set(1, 0);
                    }
                }
            }
        }
    }
}
