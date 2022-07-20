package additivepatterns.AstParsing;

import edu.lu.uni.serval.jdt.tree.ITree;
import edu.lu.uni.serval.tbar.context.ContextReader;
import edu.lu.uni.serval.tbar.utils.Checker;

import java.util.*;

/**
 *  @see {https://github.com/TruX-DTF/TBar/blob/d1b15552b5622d36cbf32cf1f0801ced207d73da/src/main/java/edu/lu/uni/serval/tbar/fixpatterns/ChangeCondition.java}
 */
public class TbarLoadPredicates {

    private static boolean ignoreOtherMethods = false; // FIXME do not ignore other methods.

    /**
     * @return Map<ITree, Integer>: ITree - Predicate Exp AST, Integer - distance to suspicious stmt.
     * @see {https://github.com/TruX-DTF/TBar/blob/d1b15552b5622d36cbf32cf1f0801ced207d73da/src/main/java/edu/lu/uni/serval/tbar/fixpatterns/ChangeCondition.java}
     */
    public static Map<ITree, Integer> identifyPredicateExpressions(ITree suspStmtAst) {
        Map<ITree, Integer> predicateExps = new HashMap<>();
        ITree parent = suspStmtAst.getParent();
        int suspIndex = parent.getChildPosition(suspStmtAst);
        List<ITree> peerStmts = parent.getChildren();
        int size = peerStmts.size();

        for (int index = 0; index < suspIndex; index++) {
            ITree peerStmt = peerStmts.get(index);
            predicateExps.putAll(identifyPredicateExpressions(peerStmt, 1, true));
        }

        List<ITree> children = suspStmtAst.getChildren();
        for (ITree child : children) {
            if (Checker.isStatement(child.getType())) {
                predicateExps.putAll(identifyPredicateExpressions(child, 1, false));
            }
        }

        for (int index = suspIndex; index < size; index++) {
            ITree peerStmt = peerStmts.get(index);
            predicateExps.putAll(identifyPredicateExpressions(peerStmt, 1, false));
        }

        identifyPredicateExpressionsInParentTree(parent, 1, predicateExps);

        return sortByValueAscending(predicateExps);
    }

    /**
     * @see {https://github.com/TruX-DTF/TBar/blob/d1b15552b5622d36cbf32cf1f0801ced207d73da/src/main/java/edu/lu/uni/serval/tbar/fixpatterns/ChangeCondition.java}
     */
    private static Map<ITree, Integer> identifyPredicateExpressions(ITree codeAst, int distance, boolean considerVarDec) {
        Map<ITree, Integer> predicateExps = new HashMap<>();
        List<String> varNames = new ArrayList<>();
        int codeAstType = codeAst.getType();
        List<ITree> children = codeAst.getChildren();
        int size = children.size();
        if (Checker.isStatement(codeAstType)) {
            if (Checker.isDoStatement(codeAstType)) {
                ITree exp = children.get(size - 1);
                identifySinglePredicateExpressions(exp, distance + 1, predicateExps, varNames);
                for (int index = 0; index < size - 1; index++) {
                    ITree child = children.get(index);
                    predicateExps.putAll(identifyPredicateExpressions(child, distance + 1, !Checker.isStatement(child.getType())));
                }
            } else if (Checker.isIfStatement(codeAstType) || Checker.isWhileStatement(codeAstType)) {
                ITree exp = children.get(0);
                identifySinglePredicateExpressions(exp, distance + 1, predicateExps, varNames);
                for (int index = 1; index < size; index++) {
                    ITree child = children.get(index);
                    predicateExps.putAll(identifyPredicateExpressions(child, distance + 1, !Checker.isStatement(child.getType())));
                }
            } else {// Other Statements.
                if (Checker.isVariableDeclarationStatement(codeAstType) && !considerVarDec) {
                    // get the variable name.
                    String varName = identifyVariableName(codeAst);
                    varNames.add(varName);
                }
                for (ITree child : children) {
                    predicateExps.putAll(identifyPredicateExpressions(child, distance + 1, !Checker.isStatement(child.getType())));
                }
            }
        } else if (Checker.isComplexExpression(codeAstType)) {
            if (Checker.isConditionalExpression(codeAstType)) {// ConditionalExpression
                identifySinglePredicateExpressions(codeAst.getChild(0), distance + 1, predicateExps, varNames);
            }
            for (ITree child : children) {
                predicateExps.putAll(identifyPredicateExpressions(child, distance + 1, false));
            }
        }

        return predicateExps;
    }


