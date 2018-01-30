package killrvideo.utils;

import java.nio.charset.Charset;

import org.apache.commons.codec.CharEncoding;
import org.apache.commons.codec.Charsets;

public class Test {
	public static void main(String[] args) {
		String hashPassword = HashUtils.hashPassword("testPassword");
		System.out.println(new String(hashPassword.getBytes(Charset.forName(CharEncoding.US_ASCII))));
	}
}
