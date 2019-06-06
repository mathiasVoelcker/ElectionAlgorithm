public class Coordenator extends Node {

    public Coordenator(int id, String host, String port) {
        super(id, host, port);
    }

    public void manageNodes() {
        manageMessages().start();
    }

    public static Thread manageMessages() {
        return new Thread(() -> {
            while (true) {
                byte[] input = new byte[256];
                DatagramPacket packet = new DatagramPacket(input, input.length);
                try {
                    System.out.println("RECEIVING: " + node.host + "-" + node.port);
                    socket.receive(packet);
                    String received = new String(packet.getData(), 0, packet.getLength());
                    System.out.println(received);
                    String[] senderData = received.split("-");
                    byte[] output = "okFromManager".getBytes();
                    DatagramPacket sendPacket = new DatagramPacket(output, output.length,
                            InetAddress.getByName(senderData[0]), Integer.parseInt(senderData[1]));
                    socket.send(sendPacket);
                } catch (NumberFormatException | IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}