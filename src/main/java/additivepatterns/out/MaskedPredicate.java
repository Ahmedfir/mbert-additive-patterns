package additivepatterns.out;

import edu.lu.uni.serval.jdt.tree.ITree;

import java.util.*;

/**
 * This class defines a predicate and its new {@code newMaskedPredicates}.
 * @see BasePredicate
 */
public class MaskedPredicate extends BasePredicate {
    public transient final String javaFilePath;
    public final int lineNumber;
    private final int astStmtType;
    public final Set<String> newMaskedPredicates;

    public MaskedPredicate(String javaFilePath, ITree astTree, int lineNumber, int astStmtType) {
        super(astTree);
        this.javaFilePath = javaFilePath;
        this.lineNumber = lineNumber;
        this.astStmtType = astStmtType;
        this.newMaskedPredicates = new HashSet<>();
    }

    public void concatPredicates(String oldP, String newP, boolean withInverse) {
        if (withInverse) {
            newMaskedPredicates.add("(" + oldP + ") && !(" + newP + ")");
            newMaskedPredicates.add("(" + oldP + ") || !(" + newP + ")");
        }
        newMaskedPredicates.add("(" + oldP + ") && (" + newP + ")");
        newMaskedPredicates.add("(" + oldP + ") || (" + newP + ")");
    }

    public boolean hasMaskedPredicates(){
        return !newMaskedPredicates.isEmpty();
    }

    public void addMaskedPredicates(BasePredicate codePiece, String fileContent) {
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
        MaskedPredicate codePiece = (MaskedPredicate) o;
        return lineNumber == codePiece.lineNumber && astStmtType == codePiece.astStmtType && Objects.equals(javaFilePath, codePiece.javaFilePath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), javaFilePath, lineNumber, astStmtType);
    }
}
