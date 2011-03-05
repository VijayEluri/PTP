//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.0.5-b02-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2011.03.05 at 01:15:27 PM CST 
//


package org.eclipse.ptp.rm.jaxb.core.data;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{}regex" minOccurs="0"/>
 *         &lt;element ref="{}target" minOccurs="0"/>
 *         &lt;element ref="{}set" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element ref="{}add" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element ref="{}put" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element ref="{}append" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="errorOnMiss" type="{http://www.w3.org/2001/XMLSchema}boolean" default="false" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "regex",
    "target",
    "set",
    "add",
    "put",
    "append"
})
@XmlRootElement(name = "match")
public class Match {

    protected Regex regex;
    protected Target target;
    protected List<Set> set;
    protected List<Add> add;
    protected List<Put> put;
    protected List<Append> append;
    @XmlAttribute
    protected Boolean errorOnMiss;

    /**
     * Gets the value of the regex property.
     * 
     * @return
     *     possible object is
     *     {@link Regex }
     *     
     */
    public Regex getRegex() {
        return regex;
    }

    /**
     * Sets the value of the regex property.
     * 
     * @param value
     *     allowed object is
     *     {@link Regex }
     *     
     */
    public void setRegex(Regex value) {
        this.regex = value;
    }

    /**
     * Gets the value of the target property.
     * 
     * @return
     *     possible object is
     *     {@link Target }
     *     
     */
    public Target getTarget() {
        return target;
    }

    /**
     * Sets the value of the target property.
     * 
     * @param value
     *     allowed object is
     *     {@link Target }
     *     
     */
    public void setTarget(Target value) {
        this.target = value;
    }

    /**
     * Gets the value of the set property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the set property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getSet().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Set }
     * 
     * 
     */
    public List<Set> getSet() {
        if (set == null) {
            set = new ArrayList<Set>();
        }
        return this.set;
    }

    /**
     * Gets the value of the add property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the add property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getAdd().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Add }
     * 
     * 
     */
    public List<Add> getAdd() {
        if (add == null) {
            add = new ArrayList<Add>();
        }
        return this.add;
    }

    /**
     * Gets the value of the put property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the put property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getPut().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Put }
     * 
     * 
     */
    public List<Put> getPut() {
        if (put == null) {
            put = new ArrayList<Put>();
        }
        return this.put;
    }

    /**
     * Gets the value of the append property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the append property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getAppend().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Append }
     * 
     * 
     */
    public List<Append> getAppend() {
        if (append == null) {
            append = new ArrayList<Append>();
        }
        return this.append;
    }

    /**
     * Gets the value of the errorOnMiss property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public boolean isErrorOnMiss() {
        if (errorOnMiss == null) {
            return false;
        } else {
            return errorOnMiss;
        }
    }

    /**
     * Sets the value of the errorOnMiss property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setErrorOnMiss(Boolean value) {
        this.errorOnMiss = value;
    }

}
