package fr.jahland.proto2xsd;


import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.XMLConstants;

/**
 * Class with methods to make it more convenient to create Element nodes with
 * namespace prefixed tagnames and with "name" and "type" attributes.
 */
public class NameTypeElementMaker {
    private String nsPrefix;
    private Document doc;

    public NameTypeElementMaker(String nsPrefix, Document doc) {
        this.nsPrefix = nsPrefix;
        this.doc = doc;
    }

    public Element createElement(String elementName, String nameAttrVal, String typeAttrVal) {
        Element element = doc.createElementNS(XMLConstants.W3C_XML_SCHEMA_NS_URI, nsPrefix + elementName);
        if (nameAttrVal != null)
            element.setAttribute("name", nameAttrVal);
        if (typeAttrVal != null)
            element.setAttribute("type", typeAttrVal);
        return element;
    }

    public Element createElement(String elementName, String nameAttrVal) {
        return createElement(elementName, nameAttrVal, null);
    }

    public Element createElement(String elementName) {
        return createElement(elementName, null);
    }

    public Element createElement(String elementName, String protoType, String min, String max) {
        Element currentType = createElement("element", elementName, protoType);
        currentType.setAttribute("minOccurs", min);
        currentType.setAttribute("maxOccurs", max);
        return  currentType;
    }
}