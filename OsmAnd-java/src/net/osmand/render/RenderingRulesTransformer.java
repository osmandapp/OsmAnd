package net.osmand.render;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class RenderingRulesTransformer {

	public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException, TransformerException {
		if(args.length == 0) {
			System.out.println("Please specify source and target file path.");
			return;
		}
		String srcFile = args[0];
		String targetFile = args[1];
		DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document document = db.parse(new File(srcFile));
		transform(document);
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		DOMSource source = new DOMSource(document.getDocumentElement());
		StreamResult streamResult = new StreamResult(new File(targetFile));
		transformer.transform(source, streamResult);
	}


	public static void transform(Document document) {
		replaceTag(document, "ifelse", "filter");
		replaceTag(document, "check", "filter");
		replaceTag(document, "apply", "filter");
		replaceTag(document, "check_and_apply", "filter");
	}


	protected static void replaceTag(Document document, final String tag, final String targetTag) {
		NodeList nl = document.getElementsByTagName(tag);
		while(nl.getLength() > 0) {
			Element newElement = document.createElement(targetTag);
			Element old = (Element) nl.item(0);
			copyAndReplaceElement(old, newElement);
		}
	}


	protected static void copyAndReplaceElement(Element oldElement, Element newElement) {
		while(oldElement.getChildNodes().getLength() > 0) {
			newElement.appendChild(oldElement.getChildNodes().item(0));
		}
		NamedNodeMap attrs = oldElement.getAttributes();
		for(int i = 0; i < attrs.getLength(); i++) {
			Node ns = attrs.item(i);
			newElement.setAttribute(ns.getNodeName(), ns.getNodeValue());
		}
		((Element)oldElement.getParentNode()).replaceChild(newElement, oldElement);
	}
}
