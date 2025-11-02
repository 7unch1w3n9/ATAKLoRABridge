package com.atakmap.android.LoRaBridge.GenericMessage;

import com.siemens.ct.exi.core.EXIFactory;
import com.siemens.ct.exi.core.helpers.DefaultEXIFactory;
import com.siemens.ct.exi.main.api.sax.EXIResult;
import com.siemens.ct.exi.main.api.sax.EXISource;
import com.atakmap.coremap.xml.XMLUtils;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import java.io.*;

/** EXI <-> XML tool */
public final class ExiUtils {
    private ExiUtils(){}

    /** XML -> EXI bytes */
    public static byte[] toExi(String xml) throws Exception {
        EXIFactory f = DefaultEXIFactory.newInstance();
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        EXIResult exi = new EXIResult(f);
        exi.setOutputStream(os);

        SAXParserFactory spf = SAXParserFactory.newInstance();
        try {
            spf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            spf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            spf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            spf.setFeature("http://xml.org/sax/features/validation", false);
            spf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        } catch (Throwable ignored) {}

        SAXParser p = spf.newSAXParser();
        XMLReader xr = p.getXMLReader();
        xr.setContentHandler(exi.getHandler());
        xr.parse(new InputSource(new StringReader(xml)));
        return os.toByteArray();
    }

    /** EXI bytes -> XML string */
    public static String fromExi(byte[] exi) throws Exception {
        EXIFactory f = DefaultEXIFactory.newInstance();
        InputSource is = new InputSource(new ByteArrayInputStream(exi));
        EXISource src = new EXISource(f);
        src.setInputSource(is);

        TransformerFactory tf = XMLUtils.getTransformerFactory();
        Transformer t = tf.newTransformer();
        StringWriter w = new StringWriter();
        Result result = new StreamResult(w);
        t.transform(src, result);
        return w.toString();
    }
}

