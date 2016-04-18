package src.com.example.crawler;

import java.io.*;
import java.util.HashSet;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/*
 * Want to keep text between <a> and </a> and 
 * Keep meta content?
 * Anything inside <p> (already handles <a> inside p)
 * Figure out how to not include nav bars / footers 
 * <p>&nbsp;</p>  don't put that in text 
 * 
 * Don't put anything in <ul> tags 
 * 
 * duplicates?
 */
public class Reducer {
	private static File textRepo = new File("textRepo");
	private static File htmlRepo = new File("/Users/progalski/Documents/Crawler/Crawler/repository");
	private static File stopWords = new File("/Users/progalski/Documents/Crawler/Crawler/stopwords.txt");
	private static HashSet<Element> excludeElements = new HashSet<>();

	public static void main(String[] args) {

		/*
		 * If the "repository" directory already exists delete it by removing
		 * all of its files and then the directory itself. Ensures only pages
		 * from this run are added to the folder.
		 */
		if (textRepo.exists()) {
			String[] entries = textRepo.list();
			for (String s : entries) { // Delete all pages in repository
				File current = new File(textRepo.getPath(), s);
				current.delete();
			}
			textRepo.delete(); // Then delete repository
		}

		/*
		 * Create a new "repository" directory to hold all of the pages crawled.
		 */
		if (textRepo.mkdir()) {
			System.out.println("Repository created.");
		} else {
			System.out.println("Failed to create repository."); // Error message
		}

		readHTML();

	}

	public static void readHTML() {
		BufferedWriter bw;

		String[] entries = htmlRepo.list();
		for (String s : entries) {
			File current = new File(htmlRepo.getPath(), s);

			try {

				Document doc = Jsoup.parse(current, null);

				// write stuff to text file
				String filename = "textRepo/" + doc.title() + ".text";
				File outputFile = new File(filename);

				if (!outputFile.exists()) {
					outputFile.createNewFile();
				}

				bw = new BufferedWriter(new FileWriter(outputFile));

				// Get the elements we do not want to write to the text file
				// Element header = doc.getElementById("header");
				// if(header != null) {
				// //System.out.println(header.text());
				// excludeElements.add(header);
				// }
				//
				// Element footer = doc.getElementById("footer");
				// if(footer != null) {
				// //System.out.println(footer.text());
				// excludeElements.add(footer);
				// }

				// get <tr> and <ul> tags and put them in exclude elements
				boolean canWrite = true;
				// TODO: doesn't work ends up getting the p with id=footlinks
				// problem with nested DIVS
				Elements divs = doc.getElementsByTag("div");
				for (Element div : divs) {
					if (!div.id().equals("header") && !div.id().equals("footer")) {
						Elements pgraphs = div.getElementsByTag("p");
						for (Element pgraph : pgraphs) {
							Elements parents = pgraph.parents();
							for (Element parent : parents) {
								if (parent.id().equals("header") || parent.id().equals("footer")) {
									// if the <p> element's parent is not header
									// or footer then can write it to file
									canWrite = false;
									break;
								}
							}
							if (canWrite) {
								String pText = removeStopWords(pgraph.text());
								System.out.println(pText);
								bw.write(pText);
								bw.newLine();
							}
						}
					}
				}

				// for(Element div : divs) {
				// if(!div.id().equals("header") && !div.id().equals("footer"))
				// {
				// Elements divKids = div.children();
				// for(Element divKid : divKids) {
				// if(!divKid.id().equals("header") &&
				// !divKid.id().equals("footer"))
				// {
				//
				// Elements pgraphs = divKid.getElementsByTag("p");
				// for (Element pgraph : pgraphs) {
				// String pText = removeStopWords(pgraph.text());
				// System.out.println(pText);
				// bw.write(pText);
				// bw.newLine();
				//
				// }
				// }
				// }
				// }
				// }
				//
				//
				//

				bw.close();

			} catch (FileNotFoundException e) {
				System.out.println("HTML file not found.");
				return;
			} catch (IOException e) {
				System.out.println("Error reading HTML file.");
				return;
			}
		}

	}

	private static String removeStopWords(String text) {
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(stopWords));
			String line;
			while ((line = br.readLine()) != null) {
				// System.out.println(text);
				// line.replaceAll("\\n", "");
				// System.out.println(line);
				// text.replace(line, "");
			}
			br.close();
			return text;
		} catch (IOException e) {
			e.printStackTrace();
			return text;
		}
	}

}
