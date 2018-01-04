package com.edi.translation.util;

import java.io.File;
import java.io.IOException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.xml.sax.SAXException;

public class XMLValidationUtil {
	public static void main(String[] args) {
		XMLValidationUtil val = new XMLValidationUtil();
		boolean valid = false;
		try {
			valid = val.validate("C:/Parimanshu/FlatFileTranslation/Validation/XSD/BenefitEnrollmentRequestResponse.xsd", "C:/Parimanshu/FlatFileTranslation/Output/FFM.I834BU.D20140709.T101136735.T.xml");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.println("Returned value: " + valid);
	}
	
	public boolean validate(String xsdFileWithPath, String xmlFileWithPath) throws IOException, SAXException {
		try {
            SchemaFactory factory = 
                    SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = factory.newSchema(new File(xsdFileWithPath));
            Validator validator = schema.newValidator();
            validator.validate(new StreamSource(new File(xmlFileWithPath)));
        } catch (IOException | SAXException e) {
            System.out.println("Exception: "+e.getMessage());

            throw e;
        }
        return true;
	}
}
