package additivepatterns;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AddConditionToPredictionRunner {

    private final List<AddConditionToPredictionFileRequest> fileRequests;

    public AddConditionToPredictionRunner(List<File> javaFiles) {
        assert javaFiles != null && !javaFiles.isEmpty();
        fileRequests = new ArrayList<>();
        for (File javaFile : javaFiles) {
            if (javaFile == null || !javaFile.exists()) {
                System.out.println("null or not existing file " + javaFile);
                continue;
            }
            fileRequests.add(new AddConditionToPredictionFileRequest(javaFile));
        }
    }

    public void generateMaskedPatches(){
        for (AddConditionToPredictionFileRequest fr : fileRequests){
            fr.generateMaskedPatches();
        }
    }


}
