package com.example.crawler;

import java.io.*;

import java.net.MalformedURLException;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

/*
 * Want to keep text between <a> and </a> and 
 * Keep meta content?
 * Anything inside <p>
 */
public class Reducer {

	public static void main(String[] args){
		String html = "MIT - Massachusetts Institute of Technology.html";
		FileReader input = new FileReader(html);
        File inputFile = new File(html);
        
        if(inputFile.exists())
        {
        	inputFile.delete();
        }
        
        readHTML();
        
	}
	
	public static void readHTML(){
        String html = "MIT - Massachusetts Institute of Technology.html";

        BufferedReader reader = null;

        String buffer = "";
        String separator = ",";

        try{
            FileReader input = new FileReader(html);
            File inputFile = new File(html);
            Document doc = Jsoup.parse(inputFile, null);

            Element content = doc.getElementById("content");
            Elements links = content.getElementsByTag("a");
            for (Element link : links) {
              String linkHref = link.attr("href");
              String linkText = link.text();
              
              //write stuff to text file
              File outputFile = new File("MIT - Massachusetts Institute of Technology.txt");
              
              if (!outputFile.exists()) {
         	     outputFile.createNewFile();
         	  }
             

         	  FileWriter fw = new FileWriter(outputFile);
         	  bw = new BufferedWriter(fw);
         	  bw.write(linkText);
         	  bw.newLine();
            
        }catch (FileNotFoundException e) {
            System.out.println("HTML file not found.");
            return null;
        }
        catch (IOException e) {
            System.out.println("Error reading HTML file.");
            return null;
        }

        
    }
	
}
