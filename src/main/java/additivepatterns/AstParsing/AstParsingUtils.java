package additivepatterns.AstParsing;

import edu.lu.uni.serval.jdt.tree.ITree;
import edu.lu.uni.serval.tbar.utils.Checker;

import java.util.ArrayList;
import java.util.List;

/**
 * @see {edu.lu.uni.serval.ibir.utils.AstParsingUtils}
 */
public final class AstParsingUtils {

    private AstParsingUtils(){
        throw new IllegalArgumentException("utility class: static access only.");
    }


    public static List<Integer> readAllNodeTypes(ITree suspCodeAstNode) {
        List<Integer> nodeTypes = new ArrayList<>();
        nodeTypes.add(suspCodeAstNode.getType());
        List<ITree> children = suspCodeAstNode.getChildren();
        for (ITree child : children) {
            int childType = child.getType();
            if (Checker.isFieldDeclaration(childType) ||
                    Checker.isMethodDeclaration(childType) ||
                    Checker.isTypeDeclaration(childType) ||
                    Checker.isStatement(childType)) break;
            nodeTypes.addAll(readAllNodeTypes(child));
        }
        return nodeTypes;
    }

}
