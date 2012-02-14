package es.cgalesanco.olap4j.query;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class FixtureUtils {
	public static void copyResource(String resourceName, File directory, String fileName) throws IOException {
		InputStream is = ClassLoader.getSystemResourceAsStream(resourceName);
		FileOutputStream os = new FileOutputStream(new File(directory, fileName));
		byte[] buffer = new byte[4096];
		try {
			int sz;
			while( (sz = is.read(buffer)) > 0 )
				os.write(buffer, 0, sz);
		} finally {
			os.close();
		}
		is.close();
	}
}
