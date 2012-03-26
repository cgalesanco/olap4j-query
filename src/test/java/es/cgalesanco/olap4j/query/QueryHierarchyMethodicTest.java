package es.cgalesanco.olap4j.query;

import static org.junit.Assert.assertEquals;

import java.io.StringWriter;
import java.util.Arrays;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.olap4j.mdx.ParseTreeNode;
import org.olap4j.mdx.ParseTreeWriter;
import org.olap4j.metadata.Cube;
import org.olap4j.metadata.Member;

import es.cgalesanco.olap4j.query.Selection.Operator;
import es.cgalesanco.olap4j.query.Selection.Sign;

public class QueryHierarchyMethodicTest {
	static private Cube cube;
	private static QueryHierarchy qh;
	private Member rootMember;
	private Member childMember;
	private HierarchyExpander expander;

	public enum State {
		Ed_Ech_Em(Sign.EXCLUDE), Ed_Ech_Im(Sign.EXCLUDE, Sign.EXCLUDE,
				Sign.INCLUDE), Ed_Ich_Em(Sign.EXCLUDE, Sign.INCLUDE), Ed_Ich_Im(
				Sign.EXCLUDE, Sign.INCLUDE, Sign.INCLUDE), Id_Ech_Em(
				Sign.INCLUDE, Sign.EXCLUDE, Sign.EXCLUDE), Id_Ech_Im(
				Sign.INCLUDE, Sign.EXCLUDE), Id_Ich_Em(Sign.INCLUDE,
				Sign.INCLUDE, Sign.EXCLUDE), Id_Ich_Im(Sign.INCLUDE);

		private Sign[] signs;

		State(Sign... signs) {
			this.signs = signs;
		}

		void apply(Member m) {
			qh.apply(new SelectionAction(m, signs[0], Operator.DESCENDANTS));

			if (signs.length > 1)
				qh.apply(new SelectionAction(m, signs[1], Operator.CHILDREN));

			if (signs.length > 2)
				qh.apply(new SelectionAction(m, signs[2], Operator.MEMBER));
		}
	}

	@BeforeClass
	static public void setUpFixture() throws Exception {
		cube = MetadataFixture.createCube();
	}

	@Before
	public void setUp() throws Exception {
		Query q = new Query("Hierarchy Test", cube);
		qh = q.getHierarchy("Time");
		rootMember = qh.getHierarchy().getRootMembers().get(0);
		childMember = rootMember.getChildMembers().get(0);
		expander = new HierarchyExpander();
	}

	@Test
	public void testInitial_noDrill() {
		String[] notDrilled = new String[] { "{}", "{%1$s}", "%1$s.Children",
				"{%1$s}", "Descendants(%1$s, 2)", "{%1$s}", "%1$s.Children",
				"{%1$s}" };

		int i = 0;
		for (State st : State.values()) {
			System.out.println(st);
			qh.clear();
			st.apply(rootMember);
			assertDrillExpression(notDrilled[i++], qh.toOlap4j(), rootMember);
		}
	}

	@Test
	public void testInitial_drillRoot() {
		String[] notDrilled = new String[] { "{}", "{%1$s}", "%1$s.Children",
				"DrilldownMember({%1$s}, {%1$s}, RECURSIVE)",
				"Descendants(%1$s, 2)", "Union({%1$s}, Descendants(%1$s, 2))",
				"%1$s.Children", "DrilldownMember({%1$s}, {%1$s}, RECURSIVE)" };

		int i = 0;
		for (State st : State.values()) {
			System.out.println(st);
			qh.clear();
			st.apply(rootMember);
			expander.setDrills(Arrays.asList(rootMember));
			assertDrillExpression(notDrilled[i++],
					qh.toOlap4j(expander), rootMember);
		}
	}

