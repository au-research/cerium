<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"  version="1.0">
    <xsl:output indent="yes"/>
    <xsl:strip-space elements="*"/>

    <!--

        XSL Stylesheet to group elements together by their name
        first elemet found by that name will attach all following siblings by the same name to that group
        written for RIFCS but works with any XML schemas
        https://github.com/FasterXML/jackson-dataformat-xml/issues/403
        -->

    <xsl:template match='/'>
        <xsl:apply-templates/>
    </xsl:template>



    <!--
   soon as you find an element copy all of its siblings that are named the same
    -->
    <xsl:template match="node()">
        <xsl:choose>
            <xsl:when test="not(preceding-sibling::node()[name() = name(current())])">
                <xsl:copy>
                    <xsl:apply-templates select="@*|node()"/>
                </xsl:copy>
                <xsl:apply-templates select="following-sibling::node()[name() = name(current())]" mode="copy"/>
            </xsl:when>
            <!--
            these nodes are copied by template mode=copy so nothing to do here
            -->
            <xsl:otherwise/>
        </xsl:choose>
    </xsl:template>

    <!--
        these are the elements that's getting groupped together
        -->
    <xsl:template match="node()" mode="copy">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="text()">
        <xsl:copy>
            <xsl:copy-of select="."/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="@*">
        <xsl:attribute name="{local-name()}">
            <xsl:value-of select="."/>
        </xsl:attribute>
    </xsl:template>

    <!-- xml:lang breaks jackson mapping in description -->
    <xsl:template match="@xml:lang"/>

    <xsl:template match="@xsi:schemaLocation">
        <xsl:attribute name="xsi:schemaLocation">
            <xsl:value-of select="."/>
        </xsl:attribute>
    </xsl:template>


</xsl:stylesheet>