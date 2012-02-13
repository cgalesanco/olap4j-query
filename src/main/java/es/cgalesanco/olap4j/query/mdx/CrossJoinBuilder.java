package es.cgalesanco.olap4j.query.mdx;

import java.util.ArrayList;
import java.util.List;

import org.olap4j.mdx.CallNode;
import org.olap4j.mdx.MemberNode;
import org.olap4j.mdx.ParseTreeNode;
import org.olap4j.mdx.Syntax;

/**
 * Helper class to create MDX Crossjoin expressions.
 */
public class CrossJoinBuilder {
	public ParseTreeNode expression;

	public void join(ParseTreeNode n) {
		if (n == null)
			return;
		
		MemberNode mExp = convertToMemberExpression(n);
		if (mExp != null )
			n = mExp;

		if (expression == null) {
			expression = n;
			return;
		}

		if ( expression instanceof MemberNode ) {
			if (n instanceof MemberNode ) {
				expression = new CallNode(null, "()", Syntax.Parentheses,
						expression, n);
			} else if (isTuple(n)) {
				CallNode cN = (CallNode) n;
				List<ParseTreeNode> args = new ArrayList<ParseTreeNode>();
				args.add(expression);
				args.addAll(cN.getArgList());
				expression = new CallNode(null, "()", Syntax.Parentheses, args);
			} else {
				expression = new CallNode(null, "CrossJoin", Syntax.Function,
						expression, n);
			}
		} else if (isTuple(expression)) {
			CallNode cExpression = (CallNode) expression;
			if (n instanceof MemberNode) {
				ParseTreeNode[] args = new ParseTreeNode[cExpression.getArgList().size()+1];
				cExpression.getArgList().toArray(args);
				args[args.length-1] = n;
				expression = new CallNode(null,"()",Syntax.Parentheses, args);
			} else if (isTuple(n)) {
				int originalSize = cExpression.getArgList().size();
				CallNode addNode = (CallNode) n;
				int addedSize = addNode.getArgList().size();
				ParseTreeNode[] args = new ParseTreeNode[originalSize+addedSize];
				for(int i = 0; i < addedSize; ++i )
					args[originalSize+i] = addNode.getArgList().get(i);
				expression = new CallNode(null,"()",Syntax.Parentheses, args);
			} else {
				expression = new CallNode(null, "CrossJoin", Syntax.Function,
						expression, n);
			}
		} else {
			expression = new CallNode(null, "CrossJoin", Syntax.Function,
					expression, n);
		}
	}

	private MemberNode convertToMemberExpression(ParseTreeNode expression) {
		if (expression instanceof MemberNode )
			return (MemberNode) expression;
		
		if ( expression instanceof CallNode ) {
			CallNode call = (CallNode) expression;
			if ( call.getSyntax() == Syntax.Braces && call.getArgList().size() == 1 )
				return (MemberNode) call.getArgList().get(0);
		}
		return null;
	}

	private static boolean isTuple(ParseTreeNode e) {
		return e instanceof CallNode
				&& ((CallNode) e).getSyntax() == Syntax.Parentheses;
	}

	public ParseTreeNode getJoinNode() {
		return expression;
	}

}
