package es.cgalesanco.olap4j.query;

import static org.junit.Assert.assertEquals;

import java.io.StringWriter;

import org.olap4j.mdx.ParseTreeNode;
import org.olap4j.mdx.ParseTreeWriter;
import org.olap4j.metadata.Cube;

public class QueryTestBase {
	private static Cube cube;

	public static Cube getCube() {
		return cube;
	}
	
	protected static void setUpFixture() throws Exception {
		cube = MetadataFixture.createCube();
	}

	protected void assertMdx(String expected, ParseTreeNode actual, Object...args) {
		StringWriter swr = new StringWriter();
		ParseTreeWriter wr = new ParseTreeWriter(swr);
		actual.unparse(wr);
		assertEquals(String.format(expected, args), swr.toString());
	}
}
