//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 in JDK 6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2011.08.29 at 09:38:31 AM EDT 
//


package org.eclipse.ptp.rm.jaxb.core.data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;


/**
 * <p>Java class for arg-type complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="arg-type">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="isUndefinedIfMatches" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="resolve" type="{http://www.w3.org/2001/XMLSchema}boolean" default="true" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "arg-type", propOrder = {
    "content"
})
public class ArgType {

    @XmlValue
    protected String content;
    @XmlAttribute
    protected String isUndefinedIfMatches;
    @XmlAttribute
    protected Boolean resolve;

    /**
     * Gets the value of the content property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getContent() {
        return content;
    }

    /**
     * Sets the value of the content property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setContent(String value) {
        this.content = value;
    }

    /**
     * Gets the value of the isUndefinedIfMatches property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getIsUndefinedIfMatches() {
        return isUndefinedIfMatches;
    }

    /**
     * Sets the value of the isUndefinedIfMatches property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setIsUndefinedIfMatches(String value) {
        this.isUndefinedIfMatches = value;
    }

    /**
     * Gets the value of the resolve property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public boolean isResolve() {
        if (resolve == null) {
            return true;
        } else {
            return resolve;
        }
    }

    /**
     * Sets the value of the resolve property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setResolve(Boolean value) {
        this.resolve = value;
    }

}
