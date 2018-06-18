package net.osmand.render;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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
		final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = factory.newDocumentBuilder();
		Document document = db.parse(new File(srcFile));
		transform(document);
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
//		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		DOMSource source = new DOMSource(document.getDocumentElement());
		StreamResult streamResult = new StreamResult(new File(targetFile));
		transformer.transform(source, streamResult);
	}
	

	static Map<String, Element> patterns = new HashMap<String, Element>(); 

	public static void transform(Document document) {
//		collectPatterns(document);
//		applyPatterns(document);
//		combineAllApplyTags(document);
//		
//		replaceTag(document, "ifelse", "filter");
//		replaceTag(document, "check", "filter");
//		replaceTag(document, "check_and_apply", "filter");
	}


	public static void combineAllApplyTags(Document document) {
		NodeList nl = document.getElementsByTagName("apply");
		while(nl.getLength() > 0) {
			Element app = (Element) nl.item(0);
			Element parent = (Element) app.getParentNode();
			NamedNodeMap attrs = app.getAttributes();
			for(int i = 0; i < attrs.getLength(); i++) {
				Node ns = attrs.item(i);
				parent.setAttribute(ns.getNodeName(), ns.getNodeValue());
			}
			while(app.getChildNodes().getLength() > 0) {
				Node ni = app.getChildNodes().item(0);
				app.getParentNode().insertBefore(ni, app);
			}
			app.getParentNode().removeChild(app);
		}
	}


	public static void applyPatterns(Document document) {
		NodeList nl = document.getElementsByTagName("apply");
		for (int i = 0; i < nl.getLength();) {
			Element app = (Element) nl.item(i);
			String pt = app.getAttribute("pattern");
			if (!pt.equals("")) {
				if (!patterns.containsKey(pt)) {
					throw new IllegalStateException("Pattern '" + pt + "' is not defined");
				}
				Element patt = patterns.get(pt);
				final NodeList pattChildren = patt.getChildNodes();
				for(int ki = 0; ki < pattChildren.getLength(); ki++) {
					Node ni = patt.getChildNodes().item(ki);
					app.getParentNode().insertBefore(ni.cloneNode(true), app);
				}
				app.getParentNode().removeChild(app);
			} else {
				i++;
			}
		}
	}


	public static void collectPatterns(Document document) {
		NodeList nl = document.getElementsByTagName("pattern");
		while(nl.getLength() > 0) {
			Element pt = (Element) nl.item(0);
			String id = pt.getAttribute("id");
			patterns.put(id, pt);
			pt.getParentNode().removeChild(pt);
		}
		
	}


	public static void replaceTag(Document document, final String tag, final String targetTag) {
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
