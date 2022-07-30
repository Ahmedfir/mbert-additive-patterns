package additivepatterns.cli;

import additivepatterns.out.AddConditionToPredictionFileRequest;
import additivepatterns.out.RequestsOutput;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class CliRequest {
    private static Logger log = LoggerFactory.getLogger(CliRequest.class);
    public static final String DEFAULT_JSON_PREDICATES_FILE_NAME = "add_predicates.json";
    private String locationsFileName = DEFAULT_JSON_PREDICATES_FILE_NAME;
    private static final String DEFAULT_OUTPUT_DIR = "output";
    public static final String ERROR_MESSAGE = "- available requests features:\n" +
            "You can either include:" +
            "1) a file or \n " +
            "- available arguments:\n" +
            Arrays.toString(CliArgPrefix.values()) + "\n" +
            "- example args usage:\n" +
            "additivepatterns.AddConditionToPredictionRunner \n" +
            "-in=source_file_name:method_name@line@line@line:method_name@line@line@line \n" +
            "-in=source_file_name::line@line@line \n" +
            "-out=locations_directory\n";
    final RequestsOutput requestsOutput;
    final String outputDir;
    private Gson gson;

    public static CliRequest parseArgs(String[] args) {
        List<AddConditionToPredictionFileRequest> fileRequests = new ArrayList<>();
        String locationsOutputDirectory = DEFAULT_OUTPUT_DIR;


        for (String arg : args) {
            try {
                CliArgPrefix cliArgPrefix = CliArgPrefix.startsWithPrefix(arg);
                String argBody = arg.replace(cliArgPrefix.argPrefix, "");

                switch (cliArgPrefix) {
                    case FILE_INCLUDE_REQUEST:
                        fileRequests.add(parseFileArgs(argBody, cliArgPrefix));
                        break;
                    case OUTPUT_DIR:
                        locationsOutputDirectory = argBody;
                        break;

                }
            } catch (IllegalArgumentException e) {
                correctUssage("Wrong arg prefix!", e);
            }
        }

        if (fileRequests.isEmpty()) {
            correctUssage("No File passed!");
            System.exit(1);
        }

        return new CliRequest(fileRequests, locationsOutputDirectory);
    }

    private CliRequest(List<AddConditionToPredictionFileRequest> fileRequests, String outputDir) {
        this.requestsOutput = new RequestsOutput(fileRequests);
        this.outputDir = outputDir;

    }


    public CliRequest generateMaskedPatches(){
        for (AddConditionToPredictionFileRequest fr : requestsOutput.getFileRequests()){
            fr.generateMaskedPatches();
        }
        return this;
    }


    public void outputResults() {
        if (gson == null) gson = new Gson();
        // print the results to file as json.
        try {
            Files.createDirectories(Paths.get(outputDir));
            String statsFilePath = Paths.get(outputDir).resolve(locationsFileName).toString();
            System.out.println("printing_json_results in " + statsFilePath);
            FileWriter writer = new FileWriter(statsFilePath);
            gson.toJson(requestsOutput, writer);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("++++");
            System.err.println(gson.toJson(requestsOutput));
        }
    }

    private static AddConditionToPredictionFileRequest parseFileArgs(String argBody, CliArgPrefix cliArgPrefix) {
        assert cliArgPrefix == CliArgPrefix.FILE_INCLUDE_REQUEST;
        String[] splits = argBody.split(":");
        String filePath = splits[0];
        File file = new File(filePath);
        if (!file.exists()) {
            correctUssage("wrong file path", new IllegalArgumentException(filePath));
            return null;
        }

        return new AddConditionToPredictionFileRequest(file);
    }


    private static void correctUssage(String s, RuntimeException... runtimeExceptions) {
        System.err.println("error: " + s);
        System.err.println(ERROR_MESSAGE);
        if (runtimeExceptions != null && runtimeExceptions.length > 0) {
            throw runtimeExceptions[0];
        }
    }

    @Override
    public String toString() {
        return "CliRequest{" +
                "fileRequests=" + requestsOutput +
                ", outputDir='" + outputDir + '\'' +
                '}';
    }
}
