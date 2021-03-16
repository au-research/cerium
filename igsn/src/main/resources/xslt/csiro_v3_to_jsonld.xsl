<?xml version="1.0" encoding="UTF-8"?>
    <xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                    xmlns:igsn="https://igsn.csiro.au/schemas/3.0"
                    xmlns:xslUtil="au.edu.ardc.registry.common.util.XSLUtil"
                    version="2.0"
                    exclude-result-prefixes="igsn">
        <xsl:output indent="yes" method="text"/>

        <!--
        XSLT transformation to create json-ld from an CSIRO v3 record
        -->

        <xsl:template match="/">
            <xsl:apply-templates select="igsn:resources/igsn:resource" mode="json-ld"/>
        </xsl:template>


        <xsl:template match="igsn:resource" mode="json-ld">
<xsl:text>{"@context": ["http://schema.org/",
                "https://raw.githubusercontent.com/IGSN/igsn-json/master/schema.igsn.org/json/registration/v0.1/context.jsonld"],
</xsl:text>
            <xsl:apply-templates select="igsn:resourceIdentifier" mode="igsn-id"/>
            <xsl:apply-templates select="igsn:resourceIdentifier" mode="igsn"/>
            <xsl:text>
    "registrant": {"@type":"registrant", "name": "ARDC",
     "identifiers": [
      {
        "id": "https://ror.org/038sjwq14",
        "kind": "ror"
      }
    ]},
    "igsnvoc:log": [
</xsl:text>
            <xsl:apply-templates select="igsn:logDate" mode="json-ld"/>
            <xsl:text>
    ],
    "description" :{
    "@context": [{"objectType": "http://pid.geoscience.gov.au/def/voc/igsn-codelists/"},
                 {"materialType": "http://vocabulary.odm2.org/medium/"},
                 {"sampleType": "http://vocabulary.odm2.org/specimentype/"},
                 {"abstract": "http://purl.org/dc/terms/abstract"},
                 {"classifications": "https://igsn.csiro.au/schemas/3.0/classifications"}],
    "@type":"igsn",
    "objectType"  : "Sample",
</xsl:text>
            <xsl:apply-templates select="igsn:landingPage" mode="URL"/>
            <xsl:apply-templates select="igsn:resourceIdentifier" mode="igsn-id"/>
            <xsl:apply-templates select="igsn:landingPage" mode="json-ld"/>
            <xsl:apply-templates select="igsn:resourceTitle" mode="json-ld"/>
            <xsl:apply-templates select="igsn:curationDetails" mode="json-ld"/>
            <xsl:apply-templates select="igsn:contributors" mode="json-ld"/>
            <xsl:apply-templates select="igsn:classifications" mode="json-ld"/>
            <xsl:apply-templates select="igsn:resourceTypes" mode="json-ld"/>
            <xsl:apply-templates select="igsn:materialTypes" mode="json-ld"/>
            <xsl:apply-templates select="igsn:location" mode="json-ld"/>
            <xsl:text>
    }}
</xsl:text>
        </xsl:template>

        <xsl:template match="igsn:resourceIdentifier" mode="igsn-id">
            <xsl:text>    "@id": "http://hdl.handle.net/</xsl:text><xsl:value-of select="xslUtil:escapeJsonString(.)"/><xsl:text>",
</xsl:text>
        </xsl:template>

        <xsl:template match="igsn:resourceIdentifier" mode="igsn">
            <xsl:text>    "@igsn" : "</xsl:text><xsl:value-of select="xslUtil:escapeJsonString(.)"/><xsl:text>",
</xsl:text>
        </xsl:template>


        <xsl:template match="igsn:landingPage" mode="URL">
            <xsl:text>    "URL" : "</xsl:text><xsl:value-of select="xslUtil:escapeJsonString(.)"/><xsl:text>",
</xsl:text>
        </xsl:template>


        <xsl:template match="igsn:landingPage" mode="json-ld">
            <xsl:text>    "landingPage" : "</xsl:text><xsl:value-of select="xslUtil:escapeJsonString(.)"/><xsl:text>",
</xsl:text>
        </xsl:template>

        <xsl:template match="igsn:resourceTitle" mode="json-ld">
            <xsl:text>    "title" : "</xsl:text><xsl:value-of select="xslUtil:escapeJsonString(.)"/><xsl:text>",
