package src.com.example.crawler;

import java.io.*;
import java.util.HashSet;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;


import org.jsoup.select.Elements;

/*
 * Keep meta content?
 * <p>&nbsp;</p>  don't put that in text 
 * 
 * 
 */
public class Reducer {
	private static File textRepo = new File("textRepo");
	private static File htmlRepo = new File("/Users/progalski/Documents/Crawler/Crawler/repository");
	private static File stopWords = new File("/Users/progalski/Documents/Crawler/Crawler/stopwords_en.txt");
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
			//File current = new File(htmlRepo.getPath(), "TRS.htm");
			//File current = new File(htmlRepo.getPath(), "CNN.htm");


			try {

				Document doc = Jsoup.parse(current, null);

				//create output text file
				String filename = "textRepo/" + doc.title() + ".text";
				File outputFile = new File(filename);

				if (!outputFile.exists()) {
					outputFile.createNewFile();
				}

				bw = new BufferedWriter(new FileWriter(outputFile));

				 //Get the elements we do not want to write to the text file
				 Elements headers = doc.select("div[id~=header], div[class~=header]");
				 for(Element head : headers) {
					 head.attr("written", false);
					 excludeElements.add(head);
				 }
				 
				 Elements footers = doc.select("div[id~=foot], div[class~=foot]");
				 for(Element foot : footers) {
					 foot.attr("written", false);
					 excludeElements.add(foot);
				 }
				
				Elements navs = doc.select("div[id~=nav], div[class~=nav], nav");	//gets any div with id containing nav
				for(Element nav : navs)
				{
					nav.attr("written", false);
					excludeElements.add(nav);
				}
				
				//handle ads 
				Elements ads = doc.select("div[id~=ad], div[class~=ad]");
				for(Element ad : ads)
				{
					excludeElements.add(ad);
				}
				
				
				boolean canWrite = true;
				
				//Hanlde divs with text 
//				Elements divs = doc.getElementsByTag("div");
//				for(Element div : divs) {
//					if(!excludeElements.contains(div))
//					{
//						Elements parents = div.parents();
//						for (Element parent : parents) {
//							if (excludeElements.contains(parent) || parent.hasAttr("written")) {
//								// if the <div> element's parent is header or footer do not write it to file
//								canWrite = false;
//								div.attr("written", false);
//								break;
//							}
//						}
//					}
//					if (canWrite) {
//						div.attr("written", true);
//						String divText = removeStopWords(div.text());
//						bw.write(divText);
//						bw.newLine();
//					}
//				}
						
				Elements pgraphs = doc.getElementsByTag("p");
				for (Element pgraph : pgraphs) {
					if(!excludeElements.contains(pgraph)) {
						Elements parents = pgraph.parents();
						for (Element parent : parents) {
							if (excludeElements.contains(parent) || parent.hasAttr("written")) {
								// if the <p> element's parent is header or footer do not write it to file
								canWrite = false;
								break;
							}
						}
						if (canWrite) {
							String pText = removeStopWords(pgraph.text());
							bw.write(pText);
							bw.newLine();
						}
					}
				}
				
				//Handle list items - we want to include these because list are usually important
				// example is in Wikipedia references, those are usually relevant to the page
				canWrite = true;
				Elements listElements = doc.getElementsByTag("li");
				for(Element listItem : listElements) {
					//need to check if parent has already been written 
					Elements listParents = listItem.parents();
					for(Element parent : listParents) {
						if(excludeElements.contains(parent) || parent.hasAttr("written")) {
							canWrite = false;
							break;
						}
					}
					if(canWrite) {
						bw.write(listItem.text());
						bw.newLine();
					}
				}
				
				//Handle img alt tags 
				canWrite = true;
				Elements imgElements = doc.getElementsByTag("img");
				for(Element img : imgElements) {
					Elements imgParents = imgElements.parents();
					for(Element parent : imgParents) {
						if(excludeElements.contains(parent) || parent.hasAttr("written")) {
							canWrite = false;
							break;
						}
					}
					if(canWrite) {
						bw.write(img.attr("alt"));
						bw.newLine();
					}
				}
					
							
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
			text = text.replace("&nbsp;","");
			String line;
			while ((line = br.readLine()) != null) {
				text = text.replaceAll("\\b"+line+"\\b", "");
			}
			br.close();
			return text;
		} catch (IOException e) {
			e.printStackTrace();
			return text;
		}
	}


}
