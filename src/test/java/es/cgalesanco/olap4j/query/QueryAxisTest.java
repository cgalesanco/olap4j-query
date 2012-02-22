package es.cgalesanco.olap4j.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.StringWriter;
import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.olap4j.Axis;
import org.olap4j.OlapException;
import org.olap4j.mdx.AxisNode;
import org.olap4j.mdx.ParseTreeNode;
import org.olap4j.mdx.ParseTreeWriter;
import org.olap4j.metadata.Cube;
import org.olap4j.metadata.Member;

import es.cgalesanco.olap4j.query.Selection.Operator;

public class QueryAxisTest {
	private static Cube cube;
	private Query query;

	@BeforeClass
	static public void setUpFixture() throws Exception {
		cube = MetadataFixture.createCube();
	}

	@Before
	public void setUp() {
		query = new Query("QueryAxis test query", cube);
	}

	/**
	 * Tests initial state of a {@link QueryAxis}.
	 */
	@Test
	public void testConstructor() {
		Axis location = Axis.ROWS;
		// Let Query call the constructor
		QueryAxis axis = query.getAxis(location);

		assertFalse(axis.isNonEmpty());
		assertEquals(location, axis.getLocation());
		assertSame(query, axis.getQuery());
		assertTrue(axis.getHierarchies().isEmpty());
		assertTrue(axis.listDrills().isEmpty());
	}

	@Test
	public void testAddHierarchy_unused() {
		QueryAxis axis = query.getAxis(Axis.ROWS);

		QueryHierarchy addedHierarchy = query.getHierarchy("Measures");
		axis.addHierarchy(addedHierarchy);

		assertEquals(1, axis.getHierarchies().size());
		assertSame(addedHierarchy, axis.getHierarchies().get(0));
		assertEquals(axis, addedHierarchy.getAxis());

		assertFalse(query.getUnusedAxis().getHierarchies()
				.contains(addedHierarchy));
	}

	@Test
	public void testAddHierarchy_inOtherAxis() {
		QueryAxis currentAxis = query.getAxis(Axis.COLUMNS);
		QueryHierarchy addedHierarchy = query.getHierarchy("Measures");
		currentAxis.addHierarchy(addedHierarchy);

		QueryAxis targetAxis = query.getAxis(Axis.ROWS);
		targetAxis.addHierarchy(addedHierarchy);

		assertEquals(0, currentAxis.getHierarchies().size());
		assertEquals(1, targetAxis.getHierarchies().size());
		assertTrue(targetAxis.getHierarchies().contains(addedHierarchy));
		assertEquals(targetAxis, addedHierarchy.getAxis());

		assertFalse(query.getUnusedAxis().getHierarchies()
				.contains(addedHierarchy));

	}

