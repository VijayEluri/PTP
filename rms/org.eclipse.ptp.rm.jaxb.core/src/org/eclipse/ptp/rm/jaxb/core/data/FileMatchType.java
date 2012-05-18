//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 in JDK 6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2012.05.16 at 09:58:59 AM EDT 
//


package org.eclipse.ptp.rm.jaxb.core.data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for file-match-type complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="file-match-type">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="efsAttributes" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="lastModifiedBefore" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="lastModifiedAfter" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="length" type="{http://www.w3.org/2001/XMLSchema}long" />
 *       &lt;attribute name="isDirectory" type="{http://www.w3.org/2001/XMLSchema}boolean" default="false" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "file-match-type")
public class FileMatchType {

    @XmlAttribute
    protected String efsAttributes;
    @XmlAttribute
    protected String lastModifiedBefore;
    @XmlAttribute
    protected String lastModifiedAfter;
    @XmlAttribute
    protected Long length;
    @XmlAttribute
    protected Boolean isDirectory;

    /**
     * Gets the value of the efsAttributes property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getEfsAttributes() {
        return efsAttributes;
    }

    /**
     * Sets the value of the efsAttributes property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setEfsAttributes(String value) {
        this.efsAttributes = value;
    }

    /**
     * Gets the value of the lastModifiedBefore property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getLastModifiedBefore() {
        return lastModifiedBefore;
    }

    /**
     * Sets the value of the lastModifiedBefore property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setLastModifiedBefore(String value) {
        this.lastModifiedBefore = value;
    }

    /**
     * Gets the value of the lastModifiedAfter property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getLastModifiedAfter() {
        return lastModifiedAfter;
    }

    /**
     * Sets the value of the lastModifiedAfter property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setLastModifiedAfter(String value) {
        this.lastModifiedAfter = value;
    }

    /**
     * Gets the value of the length property.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public Long getLength() {
        return length;
    }

    /**
     * Sets the value of the length property.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setLength(Long value) {
        this.length = value;
    }

    /**
     * Gets the value of the isDirectory property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public boolean isIsDirectory() {
        if (isDirectory == null) {
            return false;
        } else {
            return isDirectory;
        }
    }

    /**
     * Sets the value of the isDirectory property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setIsDirectory(Boolean value) {
        this.isDirectory = value;
    }

}
