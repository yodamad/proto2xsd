package fr.jahland.proto2xsd;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by mvincent on 08/02/2016.
 */
public class Generator {

    private final static String NS_PREFIX = "xs:";
    private final static String TNS_PREFIX = "tns:";

    private static Element rootElement = null;
    private static String rootNS = "";
    private static Element currentElement = null;

    private static Map<String, String> imports = new HashMap<>();
    private static Map<String, String> namespaces = new HashMap<>();

    private static String generatedNS;
    private static String generatedPrefixNS;

    /**
     * Cleaning an input string, removing special character.
     *
     * @param s Line to clean
     * @return line cleaned
     */
    private static String cleaning(String s) {
        return s.trim().replace(";", "");
    }

    /**
     * Cleaning an input string, removing special character.
     *
     * @param s Line to clean
     * @return line cleaned
     */
    private static String cleaningImport(String s, boolean transformToXsd) {
        String clean = s.replace("\"", "");
        return transformToXsd ? clean.trim().replace("proto", "xsd") : clean;
    }

    /**
     * Filtering line not interesting like option or import
     *
     * @param line Line to check
     * @return if line is valid
     */
    private static boolean filtering(String line) {
        return !line.isEmpty() && !line.trim().startsWith("option ");
    }

    private static String namespace(String pack) {
        String[] words = pack.split("\\.");
        List<String> listOfWords = Arrays.asList(words);
        Collections.reverse(listOfWords);
        return "http://" + String.join(".", listOfWords);
    }

    public static void generateFile(Path file) {
        try {
            // Initialize document
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();
            Element schemaRoot = doc.createElementNS(XMLConstants.W3C_XML_SCHEMA_NS_URI, NS_PREFIX + "schema");
            doc.appendChild(schemaRoot);
            NameTypeElementMaker elMaker = new NameTypeElementMaker(NS_PREFIX, doc);

            // Process each line
            try (Stream<String> stream = Files.lines(file)) {

                stream
                        .filter(Generator::filtering)
                        .map(Generator::cleaning)
                        .map((Function<String, String>) line -> {
                            // Splitting elements for treatment
                            String[] elements = line.split(" ");
                            switch (elements[0]) {
                                case "package":
                                    // Package contains namespace information
                                    String ns = namespace(elements[1]);
                                    schemaRoot.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:tns", ns);
                                    rootNS = ns;
                                    break;
                                case "import":
                                    // Filtering option import (specific protobuf scope)
                                    if (!elements[1].contains("option")) {
                                        // Manage recursively imports
                                        manageImport(cleaningImport(elements[1], false));

                                        // Generate import node
                                        Element importXsd = elMaker.createElement("import");
                                        importXsd.setAttribute("schemaLocation", cleaningImport(elements[1], true));
                                        // If import uses a different namespace
                                        if (!generatedNS.equals(rootNS)) {
                                            importXsd.setAttribute("namespace", generatedNS);
                                            schemaRoot.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:" + generatedPrefixNS, generatedNS);
                                        }
                                        schemaRoot.appendChild(importXsd);

                                        // Reset
                                        generatedPrefixNS = "";
                                        generatedNS = "";
                                    }
                                    break;
                                case "message":
                                    // ComplexType
                                    currentElement = elMaker.createElement("complexType", elements[1]);
                                    rootElement = currentElement;
                                    schemaRoot.appendChild(rootElement);
                                    break;
                                case "extend":
                                    Element complexContent = elMaker.createElement("complexContent");
                                    currentElement.appendChild(complexContent);
                                    currentElement = complexContent;
                                    Element extension = elMaker.createElement("extension");
                                    extension.setAttribute("base", TNS_PREFIX + elements[1]);
                                    currentElement.appendChild(extension);
                                    currentElement = extension;
                                    break;
                                case "required":
                                    String node = currentElement.getNodeName();
                                    // Check if current element is <extension>
                                    if (node.contains("extension")) {
                                        // Skip this one, not used in XSD
                                        break;
                                    }
                                    // Check if current element is <sequence>
                                    if (!node.contains("sequence")) {
                                        // Not in sequence yet, create node
                                        Element sequence = elMaker.createElement("sequence");
                                        currentElement.appendChild(sequence);
                                        currentElement = sequence;
                                    }
                                    Element currentType = elMaker.createElement("element", elements[2], typeMapper(elements[1]));
                                    currentType.setAttribute("maxOccurs", "1");
                                    currentType.setAttribute("minOccurs", "1");
                                    currentElement.appendChild(currentType);
                                    break;
                                case "optional":
                                    // Check if current element is <sequence>
                                    node = currentElement.getNodeName();
                                    if (!node.contains("sequence")) {
                                        // Not in sequence yet, create node
                                        Element sequence = elMaker.createElement("sequence");
                                        currentElement.appendChild(sequence);
                                        currentElement = sequence;
                                    }
                                    currentType = elMaker.createElement("element", elements[2], typeMapper(elements[1]));
                                    currentType.setAttribute("maxOccurs", "1");
                                    currentType.setAttribute("minOccurs", "0");
                                    currentElement.appendChild(currentType);
                                    break;
                                case "repeated":
                                    // Check if current element is <sequence>
                                    node = currentElement.getNodeName();
                                    if (!node.contains("sequence")) {
                                        // Not in sequence yet, create node
                                        Element sequence = elMaker.createElement("sequence");
                                        currentElement.appendChild(sequence);
                                        currentElement = sequence;
                                    }
                                    currentType = elMaker.createElement("element", elements[2], typeMapper(elements[1]));
                                    currentType.setAttribute("maxOccurs", "unbounded");
                                    currentType.setAttribute("minOccurs", "0");
                                    currentElement.appendChild(currentType);
                                    break;
                                case "}":
                                    break;
                                default:
                                    System.err.println("Unknown reserved word : " + elements[0]);
                            }
                            return line;
                        })
                        .collect(Collectors.toList());
            } catch (IOException e) {
                e.printStackTrace();
            }

            TransformerFactory tFactory = TransformerFactory.newInstance();
            Transformer transformer = tFactory.newTransformer();
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            DOMSource domSource = new DOMSource(doc);
            //to create a file use something like this:
            transformer.transform(domSource, new StreamResult(new File("mySchema.xsd")));
            //to print to console use this:
            transformer.transform(domSource, new StreamResult(System.out));
        } catch (ParserConfigurationException | TransformerException e) {
            e.printStackTrace();
        }
    }

