package com.example.crawler;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;


import org.jsoup.select.Elements;

/*
 * Keep meta content?
 * 
 * 
 */
public class Reducer {
	private static File textRepo = new File("textRepo");
	private static File htmlRepo = new File("repository");
	private static File stopWords = new File("stopwords_en.txt");
	private static List<Element> excludeElements = new ArrayList<>();
	private static String comparison = "This is a comparison string for comparing to remove noise hopefully";
	private static BufferedWriter bw;

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


	public static void handleLists(Document doc, BufferedWriter bw)
	{
		//Handle list items - we want to include these because list are usually important
		// example is in Wikipedia references, those are usually relevant to the page
		boolean canWrite = true;
		Elements listElements = doc.getElementsByTag("li");
		for(Element listItem : listElements) {
			//need to check if parent has already been written 
			Elements listParents = listItem.parents();
			for(Element parent : listParents) {
				if(excludeElements.contains(parent)) {
					canWrite = false;
					break;
				}
			}
			if(canWrite) {
				try {
					bw.write(listItem.text());
					bw.newLine();
				} catch (IOException e) {
					e.printStackTrace();
					System.out.println(e.getMessage());
					return;
				}
			}
		}
	}

	public static void handleImages(Element elt)
	{
		//Handle img alt tags

		try {
			if(elt.attr("alt") != ""){
				bw.write(sanitizeTree(elt.attr("alt")));
				bw.newLine();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}


	}

	public static void excludeElements(Document doc)
	{
		Elements headers = doc.select("div[id~=header], div[class~=header], header");
		 for(Element head : headers) {
			 excludeElements.add(head);
		 }
		 
		 Elements footers = doc.select("div[id~=foot], div[class~=foot], footer");
		 for(Element foot : footers) {
			 excludeElements.add(foot);
		 }
		
		Elements navs = doc.select("div[id~=nav], div[class~=nav], nav");	//gets any div with id containing nav
		for(Element nav : navs)
		{
			excludeElements.add(nav);
		}
		
		//handle ads 
		Elements ads = doc.select("[id~=^ad],[id~=ad$],[class~=^ad],[class~=ad$], [data-analytics~=^Paid], [id~=^paid], [class~=^paid]");
		for(Element ad : ads)
		{
			excludeElements.add(ad);
		}
		
		//handle banners
		Elements banners = doc.select("div.banner, div[id~=banner]");
		for(Element banner : banners) 
		{
			excludeElements.add(banner);
		}

		//handle noscript -> will be redundant info
		Elements noscripts = doc.select("noscript");
		for(Element noscript : noscripts)
		{
			excludeElements.add(noscript);
		}

	}
	public static void readHTML() {


		String[] entries = htmlRepo.list();
		for (String s : entries) {
		File current = new File(htmlRepo.getPath(), s);

		try {

			Document doc = Jsoup.parse(current, null);

			// write stuff to text file
			String filename = "textRepo/" + s.substring(0, s.length() - 5) + ".txt"; // Follow same naming convention as html page
			File outputFile = new File(filename);

			if (!outputFile.exists()) {
				outputFile.createNewFile();
			}

			bw = new BufferedWriter(new FileWriter(outputFile));
			
			excludeElements(doc);

			Elements tree = TopicTree(doc.select("body"));

			String treeText = sanitizeTree(tree.text());

			bw.write(treeText);

			//handleImages(doc.select("body"));
			//handleLists(doc, bw);

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

	private static Elements TopicTree(Elements elt){
		boolean canWrite = true;
		if(elt != null && (elt.size() > 0)){
			for(Element child : elt){
				if(!excludeElements.contains(child)) {
					double comp = ((len(child)/comparison.length())*(1/.3)-1);

					if((int) Math.signum(comp) < 1 && !tagImportant(child.tag())){
						child.remove();
					}
					else if(child.tagName().equals("img")){
						Elements parents = child.parents();
						for(Element parent : parents) {
							if(excludeElements.contains(parent)) {
								canWrite = false;
								break;
							}
						}

						if(canWrite)
							handleImages(child);
					}
				} else if(excludeElements.contains(child)) {
					//don't want to write this to file so delete

					child.remove();
				}
			}
			for(Element next : elt){
				TopicTree(next.children());
			}
		}
		return elt;
	}

	private static boolean tagImportant(Tag t){
		String tag = t.getName();
		// Check for special tags, whose text content is likely less than comparison, but still have value content
		// Only h1-h3 chosen because these are the most important
		if(tag.equals("a") || tag.equals("h1") || tag.equals("h2") || tag.equals("h3") || tag.equals("img") || tag.equals("figcaption")) {
			return true;    //tag is important
		}
		return false;	//tag not important
	}

	private static int len(Element elt){
		String txt = elt.text();
		return txt.length();
	}

	private static String sanitizeTree(String text) {
		/*BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(stopWords));
			text = text.replace("&nbsp;","");
			text = text.toLowerCase();	// convert all to lower case so case doesn't mess up match with stopwords (since all of those are lowercase)
			String line;
			while ((line = br.readLine()) != null) {
				text = text.replaceAll("\\b"+line+"\\b", "");
			}
			br.close();
			return text;
		} catch (IOException e) {
			e.printStackTrace();
			return text;
		}*/
		text = text.replace("&nbsp;","");
		text = text.toLowerCase();
		return text;
	}


}
