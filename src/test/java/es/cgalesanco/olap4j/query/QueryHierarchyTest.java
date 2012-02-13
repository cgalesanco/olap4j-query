package es.cgalesanco.olap4j.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.StringWriter;
import java.util.Arrays;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.olap4j.OlapException;
import org.olap4j.impl.IdentifierParser;
import org.olap4j.mdx.ParseTreeNode;
import org.olap4j.mdx.ParseTreeWriter;
import org.olap4j.metadata.Cube;
import org.olap4j.metadata.Member;

import es.cgalesanco.olap4j.query.Selection.Operator;
import es.cgalesanco.olap4j.query.Selection.Sign;

public class QueryHierarchyTest {
	static private Cube cube;
	private QueryHierarchy qh;

	@BeforeClass
	static public void setUpFixture() throws Exception {
		cube = MetadataFixture.createCube();
	}

	@Before
	public void setUp() throws Exception {
		Query q = new Query("Hierarchy Test", cube);
		qh = q.getHierarchy("Time");
	}

	@Test
	public void testIsLeaf_leafHiearchyMember() throws Exception {
		Member from = cube.lookupMember(IdentifierParser
				.parseIdentifier("[Time].[1997].[Q1].[2].[1]"));
		qh.include(Operator.DESCENDANTS, from);

		assertTrue(qh.isLeaf(from));
	}

	@Test
	public void testIsLeaf_includesChildren_includesDescendants_noOverride()
			throws OlapException {
		Member from = qh.getHierarchy().getRootMembers().get(0);
		qh.include(Operator.DESCENDANTS, from);

		assertFalse(qh.isLeaf(from));
	}

	@Test
	public void testIsLeaf_includesChildren_includesDescendants_excludedChild()
			throws OlapException {
		Member from = qh.getHierarchy().getRootMembers().get(0);
		qh.include(Operator.DESCENDANTS, from);
		qh.exclude(Operator.MEMBER, from.getChildMembers().get(0));

		assertFalse(qh.isLeaf(from));
	}

	@Test
	public void testIsLeaf_includesChildren_includesDescendants_allChildExcluded()
			throws OlapException {
		Member from = qh.getHierarchy().getRootMembers().get(0);
		qh.include(Operator.DESCENDANTS, from);
		for (Member child : from.getChildMembers()) {
			qh.exclude(Operator.MEMBER, child);
		}

		assertFalse(qh.isLeaf(from));
	}

	@Test
	public void testIsLeaf_includesChildren_includesDescendants_allDescendantsExcluded()
			throws OlapException {
		Member from = qh.getHierarchy().getRootMembers().get(0);
		qh.include(Operator.DESCENDANTS, from);
		for (Member child : from.getChildMembers()) {
			qh.exclude(Operator.DESCENDANTS, child);
		}

		assertTrue(qh.isLeaf(from));
	}

	@Test
	public void testIsLeaf_excludesChildren_includesDescendants_noOverride()
			throws OlapException {
		Member from = qh.getHierarchy().getRootMembers().get(0);
		qh.include(Operator.DESCENDANTS, from);
		qh.exclude(Operator.CHILDREN, from);

		assertFalse(qh.isLeaf(from));
	}

	@Test
	public void testIsLeaf_excludesChildren_includesDescendants_overridingInclude()
			throws OlapException {
		Member from = qh.getHierarchy().getRootMembers().get(0);
		qh.include(Operator.DESCENDANTS, from);
		qh.exclude(Operator.CHILDREN, from);
		Member child = from.getChildMembers().get(0);
		qh.include(Operator.MEMBER, child);

		assertFalse(qh.isLeaf(from));
	}

	@Test
	public void testIsLeaf_excludesChildren_includesDescendants_overridingGrandsonInclude()
			throws OlapException {
		Member from = qh.getHierarchy().getRootMembers().get(0);
		qh.include(Operator.DESCENDANTS, from);
		qh.exclude(Operator.CHILDREN, from);
		Member child = from.getChildMembers().get(0);
		Member grandson = child.getChildMembers().get(0);
		qh.include(Operator.MEMBER, grandson);

		assertFalse(qh.isLeaf(from));
	}

	@Test
	public void testIsLeaf_excludesChildren_includesDescendants_noGrandson()
			throws OlapException {
		Member from = cube.lookupMember(IdentifierParser
				.parseIdentifier("[Time].[1997].[Q1].[1]"));
		qh.include(Operator.DESCENDANTS, from);
		qh.exclude(Operator.CHILDREN, from);

		assertTrue(qh.isLeaf(from));
	}

