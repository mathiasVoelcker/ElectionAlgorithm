// package election;

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

    public static DatagramSocket socketElection;

    public static boolean isManagerNode;

    public static boolean isManagerNodeTemp;

    public static boolean electionInProgress;

    public static void main(String[] args) throws InterruptedException {

        try {

            nodes = FileHelper.ReadFile("file.txt");

            node = nodes.get(Integer.parseInt(args[1]) - 1);

            managerNodeId = nodes.size();

            socket = new DatagramSocket(Integer.parseInt(node.port));

            socketElection = new DatagramSocket(Integer.parseInt(node.port.replace('8', '9')));

            start();
        } catch (NumberFormatException | IOException e) {
            e.printStackTrace();
        }
    }

    private static void start() throws InterruptedException, IOException {
        if (node.id == nodes.get(nodes.size() - 1).id) {
            System.out.println("Node is manager");
            // declareAsManagerNode();
            manageMessages().start();

        } else {
            System.out.println("Node is not manager");
            sendMessageToManager().start();
            receiveMessage().start();
        }
    }

    public static Thread manageMessages() {
        return new Thread(() -> {
            while (true) {
                Node keyHolder = null;
                byte[] input = new byte[256];
                DatagramPacket packet = new DatagramPacket(input, input.length);
                try {
                    System.out.println("RECEIVING: " + node.host + "-" + node.port);
                    socket.receive(packet);
                    String received = new String(packet.getData(), 0, packet.getLength());
                    System.out.println(received);
                    String[] senderData = received.split("-");
                    keyHolder = new Node(0, senderData[0], senderData[1]);
                    byte[] output = "okFromManager".getBytes();
                    DatagramPacket sendPacket = new DatagramPacket(output, output.length,
                            InetAddress.getByName(keyHolder.host), Integer.parseInt(keyHolder.port));
                    socket.send(sendPacket);
                    while (!received.equals("cleared")) {
                        try {
                            socket.setSoTimeout(3500);
                            socket.receive(packet);
                            received = new String(packet.getData(), 0, packet.getLength());
                            if (!received.equals("cleared")) {
                                System.out.println("sending not ok to " + received);
                                String[] senderData2 = received.split("-");
                                output = "notOkFromManager".getBytes();
                                sendPacket = new DatagramPacket(output, output.length,
                                        InetAddress.getByName(senderData2[0]), Integer.parseInt(senderData2[1]));
                                socket.send(sendPacket);
                            }
                        } catch (SocketTimeoutException e) {
                            break;
                        }
                    }
                } catch (NumberFormatException | IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private static Thread sendMessageToManager() throws IOException {
        socket.setSoTimeout(5000);
        return new Thread(() -> {
            while (!isManagerNode) {
                if (!electionInProgress) {
                    String message = node.host + "-" + node.port;
                    byte[] output = message.getBytes();
                    Node managerNode = nodes.get(managerNodeId - 1);
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
                        if (received.equals("okFromManager")) {
                            System.out.println("Sleeping");

                            String messageToMachine = "Acessando à zona crítica: " + node.host + "-" + node.port;
                            byte[] outputToMachine = messageToMachine.getBytes();
                            DatagramPacket packetToMachine = new DatagramPacket(outputToMachine, outputToMachine.length,
                                InetAddress.getByName("localhost"), Integer.parseInt("7070"));
                            socket.send(packetToMachine);

                            try {
                                Thread.sleep(3000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            messageToMachine = "Saíndo da zona crítica: " + node.host + "-" + node.port;
                            outputToMachine = messageToMachine.getBytes();
                            packetToMachine = new DatagramPacket(outputToMachine, outputToMachine.length,
                                InetAddress.getByName("localhost"), Integer.parseInt("7070"));
                            socket.send(packetToMachine);

                            output = "cleared".getBytes();
                            datagramPacket = new DatagramPacket(output, output.length,
                                    InetAddress.getByName(managerNode.host), Integer.parseInt(managerNode.port));
                            System.out.println("Sending clear to : " + managerNode.host + "-" + managerNode.port);
                            socket.send(datagramPacket);
                        } else {
                            System.out.println("Critical Zone already in use");
                        }
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
        if (!electionInProgress) {
            isManagerNodeTemp = true;
            electionInProgress = true;
            System.out.println("Start election");
            for (Node nodeCandidate : nodes) {
                if (nodeCandidate.id > node.id) {
                    sendElectionMessage(nodeCandidate);
                }
            }
            // if (node.id <= 3) {
            // isManagerNodeTemp = false;
            // }
            new Thread(() -> {
                try {
					Thread.sleep(5000);
                    if (isManagerNodeTemp) {
                        declareAsManagerNode();
                        electionInProgress = false;
                    }
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
            }).start();
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
                        Integer.parseInt(nodeCandidate.port.replace('8', '9'))
                );
                System.out.println("Sending: " + nodeCandidate.host + "-" + nodeCandidate.port.replace('8', '9') + " - " + message);
                socketElection.send(datagramPacket);
            } catch (NumberFormatException | IOException e) {
                e.printStackTrace();
            }
            byte[] input = new byte[256];
            DatagramPacket packet = new DatagramPacket(input, input.length);
        }).start();
    }

    public static Thread receiveMessage() {
        return new Thread(() -> {
            while (true) {
                try {
                    byte[] input = new byte[256];
                    DatagramPacket packet = new DatagramPacket(input, input.length);
                    try {
                        socket.setSoTimeout(3000);
                        System.out.println("RECEIVING: " + node.host + "-" + node.port.replace('8', '9'));
                        socketElection.receive(packet);
                        String received = new String(packet.getData(), 0, packet.getLength());
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
                                        Integer.parseInt(sendingNode.port.replace('8', '9')));
                                System.out.println("Sending: " + sendingNode.host + "-" + sendingNode.port.replace('8', '9') + " - " + message);
                                socketElection.send(datagramPacket);
                                startElection();
                            } catch (NumberFormatException | IOException e) {
                                e.printStackTrace();
                            }
                        } else if (received.contains("i am the manager-")) {
                            managerNodeId = Integer.parseInt(received.split("-")[1]);
                            System.out.println(managerNodeId);
                            electionInProgress = false;
                        } else if (received.equals("ok")) {
                            isManagerNodeTemp = false;
                        }

                    } catch (NumberFormatException | SocketException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    } catch(SocketTimeoutException e1) {

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
                        Integer.parseInt(sendingNode.port.replace('8', '9')));
                System.out.println("Sending: " + sendingNode.host + "-" + sendingNode.port.replace('8', '9') + " - " + message);
                socketElection.send(datagramPacket);
            }
            isManagerNode = true;
            System.out.println("Node is manager");
            manageMessages().start();
        } catch (NumberFormatException | IOException e) {
            e.printStackTrace();
        }
    }
}
