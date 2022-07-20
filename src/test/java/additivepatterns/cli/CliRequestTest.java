package additivepatterns.cli;

import additivepatterns.out.AddConditionToPredictionFileRequest;
import additivepatterns.out.BasePredicate;
import additivepatterns.out.MaskedPredicate;
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import statistics.A;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class CliRequestTest {

    private static final String file_1 = "src/test/resources/INPUT/exampleclasses/chart_5/XYSeries.java";
    private static Path outputDir;
    private static Path expectedDir;

    @BeforeClass
    public static void beforeClass() throws Exception {
        expectedDir = Paths.get("src/test/resources/EXPECTED").resolve(CliRequestTest.class.getSimpleName());
        outputDir = Paths.get("src/test/resources/tmp").resolve(CliRequestTest.class.getSimpleName());
        Files.createDirectories(outputDir);
    }

    @Test
    public void sys_test__process_one_file() throws IOException {
        Path expectDir = expectedDir.resolve("sys_test__process_one_file");
        assertTrue(expectDir.toFile().isDirectory());
        Path expectedJson = expectDir.resolve(CliRequest.DEFAULT_JSON_PREDICATES_FILE_NAME);
        File expectedFile = expectedJson.toFile();
        assertTrue(expectedFile.isFile());

        Path outDir = outputDir.resolve("sys_test__process_one_file");
        Files.createDirectories(outDir);
        assertTrue(outputDir.toFile().isDirectory());
        File outFile = outDir.resolve(CliRequest.DEFAULT_JSON_PREDICATES_FILE_NAME).toFile();

        String[] req = {"-in=" + file_1 , "-out=" + outDir};
        CliRequest cliRequest = CliRequest.parseArgs(req);
        cliRequest.generateMaskedPatches().outputResults();
        assertTrue(outFile.exists());
        assertEquals(1, cliRequest.fileRequests.size());

        List<MaskedPredicate> allpredicates = new ArrayList<>();
        List<String> allMaskedPredicates = new ArrayList<>();
        for (AddConditionToPredictionFileRequest fileRequest : cliRequest.fileRequests) {
            allpredicates.addAll(fileRequest.getAllMaskedPredicates());
            for (MaskedPredicate allMaskedPredicate : fileRequest.getAllMaskedPredicates()) {
                allMaskedPredicates.addAll(allMaskedPredicate.newMaskedPredicates);
            }
        }

        assertEquals(34, allpredicates.size());
        assertEquals(3736, allMaskedPredicates.size());
    }

    @AfterClass
    public static void afterClass() throws Exception {
        FileUtils.deleteDirectory(outputDir.toFile());
    }
}