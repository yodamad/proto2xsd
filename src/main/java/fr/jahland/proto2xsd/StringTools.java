package fr.jahland.proto2xsd;

/**
 * String tools.
 * Created by mvincent on 11/02/2016.
 */
public class StringTools {

    /**
     * Cleaning an input string, removing special character.
     *
     * @param s Line to clean
     * @return line cleaned
     */
    public static String cleaning(String s) {
        return s.trim().replace(";", "");
    }

    /**
     * Cleaning an input string, removing special character.
     *
     * @param s Line to clean
     * @return line cleaned
     */
    public static String cleaningImport(String s, boolean transformToXsd) {
        String clean = s.replace("\"", "");
        return transformToXsd ? replaceProtoByXsd(clean.trim()) : clean;
    }

    /**
     * Replace proto by xsd in given string.
     * @param s String to transform
     * @return String transformed
     */
    public static String replaceProtoByXsd(String s) {
        return s.replace("proto", "xsd");
    }
}
