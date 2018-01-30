package killrvideo.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class UserDataUtils {
	public static void main(String[] args) throws IOException {
		Path inPath = Paths.get("C:\\dev\\work\\cassandra-learning\\killrvido-db","users.csv");
		Path outPath = Paths.get("C:\\dev\\work\\cassandra-learning\\killrvido-db","usersout.csv");
		Path outPathCredentials = Paths.get("C:\\dev\\work\\cassandra-learning\\killrvido-db","user_credentials.csv");
		
		List<String> outLines = Files.readAllLines(inPath).stream().map(e ->{
			String [] data = e.split(",");
			data[1] = "2018-01-29 10:00:00+0000"; 
			data[2]	= data[3] +"@gmail.com"; 	
			List<String>  strList = Arrays.asList(data);
			String result = strList.stream().collect(Collectors.joining(","));
			return result;
		}).collect(Collectors.toList());
		
		Files.write(outPath, outLines, StandardOpenOption.CREATE);
		String hashPassword = HashUtils.hashPassword("testPassword");
		
		Files.write(outPathCredentials,
		outLines.stream().map(e -> {
			String [] data = e.split(",");
			return data[2] + "," + hashPassword + "," + data[0]; 
		}).collect(Collectors.toList()),
		StandardOpenOption.CREATE);
	}
}