    private static void manageImport(String s) {
        try {
            Path directory = Paths.get("./proto2xsd").toRealPath();
            System.out.println("Reading import " + Paths.get(directory.toString(), s));

            // Process each line
            try (Stream<String> stream = Files.lines(Paths.get(directory.toString(), s))) {

                stream.filter(Generator::filtering)
                        .map(Generator::cleaning)
                        .map((Function<String, String>) line -> {
                            // Splitting elements for treatment
                            String[] elements = line.split(" ");
                            switch (elements[0]) {
                                case "package":
                                    String clean = cleaningImport(elements[1], false);

                                    String ns = namespace(clean);
                                    if (ns.equals(rootNS)) {
                                        generatedNS = rootNS;
                                        generatedPrefixNS = TNS_PREFIX;
                                    } else {
                                        generatedPrefixNS = generatePrefixNS(s);
                                        namespaces.put(generatedPrefixNS, ns);
                                    }
                                    break;
                                case "import":
                                    // TODO : recursive ?
                                    break;
                                case "message":
                                    imports.put(elements[1], generatedPrefixNS);
                                default:
                                    break;
                            }
                            return "";
                        })
                        .collect(Collectors.counting());
            }
        } catch (IOException e) {
            System.err.println("Impossible to parse import file : " + s);
        }
    }

    private static String generatePrefixNS(String input) {
        String clean = input.replace("siti.", "").replace(".proto", "");
        String ns = "";

        if (clean.contains("_")) {
            String[] elements = clean.split("_");
            for (String element : elements) {
                ns += element.substring(0, 1);
            }
        } else {
            ns = clean.substring(0, 3);
        }
        return ns + ":";
    }

    private static String typeMapper(String protoType) {
        switch (protoType) {
            case "string":
                return NS_PREFIX + "string";
            case "bool":
                return NS_PREFIX + "booleans";
            case "int32":
            case "int64":
                return NS_PREFIX + "integer";
            case "bytes":
                return NS_PREFIX + "base64Binary";
            case "double":
                return NS_PREFIX + "decimal";
            default:
                if (imports.get(protoType) == null) {
                    // Unknown type, must be a internal type
                    return TNS_PREFIX + protoType;
                } else {
                    return imports.get(protoType) + protoType;
                }
        }
    }

    /**
     * Class with methods to make it more convenient to create Element nodes with
     * namespace prefixed tagnames and with "name" and "type" attributes.
     */
    private static class NameTypeElementMaker {
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
    }
}
