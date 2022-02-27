import java.time.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
public class Server {
    //Public variables :D
    public static Clock clock = Clock.systemDefaultZone();
    public static long then = clock.millis();
    //I could make this better but I'd need to rewrite half the program
    public static List<List<Integer>> players = new ArrayList<List<Integer>>(); //x,y,proj_d,proj_x,proj_y,last_d
    public static int player_vars = 6;
    //Hardcoded level size
    public static int size_x = 40;
    public static int size_y = 40;
    public static char[][] walls = generateWalls();
    public static int await_client = 0;
    //Connection vars
    //This could be merged, but naah
    public static List<ServerSocket> ss = new ArrayList<ServerSocket>();
    public static List<Socket> s = new ArrayList<Socket>();
    public static List<DataInputStream> din = new ArrayList<DataInputStream>();
    public static List<DataOutputStream> dout = new ArrayList<DataOutputStream>();
    public static BufferedReader br=new BufferedReader(new InputStreamReader(System.in));
    public static List<Integer> ports = new ArrayList<Integer>();

    public static void main() {
        String render="";
        int await = 1;
        String console_data = "";
        // Exit on full shutdown
        while (!console_data.equals("stop")) {
            //Async console handler
            CompletableFuture<String> console_command = CompletableFuture.supplyAsync(new Supplier<String>() {
                @Override
                public String get() {
                    Scanner s = new Scanner(System.in);
                    return s.nextLine();
                }
            });

            await = 1;
            // Exit on console command
            while (await == 1) {
                //Create client handler when one is not running
                if (await_client == 0) {
                    await_client = 1;
                    CompletableFuture<Integer> await_new_client = CompletableFuture.supplyAsync(new Supplier<Integer>() {
                        @Override
                        public Integer get() {
                            try {
                                //Handshake
                                ServerSocket handshake_ss = generateServerSocket(420);
                                Socket handshake_s = establishConnection(handshake_ss);
                                System.out.println("Connection: Handshake");
                                DataOutputStream handshake_dout = new DataOutputStream(handshake_s.getOutputStream());
                                //Find empty port
                                int portNumber = 421;
                                while (true) {
                                    if (!ports.contains(portNumber)) {
                                        break;
                                    }
                                    portNumber++;
                                }
                                System.out.println("Connection: Client ID " + ss.size());
                                System.out.println("Connection: Port " + portNumber);
                                ports.add(portNumber);
                                handshake_dout.writeInt(portNumber);
                                handshake_dout.flush();
                                handshake_dout.close();
                                handshake_s.close();
                                handshake_ss.close();
                                System.out.println("Connection: Handshake closed");
                                //Socket connection
                                //If something errors out here, all clients will desync
                                ss.add(generateServerSocket(portNumber));
                                s.add(establishConnection(ss.get(ss.size()-1)));
                                din.add(new DataInputStream(s.get(s.size()-1).getInputStream()));
                                dout.add(new DataOutputStream(s.get(s.size()-1).getOutputStream()));
                                //Add new player entry
                                players.add(new ArrayList<Integer>());
                                for (int j = 0; j < player_vars; j++) {
                                    players.get(players.size() - 1).add(0);
                                }
                                players.get(players.size() - 1).set(0, 1);
                                players.get(players.size() - 1).set(1, 1);
                                //Request new client handler creation
                                await_client = 0;
                                return 1;
                            } catch (Exception e){
                                System.err.println("Thread: " + e);
                                await_client = 0;
                                return 0;
                            }
                        }
                    });
                    //Await first client
                    if (s.size() == 0) {
                        System.out.println("Waiting for initial");
                        while (!await_new_client.isDone()) {
                            try {
                                TimeUnit.MILLISECONDS.sleep(100);
                            } catch (Exception e) {
                                System.err.println("Failed to wait 100ms lmao");
                            }
                        }
                    }
                }
                //Server freeze in case no clients are connected
                if (players.size() == 0) System.out.println("Waiting for client");
                while (players.size() == 0) {
                    try {
                        TimeUnit.MILLISECONDS.sleep(100);
                    } catch (Exception e) {
                        System.err.println("Failed to wait 100ms lmao");
                    }
                }
                //Recieve packets from clients
                byte[] responses = new byte[players.size()];
                for (int i = 0; i < players.size(); i++) {
                    try {
                        responses[i]=(byte)din.get(i).read();
                        //If player has pressed "Esc"
                        if ((responses[i] & (byte)0b00000001) == 1) {
                            gracefulDisconnect(i);
                        }
                    } catch (Exception e){
                        System.err.println("Read: " + e);
                        gracefulDisconnect(i);
                    }

                }
                //System.out.println("Responses L: " + responses.length);
                if (responses.length > 0) {
                    //Run game tick
                    render = gameTick(responses);

                    //Send packet to all clients
                    for (int i = 0; i < players.size(); i++) {
                        //System.out.println("Writing: " + i);
                        try {
                            dout.get(i).writeUTF(render);
                            dout.get(i).flush();
                        } catch (Exception e){
                            System.err.println("Write: " + e);
                            gracefulDisconnect(i);
                        }
                    }
                }
                //Request console command handler
                if (console_command.isDone()) {
                    await = 0;
                }
                //Main timeout
                try {
                    TimeUnit.MILLISECONDS.sleep(30);
                } catch (Exception e) {
                    System.err.println(e);
                }
            }
            //Console command sanitizing
            try {
                console_data = console_command.get();
                console_data = console_data.trim();
                console(console_data);
            } catch (Exception e){
                System.err.println(e);
            }
        }
        //Shutdown
        for (int i = players.size()-1; i >= 0; i--) {
            gracefulDisconnect(i);
        }
    }

