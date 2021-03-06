/*
 * Reducer.java
 *
 * Authors: Amanda Holl and Paige Rogalski
 *
 * Copyright 2016
 */


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
 * Class that reduces the HTML files obtained from the Crawler and
 * outputs the text to a text file for further analysis
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

	/*
 	 * This function hanldes HTML img alt tags
 	 */

	public static void handleImages(Element elt)
	{
		/* Write alt image text if present */

		try {
			if(elt.attr("alt") != ""){
				bw.write(sanitizeTree(elt.attr("alt")));
				bw.newLine();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/*
 	 * This fucntion identifies HTML tags that we do not want to include in our text document
 	 * and adds them to a list
 	 */
	public static void excludeElements(Document doc)
	{
		Elements headers = doc.select("div[id~=header], div[class~=header], header");
		 for(Element head : headers)
			 excludeElements.add(head);
		 
		 Elements footers = doc.select("div[id~=foot], div[class~=foot], footer");
		 for(Element foot : footers)
			 excludeElements.add(foot);
		
		Elements navs = doc.select("div[id~=nav], div[class~=nav], nav");	//gets any div with id containing nav
		for(Element nav : navs)
			excludeElements.add(nav);
		
		//handle ads 
		Elements ads = doc.select("[id~=^ad],[id~=ad$],[class~=^ad],[class~=ad$], [data-analytics~=^Paid], [id~=^paid], [class~=^paid], [id~=logo], [class~=logo]");
		for(Element ad : ads)
			excludeElements.add(ad);
		
		//handle banners
		Elements banners = doc.select("div.banner, div[id~=banner]");
		for(Element banner : banners)
			excludeElements.add(banner);

		//handle noscript -> will be redundant info
		Elements noscripts = doc.select("noscript");
		for(Element noscript : noscripts)
			excludeElements.add(noscript);
	}

	/*
 	 * This function reads in the HTML file from the repository
 	 * and begins to parse it to reduce noise
 	 */
	public static void readHTML() {
		String[] entries = htmlRepo.list();
		for (String s : entries) {
			File current = new File(htmlRepo.getPath(), s);

			try {

				Document doc = Jsoup.parse(current, null);

				// write to text file
				// Follow same naming convention as html page
				String filename = "textRepo/" + s.substring(0, s.length() - 5) + ".txt";
				File outputFile = new File(filename);

				if (!outputFile.exists()) {
					outputFile.createNewFile();
				}

				bw = new BufferedWriter(new FileWriter(outputFile));

				excludeElements(doc);

				Elements tree = TopicTree(doc.select("body"));

				String treeText = sanitizeTree(tree.text());

				bw.write(treeText);		// Write sanitized, reduced content to file
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

	/* 	Recursively build the "Topic Tree," aka the DOM tree that represents the main content */
	private static Elements TopicTree(Elements elt){
		boolean canWrite = true;
		if(elt != null && (elt.size() > 0)){
			for(Element child : elt){
				if(!excludeElements.contains(child)) {
					double comp = ((len(child)/comparison.length())*(1/.3)-1);

					/* 	Compute the weight of the element and remove if below threshold,
					 * 	unless it is an important tag with potentially little content.
					 */
					if((int) Math.signum(comp) < 1 && !tagImportant(child.tag())){
						child.remove();
					}
					/*	Case for images */
					else if(child.tagName().equals("img")){
						Elements parents = child.parents();
						for(Element parent : parents) {
							if(excludeElements.contains(parent)) {
								canWrite = false;
								break;
							}
						}
						/*	Write if the image is important (aka, not part of an ad, etc...) */
						if(canWrite) {
							handleImages(child);	// Write image alt text
							child.remove();	// Then remove
						}
					}
				} else if(excludeElements.contains(child)) {
					//don't want to write this to file so delete
					child.remove();
				}
			}
			/* 	Recursively call children */
			for(Element next : elt){
				TopicTree(next.children());
			}
		}
		return elt;
	}

	/*
 	 * This function identifies other important HTML tags we want to include in the text
 	 */
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
		text = text.replace("&nbsp;","");
		text = text.toLowerCase();
		return text;
	}

}