    /**
     * @see {edu.lu.uni.serval.tbar.context.ContextReader#identifyVariableName}
     */
    public static String identifyVariableName(ITree stmtAst) {
        List<ITree> children = stmtAst.getChildren();
        int stmtAstType = stmtAst.getType();
        if (Checker.isVariableDeclarationStatement(stmtAstType)) {
            for (int index = 0, size = children.size(); index < size; index ++) {
                if (!Checker.isModifier(children.get(index).getType())) {
                    return children.get(index + 1).getChild(0).getLabel();
                }
            }
        } else if (Checker.isExpressionStatement(stmtAstType)) {
            return children.get(0).getChild(0).getLabel();
        } else if (Checker.isSingleVariableDeclaration(stmtAstType)) {
            for (int index = 0, size = children.size(); index < size; index ++) {
                if (!Checker.isModifier(children.get(index).getType())) {
                    return children.get(index + 1).getLabel();
                }
            }
        }

        return null;
    }


    private static void identifySinglePredicateExpressions(ITree expAst, int distance, Map<ITree, Integer> predicateExps, List<String> varNames) {
        if (Checker.isInfixExpression(expAst.getType())) { // InfixExpression
            String operator = expAst.getChild(1).getLabel();
            if ("||".equals(operator) || "&&".equals(operator)) {
                identifySinglePredicateExpressions(expAst.getChild(0), distance + 1, predicateExps, varNames);
                List<ITree> children = expAst.getChildren();
                int size = children.size();
                for (int index = 2; index < size; index ++) {
                    identifySinglePredicateExpressions(children.get(index), distance + 1, predicateExps, varNames);
                }
            } else {
                if (!ContextReader.containsVar(expAst, varNames)) {
                    predicateExps.put(expAst, distance);
                }
            }
//		} else { // if (Checker.isValidExpression(expAst.getType()))
//			if (this.containsVar(expAst, varNames)) {
//				predicateExps.put(expAst, distance);
//			}
        }
        if (!ContextReader.containsVar(expAst, varNames)) {
            predicateExps.put(expAst, distance);
        }
    }

    private static <K, V extends Comparable<? super V>> Map<K, V> sortByValueAscending(Map<K, V> unsortMap) {

        // 1. Convert Map to List of Map
        List<Map.Entry<K, V>> list =
                new LinkedList<Map.Entry<K, V>>(unsortMap.entrySet());

        // 2. Sort list with Collections.sort(), provide a custom Comparator
        //    Try switch the o1 o2 position for a different order
        Collections.sort(list, new Comparator<Map.Entry<K, V>>() {
            public int compare(Map.Entry<K, V> o1,
                               Map.Entry<K, V> o2) {
                return o1.getValue().compareTo(o2.getValue());
            }
        });

        // 3. Loop the sorted list and put it into a new insertion order Map LinkedHashMap
        Map<K, V> sortedMap = new LinkedHashMap<K, V>();
        for (Map.Entry<K, V> entry : list) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        return sortedMap;
    }


    private static void identifyPredicateExpressionsInParentTree(ITree tree, int distance, Map<ITree, Integer> predicateExps) {
        int treeType = tree.getType();
        if (Checker.isTypeDeclaration(treeType)) {
        } else if (Checker.isMethodDeclaration(treeType) && !ignoreOtherMethods) {
            ITree parent = tree.getParent();
            int suspIndex = parent.getChildPosition(tree);
            List<ITree> peerMethods = parent.getChildren();
            int size = peerMethods.size();

            for (int index = 0; index < size; index ++) {
                if (index != suspIndex) {
                    List<ITree> children = peerMethods.get(index).getChildren();
                    for (ITree child : children) {
                        if (Checker.isStatement(child.getType())) {
                            predicateExps.putAll(identifyPredicateExpressions(child, distance + 1, false));
                        }
                    }
                }
            }
            identifyPredicateExpressionsInParentTree(tree.getParent(), distance + 1, predicateExps);
        } else {
            ITree parent = tree.getParent();
            if (parent == null) return;
            int suspIndex = parent.getChildPosition(tree);
            List<ITree> peerStmts = parent.getChildren();
            int size = peerStmts.size();

            for (int index = 0; index < suspIndex; index ++) {
                ITree peerStmt = peerStmts.get(index);
                if (Checker.isStatement(peerStmt.getType())) {
                    predicateExps.putAll(identifyPredicateExpressions(peerStmt, distance + 1, true));
                }
            }
            List<ITree> children = tree.getChildren();
            for (ITree child : children) {
                if (!Checker.isStatement(child.getType())) {
                    predicateExps.putAll(identifyPredicateExpressions(child, distance + 1, false));
                }
            }
            for (int index = suspIndex; index < size; index ++) {
                ITree peerStmt = peerStmts.get(index);
                if (Checker.isStatement(peerStmt.getType())) {
                    predicateExps.putAll(identifyPredicateExpressions(peerStmt, distance + 1, false));
                }
            }
            identifyPredicateExpressionsInParentTree(parent, distance + 1, predicateExps);
        }
    }


}
