//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4-2 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2013.09.05 at 08:05:11 AM EDT 
//


package org.eclipse.ptp.rm.jaxb.core.data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for layout-data-type complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="layout-data-type">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;choice>
 *         &lt;element name="row-data" type="{http://eclipse.org/ptp/rm}row-data-type"/>
 *         &lt;element name="grid-data" type="{http://eclipse.org/ptp/rm}grid-data-type"/>
 *         &lt;element name="form-data" type="{http://eclipse.org/ptp/rm}form-data-type"/>
 *       &lt;/choice>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "layout-data-type", propOrder = {
    "rowData",
    "gridData",
    "formData"
})
public class LayoutDataType {

    @XmlElement(name = "row-data")
    protected RowDataType rowData;
    @XmlElement(name = "grid-data")
    protected GridDataType gridData;
    @XmlElement(name = "form-data")
    protected FormDataType formData;

    /**
     * Gets the value of the rowData property.
     * 
     * @return
     *     possible object is
     *     {@link RowDataType }
     *     
     */
    public RowDataType getRowData() {
        return rowData;
    }

    /**
     * Sets the value of the rowData property.
     * 
     * @param value
     *     allowed object is
     *     {@link RowDataType }
     *     
     */
    public void setRowData(RowDataType value) {
        this.rowData = value;
    }

    /**
     * Gets the value of the gridData property.
     * 
     * @return
     *     possible object is
     *     {@link GridDataType }
     *     
     */
    public GridDataType getGridData() {
        return gridData;
    }

    /**
     * Sets the value of the gridData property.
     * 
     * @param value
     *     allowed object is
     *     {@link GridDataType }
     *     
     */
    public void setGridData(GridDataType value) {
        this.gridData = value;
    }

    /**
     * Gets the value of the formData property.
     * 
     * @return
     *     possible object is
     *     {@link FormDataType }
     *     
     */
    public FormDataType getFormData() {
        return formData;
    }

    /**
     * Sets the value of the formData property.
     * 
     * @param value
     *     allowed object is
     *     {@link FormDataType }
     *     
     */
    public void setFormData(FormDataType value) {
        this.formData = value;
    }

}
