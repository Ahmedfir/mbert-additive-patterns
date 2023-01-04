package additivepatterns.out;

import java.util.Objects;

public class RemovingRange {

    public final String javaFilePath;
    public final int start;
    public final int end;
    public final int lineNumber;

    public RemovingRange(String javaFilePath, int start, int end, int lineNumber) {
        this.javaFilePath = javaFilePath;
        this.start = start;
        this.end = end;
        this.lineNumber = lineNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RemovingRange that = (RemovingRange) o;
        return start == that.start && end == that.end && javaFilePath.equals(that.javaFilePath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(javaFilePath, start, end);
    }
}
