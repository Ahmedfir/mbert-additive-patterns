package additivepatterns.out;

import edu.lu.uni.serval.jdt.tree.ITree;

import java.util.*;

public class CodePiece extends BaseCodePiece {
    public final String javaFilePath;
    public final int lineNumber;
    public Set<String> newMaskedPredicates = new HashSet<>();

    public CodePiece(ITree astTree, int start, int end, String javaFilePath, int lineNumber) {
        super(astTree, start, end);
        this.javaFilePath = javaFilePath;
        this.lineNumber = lineNumber;
    }

    public CodePiece(ITree astTree, String javaFilePath, int lineNumber) {
        super(astTree);
        this.javaFilePath = javaFilePath;
        this.lineNumber = lineNumber;
    }

    public CodePiece(String javaFilePath, ITree astTree, int lineNumber) {
        super(astTree);
        this.javaFilePath = javaFilePath;
        this.lineNumber = lineNumber;
    }

    public void concatPredicates(String oldP, String newP, boolean withInverse) {
        if (withInverse) {
            newMaskedPredicates.add("(" + oldP + ") && !(" + newP + ")");
            newMaskedPredicates.add("(" + oldP + ") || !(" + newP + ")");
        }
        newMaskedPredicates.add("(" + oldP + ") && (" + newP + ")");
        newMaskedPredicates.add("(" + oldP + ") || (" + newP + ")");
    }

    public void addMaskedPredicates(BaseCodePiece codePiece, String fileContent) {
        String originalString = getCodeString(fileContent);
        List<String> originalPredicateMaskedStrings = generateMaskedCodes(fileContent);
        String newString = codePiece.getCodeString(fileContent);
        List<String> newPredicateMaskedStrings = codePiece.generateMaskedCodes(fileContent);
        newMaskedPredicates.add("(" + originalString + ") " + MASK + " (" + newString + ")");
        newMaskedPredicates.add("(" + originalString + ") " + MASK + " !(" + newString + ")");
        for (String str : originalPredicateMaskedStrings) {
            concatPredicates(str, newString, true);
        }
        for (String str : newPredicateMaskedStrings) {
            concatPredicates(originalString, str, true);
        }
    }

    public void addMaskedVarsOpsPredicates(String var, List<String> varList, String[] operators, String fileContent) {
        String originalString = getCodeString(fileContent);
        List<String> originalPredicateMaskedStrings = generateMaskedCodes(fileContent);
        for (String v_ : varList) {
            for (String op : operators) {
                String newString = var + op + v_ ;
                List<String> newPredicateMaskedStrings = new ArrayList<>();
                newPredicateMaskedStrings.add(MASK + op + v_);
                newPredicateMaskedStrings.add(var + MASK + v_);
                newPredicateMaskedStrings.add(var + op + MASK);

                newMaskedPredicates.add(newString + " " + MASK + " (" + originalString + ")");
                for (String str : originalPredicateMaskedStrings) {
                    concatPredicates(newString, str, false);
                }
                for (String str : newPredicateMaskedStrings) {
                    concatPredicates(str, originalString, false);
                }
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        CodePiece codePiece = (CodePiece) o;
        return lineNumber == codePiece.lineNumber && Objects.equals(javaFilePath, codePiece.javaFilePath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), javaFilePath, lineNumber);
    }
}
