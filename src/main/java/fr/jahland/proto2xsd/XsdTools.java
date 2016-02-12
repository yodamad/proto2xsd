package fr.jahland.proto2xsd;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * XSD tools.
 * Created by mvincent on 11/02/2016.
 */
public class XsdTools {

    /**
     * Transform a package to a namespace.
     * @param pack Package to transform
     * @return namespace computed
     */
    public static String namespace(String pack) {
        String[] words = pack.split("\\.");
        List<String> listOfWords = Arrays.asList(words);
        Collections.reverse(listOfWords);
        return "http://" + StringTools.replaceProtoByXsd(String.join(".", listOfWords));
    }

    /**
     * Generate a namespace's prefix according to imported filename.
     * If filename contains _, take first letter of each word, otherwise take 3 first letters.
     * @param input Filenam
     * @return namespace generated
     */
    public static String generatePrefixNS(String input) {
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

    /**
     * Mapper between protobuf and xsd types.
     * @param protoType Input protobuf type
     * @return xsd equivalent type
     */
    public static String typeMapper(String protoType, Map<String, String> imports) {
        switch (protoType) {
            case "string":
                return XSD.NS_PREFIX + "string";
            case "bool":
                return XSD.NS_PREFIX + "boolean";
            case "int32":
            case "int64":
                return XSD.NS_PREFIX + "integer";
            case "bytes":
                return XSD.NS_PREFIX + "base64Binary";
            case "double":
                return XSD.NS_PREFIX + "decimal";
            default:
                if (imports.get(protoType) == null) {
                    // Unknown type, must be a internal type
                    return XSD.TNS_PREFIX + protoType;
                } else {
                    return imports.get(protoType) + protoType;
                }
        }
    }

}
