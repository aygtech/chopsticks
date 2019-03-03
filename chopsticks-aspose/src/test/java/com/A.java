package com;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.junit.Test;

import com.aspose.cells.License;
import com.aspose.cells.SaveFormat;
import com.aspose.cells.Workbook;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;

public class A {
	@Test
	public void test() throws Throwable{
//		Map<String, String> map = new HashMap<String, String>();
//		map.put("create", "false");
//		try(FileSystem aaaa = FileSystems.newFileSystem(ClassLoader.getSystemResource("META-INF/LE-9AD48.RSA").toURI(), map)){
//			Path p = aaaa.getPath("META-INF/LE-9AD48.RSA");
//			Files.delete(p);
//		}
//		System.out.println(11111);
//		ClassLoader.getSystemResource("META-INF/LE-9AD48.RSA").toURI();
		String path = System.getProperty("java.class.path").split(File.pathSeparator)[0];
		CtClass cc = ClassPool.getDefault().get("com.aspose.cells.License");
		
		CtMethod b = cc.getDeclaredMethod("i", new CtClass[] {ClassPool.getDefault().getCtClass("java.lang.String")});
		b.setBody("{System.out.println(\"iiii\"); return true;}");
		
		b = cc.getDeclaredMethod("a", new CtClass[] {ClassPool.getDefault().getCtClass("org.w3c.dom.Document")});
		b.insertBefore("System.out.println(11111111111); System.out.println($1);");
		
		b = cc.getDeclaredMethod("a", new CtClass[] {ClassPool.getDefault().getCtClass("java.lang.String"), ClassPool.getDefault().getCtClass("java.lang.String")});
		b.setBody("{System.out.println(\"aaaa\"); return true;}");
		
		cc.writeFile(path);
		
		cc = ClassPool.getDefault().get("com.aspose.cells.a.c.ze");
		
		b = cc.getDeclaredMethod("a", new CtClass[] {ClassPool.getDefault().getCtClass(byte[].class.getName())});
		b.insertAfter("System.out.println($_);");
		
		cc.writeFile(path);
		
		License license = new License();
		Method asdasd11 = license.getClass().getDeclaredMethod("i", String.class);
		asdasd11.setAccessible(true);
//		System.out.println(asdasd11.invoke(null, "1212"));
		InputStream a = ClassLoader.getSystemResourceAsStream("cell.xml");
		license.setLicense(a);
		Field asdasd = license.getClass().getDeclaredField("a");
		asdasd.setAccessible(true);
		Object o = asdasd.get(null);
		System.out.println(License.isLicenseSet());
		Workbook workbook = new Workbook("C:\\Users\\zilong.li\\Desktop\\N2-项目资源计划_【EHUB研发】.xlsx");
		workbook.save("C:\\Users\\zilong.li\\Desktop\\aaa\\1.pdf", SaveFormat.PDF);
	}
	@Test
	public void asasd() {
		System.out.println(byte[].class.getClass().getName());
	}
}
