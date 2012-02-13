package es.cgalesanco.olap4j.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.StringWriter;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.olap4j.Axis;
import org.olap4j.OlapException;
import org.olap4j.mdx.ParseTreeNode;
import org.olap4j.mdx.ParseTreeWriter;
import org.olap4j.metadata.Cube;
import org.olap4j.metadata.Member;

import es.cgalesanco.olap4j.query.Selection.Operator;

public class FilterHierarchyTest {
	static private Cube cube;
	private QueryHierarchy qh;

	@BeforeClass
	static public void setUpFixture() throws Exception {
		cube = MetadataFixture.createCube();
	}

	@Before
	public void setUp() {
		Query q = new Query("Hierarchy Test", cube);
		qh = q.getHierarchy("Time");
		q.getAxis(Axis.FILTER).addHierarchy(qh);
	}
	
	@Test
	public void testFullHierarchy() throws Exception {
		Member rootMember = qh.getHierarchy().getRootMembers().get(0);
		qh.include(Operator.DESCENDANTS, rootMember);
		
		assertDrillExpression("{%1$s}", rootMember);
	}

	@Test
	public void testIncludeRoot_ExcludeChild() throws Exception {
		Member rootMember = qh.getHierarchy().getRootMembers().get(0);
		Member childMember = rootMember.getChildMembers().get(0);
		qh.include(Operator.DESCENDANTS, rootMember);
		qh.exclude(Operator.DESCENDANTS, childMember);
		
		assertDrillExpression("Except(%1$s.Children, {%2$s})", rootMember, childMember);
	}
	
	@Test
	public void testIncludeRoot_ExcludeChild_includeGrandson() throws Exception {
		Member rootMember = qh.getHierarchy().getRootMembers().get(0);
		Member childMember = rootMember.getChildMembers().get(0);
		Member grandsonMember = childMember.getChildMembers().get(0);
		qh.include(Operator.DESCENDANTS, rootMember);
		qh.exclude(Operator.DESCENDANTS, childMember);
		qh.include(Operator.DESCENDANTS, grandsonMember);
		
		assertDrillExpression("Union({%3$s}, Except(%1$s.Children, {%2$s}))", rootMember, childMember, grandsonMember);
	}
	
	@Test
	public void testIsIncluded_includedMember() throws OlapException {
		Member rootMember = qh.getHierarchy().getRootMembers().get(0);
		qh.include(Operator.MEMBER, rootMember);
		
		assertTrue(qh.isIncluded(rootMember));
		assertFalse(qh.isExcluded(rootMember));
		
	}

	@Test
	public void testIsIncluded_includedChildren() throws OlapException {
		Member rootMember = qh.getHierarchy().getRootMembers().get(0);
		qh.include(Operator.CHILDREN, rootMember);
		
		// It's included because every child is included
		assertTrue(qh.isIncluded(rootMember));
		assertFalse(qh.isExcluded(rootMember));
	}
	
	@Test
	public void testIsIncluded_includedDescendants() throws OlapException {
		Member rootMember = qh.getHierarchy().getRootMembers().get(0);
		qh.include(Operator.DESCENDANTS, rootMember);
		
		assertTrue(qh.isIncluded(rootMember));
		assertFalse(qh.isExcluded(rootMember));
	}
	
	@Test
	public void testIsIncluded_includedParent() throws OlapException {
		Member rootMember = qh.getHierarchy().getRootMembers().get(0);
		Member childMember = rootMember.getChildMembers().get(0);
		qh.include(Operator.MEMBER, rootMember);
		
		// Including a member we're including every descendant
		assertTrue(qh.isIncluded(childMember));
		assertFalse(qh.isExcluded(childMember));
		
	}

	@Test
	public void testIsIncluded_includedParentChildren() throws OlapException {
		Member rootMember = qh.getHierarchy().getRootMembers().get(0);
		Member childMember = rootMember.getChildMembers().get(0);
		qh.include(Operator.CHILDREN, rootMember);
		
		assertTrue(qh.isIncluded(childMember));
		assertFalse(qh.isExcluded(childMember));
	}
	
	@Test
	public void testIsIncluded_includedParentDescendants() throws OlapException {
		Member rootMember = qh.getHierarchy().getRootMembers().get(0);
		Member childMember = rootMember.getChildMembers().get(0);
		qh.include(Operator.DESCENDANTS, rootMember);
		
		assertTrue(qh.isIncluded(childMember));
		assertFalse(qh.isExcluded(childMember));
	}
	
	@Test
	public void testIsIncluded_includedGrandparent() throws OlapException {
		Member rootMember = qh.getHierarchy().getRootMembers().get(0);
		Member childMember = rootMember.getChildMembers().get(0);
		Member grandsonMember = childMember.getChildMembers().get(0);
		qh.include(Operator.MEMBER, rootMember);
		
		assertTrue(qh.isIncluded(grandsonMember));
		assertFalse(qh.isExcluded(grandsonMember));
	}
	
	@Test
	public void testIsIncluded_includedGrandparentChildren() throws OlapException {
		Member rootMember = qh.getHierarchy().getRootMembers().get(0);
		Member childMember = rootMember.getChildMembers().get(0);
		Member grandsonMember = childMember.getChildMembers().get(0);
		qh.include(Operator.CHILDREN, rootMember);
		
		assertTrue(qh.isIncluded(grandsonMember));
		assertFalse(qh.isExcluded(grandsonMember));
	}
	
	@Test
	public void testIsIncluded_includedGrandparentDescendants() throws OlapException {
		Member rootMember = qh.getHierarchy().getRootMembers().get(0);
		Member childMember = rootMember.getChildMembers().get(0);
		Member grandsonMember = childMember.getChildMembers().get(0);
		qh.include(Operator.DESCENDANTS, rootMember);
		
		assertTrue(qh.isIncluded(grandsonMember));
		assertFalse(qh.isExcluded(grandsonMember));
	}
	

	
	private void assertMdx(String expected, ParseTreeNode actual) {
		StringWriter swr = new StringWriter();
		ParseTreeWriter wr = new ParseTreeWriter(swr);
		actual.unparse(wr);
		assertEquals(expected, swr.toString());
	}

	private void assertDrillExpression(String format, Member...args) {
		assertMdx(String.format(format, (Object[])args), qh.toOlap4j());
	}

}
