package additivepatterns.out;


import additivepatterns.AstParsing.AstParser;
import additivepatterns.AstParsing.AstParsingUtils;
import additivepatterns.AstParsing.TbarLoadPredicates;
import edu.lu.uni.serval.entity.Pair;
import edu.lu.uni.serval.jdt.tree.ITree;
import edu.lu.uni.serval.tbar.context.ContextReader;
import edu.lu.uni.serval.tbar.utils.Checker;
import edu.lu.uni.serval.tbar.utils.FileHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.Serializable;
import java.util.*;

public class AddConditionToPredictionFileRequest implements Serializable {

    private transient static Logger log = LoggerFactory.getLogger(AstParser.class);
    private static final boolean FAIL_ON_EMPTY_AST = Boolean.getBoolean("fail_empty_ast");

    private final File javaFile;
    private final Set<MaskedPredicate> allMaskedPredicates;

    public AddConditionToPredictionFileRequest(File javaFile) {
        this.javaFile = javaFile;
        this.allMaskedPredicates = new HashSet<>();
    }

    public Set<MaskedPredicate> getAllMaskedPredicates() {
        return allMaskedPredicates;
    }

    public void generateMaskedPatches() {
        log.info("attempt to generate masked predicates from " + javaFile);
        // string java file content.
        String fileContent = FileHelper.readFile(javaFile);
        // ast parsing.
        List<Pair<ITree, AstParser.AstNode>> ast = new AstParser().parseSuspiciousCode(javaFile);
        if (ast.isEmpty()) {
            String msg = "Empty AST: " + javaFile;
            IllegalArgumentException exception = new IllegalArgumentException(msg);
            if (FAIL_ON_EMPTY_AST){
                throw exception;
            } else {
                System.err.println(msg);
                return;
            }
        }
        // all file predicates
        Map<ITree, Integer> allPredicateExpressions = TbarLoadPredicates.identifyPredicateExpressions(ast.get(0).firstElement);


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
                    // if + do + while + retrun boolean stmts. --> add conditions.
                    if (Checker.isIfStatement(contInfo)
                            || Checker.isDoStatement(contInfo)
                            || Checker.isWhileStatement(contInfo)
                            || (Checker.isReturnStatement(contInfo) && "boolean".equalsIgnoreCase(ContextReader.readMethodReturnType(scn)))) {

                        AddConditionToPredictionMutator mutator = new AddConditionToPredictionMutator(scn, astScn.startLine, astScn.file.getPath());
                        mutator.generatePatches(allPredicateExpressions, fileContent);
                        MaskedPredicate pred = mutator.getTargetPredicate();
                        if (pred.hasMaskedPredicates()) {
                            allMaskedPredicates.add(pred);
                        }
                    }
                    //
                    //RemovingMutator removingMutator = new RemovingMutator(scn, astScn.startLine, astScn.file.getPath());
                }
            }

        }

        log.info("finished: " + allMaskedPredicates.size() + " predicates will be mutated in " + javaFile);
        if (log.isTraceEnabled()) {
            for (MaskedPredicate maskedPredicate : allMaskedPredicates) {
                log.trace(maskedPredicate.newMaskedPredicates.size() + " in line +" + maskedPredicate.lineNumber
                        + " -------- [ " + maskedPredicate.start + "," + maskedPredicate.end + "]");
                log.trace("p b ==  " + maskedPredicate.getCodeString(fileContent));
                for (String maskedP : maskedPredicate.newMaskedPredicates) {
                    log.trace("p m ==  " + maskedP);
                }
            }
        }
    }
}
