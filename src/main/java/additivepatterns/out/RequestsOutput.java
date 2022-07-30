package additivepatterns.out;

import java.io.Serializable;
import java.util.List;

public class RequestsOutput implements Serializable {
    private final List<AddConditionToPredictionFileRequest> fileRequests;

    public RequestsOutput(List<AddConditionToPredictionFileRequest> fileRequests) {
        this.fileRequests = fileRequests;
    }

    public List<AddConditionToPredictionFileRequest> getFileRequests() {
        return fileRequests;
    }
}
