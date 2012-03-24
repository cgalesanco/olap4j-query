package es.cgalesanco.olap4j.query.mdx;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.olap4j.mdx.CallNode;
import org.olap4j.mdx.MemberNode;
import org.olap4j.mdx.ParseTreeNode;
import org.olap4j.mdx.Syntax;
import org.olap4j.metadata.Member;

/**
 * Helper class to create MDX Union expressions.
 */
public class UnionBuilder {
	public List<MemberNode> members;
	public List<ParseTreeNode> expressions;

	public void add(MemberNode n) {
		if (members == null) {
			members = new ArrayList<MemberNode>();
		}
		members.add(n);
	}

	public void add(ParseTreeNode n) {
		if (n == null)
			return;

		if (n instanceof MemberNode) {
			add((MemberNode) n);
			return;
		}

		if (n instanceof CallNode) {
			CallNode cn = (CallNode) n;
			if (cn.getSyntax() == Syntax.Braces) {
				List<MemberNode> membersList = new ArrayList<MemberNode>();
				for (ParseTreeNode arg : cn.getArgList()) {
					if (!(arg instanceof MemberNode)) {
						membersList = null;
					} else if (membersList != null) {
						membersList.add((MemberNode) arg);
					}
				}
				if (membersList != null) {
					if (members == null)
						members = membersList;
					else
						members.addAll(membersList);
					return;
				}
			}
		}

		if (expressions == null)
			expressions = new ArrayList<ParseTreeNode>();
		expressions.add(n);
	}

	public ParseTreeNode getUnionNode() {
		ParseTreeNode memberSet = null;
		if (members != null && !members.isEmpty()) {
			List<ParseTreeNode> args = new ArrayList<ParseTreeNode>();
			args.addAll(members);
			memberSet = new CallNode(null, "{}", Syntax.Braces, args);
		}

		ParseTreeNode setExpression = null;
		if (expressions != null) {
			for (ParseTreeNode e : expressions) {
				if (setExpression == null)
					setExpression = e;
				else
					setExpression = new CallNode(null, "Union",
							Syntax.Function, setExpression, e);
			}
		}

		if (memberSet == null)
			return setExpression;
		else if (setExpression == null)
			return memberSet;
		else
			return new CallNode(null, "Union", Syntax.Function, memberSet,
					setExpression);
	}

	static public ParseTreeNode fromMembers(Collection<Member> members) {
		if ( members == null || members.isEmpty() )
			return null;
		MemberNode[] args = new MemberNode[members.size()];
		int i = 0;
		for (Member m : members) {
			args[i++] = new MemberNode(null, m);
		}
		return new CallNode(null, "{}", Syntax.Braces, args);
	}

	public boolean isEmpty() {
		return (members == null || members.isEmpty())
				&& (expressions == null || expressions.isEmpty());
	}

	public void add(Collection<Member> members) {
		if ( members != null ) {
			if ( this.members == null )
				this.members = new ArrayList<MemberNode>();
			for(Member m : members) {
				this.members.add(new MemberNode(null, m));
			}
		}
	}
}
