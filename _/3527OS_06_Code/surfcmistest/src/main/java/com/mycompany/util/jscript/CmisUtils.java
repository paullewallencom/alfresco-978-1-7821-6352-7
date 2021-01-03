package com.mycompany.util.jscript;

import org.apache.commons.io.IOUtils;
import org.springframework.extensions.webscripts.processor.BaseProcessorExtension;

import java.io.InputStream;
import java.io.StringWriter;

/**
 * Defines a new script root object that can be accessed in a
 * JavaScript Web Script controller via the 'cmisUtil' name, see also Spring context config.
 *
 * @author martin.bergljung@ixxus.com
 * @version 1.0
 */
public class CmisUtils extends BaseProcessorExtension {
    /**
     * Takes an input stream and extracts the text content from it. Then returns the text.
     *
     * @param inputStream the input stream to extract text content from
     * @return the extracted text content
     * @throws java.io.IOException if text could not be extracted from the input stream
     */
    public String inputStream2Text(InputStream inputStream) throws java.io.IOException{
        StringWriter writer = new StringWriter();
        IOUtils.copy(inputStream, writer, "UTF-8");
        return writer.toString();
    }
}
