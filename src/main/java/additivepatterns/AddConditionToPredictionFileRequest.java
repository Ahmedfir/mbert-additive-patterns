package additivepatterns;


import additivepatterns.AstParsing.AstParser;
import additivepatterns.AstParsing.AstParsingUtils;
import additivepatterns.AstParsing.TbarLoadPredicates;
import additivepatterns.out.CodePiece;
import edu.lu.uni.serval.entity.Pair;
import edu.lu.uni.serval.jdt.tree.ITree;
import edu.lu.uni.serval.tbar.context.ContextReader;
import edu.lu.uni.serval.tbar.utils.Checker;
import edu.lu.uni.serval.tbar.utils.FileHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AddConditionToPredictionFileRequest {

    private File javaFile;

    public AddConditionToPredictionFileRequest(File javaFile) {
        this.javaFile = javaFile;
    }


    public void generateMaskedPatches() {
        // string java file content.
        String fileContent = FileHelper.readFile(javaFile);
        // ast parsing.
        List<Pair<ITree, AstParser.AstNode>> ast = new AstParser().parseSuspiciousCode(javaFile);
        if (ast.isEmpty()) {
            System.err.println("Empty AST: " + javaFile);
        }
        // all file predicates
        Map<ITree, Integer> allPredicateExpressions = TbarLoadPredicates.identifyPredicateExpressions(ast.get(0).firstElement);

        List<CodePiece> allPathches = new ArrayList<>();
        // looping through the ast
        for (Pair<ITree, AstParser.AstNode> p : ast) {
            ITree scn = p.firstElement;
            AstParser.AstNode astScn = p.secondElement;

            // Parse context information of the ast node.
            List<Integer> contextInfoList = AstParsingUtils.readAllNodeTypes(scn);

            List<Integer> distinctContextInfo = new ArrayList<>();
            // looping through context info.
            for (Integer contInfo : contextInfoList) {
                if (!distinctContextInfo.contains(contInfo) && !Checker.isBlock(contInfo)) { // no redundancy + no blocks.
                    distinctContextInfo.add(contInfo);
                    // if + do + while + retrun boolean stmts.
                    if (Checker.isIfStatement(contInfo)
                            || Checker.isDoStatement(contInfo)
                            || Checker.isWhileStatement(contInfo)
                            || (Checker.isReturnStatement(contInfo) && "boolean".equalsIgnoreCase(ContextReader.readMethodReturnType(scn)))) {

                        AddConditionToPredictionMutator mutator = new AddConditionToPredictionMutator(scn, astScn.startLine, astScn.file.getPath());
                        mutator.generatePatches(allPredicateExpressions, fileContent);

                        allPathches.add(mutator.getTargetPredicate());

                    }
                }
            }

            for (CodePiece predicate : allPathches) {
                System.out.println(predicate.newMaskedPredicates.size() + " in line +" + predicate.lineNumber
                        + " -------- [ " + predicate.start + "," + predicate.end + "]");
                System.out.println("p b ==  " + predicate.getCodeString(fileContent));
                for (String maskedP : predicate.newMaskedPredicates) {
                    System.out.println("p m ==  " + maskedP);
                }
            }
            System.out.println("patches = " + allPathches.size());
        }
    }
}