	@Test
	public void testTwoLevels_noDrill() {
		String[] notDrilled = new String[] {
				// Root: Ed_Ech_Em
				"{}", "{%2$s}", "%2$s.Children", "{%2$s}",
				"Descendants(%2$s, 2)", "{%2$s}", "%2$s.Children", "{%2$s}",

				// Root: Ed_Ech_Im
				"{%1$s}", "{%1$s}", "{%1$s}", "{%1$s}", "{%1$s}", "{%1$s}", "{%1$s}", "{%1$s}",
				

				// Root: Ed_Ich_Em
				"Except(%1$s.Children, {%2$s})",
				"%1$s.Children",
				"Except(Union(%1$s.Children, %2$s.Children), {%2$s})",
				"%1$s.Children",
				"Except(Union(%1$s.Children, Descendants(%2$s, 2)), {%2$s})",
				"%1$s.Children",
				"Except(Union(%1$s.Children, %2$s.Children), {%2$s})",
				"%1$s.Children",
				
				// Root: Ed_Ich_Im
				"{%1$s}", "{%1$s}", "{%1$s}", "{%1$s}", "{%1$s}", "{%1$s}", "{%1$s}", "{%1$s}",

				// Root: Id_Ech_Em
				"Descendants(Except(%1$s.Children, {%2$s}), 1)",
				"Union({%2$s}, Descendants(Except(%1$s.Children, {%2$s}), 1))",
				"Union(Descendants(Except(%1$s.Children, {%2$s}), 1), %2$s.Children)", // Can be optimized to "Descendants(%1$s, 2)"
				"Union({%2$s}, Descendants(Except(%1$s.Children, {%2$s}), 1))",
				"Union(Descendants(Except(%1$s.Children, {%2$s}), 1), Descendants(%2$s, 2))",
				"Union({%2$s}, Descendants(Except(%1$s.Children, {%2$s}), 1))",
				"Union(Descendants(Except(%1$s.Children, {%2$s}), 1), %2$s.Children)", // Can be optimized to "Descendants(%1$s, 2)"
				"Union({%2$s}, Descendants(Except(%1$s.Children, {%2$s}), 1))",
				
				// Root: Id_Ech_Im
				"{%1$s}", "{%1$s}", "{%1$s}", "{%1$s}", "{%1$s}", "{%1$s}", "{%1$s}", "{%1$s}",

				// Root: Id_Ich_Em
				"Except(%1$s.Children, {%2$s})", 
				"%1$s.Children",
				"Except(Union(%1$s.Children, %2$s.Children), {%2$s})",
				"%1$s.Children",
				"Except(Union(%1$s.Children, Descendants(%2$s, 2)), {%2$s})",
				"%1$s.Children",
				"Except(Union(%1$s.Children, %2$s.Children), {%2$s})",
				"%1$s.Children",
				
				// Root: Id_Ich_Im
				"{%1$s}", "{%1$s}", "{%1$s}", "{%1$s}", "{%1$s}", "{%1$s}", "{%1$s}", "{%1$s}",
		};

		int i = 0;
		for (State st1 : State.values()) {
			for (State st2 : State.values()) {
				qh.clear();
				System.out.println(st1 + "/" + st2);
				st1.apply(rootMember);
				st2.apply(childMember);
				if (i < notDrilled.length) {
					assertDrillExpression(notDrilled[i++], qh.toOlap4j(),
							rootMember, childMember);
				}
			}
		}
	}

