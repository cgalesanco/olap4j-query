package es.cgalesanco.olap4j.query;

import org.olap4j.mdx.ParseTreeNode;
import org.olap4j.metadata.Member;

interface MemberSet {

	boolean containsAncestorOf(Member m);

	ParseTreeNode getMdx();

	boolean contains(Member m);

	boolean remove(Member m);
	
}
