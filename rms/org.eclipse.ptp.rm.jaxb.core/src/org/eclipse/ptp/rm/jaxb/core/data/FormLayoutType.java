//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 in JDK 6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2011.09.07 at 09:57:43 PM EDT 
//


package org.eclipse.ptp.rm.jaxb.core.data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for form-layout-type complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="form-layout-type">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="marginHeight" type="{http://www.w3.org/2001/XMLSchema}int" />
 *       &lt;attribute name="marginWidth" type="{http://www.w3.org/2001/XMLSchema}int" />
 *       &lt;attribute name="marginTop" type="{http://www.w3.org/2001/XMLSchema}int" />
 *       &lt;attribute name="marginBottom" type="{http://www.w3.org/2001/XMLSchema}int" />
 *       &lt;attribute name="marginLeft" type="{http://www.w3.org/2001/XMLSchema}int" />
 *       &lt;attribute name="marginRight" type="{http://www.w3.org/2001/XMLSchema}int" />
 *       &lt;attribute name="spacing" type="{http://www.w3.org/2001/XMLSchema}int" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "form-layout-type")
public class FormLayoutType {

    @XmlAttribute
    protected Integer marginHeight;
    @XmlAttribute
    protected Integer marginWidth;
    @XmlAttribute
    protected Integer marginTop;
    @XmlAttribute
    protected Integer marginBottom;
    @XmlAttribute
    protected Integer marginLeft;
    @XmlAttribute
    protected Integer marginRight;
    @XmlAttribute
    protected Integer spacing;

    /**
     * Gets the value of the marginHeight property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getMarginHeight() {
        return marginHeight;
    }

    /**
     * Sets the value of the marginHeight property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setMarginHeight(Integer value) {
        this.marginHeight = value;
    }

    /**
     * Gets the value of the marginWidth property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getMarginWidth() {
        return marginWidth;
    }

    /**
     * Sets the value of the marginWidth property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setMarginWidth(Integer value) {
        this.marginWidth = value;
    }

    /**
     * Gets the value of the marginTop property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getMarginTop() {
        return marginTop;
    }

    /**
     * Sets the value of the marginTop property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setMarginTop(Integer value) {
        this.marginTop = value;
    }

    /**
     * Gets the value of the marginBottom property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getMarginBottom() {
        return marginBottom;
    }

    /**
     * Sets the value of the marginBottom property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setMarginBottom(Integer value) {
        this.marginBottom = value;
    }

    /**
     * Gets the value of the marginLeft property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getMarginLeft() {
        return marginLeft;
    }

    /**
     * Sets the value of the marginLeft property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setMarginLeft(Integer value) {
        this.marginLeft = value;
    }

    /**
     * Gets the value of the marginRight property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getMarginRight() {
        return marginRight;
    }

    /**
     * Sets the value of the marginRight property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setMarginRight(Integer value) {
        this.marginRight = value;
    }

    /**
     * Gets the value of the spacing property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getSpacing() {
        return spacing;
    }

    /**
     * Sets the value of the spacing property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setSpacing(Integer value) {
        this.spacing = value;
    }

}