    public static void gracefulDisconnect(int n) { //Politely ask client to remove itself
        try {
            dout.get(n).writeUTF("0");
            dout.get(n).flush();
            TimeUnit.MILLISECONDS.sleep(50);
            dout.get(n).close();
            s.get(n).close();
            ss.get(n).close();
            System.out.println("Player " + n + " disconnected");
        } catch (Exception e) {
            System.err.println("gracefulDisconnect: " + e);
        }
        disconnect(n);
    }

    public static void disconnect(int n) { //Yeetus deletus
        players.remove(n);
        ports.remove(n);
        dout.remove(n);
        din.remove(n);
        s.remove(n);
        ss.remove(n);
    }

    public static void console(String data) { //Handle console commands
        if (data.equals("reset")) {
            //Set all players to coordinate [1,1]
            for (int i = 0; i < players.size(); i++) {
                players.get(i).set(0, 1);
                players.get(i).set(1, 1);
            }
        }
        if (data.equals("respawn")) {
            //Set specific dead player's coordinate to [1,1]
            System.out.println("Player ID:");
            Scanner s = new Scanner(System.in);
            int i = s.nextInt();
            //Only run if player is actually dead [0,0]
            if (players.get(i).get(0) == 0 && players.get(i).get(1) == 0) {
                players.get(i).set(0, 1);
                players.get(i).set(1, 1);
            }
        }
        if (data.equals("kill")) {
            //Set specific player's coordinate to [0,0]
            System.out.println("Player ID:");
            Scanner s = new Scanner(System.in);
            int i = s.nextInt();
            players.get(i).set(0, 0);
            players.get(i).set(1, 0);
        }
        if (data.equals("respawn all")) {
            //Set all dead players' coordinates to [1,1]
            for (int i = 0; i < players.size(); i++) {
                //Only run if player is actually dead [0,0]
                if (players.get(i).get(0) == 0 && players.get(i).get(1) == 0) {
                    players.get(i).set(0, 1);
                    players.get(i).set(1, 1);
                }
            }
        }
    }

    public static ServerSocket generateServerSocket(int port) { //Create ServerSocket on given port
        try {
            ServerSocket ss=new ServerSocket(port);
            return ss;
        } catch (Exception e){
            System.err.println(e);
            return null;
        }
    }

    public static Socket establishConnection(ServerSocket ss) { //Create Socket on given ServerSocket
        try {
            Socket s = ss.accept();
            return s;
        } catch (Exception e){
            System.err.println(e);
            return null;
        }
    }

    public static char[][] generateWalls() { //Create current wall layout
        char[][] walls = new char[size_x][size_y];
        for (int x = 0; x < size_x; x++) {
            for (int y = 0; y < size_y; y++) {
                if (x == 0 || x == size_x-1) { //Gen x bounds
                    walls[x][y] = 'w';
                }
                if (y == 0 || y == size_y-1) { //Gen y bounds
                    walls[x][y] = 'w';
                }
                if (y == 3 && x > 3 && x < size_x-4) { //Place y=3 line
                    walls[x][y] = 'w';
                }
                if (y == size_y-4 && x > 3 && x < size_x-4) { //Complemet previous
                    walls[x][y] = 'w';
                }
                if (x == 8 && y > 8 && y < size_y-9) { //Place x=8 line
                    walls[x][y] = 'w';
                }
                if (x == size_x-9 && y > 8 && y < size_y-9) { //Complement previous
                    walls[x][y] = 'w';
                }
                if (y == 13 && x > 13 && x < size_x-14) { //Place y=13 line
                    walls[x][y] = 'w';
                }
                if (y == size_y-14 && x > 13 && x < size_x-14) { //Complement previous
                    walls[x][y] = 'w';
                }
            }
        }
        return walls;
    }

