import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class Machine {

    public static void main(String[] args) throws NumberFormatException, IOException {
        DatagramSocket socket = new DatagramSocket(Integer.parseInt("7070"));   
        byte[] input = new byte[256]; 
        DatagramPacket packet = new DatagramPacket(input, input.length);
        socket.setSoTimeout(0);
        while(true) {
            socket.receive(packet);
            String received = new String(packet.getData(), 0, packet.getLength());
            System.out.println(received);
        }
    }

}