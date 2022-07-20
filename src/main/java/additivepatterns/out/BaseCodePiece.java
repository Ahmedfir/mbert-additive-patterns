package additivepatterns.out;

import edu.lu.uni.serval.jdt.tree.ITree;
import edu.lu.uni.serval.tbar.context.ContextReader;
import edu.lu.uni.serval.tbar.utils.Checker;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class BaseCodePiece {

    public static final String MASK = "<mask>";
    public final ITree astTree;
    public final int start;
    public final int end;
    private List<String> maskedCodes;
    private String codeString;

    public BaseCodePiece(ITree astTree, int start, int end) {
        this.astTree = astTree;
        this.start = start;
        this.end = end;
    }

    public BaseCodePiece(ITree astTree) {
        this.start = astTree.getPos();
        this.end = this.start + astTree.getLength();
        this.astTree = astTree;
    }

    public String getCodeString(String fileContent) {
        if (codeString == null){
            codeString = fileContent.substring(start, end);
        }
        return codeString;
    }

    public List<String> getVariables() {
        return readVariables(astTree);
    }

    public List<ITree> getVarsAndOperators(ITree astTree) {
        List<ITree> result = new ArrayList<>();
        List<ITree> children = astTree.getChildren();
        for (ITree child : children) {
            if (Checker.isSimpleName(child.getType())) {
                String var = ContextReader.readVariableName(child);
                if (var == null) result.addAll(getVarsAndOperators(child));
                else result.add(child);
            } else if (Checker.isOperator(child.getType())) {
                result.add(child);
            } else {
                result.addAll(getVarsAndOperators(child));
            }
        }
        return result;
    }


    public List<String> generateMaskedCodes(String fileContent) {
        if (maskedCodes == null){
            maskedCodes = new ArrayList<>();
            List<ITree> nodesToMask = getVarsAndOperators(astTree);
            for (ITree node : nodesToMask) {
                maskedCodes.add(fileContent.substring(start, node.getPos()) + MASK
                        + fileContent.substring(node.getPos() + node.getLength(), end));
            }
        }
        return maskedCodes;
    }

    private List<String> readVariables(ITree astTree) {
        List<String> vars = new ArrayList<>();
        List<ITree> children = astTree.getChildren();
        for (ITree child : children) {
            if (Checker.isSimpleName(child.getType())) {
                String var = ContextReader.readVariableName(child);
                if (var == null) vars.addAll(readVariables(child));
                else vars.add(var);
            } else vars.addAll(readVariables(child));
        }
        return vars;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BaseCodePiece that = (BaseCodePiece) o;
        return start == that.start && end == that.end && Objects.equals(astTree, that.astTree);
    }

    @Override
    public int hashCode() {
        return Objects.hash(astTree, start, end);
    }
}
