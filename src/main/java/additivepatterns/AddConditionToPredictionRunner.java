package additivepatterns;

import additivepatterns.AstParsing.AstParser;
import additivepatterns.cli.CliRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;

public class AddConditionToPredictionRunner {

    private static Logger log = LoggerFactory.getLogger(AstParser.class);

    public static void main(String...args){
        try {
            CliRequest cliRequest = CliRequest.parseArgs(args);
            System.out.println("--- Initialisation --- \n" + cliRequest + "\n -----------------");
            cliRequest.generateMaskedPatches().outputResults();
        } catch (Throwable throwable) {
            System.err.println("Failed = " + Arrays.toString(args));
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            throwable.printStackTrace(pw);
            System.err.println(sw);
            System.exit(100);
        }
    }


}
