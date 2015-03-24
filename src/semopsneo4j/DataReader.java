package semopsneo4j;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataReader {

	public DataReader() {
		
	}

	public static Map<String, ArrayList<String>> mapFromDatabase(String url, String user, String password) {

		Connection c = null;
		Statement stmt = null;
		Map<String, ArrayList<String>> imagesTags = new HashMap<String, ArrayList<String>>();

		try {
			/* Connect to database */
			Class.forName("org.postgresql.Driver");
			c = DriverManager.getConnection(url, user, password);
			c.setAutoCommit(false);

			/*
			 * Read images (their ids) and their tags from the database and
			 * saves them to a map
			 */
			stmt = c.createStatement();
			String theQuery = "SELECT \n"
					+ "  \"imagefiltred\".id, \n"
					+ "  \"imagetagfiltred\".tag \n"
					+ "FROM \n"
					+ "\"gettytag\", \n"
					+ "\"imagefiltred\", \n"
					+ "\"imagetagfiltred\"\n"
					+ "WHERE \n"
					+ "  \"imagefiltred\".id = \"imagetagfiltred\".imageid AND\n"
					+ "  \"gettytag\".text = \"imagetagfiltred\".tag AND\n"
					+ "  \"gettytag\".gettytag = true AND\n"
					+ "  \"imagefiltred\".lat >= 50 AND \n"
					+ "  \"imagefiltred\".lat <= 55 AND \n"
					+ "  \"imagefiltred\".lon >= 5 AND \n"
					+ "  \"imagefiltred\".lon <= 15";
			ResultSet rs = stmt.executeQuery(theQuery);

			/* Write into the Map */
			while (rs.next()) {
				String id = rs.getString("id");
				String tag = rs.getString("tag");

				/* If the Map doesn't already contain the key */
				if (!imagesTags.containsKey(id)) {
					/* add the key and create internal list */
					imagesTags.put(id, new ArrayList<String>());

					/* add the first tag to the internal list */
					List<String> internal = (ArrayList<String>) imagesTags.get(id);
					internal.add(tag);
				} else {
					/*
					 * Key already exists for the image; Search for tag in the
					 * internal list and add it
					 */
					List<String> internal = (ArrayList<String>) imagesTags.get(id);
					if (!(internal.contains(tag))) {
						internal.add(tag);
					}
				}
			}

			/* close reading */
			rs.close();
			stmt.close();

			/* close connection */
			c.close();

		} catch (Exception e) {
			e.printStackTrace();
			System.err.println(e.getClass().getName() + " - " + e.getMessage());
			System.exit(0);
		}

		return imagesTags;

	}

	public Map<String, ArrayList<String>> mapFromFile(String fileName) {
		Map<String, ArrayList<String>> imagesTags = new HashMap<String, ArrayList<String>>();

		/* Set path to file */
		String filepath = System.getProperty("user.dir");
		filepath = filepath + "/src/" + fileName;

		BufferedReader br = null;
		String line;
		String split = ",";

		try {

			br = new BufferedReader(new FileReader(filepath));
			while ((line = br.readLine()) != null) {

				/* Create an array with the strings of every line */
				String[] words = line.split(split);

				for (int i = 0; i < words.length; i++) {
					words[i] = words[i].replace(" \"", "").replace("\"", "");
				}

				/* add the key and create internal list */
				imagesTags.put(words[0], new ArrayList<String>());

				/* Go through the internal list and add all the tags */
				List<String> internal = (ArrayList<String>) imagesTags.get(words[0]);

				for (int i = 1; i < words.length; i++) {
					/*
					 * Key already exists for the image; Search for tag in the
					 * internal list and add it
					 */
					internal.add(words[i]);
				}
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {

			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

		}

		System.out.println("Reading done");
		return imagesTags;
	}
}
