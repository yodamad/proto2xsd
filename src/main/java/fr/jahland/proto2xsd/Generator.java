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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Generator file that compute a XSD schema from a protobuf defintion file.
 * Created by mvincent on 08/02/2016.
 */
public class Generator {

    /**
     * Root XSD element.
     */
    private static Element rootElement = null;
    /**
     * Root namespace.
     */
    private static String rootNS = "";
    /**
     * Current element in XSD definition.
     */
    private static Element currentElement = null;
    /**
     * Map of types imported with type as key and associated namespace prefix as value.
     */
    private static Map<String, String> imports = new HashMap<>();
    /**
     * Map of namespaces to manage with prefix as key and uri as value.
     */
    private static Map<String, String> namespaces = new HashMap<>();
    /**
     * Current generated namespace.
     */
    private static String generatedNS;
    /**
     * Current generated namespace prefix.
     */
    private static String generatedPrefixNS;

    /**
     * Filtering line not interesting like option or import
     *
     * @param line Line to check
     * @return if line is valid
     */
    private static boolean filtering(String line) {
        return !line.isEmpty() && !line.trim().startsWith("option ");
    }

    /**
     * Generate XSD file.
     *
     * @param file Protobuf input file
     */
    public static void generateFile(Path file, List<Options> options) throws IOException {
        try {
            // Initialize document
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();
            Element schemaRoot = doc.createElementNS(XMLConstants.W3C_XML_SCHEMA_NS_URI, XSD.NS_PREFIX + XSD.SCHEMA);
            doc.appendChild(schemaRoot);
            NameTypeElementMaker elMaker = new NameTypeElementMaker(XSD.NS_PREFIX, doc);

            // Process each line
            try (Stream<String> stream = Files.lines(file)) {

                stream
                        .filter(Generator::filtering)
                        .map(StringTools::cleaning)
                        .map(line -> {
                            // Splitting elements for treatment
                            String[] elements = line.split(" ");
                            switch (elements[0]) {
                                case "package":
                                    // Package contains namespace information
                                    String ns = XsdTools.namespace(elements[1]);
                                    schemaRoot.setAttributeNS(XSD.XMLNS_URI, XSD.XMLNS_PREFIX + "tns", ns);
                                    rootNS = ns;
                                    break;
                                case "import":
                                    // Filtering option import (specific protobuf scope)
                                    if (!elements[1].contains("option")) {
                                        // Manage recursively imports
                                        manageImport(StringTools.cleaningImport(elements[1], false), options);

                                        // Generate import node
                                        Element importXsd = elMaker.createElement("import");
                                        importXsd.setAttribute(XSD.SCHEMA_LOCATION, StringTools.cleaningImport(elements[1], true));
                                        // If import uses a different namespace
                                        if (!generatedNS.equals(rootNS)) {
                                            importXsd.setAttribute(XSD.NAMESPACE, generatedNS);
                                            schemaRoot.setAttributeNS(XSD.XMLNS_URI, XSD.XMLNS_PREFIX + generatedPrefixNS, generatedNS);
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
                                    extension.setAttribute("base", XSD.TNS_PREFIX + elements[1]);
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
                                    checkAndInitSequenceNode(node, elMaker);
                                    Element currentType = elMaker.createElement(elements[2], XsdTools.typeMapper(elements[1], imports), "1", "1");
                                    currentElement.appendChild(currentType);
                                    break;
                                case "optional":
                                    // Check if current element is <sequence>
                                    checkAndInitSequenceNode(currentElement.getNodeName(), elMaker);
                                    currentType = elMaker.createElement(elements[2], XsdTools.typeMapper(elements[1], imports), "0", "1");
                                    currentElement.appendChild(currentType);
                                    break;
                                case "repeated":
                                    // Check if current element is <sequence>
                                    checkAndInitSequenceNode(currentElement.getNodeName(), elMaker);
                                    currentType = elMaker.createElement(elements[2], XsdTools.typeMapper(elements[1], imports), "0", "unbounded");
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
                System.exit(1);
            }

            TransformerFactory tFactory = TransformerFactory.newInstance();
            Transformer transformer = tFactory.newTransformer();
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            DOMSource domSource = new DOMSource(doc);
            if (options != null && options.contains(Options.DRY_RUN)) {
                // Print to console
                transformer.transform(domSource, new StreamResult(System.out));
            } else {
                // Create a file
                String filename = file.getFileName().toString();
                transformer.transform(domSource, new StreamResult(new File(StringTools.replaceProtoByXsd(filename))));
            }
        } catch (ParserConfigurationException | TransformerException e) {
            System.exit(1);
        }
    }

    /**
     * Check if node sequence is initialized, otherwise init it.
     *
     * @param node    Node to check
     * @param elMaker Xsd element maker
     */
    private static void checkAndInitSequenceNode(String node, NameTypeElementMaker elMaker) {
        if (!node.contains("sequence")) {
            // Not in sequence yet, create node
            Element sequence = elMaker.createElement("sequence");
            currentElement.appendChild(sequence);
            currentElement = sequence;
        }
    }

    /**
     * Compute type and namespaces from an imported file.
     *
     * @param s       Imported filename
     * @param options Input options
     */
    private static void manageImport(String s, List<Options> options) {
        try {
            Path directory = Paths.get("./proto2xsd").toRealPath();
            System.out.println("Reading import " + Paths.get(directory.toString(), s));

            // Process each line
            try (Stream<String> stream = Files.lines(Paths.get(directory.toString(), s))) {

                stream.filter(Generator::filtering)
                        .map(StringTools::cleaning)
                        .map(line -> {
                            // Splitting elements for treatment
                            String[] elements = line.split(" ");
                            switch (elements[0]) {
                                case "package":
                                    String clean = StringTools.cleaningImport(elements[1], false);

                                    String ns = XsdTools.namespace(clean);
                                    if (ns.equals(rootNS)) {
                                        generatedNS = rootNS;
                                        generatedPrefixNS = XSD.TNS_PREFIX;
                                    } else {
                                        generatedPrefixNS = XsdTools.generatePrefixNS(s);
                                        namespaces.put(generatedPrefixNS, ns);
                                    }
                                    break;
                                case "import":
                                    if (options.contains(Options.RECURSIVE)) {
                                        // Manage recursively imports
                                        manageImport(StringTools.cleaningImport(elements[1], false), options);
                                    }
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

            // Generate files for import if necessary
            if (options.contains(Options.GENERATE_IMPORT)) {
                generateFile(Paths.get(directory.toString(), s), options);
            }
        } catch (IOException e) {
            System.err.println("Impossible to parse import file : " + s);
            if (!options.contains(Options.IGNORE_IMPORT)) {
                System.exit(1);
            }
        }
    }
}