	@Test
	public void testTwoLevels_drillRoot() {
		String[] notDrilled = new String[] {
				// Root: Ed_Ech_Em
				"{}", 
				"{%2$s}", 
				"%2$s.Children", 
				"{%2$s}",
				"Descendants(%2$s, 2)", 
				"{%2$s}", 
				"%2$s.Children", 
				"{%2$s}",

				// Root: Ed_Ech_Im
				"{%1$s}", 
				"{%1$s, %2$s}",
				"Union({%1$s}, %2$s.Children)",
				"{%1$s, %2$s}",
				"Union({%1$s}, Descendants(%2$s, 2))",
				"{%1$s, %2$s}",
				"Union({%1$s}, %2$s.Children)",
				"{%1$s, %2$s}",

				// Root: Ed_Ich_Em
				"Except(%1$s.Children, {%2$s})",
				"%1$s.Children",
				"Except(Union(%1$s.Children, %2$s.Children), {%2$s})",
				"%1$s.Children",
				"Except(Union(%1$s.Children, Descendants(%2$s, 2)), {%2$s})",
				"%1$s.Children",
				"Except(Union(%1$s.Children, %2$s.Children), {%2$s})",
				"%1$s.Children",

				// Root: Ed_Ich_Im
				"Except(DrilldownMember({%1$s}, {%1$s}, RECURSIVE), {%2$s})",
				"DrilldownMember({%1$s}, {%1$s}, RECURSIVE)",
				"Except(DrilldownMember(Union({%1$s}, %2$s.Children), {%1$s}, RECURSIVE), {%2$s})",
				"DrilldownMember({%1$s}, {%1$s}, RECURSIVE)",
				"Except(DrilldownMember(Union({%1$s}, Descendants(%2$s, 2)), {%1$s}, RECURSIVE), {%2$s})",
				"DrilldownMember({%1$s}, {%1$s}, RECURSIVE)",
				"Except(DrilldownMember(Union({%1$s}, %2$s.Children), {%1$s}, RECURSIVE), {%2$s})",
				"DrilldownMember({%1$s}, {%1$s}, RECURSIVE)",
				
				// Root Id_Ech_Em
				"Descendants(Except(%1$s.Children, {%2$s}), 1)",
				"Union({%2$s}, Descendants(Except(%1$s.Children, {%2$s}), 1))",
				"Union(Descendants(Except(%1$s.Children, {%2$s}), 1), %2$s.Children)", // Can be optimized to "Descendants(%1$s, 2)"
				"Union({%2$s}, Descendants(Except(%1$s.Children, {%2$s}), 1))",
				"Union(Descendants(Except(%1$s.Children, {%2$s}), 1), Descendants(%2$s, 2))",
				"Union({%2$s}, Descendants(Except(%1$s.Children, {%2$s}), 1))",
				"Union(Descendants(Except(%1$s.Children, {%2$s}), 1), %2$s.Children)", // Can be optimized to "Descendants(%1$s, 2)"
				"Union({%2$s}, Descendants(Except(%1$s.Children, {%2$s}), 1))",
				
				// Root: Id_Ech_Im
				"Union({%1$s}, Descendants(Except(%1$s.Children, {%2$s}), 1))",
				"Union({%1$s, %2$s}, Descendants(Except(%1$s.Children, {%2$s}), 1))",
				"Union({%1$s}, Union(Descendants(Except(%1$s.Children, {%2$s}), 1), %2$s.Children))",
				"Union({%1$s, %2$s}, Descendants(Except(%1$s.Children, {%2$s}), 1))",
				"Union({%1$s}, Union(Descendants(Except(%1$s.Children, {%2$s}), 1), Descendants(%2$s, 2)))",
				"Union({%1$s, %2$s}, Descendants(Except(%1$s.Children, {%2$s}), 1))",
				"Union({%1$s}, Union(Descendants(Except(%1$s.Children, {%2$s}), 1), %2$s.Children))",
				"Union({%1$s, %2$s}, Descendants(Except(%1$s.Children, {%2$s}), 1))",
				
				
				// Root: Id_Ich_Em
				"Except(%1$s.Children, {%2$s})", 
				"%1$s.Children",
				"Except(Union(%1$s.Children, %2$s.Children), {%2$s})",
				"%1$s.Children",
				"Except(Union(%1$s.Children, Descendants(%2$s, 2)), {%2$s})",
				"%1$s.Children",
				"Except(Union(%1$s.Children, %2$s.Children), {%2$s})",
				"%1$s.Children",
				
				// Root: Id_Ich_Im
				"Except(DrilldownMember({%1$s}, {%1$s}, RECURSIVE), {%2$s})",
				"DrilldownMember({%1$s}, {%1$s}, RECURSIVE)",
				"Except(DrilldownMember(Union({%1$s}, %2$s.Children), {%1$s}, RECURSIVE), {%2$s})",
				"DrilldownMember({%1$s}, {%1$s}, RECURSIVE)",
				"Except(DrilldownMember(Union({%1$s}, Descendants(%2$s, 2)), {%1$s}, RECURSIVE), {%2$s})",
				"DrilldownMember({%1$s}, {%1$s}, RECURSIVE)",
				"Except(DrilldownMember(Union({%1$s}, %2$s.Children), {%1$s}, RECURSIVE), {%2$s})",
				"DrilldownMember({%1$s}, {%1$s}, RECURSIVE)",
		};

		int i = 0;
		for (State st1 : State.values()) {
			for (State st2 : State.values()) {
				qh.clear();
				st1.apply(rootMember);
				st2.apply(childMember);
				if (i < notDrilled.length) {
					System.out.println(st1 + "/" + st2);
					expander.setDrills(Arrays.asList(rootMember));
					assertDrillExpression(notDrilled[i++], qh.toOlap4j(expander),
							rootMember, childMember);
				}
			}
		}
	}