    public static String gameTick(byte[] data_array) { //Primary gameplay
        char[][] display = new char[size_x][size_y];
        //Parse player inputs
        int[] inputs = new int[players.size()];
        for (int i = 0; i < players.size(); i++) {
            inputs[i] = data_array[i];
        }

        runMovement(inputs);

        //Draw walls
        for (int x = 0; x < size_x; x++) {
            for (int y = 0; y < size_y; y++) {
                if (walls[x][y] == 'w') {
                    display[x][y] = 'w';
                } else {
                    display[x][y] = 'e';
                }
            }
        }
        //Draw objects
        for (int i = players.size()-1; i >= 0; i--) {
            //Draw players
            display[players.get(i).get(0)][players.get(i).get(1)] = (char)(i + 48);
            //Draw bullets
            if (players.get(i).get(2) == 1 || players.get(i).get(2) == 3) {
                display[players.get(i).get(3)][players.get(i).get(4)] = 'v';
            } else if (players.get(i).get(2) == 2 || players.get(i).get(2) == 4) {
                display[players.get(i).get(3)][players.get(i).get(4)] = 'h';
            }
        }
        //Stringify output
        String output = "";
        for (int x = 0; x < size_x; x++) {
            for (int y = 0; y < size_y; y++) {
                output += display[x][y];
            }
        }
        return output;
    }

    public static int[] parseResponse(String response) { //Player inputs to array
        String[] inputs_bad = response.split(" ");
        int[] inputs = new int[inputs_bad.length];
        for (int i = 0; i < inputs.length; i++){
            inputs[i] = Integer.parseInt(inputs_bad[i]);
        }
        return inputs;
    }

    public static void runMovement(int[] inputs) { //Change player coords according to movement
        for (int i = 0; i < inputs.length; i++) {
            if ((inputs[i] & 0b10000000) != 0 && players.get(i).get(0) > 0 && walls[players.get(i).get(0)-1][players.get(i).get(1)] != 'w') {
                //Move up
                players.get(i).set(0, players.get(i).get(0) - 1);
                players.get(i).set(5, 1);
            } else if ((inputs[i] & 0b01000000) != 0 && players.get(i).get(0) < size_x-1 && walls[players.get(i).get(0)+1][players.get(i).get(1)] != 'w') {
                //Move down
                players.get(i).set(0, players.get(i).get(0) + 1);
                players.get(i).set(5, 3);
            } else if ((inputs[i] & 0b00100000) != 0 && players.get(i).get(1) > 0 && walls[players.get(i).get(0)][players.get(i).get(1)-1] != 'w') {
                //Move left
                players.get(i).set(1, players.get(i).get(1) - 1);
                players.get(i).set(5, 2);
            } else if ((inputs[i] & 0b00010000) != 0 && players.get(i).get(1) < size_x-1 && walls[players.get(i).get(0)][players.get(i).get(1)+1] != 'w') {
                //Move right
                players.get(i).set(1, players.get(i).get(1) + 1);
                players.get(i).set(5, 4);
            }

            if ((inputs[i] & 0b00001000) != 0 && players.get(i).get(2) == 0) { //Spawn bullet
                players.get(i).set(2, players.get(i).get(5));
                players.get(i).set(3, players.get(i).get(0));
                players.get(i).set(4, players.get(i).get(1));
            }

            if (walls[players.get(i).get(3)][players.get(i).get(4)] == 'w') { //Remove bullet that has hit a wall
                players.get(i).set(2, 0);
            }
        }

        if (clock.millis() > then) {
            //Bullet timer
            then += 50;
            for (int i = 0; i < players.size(); i++) { //Handle bullet movement
                //This could be done with dx and dy
                if (players.get(i).get(2) == 1) {
                    players.get(i).set(3, players.get(i).get(3) - 1);
                } else if (players.get(i).get(2) == 2) {
                    players.get(i).set(4, players.get(i).get(4) - 1);
                } else if (players.get(i).get(2) == 3) {
                    players.get(i).set(3, players.get(i).get(3) + 1);
                } else if (players.get(i).get(2) == 4) {
                    players.get(i).set(4, players.get(i).get(4) + 1);
                }
                //Handle bullet hits
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
