<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:igsn="https://igsn.csiro.au/schemas/3.0" version="2.0" exclude-result-prefixes="igsn">
    <xsl:output indent="yes" method="xml"/>
    <xsl:param name="prefix" select="'999.999'"/>


    <xsl:variable name="oldVocab" select="'http://pid.geoscience.gov.au/def/voc/igsn-codelists'"/>
    <xsl:variable name="newVocab" select="'http://pid.geoscience.gov.au/def/voc/ga/igsncode'"/>

    <xsl:template match="/">
        <xsl:apply-templates select="igsn:resources"/>
    </xsl:template>

    <xsl:template match="igsn:resources">
        <resources xmlns="https://identifiers.ardc.edu.au/schemas/ardc-igsn-desc"
            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
            xsi:schemaLocation="https://identifiers.ardc.edu.au/schemas/ardc-igsn-desc https://identifiers.ardc.edu.au/igsn-schema/descriptive/1.0/resource.xsd">
            <xsl:apply-templates select="igsn:resource"/>
        </resources>
    </xsl:template>

    <xsl:template match="igsn:resource">
        <xsl:element name="resource" xmlns="https://identifiers.ardc.edu.au/schemas/ardc-igsn-desc">
            <xsl:apply-templates select="@* | node()"/>
        </xsl:element>

    </xsl:template>
    
    
    <xsl:template match="igsn:resourceIdentifier">
        <xsl:element name="resourceIdentifier" xmlns="https://identifiers.ardc.edu.au/schemas/ardc-igsn-desc">
            <xsl:value-of select="concat($prefix, '/', .)"/>
        </xsl:element>
        
    </xsl:template>

    <xsl:template match="igsn:relatedResource">
        <xsl:element name="relatedResource"
            xmlns="https://identifiers.ardc.edu.au/schemas/ardc-igsn-desc">
            <xsl:apply-templates select="@relationType"/>
            <xsl:element name="relatedResourceIdentifier">
                <xsl:apply-templates select="@relatedResourceIdentifierType"/>
                <xsl:value-of select="."/>
            </xsl:element>
        </xsl:element>
    </xsl:template>

    <xsl:template match="igsn:curator">
        <curator xmlns="https://identifiers.ardc.edu.au/schemas/ardc-igsn-desc">
            <curatorName>
                <xsl:value-of select="."/>
            </curatorName>
        </curator>
    </xsl:template>
    
    <xsl:template match="@geometryType | @identifierType | @registrationType | @relationType | @relatedResourceIdentifierType | @contributorIdentifierType | @registeredObjectType">
        <xsl:variable name="concept">
            <xsl:value-of select="substring-after(., $oldVocab)"/>
        </xsl:variable>
        <xsl:attribute name="{name()}">
               <xsl:value-of select="concat($newVocab, $concept)"/>
        </xsl:attribute>
    </xsl:template>
    
    <xsl:template match="@*">
        <xsl:attribute name="{name()}">
            <xsl:value-of select="."/>
        </xsl:attribute>
    </xsl:template>
    
    <xsl:template match="node()">
        <xsl:element name="{name()}" xmlns="https://identifiers.ardc.edu.au/schemas/ardc-igsn-desc" >
            <xsl:apply-templates select="@* | node()"/>
        </xsl:element>
    </xsl:template>
    
    <xsl:template match="text()" priority="99">
        <xsl:value-of select="."/>
    </xsl:template>
    
</xsl:stylesheet>
