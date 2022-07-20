package additivepatterns.out;

import additivepatterns.out.BasePredicate;
import additivepatterns.out.MaskedPredicate;
import edu.lu.uni.serval.jdt.tree.ITree;
import edu.lu.uni.serval.tbar.context.ContextReader;
import edu.lu.uni.serval.tbar.utils.Checker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class AddConditionToPredictionMutator {


    List<String> triedExpCands = new ArrayList<>();
    private final MaskedPredicate targetMaskedPredicate;
    private final ITree suspStmtAst;

    public AddConditionToPredictionMutator(ITree suspStmtAst, int lineNumber, String javaFilePath) {
        ITree suspPredicateExp;
        int astStmtType = suspStmtAst.getType();
        if (Checker.isDoStatement(astStmtType)) {
            List<ITree> children = suspStmtAst.getChildren();
            suspPredicateExp = children.get(children.size() - 1);
        } else {// If, while, return statement.
            suspPredicateExp = suspStmtAst.getChild(0);
        }
        this.suspStmtAst = suspStmtAst;
        this.targetMaskedPredicate = new MaskedPredicate(javaFilePath, suspPredicateExp, lineNumber, astStmtType);
    }

    private boolean shouldSkipPredicate(String suspPredicateExpStr, String predicateExpCandidateStr) {
        return suspPredicateExpStr.contains(predicateExpCandidateStr) || predicateExpCandidateStr.equals(suspPredicateExpStr)
                || predicateExpCandidateStr.replaceAll("\\s", "").contains("==null")
                || predicateExpCandidateStr.replaceAll("\\s", "").contains("!=null")
                || triedExpCands.contains(predicateExpCandidateStr);
    }


    public void generatePatches(Map<ITree, Integer> allPredicateExpressions, String fileContent) {

        String targetPredicateStr = targetMaskedPredicate.getCodeString(fileContent);
        List<String> targetPredicateVars = targetMaskedPredicate.getVariables();

        for (Map.Entry<ITree, Integer> entry : allPredicateExpressions.entrySet()) {

            BasePredicate newPredicate = new BasePredicate(entry.getKey());
            String newPredicateStr = newPredicate.getCodeString(fileContent);
            if (shouldSkipPredicate(targetPredicateStr, newPredicateStr)) continue;
            triedExpCands.add(newPredicateStr);

            List<String> newPredicateVars = newPredicate.getVariables();
            newPredicateVars.retainAll(targetPredicateVars);
            if (newPredicateVars.isEmpty()) continue;
            targetMaskedPredicate.addMaskedPredicates(newPredicate, fileContent);
        }

        if (!Checker.isIfStatement(suspStmtAst.getType())) return;

        List<String> vars = new ArrayList<>();
        ContextReader.identifySuspiciousVariables(suspStmtAst, null, vars);
        if (vars.isEmpty()) return;
        // todo do we really need to redo this for every predicate separately or we can do it once.
        Map<String, List<String>> allVarNamesMap = new HashMap<>();
        Map<String, String> varTypesMap = new HashMap<>();
        List<String> allVarNamesList = new ArrayList<>();

        // todo pass a dictionary to win some performance - use a new version of Tbar.
        ContextReader.readAllVariablesAndFields(suspStmtAst, allVarNamesMap, varTypesMap, allVarNamesList, targetMaskedPredicate.javaFilePath);

        for (String var : vars) {
            String varType = varTypesMap.get(var);
            if (varType == null) continue;
            List<String> varList = allVarNamesMap.get(varType);
            varList.remove(var);
            if (varList.isEmpty()) continue;
            String[] operators = selectOperators(varType);
            targetMaskedPredicate.addMaskedVarsOpsPredicates(var, varList, operators, fileContent);
        }
    }


    public MaskedPredicate getTargetPredicate() {
        return targetMaskedPredicate;
    }


    private String[] selectOperators(String varType) {
        if ("byte".equals(varType) || "Byte".equals(varType)
                || "short".equals(varType) || "Short".equals(varType)
                || "int".equals(varType) || "Integer".equals(varType)
                || "long".equals(varType) || "Long".equals(varType)
                || "double".equals(varType) || "Double".equals(varType)
                || "float".equals(varType) || "Float".equals(varType)) {
            return new String[]{" != ", " == ", " < ", " <= ", " > ", " >= "};
        }
        return new String[]{" != ", " == "};
    }
}
