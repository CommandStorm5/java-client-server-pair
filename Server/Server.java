import java.io.*;  
import java.net.*;
import java.util.*;
public class Server {  
    public static void main() {  
        try {  
            ServerSocket ss=new ServerSocket(505);  
            Socket s=ss.accept();//establishes connection   
            DataInputStream din=new DataInputStream(s.getInputStream());  
            DataOutputStream dout=new DataOutputStream(s.getOutputStream());  
            BufferedReader br=new BufferedReader(new InputStreamReader(System.in));   
            String response="",render="";
            while(!response.equals("100 100")){  
                response=din.readUTF();
                if (!response.equals("100 100")) {
                    int coords[] = ParseResponse(response);
                    render = Game(coords);
                    dout.writeUTF(render);  
                    dout.flush();
                } else {
                    dout.writeUTF("stop");  
                    dout.flush();
                }
            }
            din.close();  
            s.close();  
            ss.close();
        } catch (Exception e){
            System.err.println(e);
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