	@Test
	public void testTwoLevels_drillRoot_drillChild() {
		String[] notDrilled = new String[] {
				// Root: Ed_Ech_Em
				"{}", 
				"{%2$s}", 
				"%2$s.Children", 
				"DrilldownMember({%2$s}, {%2$s}, RECURSIVE)",
				"Descendants(%2$s, 2)", 
				"Union({%2$s}, Descendants(%2$s, 2))", 
				"%2$s.Children", 
				"DrilldownMember({%2$s}, {%2$s}, RECURSIVE)",

				// Root: Ed_Ech_Im
				"{%1$s}", 
				"{%1$s, %2$s}",
				"Union({%1$s}, %2$s.Children)",
				"DrilldownMember({%1$s, %2$s}, {%2$s}, RECURSIVE)",
				"Union({%1$s}, Descendants(%2$s, 2))",
				"Union({%1$s, %2$s}, Descendants(%2$s, 2))",
				"Union({%1$s}, %2$s.Children)",
				"DrilldownMember({%1$s, %2$s}, {%2$s}, RECURSIVE)",

				// Root: Ed_Ich_Em
				"Except(%1$s.Children, {%2$s})",
				"%1$s.Children",
				"Except(Union(%1$s.Children, %2$s.Children), {%2$s})",
				"DrilldownMember(%1$s.Children, {%2$s}, RECURSIVE)",
				"Except(Union(%1$s.Children, Descendants(%2$s, 2)), {%2$s})",
				"Union(%1$s.Children, Descendants(%2$s, 2))",
				"Except(Union(%1$s.Children, %2$s.Children), {%2$s})",
				"DrilldownMember(%1$s.Children, {%2$s}, RECURSIVE)",

				// Root: Ed_Ich_Im
				"Except(DrilldownMember({%1$s}, {%1$s}, RECURSIVE), {%2$s})",
				"DrilldownMember({%1$s}, {%1$s}, RECURSIVE)",
				"Except(DrilldownMember(Union({%1$s}, %2$s.Children), {%1$s}, RECURSIVE), {%2$s})",
				"DrilldownMember({%1$s}, {%1$s, %2$s}, RECURSIVE)",
				"Except(DrilldownMember(Union({%1$s}, Descendants(%2$s, 2)), {%1$s}, RECURSIVE), {%2$s})",
				"DrilldownMember(Union({%1$s}, Descendants(%2$s, 2)), {%1$s}, RECURSIVE)",
				"Except(DrilldownMember(Union({%1$s}, %2$s.Children), {%1$s}, RECURSIVE), {%2$s})",
				"DrilldownMember({%1$s}, {%1$s, %2$s}, RECURSIVE)",
				
				// Root Id_Ech_Em
				"Descendants(Except(%1$s.Children, {%2$s}), 1)",
				"Union({%2$s}, Descendants(Except(%1$s.Children, {%2$s}), 1))",
				"Union(Descendants(Except(%1$s.Children, {%2$s}), 1), %2$s.Children)", // Can be optimized to "Descendants(%1$s, 2)"
				"DrilldownMember(Union({%2$s}, Descendants(Except(%1$s.Children, {%2$s}), 1)), {%2$s}, RECURSIVE)",
				"Union(Descendants(Except(%1$s.Children, {%2$s}), 1), Descendants(%2$s, 2))",
				"Union({%2$s}, Union(Descendants(Except(%1$s.Children, {%2$s}), 1), Descendants(%2$s, 2)))",
				"Union(Descendants(Except(%1$s.Children, {%2$s}), 1), %2$s.Children)", // Can be optimized to "Descendants(%1$s, 2)"
				"DrilldownMember(Union({%2$s}, Descendants(Except(%1$s.Children, {%2$s}), 1)), {%2$s}, RECURSIVE)",
				
				// Root: Id_Ech_Im
				"Union({%1$s}, Descendants(Except(%1$s.Children, {%2$s}), 1))",
				"Union({%1$s, %2$s}, Descendants(Except(%1$s.Children, {%2$s}), 1))",
				"Union({%1$s}, Union(Descendants(Except(%1$s.Children, {%2$s}), 1), %2$s.Children))",
				"DrilldownMember(Union({%1$s, %2$s}, Descendants(Except(%1$s.Children, {%2$s}), 1)), {%2$s}, RECURSIVE)",
				"Union({%1$s}, Union(Descendants(Except(%1$s.Children, {%2$s}), 1), Descendants(%2$s, 2)))",
				"Union({%1$s, %2$s}, Union(Descendants(Except(%1$s.Children, {%2$s}), 1), Descendants(%2$s, 2)))", 
				"Union({%1$s}, Union(Descendants(Except(%1$s.Children, {%2$s}), 1), %2$s.Children))",
				"DrilldownMember(Union({%1$s, %2$s}, Descendants(Except(%1$s.Children, {%2$s}), 1)), {%2$s}, RECURSIVE)",

				// Root: Id_Ich_Em
				"Except(%1$s.Children, {%2$s})", 
				"%1$s.Children",
				"Except(Union(%1$s.Children, %2$s.Children), {%2$s})",
				"DrilldownMember(%1$s.Children, {%2$s}, RECURSIVE)",
				"Except(Union(%1$s.Children, Descendants(%2$s, 2)), {%2$s})",
				"Union(%1$s.Children, Descendants(%2$s, 2))",
				"Except(Union(%1$s.Children, %2$s.Children), {%2$s})",
				"DrilldownMember(%1$s.Children, {%2$s}, RECURSIVE)",
				
				// Root: Id_Ich_Im
				"Except(DrilldownMember({%1$s}, {%1$s}, RECURSIVE), {%2$s})",
				"DrilldownMember({%1$s}, {%1$s}, RECURSIVE)",
				"Except(DrilldownMember(Union({%1$s}, %2$s.Children), {%1$s}, RECURSIVE), {%2$s})",
				"DrilldownMember({%1$s}, {%1$s, %2$s}, RECURSIVE)",
				"Except(DrilldownMember(Union({%1$s}, Descendants(%2$s, 2)), {%1$s}, RECURSIVE), {%2$s})",
				"DrilldownMember(Union({%1$s}, Descendants(%2$s, 2)), {%1$s}, RECURSIVE)",
				"Except(DrilldownMember(Union({%1$s}, %2$s.Children), {%1$s}, RECURSIVE), {%2$s})",
				"DrilldownMember({%1$s}, {%1$s, %2$s}, RECURSIVE)",
		};

		int i = 0;
		for (State st1 : State.values()) {
			for (State st2 : State.values()) {
				System.out.println(st1 + "/" + st2);
				qh.clear();
				st1.apply(rootMember);
				st2.apply(childMember);
				if (i < notDrilled.length) {
					expander.setDrills(Arrays.asList(rootMember,childMember));
					assertDrillExpression(notDrilled[i++], qh.toOlap4j(expander),
							rootMember, childMember);
				}
			}
		}
	}