	@Test
	public void testIsLeaf_excludesChildren_excludesDescendants_noOverride()
			throws OlapException {
		Member from = qh.getHierarchy().getRootMembers().get(0);

		assertFalse(qh.isLeaf(from));
	}

	@Test
	public void testIsLeaf_excludesChildren_excludesDescendants_inclusionOverride()
			throws OlapException {
		Member from = qh.getHierarchy().getRootMembers().get(0);
		Member child = from.getChildMembers().get(0);
		qh.include(Operator.MEMBER, child);

		assertFalse(qh.isLeaf(from));
	}

	@Test
	public void testIsLeaf_excludesChildren_excludesDescendants_grandsonInclusionOverride()
			throws OlapException {
		Member from = qh.getHierarchy().getRootMembers().get(0);
		Member child = from.getChildMembers().get(0);
		Member grandson = child.getChildMembers().get(0);
		qh.include(Operator.MEMBER, grandson);

		assertFalse(qh.isLeaf(from));
	}

	@Test
	public void testIsLeaf_withinFullyIncludeSubtree() throws OlapException {
		Member from = qh.getHierarchy().getRootMembers().get(0);
		Member child = from.getChildMembers().get(0);
		Member grandson = child.getChildMembers().get(0);
		qh.include(Operator.DESCENDANTS, from);

		assertFalse(qh.isLeaf(grandson));
	}

	@Test
	public void testIsLeaf_withinFullyExcludedSubtree() throws OlapException {
		Member from = qh.getHierarchy().getRootMembers().get(0);
		Member child = from.getChildMembers().get(0);
		Member grandson = child.getChildMembers().get(0);
		qh.exclude(Operator.DESCENDANTS, from);

		assertTrue(qh.isLeaf(grandson));
	}

	@Test
	public void testFullHierarchy_initial() throws OlapException {
		selectAll();
		assertDrillExpression("{[Time].[1997], [Time].[1998]}", qh.toOlap4j());
	}

	@Test
	public void testChildrenMembers_initialStatus() throws OlapException {
		Member rootMember = qh.getHierarchy().getRootMembers().get(0);
		qh.include(Operator.CHILDREN, rootMember);

		ParseTreeNode expression = qh.toOlap4j();

		assertDrillExpression("%1$s.Children", expression, rootMember);
	}

	@Test
	public void testChildrenMembers_drill() throws OlapException {
		Member rootMember = qh.getHierarchy().getRootMembers().get(0);
		qh.include(Operator.CHILDREN, rootMember);
		Member childMember = rootMember.getChildMembers().get(0);

		ParseTreeNode expression = qh.toOlap4j(Arrays.asList(childMember));

		assertDrillExpression("%1$s.Children", expression, rootMember);
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

		assertFalse(qh.isIncluded(rootMember));
		assertTrue(qh.isExcluded(rootMember));
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

		assertFalse(qh.isIncluded(childMember));
		assertTrue(qh.isExcluded(childMember));

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

		assertFalse(qh.isIncluded(grandsonMember));
		assertTrue(qh.isExcluded(grandsonMember));
	}

	@Test
	public void testIsIncluded_includedGrandparentChildren()
			throws OlapException {
		Member rootMember = qh.getHierarchy().getRootMembers().get(0);
		Member childMember = rootMember.getChildMembers().get(0);
		Member grandsonMember = childMember.getChildMembers().get(0);
		qh.include(Operator.CHILDREN, rootMember);

		assertFalse(qh.isIncluded(grandsonMember));
		assertTrue(qh.isExcluded(grandsonMember));
	}

	@Test
	public void testIsIncluded_includedGrandparentDescendants()
			throws OlapException {
		Member rootMember = qh.getHierarchy().getRootMembers().get(0);
		Member childMember = rootMember.getChildMembers().get(0);
		Member grandsonMember = childMember.getChildMembers().get(0);
		qh.include(Operator.DESCENDANTS, rootMember);

		assertTrue(qh.isIncluded(grandsonMember));
		assertFalse(qh.isExcluded(grandsonMember));
	}

	private void selectAll() throws OlapException {
		for (Member m : qh.getHierarchy().getRootMembers()) {
			qh.apply(new SelectionAction(m, Sign.INCLUDE, Operator.DESCENDANTS));
		}
	}

	private void assertMdx(String expected, ParseTreeNode actual) {
		StringWriter swr = new StringWriter();
		ParseTreeWriter wr = new ParseTreeWriter(swr);
		actual.unparse(wr);
		assertEquals(expected, swr.toString());
	}

	private void assertDrillExpression(String format, ParseTreeNode expression,
			Member... args) {
		assertMdx(String.format(format, (Object[]) args), expression);
	}
}
