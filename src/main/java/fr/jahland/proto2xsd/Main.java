package fr.jahland.proto2xsd;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by mvincent on 11/02/2016.
 */
public class Main {
    public static void main(String[] args) throws IOException {

        if (args.length == 0) {
            System.out.println("Usage :");
            System.exit(0);
        }

        int index = 0;
        while (index < args.length && args[index].startsWith("-")) {

            String currentOpt = args[index];
            switch (currentOpt) {
                case "-g" :
                case "--generate-imports" :
                    // TODO : add option
                    break;
                case "-i" :
                case "--ignore-missing-imports" :
                    // TODO : add option
                    break;
            }
            index++;
        }

        // Filename is the last argument
        String fileName = args[args.length - 1];

        // FIXME
        Path directory = Paths.get("./proto2xsd").toRealPath();
        System.out.println("Reading " + new File(fileName).getAbsolutePath());

        //Generator.generateFile(Paths.get(directory.toString(), fileName));
    }
}