</xsl:text>
        </xsl:template>

        <xsl:template match="igsn:resourceTypes" mode="json-ld">
            <xsl:text>    "sampleType" : [</xsl:text>
            <xsl:apply-templates select="igsn:resourceType" mode="json-ld"/>
            <xsl:text>],
</xsl:text>
        </xsl:template>

        <xsl:template match="igsn:materialTypes" mode="json-ld">
            <xsl:text>    "materialType" : [</xsl:text>
            <xsl:apply-templates select="igsn:materialType" mode="json-ld"/>
            <xsl:text>],
</xsl:text>
        </xsl:template>

        <xsl:template match="igsn:classifications" mode="json-ld">
            <xsl:text>    "classifications" : [</xsl:text>
            <xsl:apply-templates select="igsn:classification" mode="json-ld"/>
            <xsl:text>],
</xsl:text>
        </xsl:template>

        <xsl:template match="igsn:classification" mode="json-ld">
            <xsl:text>"</xsl:text><xsl:value-of select="xslUtil:escapeJsonString(.)"/><xsl:text>"</xsl:text>
            <xsl:if test="following-sibling::igsn:classification">
                <xsl:text>,</xsl:text>
            </xsl:if>
        </xsl:template>

        <xsl:template match="igsn:logDate" mode="json-ld">
<xsl:text>          {
        "igsnvoc:@type": "</xsl:text><xsl:value-of select="@eventType"/><xsl:text>",
        "igsnvoc:timestamp": "</xsl:text><xsl:value-of select="xslUtil:escapeJsonString(.)"/><xsl:text>"
        }</xsl:text>
            <xsl:if test="following-sibling::igsn:logDate">
                <xsl:text>,</xsl:text>
            </xsl:if>
        </xsl:template>

        <xsl:template match="igsn:location" mode="json-ld">
<xsl:text>"location":{
    "@type": "Place",
</xsl:text>
            <xsl:apply-templates select="igsn:locality" mode="json-ld"/>
            <xsl:apply-templates select="igsn:geometry" mode="json-ld"/>
            <xsl:text>}</xsl:text>
        </xsl:template>

        <xsl:template match="igsn:geometry" mode="json-ld">
<xsl:text>
    "geo" : {
    "@type": "GeoShape",
    "polygon": "</xsl:text><xsl:value-of select="normalize-space(.)"/><xsl:text>"
    }</xsl:text>
            <xsl:if test="following-sibling::igsn:locality">
                <xsl:text>,</xsl:text>
            </xsl:if>
            <xsl:if test="following-sibling::igsn:geometry">
                <xsl:text>,</xsl:text>
            </xsl:if>
        </xsl:template>

        <xsl:template match="igsn:resourceType | igsn:materialType" mode="json-ld">
            <xsl:text>"</xsl:text><xsl:value-of select="xslUtil:escapeJsonString(.)"/><xsl:text>"</xsl:text>
            <xsl:if test="following-sibling::igsn:resourceType">
                <xsl:text>,</xsl:text>
            </xsl:if>
            <xsl:if test="following-sibling::igsn:materialType">
                <xsl:text>,</xsl:text>
            </xsl:if>
        </xsl:template>

        <xsl:template match="igsn:locality" mode="json-ld">
            <xsl:text>"address": "</xsl:text><xsl:value-of select="xslUtil:escapeJsonString(.)"/><xsl:text>"</xsl:text>
            <xsl:if test="following-sibling::igsn:locality">
                <xsl:text>,</xsl:text>
            </xsl:if>
            <xsl:if test="following-sibling::igsn:geometry">
                <xsl:text>,</xsl:text>
            </xsl:if>
        </xsl:template>

        <xsl:template match="igsn:curationDetails" mode="json-ld">
            <xsl:variable name="nodeList" select="//igsn:curatorName/text()"/>
            <xsl:text>    "curator" : </xsl:text><xsl:value-of select="xslUtil:createJsonArray($nodeList)"/><xsl:text>,
</xsl:text>
        </xsl:template>

        <xsl:template match="igsn:contributors" mode="json-ld">
            <xsl:variable name="nodeList" select="//igsn:contributorName/text()"/>
            <xsl:text>    "contributor" : </xsl:text><xsl:value-of select="xslUtil:createJsonArray($nodeList)"/><xsl:text>,
</xsl:text>
        </xsl:template>

</xsl:stylesheet>