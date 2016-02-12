package fr.jahland.proto2xsd;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Main class to launch tool from command line.
 * Created by mvincent on 11/02/2016.
 */
public class Main {
    public static void main(String[] args) throws IOException {

        if (args.length == 0) {
            System.out.println("Usage : java -jar proto2xsd.jar [options] input_file_name");
            System.out.println("Options");
            
            System.exit(0);
        }

        int index = 0;
        List<Options> options = new ArrayList<>();
        while (index < args.length && args[index].startsWith("-")) {

            String currentOpt = args[index];
            switch (currentOpt) {
                case "-g" :
                case "--generate-imports" :
                    options.add(Options.GENERATE_IMPORT);
                    break;
                case "-i" :
                case "--ignore-missing-imports" :
                    options.add(Options.IGNORE_IMPORT);
                    break;
                case "-r" :
                case "--recursive-imports" :
                    options.add(Options.RECURSIVE);
                    break;
                case "-d" :
                case "--dry-run":
                    options.add(Options.DRY_RUN);
                    break;
            }
            index++;
        }

        // Filename is the last argument
        String fileName = args[args.length - 1];

        // FIXME
        Path directory = Paths.get("./proto2xsd").toRealPath();
        System.out.println("Reading " + new File(fileName).getAbsolutePath());

        Generator.generateFile(Paths.get(directory.toString(), fileName), options);
    }
}
