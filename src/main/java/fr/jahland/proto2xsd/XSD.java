package fr.jahland.proto2xsd;

/**
 * XSD constants.
 * Created by mvincent on 12/02/2016.
 */
public abstract class XSD {

    public static String SCHEMA = "schema";

    public static String SCHEMA_LOCATION = "schemaLocation";

    public static String NAMESPACE = "namespace";

    public static String XMLNS_URI = "http://www.w3.org/2000/xmlns/";


    /** Default XSD prefix. */
    public final static String NS_PREFIX = "xs:";
    /** Default custom prefix. */
    public final static String TNS_PREFIX = "tns:";
    /** Default XMLNS prefix. */
    public final static String XMLNS_PREFIX = "xmlns:";
}