	@Test
	public void testTwoLevels_drillChild() {
		String[] notDrilled = new String[] {
				// Root: Ed_Ech_Em
				"{}", 
				"{%2$s}", 
				"%2$s.Children", 
				"DrilldownMember({%2$s}, {%2$s}, RECURSIVE)",
				"Descendants(%2$s, 2)", 
				"Union({%2$s}, Descendants(%2$s, 2))", 
				"%2$s.Children", 
				"DrilldownMember({%2$s}, {%2$s}, RECURSIVE)",

				// Root: Ed_Ech_Im
				"{%1$s}", "{%1$s}", "{%1$s}", "{%1$s}", "{%1$s}", "{%1$s}", "{%1$s}", "{%1$s}",

				// Root: Ed_Ich_Em
				"Except(%1$s.Children, {%2$s})",
				"%1$s.Children",
				"Except(Union(%1$s.Children, %2$s.Children), {%2$s})",
				"DrilldownMember(%1$s.Children, {%2$s}, RECURSIVE)",
				"Except(Union(%1$s.Children, Descendants(%2$s, 2)), {%2$s})",
				"Union(%1$s.Children, Descendants(%2$s, 2))",
				"Except(Union(%1$s.Children, %2$s.Children), {%2$s})",
				"DrilldownMember(%1$s.Children, {%2$s}, RECURSIVE)",

				// Root: Ed_Ich_Im
				"{%1$s}", "{%1$s}", "{%1$s}", "{%1$s}", "{%1$s}", "{%1$s}", "{%1$s}", "{%1$s}",
				
				// Root Id_Ech_Em
				"Descendants(Except(%1$s.Children, {%2$s}), 1)",
				"Union({%2$s}, Descendants(Except(%1$s.Children, {%2$s}), 1))",
				"Union(Descendants(Except(%1$s.Children, {%2$s}), 1), %2$s.Children)", // Can be optimized to "Descendants(%1$s, 2)"
				"DrilldownMember(Union({%2$s}, Descendants(Except(%1$s.Children, {%2$s}), 1)), {%2$s}, RECURSIVE)",
				"Union(Descendants(Except(%1$s.Children, {%2$s}), 1), Descendants(%2$s, 2))",
				"Union({%2$s}, Union(Descendants(Except(%1$s.Children, {%2$s}), 1), Descendants(%2$s, 2)))",
				"Union(Descendants(Except(%1$s.Children, {%2$s}), 1), %2$s.Children)", // Can be optimized to "Descendants(%1$s, 2)"
				"DrilldownMember(Union({%2$s}, Descendants(Except(%1$s.Children, {%2$s}), 1)), {%2$s}, RECURSIVE)",
				
				// Root: Id_Ech_Im
				"{%1$s}", "{%1$s}", "{%1$s}", "{%1$s}", "{%1$s}", "{%1$s}", "{%1$s}", "{%1$s}",
				
				// Root: Id_Ich_Em
				"Except(%1$s.Children, {%2$s})", 
				"%1$s.Children",
				"Except(Union(%1$s.Children, %2$s.Children), {%2$s})",
				"DrilldownMember(%1$s.Children, {%2$s}, RECURSIVE)",
				"Except(Union(%1$s.Children, Descendants(%2$s, 2)), {%2$s})",
				"Union(%1$s.Children, Descendants(%2$s, 2))",
				"Except(Union(%1$s.Children, %2$s.Children), {%2$s})",
				"DrilldownMember(%1$s.Children, {%2$s}, RECURSIVE)",
				
				// Root: Id_Ich_Im
				"{%1$s}", "{%1$s}", "{%1$s}", "{%1$s}", "{%1$s}", "{%1$s}", "{%1$s}", "{%1$s}",
		};

		int i = 0;
		for (State st1 : State.values()) {
			for (State st2 : State.values()) {
				qh.clear();
				st1.apply(rootMember);
				st2.apply(childMember);
				if (i < notDrilled.length) {
					System.out.println(st1 + "/" + st2);
					expander.setDrills(Arrays.asList(childMember));
					assertDrillExpression(notDrilled[i++], qh.toOlap4j(expander),
							rootMember, childMember);
				}
			}
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

