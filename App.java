
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.List;

public class App {

    public static Node node;

    public static List<Node> nodes;

    public static int managerNodeId;

    public static DatagramSocket socket;

    public static boolean isManagerNode;

    public static boolean electionInProgress;

    public static void main(String[] args) throws InterruptedException {

        try {

            nodes = FileHelper.ReadFile("file.txt");

            node = nodes.get(Integer.parseInt(args[1]) - 1);

            managerNodeId = nodes.size() - 1;

            socket = new DatagramSocket(Integer.parseInt(node.port));

            start();
        } catch (NumberFormatException | IOException e) {
            e.printStackTrace();
        }
    }

    private static void start() throws InterruptedException, IOException {
        if (node.id == nodes.get(nodes.size() - 1).id) {
            Coordenator coordenator = node;
            System.out.println("Node is manager");
            coordenator.manageNodes();
            
        } else {
            System.out.println("Node is not manager");
            sendMessageToManager().start();
            // System.out.println("puts");
            receiveMessage().start();
        }
        // Thread.sleep(10000);
    }

    private static Thread sendMessageToManager() throws IOException {
        socket.setSoTimeout(5000);
        return new Thread(() -> {
            while (true) {
                if (!electionInProgress) {
                    String message = node.host + "-" + node.port;
                    byte[] output = message.getBytes();
                    Node managerNode = nodes.get(managerNodeId);
                    DatagramPacket datagramPacket;
                    try {
                        datagramPacket = new DatagramPacket(output, output.length,
                                InetAddress.getByName(managerNode.host), Integer.parseInt(managerNode.port));
                        System.out.println("Sending: " + managerNode.host + "-" + managerNode.port);
                        socket.send(datagramPacket);
                    } catch (NumberFormatException | IOException e) {
                        e.printStackTrace();
                    }
                    byte[] input = new byte[256];
                    DatagramPacket packet = new DatagramPacket(input, input.length);
                    try {

                        socket.receive(packet);
                        String received = new String(packet.getData(), 0, packet.getLength());
                        System.out.println(received);
                    } catch (SocketTimeoutException e) {
                        try {
                            startElection();
                        } catch (InterruptedException e1) {
                            e1.printStackTrace();
                        }
                    } catch (NumberFormatException | IOException e) {
                        e.printStackTrace();
                    }
                }
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private static void startElection() throws InterruptedException {
        isManagerNode = true;
        electionInProgress = true;
        System.out.println("Start election");
        for (Node nodeCandidate : nodes) {
            if (nodeCandidate.id > node.id) {
                sendElectionMessage(nodeCandidate);
            }
        }
        Thread.sleep(3500);
        if (isManagerNode) {
            declareAsManagerNode();
            electionInProgress = false;
        }
    }

    private static void sendElectionMessage(Node nodeCandidate) {
        new Thread(() -> {
            // DatagramSocket sendSocket = new DatagramSocket();
            String message = "elections-" + node.id;
            byte[] output = message.getBytes();
            try {
                DatagramPacket datagramPacket = new DatagramPacket(
                    output, 
                    output.length,
                    InetAddress.getByName(nodeCandidate.host), 
                    Integer.parseInt(nodeCandidate.port.replace('8', '9')));
                System.out.println("Sending: " + nodeCandidate.host + "-" + nodeCandidate.port.replace('8', '9'));
                socket.send(datagramPacket);
            } catch (NumberFormatException | IOException e) {
                e.printStackTrace();
            }
            byte[] input = new byte[256];
            DatagramPacket packet = new DatagramPacket(input, input.length);
            try {
                socket.setSoTimeout(3000);
                socket.receive(packet);
                String received = new String(packet.getData(), 0, packet.getLength());
                if (received.equals("ok")) {
                    isManagerNode = false;
                }
            } catch (IOException e) {

            }
        }).start();
    }

    public static Thread receiveMessage() {
        return new Thread(() -> {
            while (true) {
                try {
                    DatagramSocket otherSocket = new DatagramSocket(Integer.parseInt(node.port.replace('8', '9')));
                    byte[] input = new byte[256];
                    DatagramPacket packet = new DatagramPacket(input, input.length);
                    try {
                        System.out.println("RECEIVING: " + node.host + "-" + node.port.replace('8', '9'));
                        otherSocket.receive(packet);
                        String received = new String(packet.getData(), 0, packet.getLength());
                        otherSocket.close();
                        System.out.println("Received: " + received);
                        if (received.contains("elections")) {
                            try {
                                String message = "ok";
                                byte[] output = message.getBytes();
                                int sendingNodeId = Integer.parseInt(received.split("-")[1]);
                                Node sendingNode = nodes.get(sendingNodeId - 1);
                                DatagramPacket datagramPacket = new DatagramPacket(
                                    output, 
                                    output.length, 
                                    InetAddress.getByName(sendingNode.host),
                                    Integer.parseInt(sendingNode.port));
                                System.out.println("Sending: " + sendingNode.host + "-" + sendingNode.port);
                                socket.send(datagramPacket);
                                startElection();
                            }  catch (NumberFormatException | IOException e) {
                                e.printStackTrace();
                            }
                        } else if (received.contains("i am the manager-")) {
                            managerNodeId = Integer.parseInt(received.split("-")[1]);
                            electionInProgress = false;
                        }
                } catch (NumberFormatException | SocketException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
                } catch (Exception ex) {

                }
            }
        });
    }

    public static void declareAsManagerNode() {
        try {
            String message = "i am the manager-" + node.id;
            byte[] output = message.getBytes();
            for (Node sendingNode : nodes) {
                DatagramPacket datagramPacket = new DatagramPacket(
                    output, 
                    output.length, 
                    InetAddress.getByName(sendingNode.host),
                    Integer.parseInt(sendingNode.port));
                System.out.println("Sending: " + sendingNode.host + "-" + sendingNode.port.replace('8', '9'));
                socket.send(datagramPacket);
            }
        }  catch (NumberFormatException | IOException e) {
            e.printStackTrace();
        }
    }
}