	@Test
	public void testAddHierarchy_alreadyExisting() {
		QueryAxis currentAxis = query.getAxis(Axis.COLUMNS);
		QueryHierarchy addedHierarchy = query.getHierarchy("Measures");
		currentAxis.addHierarchy(addedHierarchy);

		currentAxis.addHierarchy(addedHierarchy);

		assertEquals(1, currentAxis.getHierarchies().size());
		assertTrue(currentAxis.getHierarchies().contains(addedHierarchy));
		assertEquals(currentAxis, addedHierarchy.getAxis());

		assertFalse(query.getUnusedAxis().getHierarchies()
				.contains(addedHierarchy));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testAddHierarchy_fromOtherQuery() {
		Query otherQuery = new Query("Other", cube);
		QueryHierarchy addedHierarchy = otherQuery.getHierarchy("Measures");

		QueryAxis currentAxis = query.getAxis(Axis.COLUMNS);
		currentAxis.addHierarchy(addedHierarchy);
	}

	@Test
	public void testAddHierarchy_usingOtherHierarchySameDimension() {
		QueryAxis currentAxis = query.getAxis(Axis.COLUMNS);
		QueryHierarchy currentHierarchy = query.getHierarchy("Time");
		currentAxis.addHierarchy(currentHierarchy);

		QueryHierarchy addedHierarchy = query.getHierarchy("Time.Weekly");
		currentAxis.addHierarchy(addedHierarchy);

		assertEquals(1, currentAxis.getHierarchies().size());
		assertTrue(currentAxis.getHierarchies().contains(addedHierarchy));
		assertEquals(currentAxis, addedHierarchy.getAxis());

		assertTrue(query.getUnusedAxis().getHierarchies()
				.contains(currentHierarchy));
	}
	
	@Test
	public void testFilterAxisMDX() throws OlapException {
		QueryAxis currentAxis = query.getAxis(Axis.FILTER);
		QueryHierarchy currentHierarchy = query.getHierarchy("Time");
		currentAxis.addHierarchy(currentHierarchy);

		currentHierarchy.include(Operator.DESCENDANTS, currentHierarchy.getHierarchy().getRootMembers().get(0));
		
		AxisNode node = currentAxis.toOlap4j();
		assertEquals(Axis.FILTER, node.getAxis());
	}

	@Test
	public void testRemove() {
		QueryAxis currentAxis = query.getAxis(Axis.COLUMNS);
		QueryHierarchy currentHierarchy = query.getHierarchy("Time");
		currentAxis.addHierarchy(currentHierarchy);

		currentAxis.removeHierarchy(currentHierarchy);

		assertTrue(currentAxis.getHierarchies().isEmpty());
		assertTrue(query.getUnusedAxis().getHierarchies()
				.contains(currentHierarchy));
	}

	@Test
	public void testPullUp() {
		QueryAxis currentAxis = query.getAxis(Axis.COLUMNS);
		currentAxis.addHierarchy(query.getHierarchy("Time"));
		currentAxis.addHierarchy(query.getHierarchy("Gender"));

		currentAxis.pullUp(1);

		assertEquals(query.getHierarchy("Gender"), currentAxis.getHierarchies()
				.get(0));
		assertEquals(query.getHierarchy("Time"), currentAxis.getHierarchies()
				.get(1));
	}

	@Test
	public void testPullUp_invalidIndex() {
		QueryAxis currentAxis = query.getAxis(Axis.COLUMNS);
		currentAxis.addHierarchy(query.getHierarchy("Time"));
		currentAxis.addHierarchy(query.getHierarchy("Gender"));

		currentAxis.pullUp(0);

		assertEquals(query.getHierarchy("Time"), currentAxis.getHierarchies()
				.get(0));
		assertEquals(query.getHierarchy("Gender"), currentAxis.getHierarchies()
				.get(1));
	}

	@Test
	public void testPushDown() {
		QueryAxis currentAxis = query.getAxis(Axis.COLUMNS);
		currentAxis.addHierarchy(query.getHierarchy("Time"));
		currentAxis.addHierarchy(query.getHierarchy("Gender"));

		currentAxis.pushDown(0);

		assertEquals(query.getHierarchy("Gender"), currentAxis.getHierarchies()
				.get(0));
		assertEquals(query.getHierarchy("Time"), currentAxis.getHierarchies()
				.get(1));
	}

	@Test
	public void testPushDown_invalidIndex() {
		QueryAxis currentAxis = query.getAxis(Axis.COLUMNS);
		currentAxis.addHierarchy(query.getHierarchy("Time"));
		currentAxis.addHierarchy(query.getHierarchy("Gender"));

		currentAxis.pushDown(1);

		assertEquals(query.getHierarchy("Time"), currentAxis.getHierarchies()
				.get(0));
		assertEquals(query.getHierarchy("Gender"), currentAxis.getHierarchies()
				.get(1));
	}

	@Test
	public void testAxisExpression_twoHierarchies_drillRightMember()
			throws Exception {
		QueryAxis currentAxis = query.getAxis(Axis.COLUMNS);
		QueryHierarchy h1 = query.getHierarchy("Time");
		currentAxis.addHierarchy(h1);
		QueryHierarchy h2 = query.getHierarchy("Gender");
		currentAxis.addHierarchy(h2);

		Member h1Root = h1.getHierarchy().getRootMembers().get(0);
		h1.include(Operator.DESCENDANTS, h1Root);
		Member h1Child = h1Root.getChildMembers().get(0);
		Member h2Root = h2.getHierarchy().getRootMembers().get(0);
		h2.include(Operator.DESCENDANTS, h2Root);

		currentAxis.drill(h1Root);
		currentAxis.drill(h1Child, h2Root);

		AxisNode node = currentAxis.toOlap4j();

		assertMdx(
				String.format(
						"Hierarchize("+
							"Union("+
								"CrossJoin(" +
									"Except(DrilldownMember({%1$s}, {%1$s}, RECURSIVE), {%3$s}), " +
									"%2$s" +
								"), " +
								"CrossJoin(" +
									"%3$s, " +
									"DrilldownMember({%2$s}, {%2$s}, RECURSIVE)" +
								")" +
							")" +
						")",
						h1Root, h2Root, h1Child), node.getExpression());
	}

	@Test
	public void testAxisExpression_twoHierarchies_drillLeftMember()
			throws Exception {
		QueryAxis currentAxis = query.getAxis(Axis.COLUMNS);
		QueryHierarchy h1 = query.getHierarchy("Time");
		currentAxis.addHierarchy(h1);
		QueryHierarchy h2 = query.getHierarchy("Gender");
		currentAxis.addHierarchy(h2);

		Member h1Root = h1.getHierarchy().getRootMembers().get(0);
		h1.include(Operator.DESCENDANTS, h1Root);
		Member h2Root = h2.getHierarchy().getRootMembers().get(0);
		h2.include(Operator.DESCENDANTS, h2Root);

		currentAxis.drill(h1Root);

		AxisNode node = currentAxis.toOlap4j();

		assertMdx(
				String.format(
						"Hierarchize(CrossJoin(DrilldownMember({%1$s}, {%1$s}, RECURSIVE), %2$s))",
						h1Root, h2Root), node.getExpression());
	}

	@Test
	public void testAxisExpression_twoHierarchies_drillExcludedMember()
			throws Exception {
		QueryAxis currentAxis = query.getAxis(Axis.COLUMNS);
		QueryHierarchy h1 = query.getHierarchy("Time");
		currentAxis.addHierarchy(h1);
		QueryHierarchy h2 = query.getHierarchy("Gender");
		currentAxis.addHierarchy(h2);

		Member h1Root = h1.getHierarchy().getRootMembers().get(0);
		h1.include(Operator.DESCENDANTS, h1Root);
		Member h1Child = h1Root.getChildMembers().get(0);
		h1.exclude(Operator.MEMBER, h1Child);
		Member h2Root = h2.getHierarchy().getRootMembers().get(0);
		h2.include(Operator.DESCENDANTS, h2Root);

		currentAxis.drill(h1Child, h2Root);

		AxisNode node = currentAxis.toOlap4j();

		assertMdx(String.format("Hierarchize((%1$s, %2$s))", h1Root, h2Root),
				node.getExpression());
	}

	@Test
	public void testListDrills_noDrill() {
		QueryAxis currentAxis = query.getAxis(Axis.COLUMNS);
		QueryHierarchy h1 = query.getHierarchy("Time");
		currentAxis.addHierarchy(h1);
		QueryHierarchy h2 = query.getHierarchy("Gender");
		currentAxis.addHierarchy(h2);

		assertTrue(currentAxis.listDrills().isEmpty());
	}

	@Test
	public void testListDrills_singleLeftDrill() throws Exception {
		QueryAxis currentAxis = query.getAxis(Axis.COLUMNS);
		QueryHierarchy h1 = query.getHierarchy("Time");
		currentAxis.addHierarchy(h1);
		QueryHierarchy h2 = query.getHierarchy("Gender");
		currentAxis.addHierarchy(h2);

		Member drilled = h1.getHierarchy().getRootMembers().get(0);
		currentAxis.drill(drilled);

		assertDrills(currentAxis, new Member[] { drilled });
	}

	@Test
	public void testListDrills_singleRightDrill() throws Exception {
		QueryAxis currentAxis = query.getAxis(Axis.COLUMNS);
		QueryHierarchy h1 = query.getHierarchy("Time");
		currentAxis.addHierarchy(h1);
		QueryHierarchy h2 = query.getHierarchy("Gender");
		currentAxis.addHierarchy(h2);

		Member[] drilled = new Member[] {
				h1.getHierarchy().getRootMembers().get(0),
				h2.getHierarchy().getRootMembers().get(0) };
		currentAxis.drill(drilled);

		assertDrills(currentAxis, drilled);
	}

	@Test
	public void testListDrills_disparateDrills() throws Exception {
		QueryAxis currentAxis = query.getAxis(Axis.COLUMNS);
		QueryHierarchy h1 = query.getHierarchy("Time");
		currentAxis.addHierarchy(h1);
		QueryHierarchy h2 = query.getHierarchy("Gender");
		currentAxis.addHierarchy(h2);

		Member[] firstDrill = new Member[] {
				h1.getHierarchy().getRootMembers().get(0),
				h2.getHierarchy().getRootMembers().get(0) };
		currentAxis.drill(firstDrill);
		Member[] secondDrill = new Member[] { h1.getHierarchy()
				.getRootMembers().get(1), };
		currentAxis.drill(secondDrill);

		assertDrills(currentAxis, secondDrill, firstDrill);
	}

	@Test
	public void testListDrills_sharingPrefixDrills() throws Exception {
		QueryAxis currentAxis = query.getAxis(Axis.COLUMNS);
		QueryHierarchy h1 = query.getHierarchy("Time");
		currentAxis.addHierarchy(h1);
		QueryHierarchy h2 = query.getHierarchy("Gender");
		currentAxis.addHierarchy(h2);

		Member[] firstDrill = new Member[] {
				h1.getHierarchy().getRootMembers().get(0),
				h2.getHierarchy().getRootMembers().get(0) };
		currentAxis.drill(firstDrill);
		Member[] secondDrill = new Member[] { firstDrill[0],
				firstDrill[1].getChildMembers().get(0), };
		currentAxis.drill(secondDrill);

		assertDrills(currentAxis, firstDrill, secondDrill);
	}

	@Test
	public void testDrill_twice() throws Exception {
		QueryAxis currentAxis = query.getAxis(Axis.COLUMNS);
		QueryHierarchy h1 = query.getHierarchy("Time");
		currentAxis.addHierarchy(h1);
		QueryHierarchy h2 = query.getHierarchy("Gender");
		currentAxis.addHierarchy(h2);

		Member[] firstDrill = new Member[] { h1.getHierarchy().getRootMembers()
				.get(0) };
		currentAxis.drill(firstDrill);
		currentAxis.drill(firstDrill);

		assertDrills(currentAxis, firstDrill);
	}

	@Test
	public void testRemoveDrills_nonExistent() throws Exception {
		QueryAxis currentAxis = query.getAxis(Axis.COLUMNS);
		QueryHierarchy h1 = query.getHierarchy("Time");
		currentAxis.addHierarchy(h1);
		QueryHierarchy h2 = query.getHierarchy("Gender");
		currentAxis.addHierarchy(h2);

		Member[] firstDrill = new Member[] { h1.getHierarchy().getRootMembers()
				.get(0) };
		currentAxis.undrill(firstDrill);

		assertTrue(currentAxis.listDrills().isEmpty());
	}

	@Test
	public void testRemoveDrills_nonExistentNoSharingPrefix() throws Exception {
		QueryAxis currentAxis = query.getAxis(Axis.COLUMNS);
		QueryHierarchy h1 = query.getHierarchy("Time");
		currentAxis.addHierarchy(h1);
		QueryHierarchy h2 = query.getHierarchy("Gender");
		currentAxis.addHierarchy(h2);

		Member[] firstDrill = new Member[] {
				h1.getHierarchy().getRootMembers().get(0),
				h2.getHierarchy().getRootMembers().get(0) };
		currentAxis.drill(firstDrill);

		Member[] undrill = new Member[] {
				firstDrill[0].getChildMembers().get(0),
				firstDrill[1].getChildMembers().get(0) };
		currentAxis.undrill(undrill);

		assertDrills(currentAxis, firstDrill);
		assertFalse(currentAxis.isDrilled(undrill));
	}

	@Test
	public void testRemoveDrills_unique() throws Exception {
		QueryAxis currentAxis = query.getAxis(Axis.COLUMNS);
		QueryHierarchy h1 = query.getHierarchy("Time");
		currentAxis.addHierarchy(h1);
		QueryHierarchy h2 = query.getHierarchy("Gender");
		currentAxis.addHierarchy(h2);

		Member[] firstDrill = new Member[] { h1.getHierarchy().getRootMembers()
				.get(0) };
		currentAxis.drill(firstDrill);
		currentAxis.undrill(firstDrill);

		assertTrue(currentAxis.listDrills().isEmpty());
	}

	@Test
	public void testRemoveDrills_removeRight() throws Exception {
		QueryAxis currentAxis = query.getAxis(Axis.COLUMNS);
		QueryHierarchy h1 = query.getHierarchy("Time");
		currentAxis.addHierarchy(h1);
		QueryHierarchy h2 = query.getHierarchy("Gender");
		currentAxis.addHierarchy(h2);

		Member h1Root = h1.getHierarchy().getRootMembers().get(0);
		Member h2Root = h2.getHierarchy().getRootMembers().get(0);
		Member[] firstDrill = new Member[] { h1Root, h2Root };
		Member[] secondDrill = new Member[] { h1Root };

		currentAxis.drill(firstDrill);
		currentAxis.drill(secondDrill);
		currentAxis.undrill(firstDrill);

		assertDrills(currentAxis, secondDrill);
	}

	@Test
	public void testIsDrilled_notFound() throws Exception {
		QueryAxis currentAxis = query.getAxis(Axis.COLUMNS);
		QueryHierarchy h1 = query.getHierarchy("Time");
		currentAxis.addHierarchy(h1);
		QueryHierarchy h2 = query.getHierarchy("Gender");
		currentAxis.addHierarchy(h2);

		Member h1Root = h1.getHierarchy().getRootMembers().get(0);
		Member h2Root = h2.getHierarchy().getRootMembers().get(0);
		Member[] drill = new Member[] { h1Root, h2Root };
		currentAxis.drill(drill);

		assertFalse(currentAxis.isDrilled(h1Root));

	}

	@Test(expected = IllegalArgumentException.class)
	public void testDrill_structureException() throws Exception {
		QueryAxis currentAxis = query.getAxis(Axis.COLUMNS);
		QueryHierarchy h1 = query.getHierarchy("Time");
		currentAxis.addHierarchy(h1);
		QueryHierarchy h2 = query.getHierarchy("Gender");
		currentAxis.addHierarchy(h2);

		Member h1Root = h1.getHierarchy().getRootMembers().get(0);
		Member h2Root = h2.getHierarchy().getRootMembers().get(0);
		Member[] drill = new Member[] { h2Root, h1Root };
		currentAxis.drill(drill);

	}

	private void assertDrills(QueryAxis axis, Member[]... expectedDrills) {
		List<Member[]> actualDrills = axis.listDrills();
		assertEquals(expectedDrills.length, actualDrills.size());
		for (int i = 0; i < expectedDrills.length; ++i) {
			Member[] actualDrill = actualDrills.get(i);
			Member[] expectedDrill = expectedDrills[i];

			assertArrayEquals("wrong drill at position " + i, expectedDrill,
					actualDrill);
			assertTrue(axis.isDrilled(expectedDrill));
		}
	}

	private void assertMdx(String expected, ParseTreeNode actual) {
		StringWriter swr = new StringWriter();
		ParseTreeWriter wr = new ParseTreeWriter(swr);
		actual.unparse(wr);
		assertEquals(expected, swr.toString());
	}
}
