package additivepatterns.out;

import edu.lu.uni.serval.jdt.tree.ITree;
import edu.lu.uni.serval.tbar.context.ContextReader;
import edu.lu.uni.serval.tbar.utils.Checker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class RemovingMutator {


    public void generatePatches(ITree suspStmtAst, int lineNumber, String javaFilePath, String fileContent) {

        int astStmtType = suspStmtAst.getType();

        if (Checker.isSynchronizedStatement(astStmtType)) {
            List<ITree> children = suspStmtAst.getChildren();
            ITree firstStatement = null;
            ITree lastStatement = null;
            for (ITree child : children) {
                if (Checker.isStatement(child.getType())) {
                    if (firstStatement == null) firstStatement = child;
                    lastStatement = child;
                }
            }
            if (firstStatement == null) return;
            int startPos = firstStatement.getPos();
            int endPos = lastStatement.getPos() + lastStatement.getLength();
            String code = this.getSubSuspiciouCodeStr(startPos, endPos);
            this.generatePatch(code);
//		} else {
//			ITree parent = suspCodeTree.getParent();
//			if (Checker.isSynchronizedStatement(parent.getType())) {
//
//			}
        } else if (Checker.isIfStatement(suspCodeTree.getType())) {
            /*
             * FB	UCFUselessControlFlow
             *
             * Fix Pattern:
             * 1. DEL IfStatement@...
             * 2. DEL SwithStatement@...
             *
             */
            int endPos1 = 0;
            List<ITree> children = this.getSuspiciousCodeTree().getChildren();
            int size = children.size();
            ITree lastChild = children.get(size - 1);

            if ("ElseBody".equals(lastChild.getLabel())) {
                // Remove the control flow, but keep the statements in else block.
                List<ITree> subChildren = lastChild.getChildren();
                if (!subChildren.isEmpty()) {
                    endPos1 = subChildren.get(0).getPos();
                    ITree lastStmt = subChildren.get(subChildren.size() - 1);
                    int endPos2 = lastStmt.getPos() + lastStmt.getLength();

                    String fixedCodeStr1 = this.getSubSuspiciouCodeStr(endPos1, endPos2);
                    this.generatePatch(fixedCodeStr1);
                }
                lastChild = children.get(size - 2);
            }
            if ("ThenBody".equals(lastChild.getLabel())) {// Then block
                // Remove the control flow, but keep the statements in then block.
                List<ITree> subChildren = lastChild.getChildren();
                if (!subChildren.isEmpty()) {
                    endPos1 = subChildren.get(0).getPos();
                    ITree lastStmt = subChildren.get(subChildren.size() - 1);
                    int endPos2 = lastStmt.getPos() + lastStmt.getLength();

                    String fixedCodeStr1 = this.getSubSuspiciouCodeStr(endPos1, endPos2);
                    this.generatePatch(fixedCodeStr1);
                }
            }

            if (endPos1 == 0) {
                // No Statement in the control flow.
                return;
            }
        }

        if (!Checker.isReturnStatement(stmtType) && !Checker.isContinueStatement(stmtType)
                && !Checker.isBreakStatement(stmtType) && !Checker.isSwitchCase(stmtType)) {
            this.generatePatch("");
        }

        if (!parentMethodRemovingAllowed){
            return;
        }

        /*
         * FB	UPMUncalledPrivateMethod
         *
         * Fix Pattern:
         * DEL MethodDeclaration@...
         *
         */
        ITree parentTree = suspCodeTree;
        while (true) {
            if (Checker.isMethodDeclaration(parentTree.getType())) break;
            parentTree = parentTree.getParent();
            if (parentTree == null) break;
        }
        if (parentTree == null) return;

        int startPos = parentTree.getPos();
        int endPos = startPos + parentTree.getLength();
        this.generatePatch(startPos, endPos, "", "");

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
