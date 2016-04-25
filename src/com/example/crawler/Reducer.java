package com.example.crawler;

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
	private static File htmlRepo = new File("repository");
	private static File stopWords = new File("stopwords_en.txt");
	private static HashSet<Element> excludeElements = new HashSet<>();
	private static String comparison = "Kenya defends forcing 45 Taiwanese onto a plane to China";

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

		//readHTML();
		readHTML2();

	}

	public static void readHTML() {
		BufferedWriter bw;

		String[] entries = htmlRepo.list();
		System.out.println(entries.length);
		//for (String s : entries) {
			//File current = new File(htmlRepo.getPath(), s);
			//File current = new File(htmlRepo.getPath(), "TRS.htm");
			File current = new File(htmlRepo.getPath(), "CharlesKochPossibleClintoncouldbebetterthanGOPnomineeCNNPoliticscom1.html");


			try {

				Document doc = Jsoup.parse(current, null);

				// write stuff to text file
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
				//Elements ads = doc.select("div[id~=ad], div[class~=ad], [data-analytics~=Paid], [id~=paid]");
				Elements ads = doc.select("[id~=ad], [data-analytics~=Paid], [id~=paid], [class~=paid]");
				for(Element ad : ads)
				{
					ad.attr("written", false);
					excludeElements.add(ad);
				}
				
				System.out.println(excludeElements);
				boolean canWrite = true;
				
				//Hanlde divs with text 
				//Elements divs = doc.getElementsByTag("div");
				Elements divs = doc.select("span, h3, h2");
				for(Element div : divs) {
					//if(excludeElements.contains(div))
						//System.out.println(excludeElements.contains(div));
					if(!excludeElements.contains(div))
					{
						bw.write(div.text());

						Elements parents = div.parents();
						for (Element parent : parents) {
							if (excludeElements.contains(parent) || parent.hasAttr("written")) {
								// if the <div> element's parent is header or footer do not write it to file
								canWrite = false;
								div.attr("written", false);
								break;
							}
						}
					}
					if (canWrite) {
						System.out.println("HERE");
						div.attr("written", true);
						String divText = removeStopWords(div.text());
						//bw.write(divText);
						bw.write(div.text());
						bw.newLine();
					}

				}


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
		//}


	}

	public static void readHTML2() {
		BufferedWriter bw;

		String[] entries = htmlRepo.list();
		System.out.println(entries.length);
		//for (String s : entries) {
		//File current = new File(htmlRepo.getPath(), s);
		//File current = new File(htmlRepo.getPath(), "TRS.htm");
		File current = new File(htmlRepo.getPath(), "CharlesKochPossibleClintoncouldbebetterthanGOPnomineeCNNPoliticscom1.html");


		try {

			Document doc = Jsoup.parse(current, null);

			// write stuff to text file
			String filename = "textRepo/" + doc.title() + ".text";
			File outputFile = new File(filename);

			if (!outputFile.exists()) {
				outputFile.createNewFile();
			}

			bw = new BufferedWriter(new FileWriter(outputFile));

			Elements tree = TopicTree(doc.select("body"));
			bw.write(tree.text());


			bw.close();

		} catch (FileNotFoundException e) {
			System.out.println("HTML file not found.");
			return;
		} catch (IOException e) {
			System.out.println("Error reading HTML file.");
			return;
		}
		//}


	}

	private static Elements TopicTree(Elements elt){
		if(elt != null && (elt.size() > 0)){
			for(Element child : elt){
				double comp = ((len(child)/comparison.length())*(1/.3)-1);
				if((int) Math.signum(comp) < 1){
					child.remove();
				}
			}
			for(Element next : elt){
				TopicTree(next.children());
			}
		}
		return elt;
	}

	private static int len(Element elt){
		String txt = elt.text();
		return txt.length();
	}

	private static String removeStopWords(String text) {
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(stopWords));
			String line;
			while ((line = br.readLine()) != null) {
				text.replaceAll(line, "");
			}
			br.close();
			return text;
		} catch (IOException e) {
			e.printStackTrace();
			return text;
		}
	}


}
