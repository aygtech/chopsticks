package com.chopsticks.office;

import java.io.File;

import org.junit.Test;

import com.aspose.cells.License;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.model.FileHeader;

public class SimpleTest {
	@Test
	public void test() throws Throwable{
//		System.out.println(Joiner.on("\n").withKeyValueSeparator("=").join(System.getProperties()));
//		String newClassPath= System.getProperty("java.io.tmpdir") + UUID.randomUUID();
//		File dir = new File(newClassPath);
//		dir.mkdirs();
//		File txt = new File(dir + File.separator + "META-INF", "LE-9AD48.RSA");
//		Files.createParentDirs(txt);
//		txt.createNewFile();
//		Reflect.on(ClassLoader.getSystemClassLoader()).call("addURL", dir.toURI().toURL());
//		InputStream txtIn = ClassLoader.getSystemResourceAsStream("META-INF/LE-9AD48.RSA");
//		txtIn.close();
		File file= new File(License.class.getProtectionDomain().getCodeSource().getLocation().toURI());
		ZipFile zipFile= new ZipFile(file);
		if(zipFile.isValidZipFile()) {
			zipFile.removeFile("META-INF/LE-9AD48.RSA");
		}
		
	}
}
