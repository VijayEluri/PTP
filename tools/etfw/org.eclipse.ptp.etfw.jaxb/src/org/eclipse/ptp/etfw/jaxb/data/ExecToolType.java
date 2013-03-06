//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4-2 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2012.10.24 at 03:00:32 PM CDT 
//


package org.eclipse.ptp.etfw.jaxb.data;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ExecToolType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ExecToolType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="global" type="{http://org.eclipse.ptp/etfw}ToolAppType" minOccurs="0"/>
 *         &lt;element name="execUtils" type="{http://org.eclipse.ptp/etfw}ToolAppType" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="tool-id" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="tool-name" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="tool-type" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="require-true" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="prepend-execution" type="{http://www.w3.org/2001/XMLSchema}boolean" default="false" />
 *       &lt;attribute name="replace-execution" type="{http://www.w3.org/2001/XMLSchema}boolean" default="false" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ExecToolType", namespace = "http://org.eclipse.ptp/etfw", propOrder = {
    "global",
    "execUtils"
})
public class ExecToolType {

    protected ToolAppType global;
    protected List<ToolAppType> execUtils;
    @XmlAttribute(name = "tool-id")
    protected String toolId;
    @XmlAttribute(name = "tool-name")
    protected String toolName;
    @XmlAttribute(name = "tool-type")
    protected String toolType;
    @XmlAttribute(name = "require-true")
    protected String requireTrue;
    @XmlAttribute(name = "prepend-execution")
    protected Boolean prependExecution;
    @XmlAttribute(name = "replace-execution")
    protected Boolean replaceExecution;

    /**
     * Gets the value of the global property.
     * 
     * @return
     *     possible object is
     *     {@link ToolAppType }
     *     
     */
    public ToolAppType getGlobal() {
        return global;
    }

    /**
     * Sets the value of the global property.
     * 
     * @param value
     *     allowed object is
     *     {@link ToolAppType }
     *     
     */
    public void setGlobal(ToolAppType value) {
        this.global = value;
    }

    /**
     * Gets the value of the execUtils property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the execUtils property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getExecUtils().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link ToolAppType }
     * 
     * 
     */
    public List<ToolAppType> getExecUtils() {
        if (execUtils == null) {
            execUtils = new ArrayList<ToolAppType>();
        }
        return this.execUtils;
    }

    /**
     * Gets the value of the toolId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getToolId() {
        return toolId;
    }

    /**
     * Sets the value of the toolId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setToolId(String value) {
        this.toolId = value;
    }

    /**
     * Gets the value of the toolName property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getToolName() {
        return toolName;
    }

    /**
     * Sets the value of the toolName property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setToolName(String value) {
        this.toolName = value;
    }

    /**
     * Gets the value of the toolType property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getToolType() {
        return toolType;
    }

    /**
     * Sets the value of the toolType property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setToolType(String value) {
        this.toolType = value;
    }

    /**
     * Gets the value of the requireTrue property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getRequireTrue() {
        return requireTrue;
    }

    /**
     * Sets the value of the requireTrue property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setRequireTrue(String value) {
        this.requireTrue = value;
    }

    /**
     * Gets the value of the prependExecution property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public boolean isPrependExecution() {
        if (prependExecution == null) {
            return false;
        } else {
            return prependExecution;
        }
    }

    /**
     * Sets the value of the prependExecution property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setPrependExecution(Boolean value) {
        this.prependExecution = value;
    }

    /**
     * Gets the value of the replaceExecution property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public boolean isReplaceExecution() {
        if (replaceExecution == null) {
            return false;
        } else {
            return replaceExecution;
        }
    }

    /**
     * Sets the value of the replaceExecution property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setReplaceExecution(Boolean value) {
        this.replaceExecution = value;
    }

}
