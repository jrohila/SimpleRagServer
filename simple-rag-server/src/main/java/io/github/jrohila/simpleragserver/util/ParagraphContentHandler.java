/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.github.jrohila.simpleragserver.util;

import java.util.ArrayList;
import java.util.List;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 * @author Lenovo
 */
public class ParagraphContentHandler extends DefaultHandler {

    private static final String XHTML_NS = "http://www.w3.org/1999/xhtml";

    private final List<String> paragraphs = new ArrayList<>();
    private boolean inP = false;
    private StringBuilder buf = new StringBuilder();

    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
        if (XHTML_NS.equals(uri) && "p".equals(localName)) {
            inP = true;
            buf.setLength(0);
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (inP) {
            buf.append(ch, start, length);
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (XHTML_NS.equals(uri) && "p".equals(localName)) {
            String text = buf.toString().replaceAll("\\s+", " ").trim();
            if (!text.isEmpty()) {
                paragraphs.add(text);
            }
            inP = false;
        }
    }

    public List<String> getParagraphs() {
        return paragraphs;
    }

}
