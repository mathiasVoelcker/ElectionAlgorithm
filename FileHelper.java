import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FileHelper {


    public static List<Node> ReadFile(String fileName) throws NumberFormatException, IOException{
        // FileReader reads text files in the default encoding.
        FileReader fileReader = new FileReader(fileName);

        // Always wrap FileReader in BufferedReader.
        BufferedReader bufferedReader = new BufferedReader(fileReader);

        List<Node> nodes = new ArrayList<>();

        String line = "";

        while((line = bufferedReader.readLine()) != null) {
            String[] infos = line.split(" ");
            nodes.add(new Node(Integer.parseInt(infos[0]), infos[1], infos[2]));
        }   

        // Always close files.
        bufferedReader.close();

        return nodes;
    }

}